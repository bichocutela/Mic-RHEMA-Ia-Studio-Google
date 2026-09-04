import { importPKCS8, SignJWT } from "npm:jose@5.10.0";

const GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
const GOOGLE_SCOPE = "https://www.googleapis.com/auth/firebase.messaging https://www.googleapis.com/auth/datastore";
const FIREBASE_API_KEY = "AIzaSyD-GPqTLRFmOiNATJwzKUHGqJeTPQcf0E8";
const DEFAULT_LINK = "https://bichocutela.github.io/Mic-RHEMA-Ia-Studio-Google/";
const cors = { "access-control-allow-origin": "*", "access-control-allow-headers": "authorization, apikey, content-type, x-client-info", "access-control-allow-methods": "POST, OPTIONS", "content-type": "application/json; charset=utf-8", "cache-control": "no-store" };
type ServiceAccount = { project_id?: string; client_email?: string; private_key?: string };
type PushInput = { title?: unknown; body?: unknown; link?: unknown; category?: unknown };
type TokenDoc = { token: string; fields: Record<string, any> };
function json(body: Record<string, unknown>, status = 200) { return new Response(JSON.stringify(body), { status, headers: cors }); }
function text(value: unknown, fallback: string) { return String(value ?? fallback).trim().slice(0, 180) || fallback; }

async function requireAdmin(request: Request) {
  const idToken = request.headers.get("authorization")?.replace(/^Bearer\s+/i, "");
  if (!idToken) throw new Error("Acesso administrativo obrigatório.");
  const response = await fetch(`https://identitytoolkit.googleapis.com/v1/accounts:lookup?key=${FIREBASE_API_KEY}`, { method: "POST", headers: { "content-type": "application/json" }, body: JSON.stringify({ idToken }) });
  const payload = await response.json() as { users?: Array<{ localId?: string; customAttributes?: string }> };
  if (!response.ok || !payload.users?.[0]?.localId) throw new Error("Acesso administrativo obrigatório.");
  let claims: Record<string, unknown> = {};
  try { claims = JSON.parse(payload.users[0].customAttributes || "{}"); } catch { claims = {}; }
  if (claims.isAdmin !== true) throw new Error("Acesso administrativo obrigatório.");
}

async function googleAccessToken(account: ServiceAccount) {
  if (!account.client_email || !account.private_key) throw new Error("Credencial Firebase incompleta.");
  const now = Math.floor(Date.now() / 1000);
  const privateKey = await importPKCS8(account.private_key.replace(/\\n/g, "\n"), "RS256");
  const assertion = await new SignJWT({ iss: account.client_email, scope: GOOGLE_SCOPE, aud: GOOGLE_TOKEN_URL }).setProtectedHeader({ alg: "RS256", typ: "JWT" }).setIssuedAt(now).setExpirationTime(now + 3600).sign(privateKey);
  const response = await fetch(GOOGLE_TOKEN_URL, { method: "POST", headers: { "content-type": "application/x-www-form-urlencoded" }, body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", assertion }) });
  const payload = await response.json();
  if (!response.ok || !payload.access_token) throw new Error("Não foi possível autenticar o envio de avisos.");
  return payload.access_token as string;
}

function bool(fields: Record<string, any>, key: string, fallback = true) { const field = fields?.[key]; return typeof field?.booleanValue === "boolean" ? field.booleanValue : fallback; }
function preferenceKey(category: string) {
  const normalized = category.toLowerCase();
  if (normalized.includes("devotional") || normalized.includes("devocional")) return "prefDevotional";
  if (normalized.includes("news") || normalized.includes("noticia") || normalized.includes("notícia")) return "prefNews";
  if (normalized.includes("sermon") || normalized.includes("pregacao") || normalized.includes("pregação")) return "prefSermons";
  if (normalized.includes("media") || normalized.includes("midia") || normalized.includes("mídia") || normalized.includes("audio") || normalized.includes("video") || normalized.includes("book")) return "prefMedia";
  if (normalized.includes("ibr") || normalized.includes("lesson") || normalized.includes("module") || normalized.includes("aula")) return "prefIbr";
  if (normalized.includes("course") || normalized.includes("curso")) return "prefCourses";
  if (normalized.includes("service") || normalized.includes("next_service") || normalized.includes("culto_today") || normalized.includes("culto_tomorrow")) return "prefService";
  if (normalized.includes("event") || normalized.includes("evento") || normalized === "culto") return "prefEvents";
  return "";
}
function requiresIbr(category: string) { const value = category.toLowerCase(); return value.includes("ibr") || value.includes("course") || value.includes("curso"); }
function accepts(doc: TokenDoc, category: string) {
  if (!bool(doc.fields, "enabled", true)) return false;
  if (requiresIbr(category) && !bool(doc.fields, "isIbr", false)) return false;
  const key = preferenceKey(category);
  return !key || bool(doc.fields, key, true);
}
function categoryLink(category: string) {
  const value = category.toLowerCase();
  if (value.includes("devotional")) return `${DEFAULT_LINK}?view=devotionals`;
  if (value.includes("news") || value.includes("noticia")) return `${DEFAULT_LINK}?view=news`;
  if (value.includes("event") || value.includes("service") || value.includes("culto")) return `${DEFAULT_LINK}?view=cultos`;
  if (value.includes("ibr") || value.includes("course")) return `${DEFAULT_LINK}?view=ibr`;
  if (/sermon|media|audio|video|book/.test(value)) return `${DEFAULT_LINK}?view=media`;
  if (value.includes("prayer") || value.includes("oracao")) return `${DEFAULT_LINK}?view=prayer`;
  return DEFAULT_LINK;
}

async function pwaTokens(projectId: string, token: string) {
  const response = await fetch(`https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents/pwa_push_tokens?pageSize=500`, { headers: { authorization: `Bearer ${token}` } });
  if (!response.ok) throw new Error("Não foi possível consultar inscrições web.");
  const payload = await response.json() as { documents?: Array<{ fields?: Record<string, any> }> };
  return (payload.documents || []).map((document) => ({ token: document.fields?.token?.stringValue || "", fields: document.fields || {} })).filter((item) => Boolean(item.token));
}

Deno.serve(async (request) => {
  if (request.method === "OPTIONS") return new Response("ok", { headers: cors });
  if (request.method !== "POST") return json({ error: "Método não permitido." }, 405);
  try {
    await requireAdmin(request);
    const input = await request.json() as PushInput;
    const title = text(input.title, "MIC Rhema");
    const body = text(input.body, "Há uma novidade para você.");
    const category = text(input.category, "content_updates");
    const explicitLink = String(input.link ?? "").trim();
    const link = explicitLink || categoryLink(category);
    const account = JSON.parse(Deno.env.get("FIREBASE_SERVICE_ACCOUNT_JSON") || "{}") as ServiceAccount;
    const projectId = account.project_id || Deno.env.get("FIREBASE_PROJECT_ID") || "mic-rhema";
    const accessToken = await googleAccessToken(account);
    const all = await pwaTokens(projectId, accessToken);
    const tokens = all.filter((item) => accepts(item, category));
    const results = await Promise.all(tokens.map(async ({ token: registrationToken }) => {
      const response = await fetch(`https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`, {
        method: "POST",
        headers: { authorization: `Bearer ${accessToken}`, "content-type": "application/json; UTF-8" },
        body: JSON.stringify({ message: { token: registrationToken, notification: { title, body }, data: { category, title, body }, webpush: { headers: { Urgency: "high" }, notification: { title, body, icon: `${DEFAULT_LINK}icons/icon-192.png` }, fcm_options: { link } } } }),
      });
      return response.ok;
    }));
    return json({ ok: true, recipients: tokens.length, filtered: all.length - tokens.length, sent: results.filter(Boolean).length });
  } catch (error) {
    console.error("pwa-push-send failed", error instanceof Error ? error.message : "unknown");
    return json({ error: error instanceof Error ? error.message : "Não foi possível enviar o aviso." }, 403);
  }
});
