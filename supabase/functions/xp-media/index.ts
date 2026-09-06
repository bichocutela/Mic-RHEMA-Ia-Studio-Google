import { createClient } from "npm:@supabase/supabase-js@2.57.4";
import { createRemoteJWKSet, importPKCS8, jwtVerify, SignJWT } from "npm:jose@5.10.0";

const FIRESTORE_SCOPE = "https://www.googleapis.com/auth/datastore";
const TOKEN_URL = "https://oauth2.googleapis.com/token";
const DEFAULT_PROJECT_ID = "mic-rhema";
const FIREBASE_ISSUER = "https://securetoken.google.com/mic-rhema";
const FIREBASE_JWKS = createRemoteJWKSet(new URL("https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com"));
const LEVEL_8_PLUS = new Set([
  "semente_da_fe", "caminho_da_promessa", "escudo_da_fe", "aguas_vivas", "videira_verdadeira",
  "luz_do_mundo", "armadura_de_deus", "leao_de_juda", "chama_do_espirito", "coroa_da_vida",
  "asas_da_promessa", "tabernaculo", "arca_da_alianca", "nova_jerusalem", "gloria_eterna",
]);

type ServiceAccount = { project_id?: string; client_email?: string; private_key?: string };
type FirestoreValue = {
  stringValue?: string; booleanValue?: boolean; integerValue?: string; doubleValue?: number;
  timestampValue?: string; arrayValue?: { values?: FirestoreValue[] };
  mapValue?: { fields?: Record<string, FirestoreValue> }; nullValue?: null;
};
type FirestoreDocument = { name?: string; fields?: Record<string, FirestoreValue> };
type MediaType = "book" | "audio" | "video";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-client-info",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Content-Type": "application/json; charset=utf-8",
  "Cache-Control": "no-store",
};
const json = (body: Record<string, unknown>, status = 200) => new Response(JSON.stringify(body), { status, headers: corsHeaders });

function fromValue(value?: FirestoreValue): unknown {
  if (!value) return null;
  if (value.stringValue !== undefined) return value.stringValue;
  if (value.booleanValue !== undefined) return value.booleanValue;
  if (value.integerValue !== undefined) return Number(value.integerValue);
  if (value.doubleValue !== undefined) return value.doubleValue;
  if (value.timestampValue !== undefined) return value.timestampValue;
  if (value.arrayValue !== undefined) return (value.arrayValue.values ?? []).map(fromValue);
  if (value.mapValue !== undefined) return Object.fromEntries(Object.entries(value.mapValue.fields ?? {}).map(([key, item]) => [key, fromValue(item)]));
  return null;
}
function documentData(document?: FirestoreDocument | null): Record<string, unknown> {
  return document ? Object.fromEntries(Object.entries(document.fields ?? {}).map(([key, value]) => [key, fromValue(value)])) : {};
}
const stringList = (value: unknown): string[] => Array.isArray(value) ? value.map(String).filter(Boolean) : [];
function activityMap(value: unknown): Record<string, string[]> {
  if (!value || typeof value !== "object" || Array.isArray(value)) return {};
  return Object.fromEntries(Object.entries(value as Record<string, unknown>).map(([key, item]) => [key, stringList(item)]));
}
function parseLegacyXp(entry: string) {
  const value = Number(entry.slice(entry.lastIndexOf("=") + 1));
  return Number.isFinite(value) && value > 0 ? Math.floor(value) : 0;
}
function baseUrl(projectId: string) {
  return `https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents`;
}
async function googleAccessToken(account: ServiceAccount) {
  if (!account.client_email || !account.private_key) throw new Error("Conta de serviço Firebase incompleta.");
  const now = Math.floor(Date.now() / 1000);
  const assertion = await new SignJWT({ iss: account.client_email, scope: FIRESTORE_SCOPE, aud: TOKEN_URL })
    .setProtectedHeader({ alg: "RS256", typ: "JWT" }).setIssuedAt(now).setExpirationTime(now + 3600)
    .sign(await importPKCS8(account.private_key.replace(/\\n/g, "\n"), "RS256"));
  const response = await fetch(TOKEN_URL, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth2:grant-type:jwt-bearer", assertion }),
  });
  const payload = await response.json();
  if (!response.ok || !payload.access_token) throw new Error("Falha ao autenticar no Google.");
  return String(payload.access_token);
}
async function getDocument(projectId: string, token: string, collection: string, id: string): Promise<FirestoreDocument | null> {
  const response = await fetch(`${baseUrl(projectId)}/${collection}/${encodeURIComponent(id)}`, { headers: { Authorization: `Bearer ${token}` } });
  if (response.status === 404) return null;
  if (!response.ok) throw new Error(`Falha ao ler ${collection}: ${response.status}`);
  return await response.json() as FirestoreDocument;
}
async function listDocuments(projectId: string, token: string, collection: string): Promise<FirestoreDocument[]> {
  const documents: FirestoreDocument[] = [];
  let pageToken = "";
  for (let page = 0; page < 5; page++) {
    const url = new URL(`${baseUrl(projectId)}/${collection}`);
    url.searchParams.set("pageSize", "200");
    if (pageToken) url.searchParams.set("pageToken", pageToken);
    const response = await fetch(url, { headers: { Authorization: `Bearer ${token}` } });
    if (response.status === 404) return documents;
    if (!response.ok) throw new Error(`Falha ao consultar catálogo ${collection}: ${response.status}`);
    const payload = await response.json() as { documents?: FirestoreDocument[]; nextPageToken?: string };
    documents.push(...(payload.documents ?? []));
    pageToken = payload.nextPageToken ?? "";
    if (!pageToken) break;
  }
  return documents;
}

async function firebaseMemberId(request: Request) {
  const bearer = (request.headers.get("authorization") ?? "").match(/^Bearer\s+(.+)$/i)?.[1] ?? "";
  if (!bearer) throw new Error("Sessão Firebase ausente.");
  const verified = await jwtVerify(bearer, FIREBASE_JWKS, {
    issuer: FIREBASE_ISSUER,
    audience: DEFAULT_PROJECT_ID,
    algorithms: ["RS256"],
  });
  const memberId = String(verified.payload.sub ?? "").trim();
  if (!memberId) throw new Error("Sessão Firebase inválida.");
  return memberId;
}
async function loadMember(projectId: string, token: string, memberId: string) {
  const access = documentData(await getDocument(projectId, token, "acessos_pendentes", memberId));
  if (!Object.keys(access).length) throw new Error("Cadastro do membro não encontrado.");
  if (access.isApproved !== true && access.isAdmin !== true) throw new Error("Acesso do membro ainda não aprovado.");
  const user = documentData(await getDocument(projectId, token, "users", memberId));
  const unlocked = new Set([...stringList(access.unlockedBadgeIds), ...stringList(user.unlockedBadgeIds)]);
  const legacyEntries = [...new Set([
    ...(activityMap(access.badgeActivityIds).journey_xp_awards ?? []),
    ...(activityMap(user.badgeActivityIds).journey_xp_awards ?? []),
  ])];
  return {
    xpUnlocked: [...unlocked].some((id) => LEVEL_8_PLUS.has(id)),
    legacyXp: legacyEntries.reduce((sum, item) => sum + parseLegacyXp(item), 0),
  };
}

function youtubeId(value: string) {
  for (const pattern of [
    /(?:youtube\.com\/watch\?[^#]*v=)([A-Za-z0-9_-]{6,})/i,
    /(?:youtu\.be\/)([A-Za-z0-9_-]{6,})/i,
    /(?:youtube\.com\/(?:shorts|embed|live)\/)([A-Za-z0-9_-]{6,})/i,
  ]) {
    const match = value.match(pattern);
    if (match?.[1]) return match[1];
  }
  return "";
}
function driveId(value: string) {
  for (const pattern of [
    /\/d\/([A-Za-z0-9_-]{10,})/,
    /[?&]id=([A-Za-z0-9_-]{10,})/,
    /\/file\/d\/([A-Za-z0-9_-]{10,})/,
  ]) {
    const match = value.match(pattern);
    if (match?.[1]) return match[1];
  }
  return "";
}
function runCatchingDecode(value: string) {
  try { return decodeURIComponent(value); } catch { return value; }
}
function canonicalMediaKey(value: unknown) {
  const raw = String(value ?? "").trim();
  if (!raw) return "";
  const decoded = runCatchingDecode(raw);
  const yt = youtubeId(decoded);
  if (yt) return `youtube:${yt}`;
  const drive = driveId(decoded);
  if (drive) return `drive:${drive}`;
  try {
    const url = new URL(decoded);
    const path = url.pathname.replace(/\/+$/, "");
    return `url:${url.protocol.toLowerCase()}//${url.hostname.toLowerCase()}${path}`;
  } catch {
    return `raw:${decoded}`;
  }
}
function mediaSpec(type: MediaType) {
  if (type === "book") return { collection: "conteudos_books", primaryField: "bookUrl" };
  if (type === "audio") return { collection: "conteudos_audios", primaryField: "audioUrl" };
  return { collection: "conteudos_videos", primaryField: "videoUrl" };
}
async function resolveCatalogId(projectId: string, token: string, type: MediaType, clientContentId: string) {
  const expected = canonicalMediaKey(clientContentId);
  if (!expected) throw new Error("Identificador da mídia inválido.");
  const spec = mediaSpec(type);
  const documents = await listDocuments(projectId, token, spec.collection);
  for (const document of documents) {
    const data = documentData(document);
    if (data.isApproved === false) continue;
    if (![data[spec.primaryField], data.mediaUrl].some((candidate) => canonicalMediaKey(candidate) === expected)) continue;
    const id = String(document.name ?? "").split("/").pop()?.trim() ?? "";
    if (id) return id;
  }
  throw new Error("Mídia não pertence ao catálogo oficial do MIC Rhema.");
}
function finiteNumber(value: unknown, fallback = 0) {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : fallback;
}

Deno.serve(async (request) => {
  if (request.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (request.method !== "POST") return json({ error: "Método não permitido." }, 405);
  try {
    const input = await request.json() as Record<string, unknown>;
    const memberId = await firebaseMemberId(request);
    const mediaType = String(input.mediaType ?? "").trim() as MediaType;
    const contentId = String(input.contentId ?? "").trim().slice(0, 1500);
    if (!(["book", "audio", "video"] as string[]).includes(mediaType) || !contentId) return json({ error: "Mídia inválida." }, 400);

    const serviceAccount = JSON.parse(Deno.env.get("FIREBASE_SERVICE_ACCOUNT_JSON") ?? "{}") as ServiceAccount;
    const projectId = serviceAccount.project_id || Deno.env.get("FIREBASE_PROJECT_ID") || DEFAULT_PROJECT_ID;
    const googleToken = await googleAccessToken(serviceAccount);
    const member = await loadMember(projectId, googleToken, memberId);

    const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
    const serviceRole = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
    if (!supabaseUrl || !serviceRole) throw new Error("Backend de mídia XP não configurado.");
    const supabase = createClient(supabaseUrl, serviceRole, { auth: { persistSession: false, autoRefreshToken: false } });

    const { data: accountRows, error: accountError } = await supabase.rpc("xp_ensure_account", { p_member_id: memberId, p_legacy_xp: member.legacyXp });
    if (accountError) throw accountError;
    const account = Array.isArray(accountRows) ? accountRows[0] : accountRows;
    if (!account) throw new Error("Conta XP não pôde ser inicializada.");

    if (!member.xpUnlocked) {
      return json({ ok: true, unlocked: false, reason: "xp_locked", account, tenGranted: 0, completeGranted: 0, qualified: false });
    }

    const { error: baselineError } = await supabase.rpc("xp_capture_journey_baseline", { p_member_id: memberId });
    if (baselineError) throw baselineError;

    const canonicalId = await resolveCatalogId(projectId, googleToken, mediaType, contentId);
    const positionMs = Math.max(0, Math.floor(finiteNumber(input.positionMs)));
    const durationMs = Math.max(0, Math.floor(finiteNumber(input.durationMs)));
    const clientFraction = Math.min(1, Math.max(0, finiteNumber(input.fraction)));
    const fraction = mediaType === "book" ? clientFraction : durationMs > 0 ? Math.min(1, positionMs / durationMs) : 0;
    const isActive = input.isActive !== false;

    const { data, error } = await supabase.rpc("xp_record_media_progress", {
      p_member_id: memberId,
      p_media_type: mediaType,
      p_content_id: canonicalId,
      p_position_ms: positionMs,
      p_duration_ms: durationMs,
      p_fraction: fraction,
      p_is_active: isActive,
    });
    if (error) throw error;
    const result = Array.isArray(data) ? data[0] : data;
    if (!result) throw new Error("O rastreador de mídia não retornou resultado.");

    const activeMs = Number(result.active_ms ?? 0);
    const consumedMs = Number(result.consumed_ms ?? 0);
    const maxFraction = Number(result.max_fraction ?? 0);
    const qualified = mediaType === "book"
      ? maxFraction >= 0.10 && activeMs >= 30_000
      : consumedMs >= 600_000 || (maxFraction >= 0.90 && activeMs >= 120_000);

    return json({
      ok: true,
      unlocked: true,
      canonicalId,
      qualified,
      tenGranted: Number(result.ten_granted ?? 0),
      completeGranted: Number(result.complete_granted ?? 0),
      progress: { activeMs, consumedMs, maxFraction },
      account: {
        member_id: memberId,
        total_earned: Number(result.total_earned ?? account.total_earned ?? 0),
        total_spent: Number(result.total_spent ?? account.total_spent ?? 0),
        balance: Number(result.balance ?? account.balance ?? 0),
        migrated_legacy_xp: Number(account.migrated_legacy_xp ?? 0),
        updated_at: new Date().toISOString(),
      },
    });
  } catch (error) {
    console.error("xp-media failed", error);
    const message = error instanceof Error ? error.message : "Falha no progresso verificado de mídia.";
    const lowered = message.toLowerCase();
    const status = lowered.includes("sessão") ? 401
      : lowered.includes("ainda não aprovado") ? 403
      : lowered.includes("inválid") || lowered.includes("não pertence") ? 400
      : 500;
    return json({ error: message }, status);
  }
});
