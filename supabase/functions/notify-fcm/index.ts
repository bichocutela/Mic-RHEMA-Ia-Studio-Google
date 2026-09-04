import { importPKCS8, SignJWT } from "npm:jose@5.10.0";

const GOOGLE_SCOPE = "https://www.googleapis.com/auth/firebase.messaging https://www.googleapis.com/auth/datastore";
const FCM_TOKEN_URL = "https://oauth2.googleapis.com/token";
const DEFAULT_PROJECT_ID = "mic-rhema";
const DEFAULT_LINK = "https://bichocutela.github.io/Mic-RHEMA-Ia-Studio-Google/";
const MAX_TEXT_LENGTH = 180;

type NotificationRequest = { topic?: string; token?: string; title?: string; body?: string; data?: Record<string, string | number | boolean> };
type ServiceAccount = { project_id?: string; client_email?: string; private_key?: string };
type FireField = { stringValue?: string; booleanValue?: boolean; timestampValue?: string };
type WebToken = { token: string; fields: Record<string, FireField> };

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-client-info",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Content-Type": "application/json",
};

function json(body: Record<string, unknown>, status = 200) { return new Response(JSON.stringify(body), { status, headers: corsHeaders }); }
function cleanText(value: unknown, fallback: string) { const text = String(value ?? fallback).trim(); return text.slice(0, MAX_TEXT_LENGTH) || fallback; }
function bool(fields: Record<string, FireField>, key: string, fallback = true) { const value = fields[key]?.booleanValue; return typeof value === "boolean" ? value : fallback; }

async function accessToken(account: ServiceAccount) {
  if (!account.client_email || !account.private_key) throw new Error("FIREBASE_SERVICE_ACCOUNT_JSON sem client_email ou private_key.");
  const now = Math.floor(Date.now() / 1000);
  const assertion = await new SignJWT({ iss: account.client_email, scope: GOOGLE_SCOPE, aud: FCM_TOKEN_URL })
    .setProtectedHeader({ alg: "RS256", typ: "JWT" }).setIssuedAt(now).setExpirationTime(now + 3600)
    .sign(await importPKCS8(account.private_key.replace(/\\n/g, "\n"), "RS256"));
  const response = await fetch(FCM_TOKEN_URL, {
    method: "POST", headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth-type:jwt-bearer", assertion }),
  });
  if (!response.ok) throw new Error(`Falha ao autenticar no FCM: ${response.status}`);
  const payload = await response.json();
  if (!payload.access_token) throw new Error("O Google não retornou um access_token.");
  return payload.access_token as string;
}

function categoryPreference(category: string) {
  const value = category.toLowerCase();
  if (value.includes("devotional") || value.includes("devocional")) return "prefDevotional";
  if (value.includes("news") || value.includes("noticia") || value.includes("notícia")) return "prefNews";
  if (value.includes("sermon") || value.includes("pregacao") || value.includes("pregação")) return "prefSermons";
  if (value.includes("media") || value.includes("midia") || value.includes("mídia") || value.includes("audio") || value.includes("video") || value.includes("book")) return "prefMedia";
  if (value.includes("ibr") || value.includes("lesson") || value.includes("module") || value.includes("aula")) return "prefIbr";
  if (value.includes("course") || value.includes("curso")) return "prefCourses";
  if (value.includes("service") || value.includes("next_service") || value.includes("culto_today") || value.includes("culto_tomorrow")) return "prefService";
  if (value.includes("event") || value.includes("evento") || value === "culto") return "prefEvents";
  return "";
}

function requiresIbr(category: string, topic: string) {
  const value = category.toLowerCase();
  return topic === "ibr_users" || value.includes("ibr") || value.includes("course") || value.includes("curso");
}

function isAndroidOnlyCategory(category: string) {
  return category.trim().toLowerCase() === "app_update";
}

function acceptsWebToken(doc: WebToken, category: string, topic: string) {
  if (isAndroidOnlyCategory(category)) return false;
  if (!bool(doc.fields, "enabled", true)) return false;
  if (topic === "prayer_admins" && !bool(doc.fields, "isAdmin", false)) return false;
  if (requiresIbr(category, topic) && !bool(doc.fields, "isIbr", false)) return false;
  const preference = categoryPreference(category);
  return !preference || bool(doc.fields, preference, true);
}

function pwaLink(data: Record<string, string>) {
  const collection = (data.collection || "").toLowerCase();
  const category = (data.category || "").toLowerCase();
  const destination = (data.destination || "").toLowerCase();
  const id = data.documentId || "";
  if (collection === "prayer_requests" || destination.startsWith("admin_prayer")) return `${DEFAULT_LINK}?view=admin&section=prayers&request=${encodeURIComponent(id)}`;
  if (collection === "prayer_response" || destination === "prayer") return `${DEFAULT_LINK}?view=prayer&request=${encodeURIComponent(id)}`;
  if (destination === "ibr" || category.includes("ibr") || category.includes("course")) return `${DEFAULT_LINK}?view=ibr`;
  if (destination === "content" || /sermon|media|audio|video|book/.test(category)) return `${DEFAULT_LINK}?view=media`;
  if (destination === "services" || /event|service|culto/.test(category)) return `${DEFAULT_LINK}?view=cultos`;
  if (category.includes("devotional")) return `${DEFAULT_LINK}?view=devotionals`;
  if (category.includes("news") || category.includes("noticia")) return `${DEFAULT_LINK}?view=news`;
  return DEFAULT_LINK;
}

async function send(projectId: string, accessToken: string, target: Record<string, string>, title: string, body: string, data: Record<string, string>, includeWeb = true) {
  const message: Record<string, unknown> = { ...target, data, android: { priority: "high", ttl: "86400s" } };
  if (includeWeb) message.webpush = {
    headers: { Urgency: "high" },
    notification: { title, body, icon: `${DEFAULT_LINK}icons/icon-192.png`, tag: `micrhema-${data.category || "general"}-${data.documentId || "update"}` },
    fcm_options: { link: pwaLink(data) },
  };
  const response = await fetch(`https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`, {
    method: "POST", headers: { Authorization: `Bearer ${accessToken}`, "Content-Type": "application/json; UTF-8" },
    body: JSON.stringify({ message }),
  });
  return { ok: response.ok, status: response.status, body: await response.text() };
}

async function pwaTokens(projectId: string, accessToken: string): Promise<WebToken[]> {
  const response = await fetch(`https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents/pwa_push_tokens?pageSize=500`, { headers: { authorization: `Bearer ${accessToken}` } });
  if (!response.ok) return [];
  const payload = await response.json() as { documents?: Array<{ fields?: Record<string, FireField> }> };
  return (payload.documents || []).map((document) => ({ token: document.fields?.token?.stringValue || "", fields: document.fields || {} })).filter((item) => Boolean(item.token));
}

Deno.serve(async (request) => {
  if (request.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (request.method !== "POST") return json({ error: "Método não permitido." }, 405);
  try {
    const configuredKey = Deno.env.get("SUPABASE_ANON_KEY");
    const providedKey = request.headers.get("apikey");
    if (configuredKey && providedKey && providedKey !== configuredKey) return json({ error: "Chave pública do Supabase inválida." }, 401);

    const input = await request.json() as NotificationRequest;
    const directToken = String(input.token ?? "").trim().slice(0, 4096);
    const topic = cleanText(input.topic, "all_users").replace(/[^a-zA-Z0-9_.-]/g, "");
    const title = cleanText(input.title, "Nova atualização disponível");
    const body = cleanText(input.body, "Confira as novidades no MIC Rhema.");
    const account = JSON.parse(Deno.env.get("FIREBASE_SERVICE_ACCOUNT_JSON") ?? "{}") as ServiceAccount;
    const projectId = account.project_id || Deno.env.get("FIREBASE_PROJECT_ID") || DEFAULT_PROJECT_ID;
    const token = await accessToken(account);
    const data = Object.fromEntries(Object.entries(input.data ?? {}).map(([key, value]) => [key, String(value)]));
    data.title = data.title || title;
    data.body = data.body || body;
    data.category = data.category || "content_updates";
    const androidOnly = isAndroidOnlyCategory(data.category);

    const primary = await send(projectId, token, directToken ? { token: directToken } : { topic }, title, body, data, !androidOnly);
    if (!primary.ok) {
      console.error("FCM rejected notification", primary.status, primary.body);
      return json({ error: "FCM rejeitou a notificação.", details: primary.body.slice(0, 500) }, 502);
    }

    let webRecipients = 0;
    let webSent = 0;
    let webFiltered = 0;
    if (!directToken && !androidOnly) {
      const allWeb = await pwaTokens(projectId, token);
      const selected = allWeb.filter((item) => acceptsWebToken(item, data.category, topic));
      webRecipients = selected.length;
      webFiltered = allWeb.length - selected.length;
      const results = await Promise.all(selected.map((item) => send(projectId, token, { token: item.token }, title, body, data, true)));
      webSent = results.filter((result) => result.ok).length;
    }

    return json({ ok: true, target: directToken ? "token" : `topic:${topic}`, webRecipients, webSent, webFiltered, message: primary.body });
  } catch (error) {
    console.error("notify-fcm failed", error);
    return json({ error: error instanceof Error ? error.message : "Erro ao enviar notificação." }, 500);
  }
});
