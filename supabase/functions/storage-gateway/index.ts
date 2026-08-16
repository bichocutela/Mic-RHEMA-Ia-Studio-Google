import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";

const FIREBASE_PROJECT_ID = "mic-rhema";
const FIREBASE_ISSUER = `https://securetoken.google.com/${FIREBASE_PROJECT_ID}`;
const FIREBASE_JWKS_URL = "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com";
const SIGNED_URL_TTL_SECONDS = 15 * 60;

const BUCKET_RULES = {
  "profile-photos": {
    maxBytes: 5 * 1024 * 1024,
    mimeTypes: new Set(["image/jpeg", "image/png", "image/webp"]),
  },
  "church-documents": {
    maxBytes: 50 * 1024 * 1024,
    mimeTypes: new Set(["application/pdf"]),
  },
} as const;

type FirebaseClaims = {
  sub?: string;
  user_id?: string;
  aud?: string;
  iss?: string;
  exp?: number;
  iat?: number;
};

type Jwk = JsonWebKey & { kid?: string; alg?: string; use?: string };

class HttpError extends Error {
  constructor(public status: number, message: string) {
    super(message);
  }
}

function jsonResponse(body: Record<string, unknown>, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "content-type": "application/json; charset=utf-8",
      "cache-control": "no-store",
      "access-control-allow-origin": "*",
      "access-control-allow-headers": "authorization, apikey, content-type",
      "access-control-allow-methods": "POST, OPTIONS",
    },
  });
}

function decodeBase64Url(value: string): Uint8Array {
  const normalized = value.replace(/-/g, "+").replace(/_/g, "/").padEnd(Math.ceil(value.length / 4) * 4, "=");
  const binary = atob(normalized);
  return Uint8Array.from(binary, (character) => character.charCodeAt(0));
}

function decodeJsonPart<T>(value: string): T {
  return JSON.parse(new TextDecoder().decode(decodeBase64Url(value))) as T;
}

function getBearerToken(request: Request): string {
  const value = request.headers.get("authorization") ?? "";
  const match = value.match(/^Bearer\s+(.+)$/i);
  if (!match) throw new HttpError(401, "Token Firebase ausente.");
  return match[1];
}

async function verifyFirebaseToken(token: string): Promise<FirebaseClaims> {
  const parts = token.split(".");
  if (parts.length !== 3) throw new HttpError(401, "Token Firebase inválido.");

  const header = decodeJsonPart<{ alg?: string; kid?: string }>(parts[0]);
  const claims = decodeJsonPart<FirebaseClaims>(parts[1]);
  if (header.alg !== "RS256" || !header.kid) throw new HttpError(401, "Algoritmo do token Firebase não suportado.");

  const now = Math.floor(Date.now() / 1000);
  if (
    claims.aud !== FIREBASE_PROJECT_ID ||
    claims.iss !== FIREBASE_ISSUER ||
    !claims.sub ||
    claims.sub.length > 128 ||
    typeof claims.exp !== "number" ||
    claims.exp <= now ||
    typeof claims.iat !== "number" ||
    claims.iat > now + 60
  ) {
    throw new HttpError(401, "Token Firebase expirado ou emitido para outro projeto.");
  }

  const keysResponse = await fetch(FIREBASE_JWKS_URL);
  if (!keysResponse.ok) throw new HttpError(503, "Não foi possível consultar as chaves públicas do Firebase.");
  const keySet = await keysResponse.json() as { keys?: Jwk[] };
  const jwk = keySet.keys?.find((candidate) => candidate.kid === header.kid);
  if (!jwk) throw new HttpError(401, "Chave pública do token Firebase não encontrada.");

  const publicKey = await crypto.subtle.importKey(
    "jwk",
    jwk,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["verify"],
  );
  const valid = await crypto.subtle.verify(
    { name: "RSASSA-PKCS1-v1_5" },
    publicKey,
    decodeBase64Url(parts[2]),
    new TextEncoder().encode(`${parts[0]}.${parts[1]}`),
  );
  if (!valid) throw new HttpError(401, "Assinatura do token Firebase inválida.");

  return claims;
}

function callerUid(claims: FirebaseClaims): string {
  return claims.user_id || claims.sub || "";
}

async function firestoreMemberDocument(uid: string, firebaseToken: string): Promise<Record<string, any> | null> {
  const endpoint = `https://firestore.googleapis.com/v1/projects/${FIREBASE_PROJECT_ID}/databases/(default)/documents/acessos_pendentes/${encodeURIComponent(uid)}`;
  const response = await fetch(endpoint, {
    headers: { authorization: `Bearer ${firebaseToken}` },
  });
  if (response.status === 404) return null;
  if (!response.ok) {
    console.warn("Firestore authorization lookup failed", response.status);
    return null;
  }
  return await response.json() as Record<string, any>;
}

async function isAdmin(uid: string, firebaseToken: string): Promise<boolean> {
  const document = await firestoreMemberDocument(uid, firebaseToken);
  return document?.fields?.isAdmin?.booleanValue === true;
}

function storageObjectPath(bucket: string, storagePath: string, targetUid: string): string {
  const prefix = `${bucket}/`;
  const objectPath = storagePath.startsWith(prefix) ? storagePath.slice(prefix.length) : storagePath;
  const expectedPrefix = `${targetUid}/`;
  if (!objectPath.startsWith(expectedPrefix) || objectPath.includes("..") || objectPath.startsWith("/")) {
    throw new HttpError(400, "Caminho de armazenamento inválido.");
  }
  return objectPath;
}

function requireBucket(bucket: string): keyof typeof BUCKET_RULES {
  if (!(bucket in BUCKET_RULES)) throw new HttpError(400, "Bucket não permitido.");
  return bucket as keyof typeof BUCKET_RULES;
}

async function authorize(
  claims: FirebaseClaims,
  firebaseToken: string,
  targetUid: string,
  bucket: keyof typeof BUCKET_RULES,
  allowOwnerDocumentRead = false,
): Promise<boolean> {
  const uid = callerUid(claims);
  if (!uid || !targetUid) throw new HttpError(400, "UID do usuário não informado.");
  const owner = uid === targetUid;
  const admin = await isAdmin(uid, firebaseToken);
  if (bucket === "church-documents" && !admin && !allowOwnerDocumentRead) {
    throw new HttpError(403, "Somente administradores podem gerenciar documentos IBR.");
  }
  if (!owner && !admin) throw new HttpError(403, "Usuário sem permissão para este perfil.");
  return admin;
}

function extensionForMime(mimeType: string): string {
  if (mimeType === "image/jpeg") return "jpg";
  if (mimeType === "image/png") return "png";
  if (mimeType === "image/webp") return "webp";
  if (mimeType === "application/pdf") return "pdf";
  throw new HttpError(415, "Tipo de arquivo não permitido.");
}

async function handleUpload(
  request: Request,
  claims: FirebaseClaims,
  firebaseToken: string,
): Promise<Response> {
  const form = await request.formData();
  const file = form.get("file");
  const bucket = requireBucket(String(form.get("bucket") || ""));
  const targetUid = String(form.get("targetUid") || callerUid(claims));
  await authorize(claims, firebaseToken, targetUid, bucket);

  if (!(file instanceof File)) throw new HttpError(400, "Arquivo não informado.");
  const rule = BUCKET_RULES[bucket];
  const mimeType = (file.type || "").toLowerCase();
  if (!rule.mimeTypes.has(mimeType)) throw new HttpError(415, "Tipo de arquivo não permitido para este bucket.");
  if (file.size <= 0 || file.size > rule.maxBytes) throw new HttpError(413, "Arquivo excede o limite permitido.");

  const extension = extensionForMime(mimeType);
  const objectPath = bucket === "profile-photos"
    ? `${targetUid}/profile.${extension}`
    : `${targetUid}/certificate-${crypto.randomUUID()}.${extension}`;
  const storagePath = `${bucket}/${objectPath}`;
  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!supabaseUrl || !serviceRoleKey) throw new HttpError(500, "Storage do servidor não configurado.");

  const supabase = createClient(supabaseUrl, serviceRoleKey, {
    auth: { autoRefreshToken: false, persistSession: false },
  });
  const result = await supabase.storage.from(bucket).upload(objectPath, file, {
    contentType: mimeType,
    cacheControl: "900",
    upsert: bucket === "profile-photos",
  });
  if (result.error) {
    console.error("Supabase upload failed", result.error.message);
    throw new HttpError(502, "Não foi possível gravar o arquivo no Supabase.");
  }

  if (bucket === "profile-photos") {
    const listed = await supabase.storage.from(bucket).list(targetUid, { limit: 100 });
    if (!listed.error) {
      const stalePaths = (listed.data ?? [])
        .map((entry) => `${targetUid}/${entry.name}`)
        .filter((path) => path !== objectPath);
      if (stalePaths.length > 0) await supabase.storage.from(bucket).remove(stalePaths);
    }
  }

  const signed = await supabase.storage.from(bucket).createSignedUrl(objectPath, SIGNED_URL_TTL_SECONDS);
  return jsonResponse({
    ok: true,
    bucket,
    storage_path: storagePath,
    signed_url: signed.data?.signedUrl || "",
    expires_in: SIGNED_URL_TTL_SECONDS,
  });
}

async function handleJsonOperation(
  request: Request,
  claims: FirebaseClaims,
  firebaseToken: string,
): Promise<Response> {
  const payload = await request.json() as Record<string, unknown>;
  const operation = String(payload.operation || "signed-url");
  const bucket = requireBucket(String(payload.bucket || ""));
  const targetUid = String(payload.targetUid || callerUid(claims));
  const storagePath = String(payload.storagePath || payload.storage_path || "");
  await authorize(claims, firebaseToken, targetUid, bucket, operation === "signed-url");

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!supabaseUrl || !serviceRoleKey) throw new HttpError(500, "Storage do servidor não configurado.");
  const supabase = createClient(supabaseUrl, serviceRoleKey, {
    auth: { autoRefreshToken: false, persistSession: false },
  });
  const storage = supabase.storage.from(bucket);

  if (operation === "delete-profile") {
    if (bucket !== "profile-photos") throw new HttpError(400, "Operação de perfil inválida para este bucket.");
    const listed = await storage.list(targetUid, { limit: 100 });
    if (listed.error) throw new HttpError(502, "Não foi possível listar os arquivos de perfil.");
    const paths = (listed.data ?? []).map((entry) => `${targetUid}/${entry.name}`);
    if (paths.length > 0) {
      const result = await storage.remove(paths);
      if (result.error) throw new HttpError(502, "Não foi possível remover os arquivos de perfil.");
    }
    return jsonResponse({ ok: true, storage_path: `${bucket}/${targetUid}` });
  }

  const objectPath = storageObjectPath(bucket, storagePath, targetUid);
  if (operation === "delete") {
    const result = await storage.remove([objectPath]);
    if (result.error) throw new HttpError(502, "Não foi possível remover o arquivo do Supabase.");
    return jsonResponse({ ok: true, storage_path: `${bucket}/${objectPath}` });
  }

  if (operation !== "signed-url") throw new HttpError(400, "Operação de armazenamento não suportada.");
  const signed = await storage.createSignedUrl(objectPath, SIGNED_URL_TTL_SECONDS);
  if (signed.error || !signed.data?.signedUrl) throw new HttpError(404, "Arquivo não encontrado no Supabase.");
  return jsonResponse({
    ok: true,
    bucket,
    storage_path: `${bucket}/${objectPath}`,
    signed_url: signed.data.signedUrl,
    expires_in: SIGNED_URL_TTL_SECONDS,
  });
}

Deno.serve(async (request: Request) => {
  if (request.method === "OPTIONS") return jsonResponse({ ok: true });
  if (request.method !== "POST") return jsonResponse({ error: "Método não permitido." }, 405);

  try {
    const firebaseToken = getBearerToken(request);
    const claims = await verifyFirebaseToken(firebaseToken);
    const contentType = request.headers.get("content-type") || "";
    if (contentType.toLowerCase().startsWith("multipart/form-data")) {
      return await handleUpload(request, claims, firebaseToken);
    }
    return await handleJsonOperation(request, claims, firebaseToken);
  } catch (error) {
    if (error instanceof HttpError) return jsonResponse({ error: error.message }, error.status);
    console.error("Storage gateway failed", error);
    return jsonResponse({ error: "Erro interno ao processar o armazenamento." }, 500);
  }
});
