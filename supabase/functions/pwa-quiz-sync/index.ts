import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "npm:@supabase/supabase-js@2.57.4";
import { createRemoteJWKSet, importPKCS8, jwtVerify, SignJWT } from "npm:jose@5.10.0";

const FIREBASE_PROJECT_ID = "mic-rhema";
const FIREBASE_ISSUER = `https://securetoken.google.com/${FIREBASE_PROJECT_ID}`;
const FIREBASE_JWKS = createRemoteJWKSet(new URL("https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com"));
const GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
const cors = {
  "access-control-allow-origin": "*",
  "access-control-allow-headers": "authorization, content-type",
  "access-control-allow-methods": "POST, OPTIONS",
  "content-type": "application/json; charset=utf-8",
  "cache-control": "no-store",
};

type ServiceAccount = { project_id?: string; client_email?: string; private_key?: string };
class HttpError extends Error { constructor(public status: number, message: string) { super(message); } }
const json = (body: Record<string, unknown>, status = 200) => new Response(JSON.stringify(body), { status, headers: cors });

async function memberIdFromRequest(request: Request) {
  const token = (request.headers.get("authorization") || "").match(/^Bearer\s+(.+)$/i)?.[1];
  if (!token) throw new HttpError(401, "Sessão ausente.");
  try {
    const result = await jwtVerify(token, FIREBASE_JWKS, { issuer: FIREBASE_ISSUER, audience: FIREBASE_PROJECT_ID, algorithms: ["RS256"] });
    if (!result.payload.sub) throw new Error("uid ausente");
    return String(result.payload.sub);
  } catch {
    throw new HttpError(401, "Sessão Firebase inválida.");
  }
}

async function firestoreToken(account: ServiceAccount) {
  if (!account.client_email || !account.private_key) throw new HttpError(500, "Credencial Firebase incompleta.");
  const now = Math.floor(Date.now() / 1000);
  const assertion = await new SignJWT({ iss: account.client_email, scope: "https://www.googleapis.com/auth/datastore", aud: GOOGLE_TOKEN_URL })
    .setProtectedHeader({ alg: "RS256", typ: "JWT" }).setIssuedAt(now).setExpirationTime(now + 3600)
    .sign(await importPKCS8(account.private_key.replace(/\\n/g, "\n"), "RS256"));
  const response = await fetch(GOOGLE_TOKEN_URL, {
    method: "POST",
    headers: { "content-type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth2:grant-type:jwt-bearer", assertion }),
  });
  const payload = await response.json();
  if (!response.ok || !payload.access_token) throw new HttpError(500, "Falha ao sincronizar o perfil.");
  return String(payload.access_token);
}

function base(projectId: string) { return `https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents`; }
function decode(value: any): any {
  if (!value) return undefined;
  if ("stringValue" in value) return value.stringValue;
  if ("booleanValue" in value) return value.booleanValue;
  if ("integerValue" in value) return Number(value.integerValue);
  if ("doubleValue" in value) return Number(value.doubleValue);
  if ("arrayValue" in value) return (value.arrayValue?.values || []).map(decode);
  if ("mapValue" in value) return Object.fromEntries(Object.entries(value.mapValue?.fields || {}).map(([k,v]) => [k,decode(v)]));
  return undefined;
}
function encode(value: any): any {
  if (typeof value === "boolean") return { booleanValue: value };
  if (typeof value === "number") return Number.isInteger(value) ? { integerValue: String(value) } : { doubleValue: value };
  if (Array.isArray(value)) return { arrayValue: { values: value.map(encode) } };
  return { stringValue: String(value ?? "") };
}
function fields(doc: any) { return Object.fromEntries(Object.entries(doc?.fields || {}).map(([k,v]) => [k,decode(v)])) as Record<string,any>; }
async function read(projectId: string, token: string, path: string) {
  const response = await fetch(`${base(projectId)}/${path}`, { headers: { authorization: `Bearer ${token}` } });
  if (response.status === 404) return null;
  if (!response.ok) throw new HttpError(502, "Não foi possível ler o perfil.");
  return await response.json();
}
async function patchQuizFields(projectId: string, token: string, path: string, values: Record<string,string[]>) {
  const keys = Object.keys(values);
  const masks = [...keys.map(k => `updateMask.fieldPaths=${encodeURIComponent(`badgeActivityIds.${k}`)}`), `updateMask.fieldPaths=${encodeURIComponent("updatedAt")}`].join("&");
  const quizFields = Object.fromEntries(Object.entries(values).map(([k,v]) => [k, encode(v)]));
  const response = await fetch(`${base(projectId)}/${path}?${masks}`, {
    method: "PATCH",
    headers: { authorization: `Bearer ${token}`, "content-type": "application/json" },
    body: JSON.stringify({ fields: {
      badgeActivityIds: { mapValue: { fields: quizFields } },
      updatedAt: { integerValue: String(Date.now()) },
    } }),
  });
  if (!response.ok) throw new HttpError(502, "Não foi possível salvar o progresso do Quiz.");
}
function list(value: unknown): string[] { return Array.isArray(value) ? value.map(String).filter(Boolean) : []; }
function union(...groups: string[][]) { return [...new Set(groups.flat().filter(Boolean))]; }

Deno.serve(async (request) => {
  if (request.method === "OPTIONS") return new Response("ok", { headers: cors });
  if (request.method !== "POST") return json({ error: "Método não permitido." }, 405);
  try {
    const memberId = await memberIdFromRequest(request);
    const service = JSON.parse(Deno.env.get("FIREBASE_SERVICE_ACCOUNT_JSON") || "{}") as ServiceAccount;
    const projectId = service.project_id || FIREBASE_PROJECT_ID;
    const token = await firestoreToken(service);
    const accessPath = `acessos_pendentes/${encodeURIComponent(memberId)}`;
    const accessDoc = await read(projectId, token, accessPath);
    if (!accessDoc) throw new HttpError(404, "Perfil não encontrado.");
    const accessData = fields(accessDoc);
    if (accessData.isApproved !== true && accessData.isAdmin !== true) throw new HttpError(403, "Perfil ainda não aprovado.");

    const supabaseUrl = Deno.env.get("SUPABASE_URL") || "";
    const serviceRole = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") || "";
    if (!supabaseUrl || !serviceRole) throw new HttpError(500, "Ledger do Quiz não configurado.");
    const supabase = createClient(supabaseUrl, serviceRole, { auth: { persistSession: false, autoRefreshToken: false } });
    const [{ data: attempts, error: attemptsError }, { data: hardTx, error: hardError }, { data: legacy, error: legacyError }] = await Promise.all([
      supabase.from("xp_quiz_attempts").select("question_id,variant,correct").eq("member_id", memberId).order("created_at", { ascending: true }),
      supabase.from("xp_transactions").select("content_id").eq("member_id", memberId).eq("type", "earn").eq("activity", "quiz_hard"),
      supabase.from("xp_legacy_quiz_receipts").select("question_id").eq("member_id", memberId),
    ]);
    if (attemptsError) throw attemptsError;
    if (hardError) throw hardError;
    if (legacyError) throw legacyError;

    const existing = accessData.badgeActivityIds && typeof accessData.badgeActivityIds === "object" ? accessData.badgeActivityIds as Record<string,unknown> : {};
    const centralAnswered = (attempts || []).map((x:any) => String(x.question_id)).filter(Boolean);
    const legacyAnswered = (legacy || []).map((x:any) => String(x.question_id)).filter(Boolean);
    const centralCorrect = (attempts || []).filter((x:any) => x.correct === true).map((x:any) => String(x.question_id)).filter(Boolean);
    const centralNoEasy = (attempts || []).filter((x:any) => x.correct === true && String(x.variant || "") !== "easy_hint").map((x:any) => String(x.question_id)).filter(Boolean);
    const centralNoHint = (attempts || []).filter((x:any) => x.correct === true && String(x.variant || "") === "").map((x:any) => String(x.question_id)).filter(Boolean);
    const centralHard = (hardTx || []).map((x:any) => String(x.content_id)).filter(Boolean);

    const values = {
      quiz_answered: union(list(existing.quiz_answered), centralAnswered, legacyAnswered),
      quiz_correct: union(list(existing.quiz_correct), centralCorrect),
      quiz_correct_no_easy_hint: union(list(existing.quiz_correct_no_easy_hint), centralNoEasy),
      quiz_correct_no_hint: union(list(existing.quiz_correct_no_hint), centralNoHint),
      quiz_hard_correct: union(list(existing.quiz_hard_correct), centralHard),
    };

    await patchQuizFields(projectId, token, accessPath, values);
    await patchQuizFields(projectId, token, `users/${encodeURIComponent(memberId)}`, values).catch(() => undefined);
    return json({ ok: true, answered: values.quiz_answered.length, correct: values.quiz_correct.length, noEasyHint: values.quiz_correct_no_easy_hint.length, noHint: values.quiz_correct_no_hint.length, hardCorrect: values.quiz_hard_correct.length });
  } catch (error) {
    if (error instanceof HttpError) return json({ error: error.message }, error.status);
    console.error("pwa-quiz-sync", error);
    return json({ error: "Não foi possível sincronizar o progresso confirmado do Quiz." }, 500);
  }
});