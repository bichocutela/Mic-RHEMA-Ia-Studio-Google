import { fortalezaClock, eventDigest } from "../notify-fcm/notification-policy.ts";
import { notificationPresentation } from "../notify-fcm/notification-presentation.ts";
import { importPKCS8, SignJWT } from "npm:jose@5.10.0";

const GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
const GOOGLE_SCOPE = "https://www.googleapis.com/auth/firebase.messaging https://www.googleapis.com/auth/datastore";
const DEFAULT_PROJECT_ID = "mic-rhema";
const DEFAULT_LINK = "https://bichocutela.github.io/Mic-RHEMA-Ia-Studio-Google/";
const DEFAULT_TIME_ZONE = "America/Fortaleza";

const cors = {
  "access-control-allow-origin": "*",
  "access-control-allow-headers": "authorization, apikey, content-type, x-client-info",
  "access-control-allow-methods": "POST, OPTIONS",
  "content-type": "application/json; charset=utf-8",
  "cache-control": "no-store",
};

type ServiceAccount = { project_id?: string; client_email?: string; private_key?: string };
type FireValue = {
  stringValue?: string;
  booleanValue?: boolean;
  integerValue?: string;
  timestampValue?: string;
  arrayValue?: { values?: FireValue[] };
};
type FireDoc = { name?: string; fields?: Record<string, FireValue>; updateTime?: string };
type PushToken = { name: string; token: string; fields: Record<string, FireValue> };
type LocalClock = { date: string; hour: number; minute: number; timeZone: string };
type ScheduledMessage = { title: string; body: string; category: string; link: string; tag: string; documentId?: string; destination?: string };

function json(body: Record<string, unknown>, status = 200) { return new Response(JSON.stringify(body), { status, headers: cors }); }
function str(fields: Record<string, FireValue>, key: string) { return fields[key]?.stringValue || ""; }
function bool(fields: Record<string, FireValue>, key: string, fallback = true) { const value = fields[key]?.booleanValue; return typeof value === "boolean" ? value : fallback; }
function clean(value: string, fallback = "") { return String(value || fallback).replace(/\s+/g, " ").trim(); }
function safeTimeZone(value: string) { const candidate = value || DEFAULT_TIME_ZONE; try { new Intl.DateTimeFormat("en", { timeZone: candidate }).format(new Date()); return candidate; } catch { return DEFAULT_TIME_ZONE; } }

async function googleAccessToken(account: ServiceAccount, scope = GOOGLE_SCOPE) {
  if (!account.client_email || !account.private_key) throw new Error("Credencial Firebase incompleta.");
  const now = Math.floor(Date.now() / 1000);
  const privateKey = await importPKCS8(account.private_key.replace(/\\n/g, "\n"), "RS256");
  const assertion = await new SignJWT({ iss: account.client_email, scope, aud: GOOGLE_TOKEN_URL })
    .setProtectedHeader({ alg: "RS256", typ: "JWT" }).setIssuedAt(now).setExpirationTime(now + 3600).sign(privateKey);
  const response = await fetch(GOOGLE_TOKEN_URL, {
    method: "POST", headers: { "content-type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", assertion }),
  });
  const payload = await response.json();
  if (!response.ok || !payload.access_token) throw new Error("Não foi possível autenticar o agendador de notificações.");
  return String(payload.access_token);
}

function localClock(now: Date, timeZone: string): LocalClock {
  const safe = safeTimeZone(timeZone);
  const parts = new Intl.DateTimeFormat("en-CA", {
    timeZone: safe, year: "numeric", month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit", hourCycle: "h23",
  }).formatToParts(now);
  const read = (type: string) => parts.find((item) => item.type === type)?.value || "00";
  return { date: `${read("year")}-${read("month")}-${read("day")}`, hour: Number(read("hour")), minute: Number(read("minute")), timeZone: safe };
}

function addDays(dateKey: string, days: number) {
  const [year, month, day] = dateKey.split("-").map(Number);
  const date = new Date(Date.UTC(year, month - 1, day + days, 12));
  return `${date.getUTCFullYear()}-${String(date.getUTCMonth() + 1).padStart(2, "0")}-${String(date.getUTCDate()).padStart(2, "0")}`;
}

function normalizedDay(value: string) {
  return value.normalize("NFD").replace(/\p{M}+/gu, "").toLowerCase();
}
function weekdayName(dateKey: string) {
  const [year, month, day] = dateKey.split("-").map(Number);
  return ["domingo", "segunda", "terca", "quarta", "quinta", "sexta", "sabado"][new Date(Date.UTC(year, month - 1, day, 12)).getUTCDay()];
}
function serviceMatchesDate(fields: Record<string, FireValue>, dateKey: string) {
  const explicit = str(fields, "date").trim();
  if (explicit) return explicit === dateKey;
  const day = normalizedDay(str(fields, "day"));
  return day.includes(weekdayName(dateKey));
}

async function listCollection(projectId: string, accessToken: string, collection: string, pageSize = 500): Promise<FireDoc[]> {
  const result: FireDoc[] = [];
  let pageToken = "";
  do {
    const url = new URL(`https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents/${collection}`);
    url.searchParams.set("pageSize", String(pageSize));
    if (pageToken) url.searchParams.set("pageToken", pageToken);
    const response = await fetch(url, { headers: { authorization: `Bearer ${accessToken}` } });
    if (response.status === 404) return result;
    if (!response.ok) throw new Error(`Falha ao consultar ${collection}: ${response.status}`);
    const payload = await response.json() as { documents?: FireDoc[]; nextPageToken?: string };
    result.push(...(payload.documents || []));
    pageToken = payload.nextPageToken || "";
  } while (pageToken);
  return result;
}

async function getDocument(projectId: string, accessToken: string, path: string): Promise<FireDoc | null> {
  const response = await fetch(`https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents/${path}`, { headers: { authorization: `Bearer ${accessToken}` } });
  if (response.status === 404) return null;
  if (!response.ok) throw new Error(`Falha ao consultar ${path}: ${response.status}`);
  return await response.json() as FireDoc;
}

async function patchStringFields(documentName: string, accessToken: string, values: Record<string, string>) {
  const url = new URL(`https://firestore.googleapis.com/v1/${documentName}`);
  Object.keys(values).forEach((key) => url.searchParams.append("updateMask.fieldPaths", key));
  const response = await fetch(url, {
    method: "PATCH", headers: { authorization: `Bearer ${accessToken}`, "content-type": "application/json" },
    body: JSON.stringify({ fields: Object.fromEntries(Object.entries(values).map(([key, value]) => [key, { stringValue: value }])) }),
  });
  if (!response.ok) console.error("Não foi possível atualizar deduplicação do token", response.status, await response.text());
}

async function disableToken(documentName: string, accessToken: string) {
  const url = new URL(`https://firestore.googleapis.com/v1/${documentName}`);
  url.searchParams.append("updateMask.fieldPaths", "enabled");
  await fetch(url, { method: "PATCH", headers: { authorization: `Bearer ${accessToken}`, "content-type": "application/json" }, body: JSON.stringify({ fields: { enabled: { booleanValue: false } } }) }).catch(() => undefined);
}

async function send(projectId: string, accessToken: string, registrationToken: string, message: ScheduledMessage, validateOnly = false) {
  const response = await fetch(`https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`, {
    method: "POST",
    headers: { authorization: `Bearer ${accessToken}`, "content-type": "application/json; UTF-8" },
    body: JSON.stringify({ validate_only: validateOnly, message: {
      ...(registrationToken === "all_users" ? { topic: "all_users" } : { token: registrationToken }),
      data: {
        category: message.category, title: message.title, body: message.body,
        eventKey: message.tag, notificationDate: fortalezaClock().date, expiresAt: String(Date.now() + 3600000),
        ...(message.documentId ? { documentId: message.documentId } : {}),
        ...(message.destination ? { destination: message.destination } : {}),
      },
      android: { priority: "high", ttl: "3600s" },
      webpush: {
        headers: { Urgency: "normal", TTL: "3600" },
        notification: { title: message.title, body: message.body, icon: notificationPresentation({category:message.category},DEFAULT_LINK).icon, badge: notificationPresentation({category:message.category},DEFAULT_LINK).badge, tag: message.tag },
        fcm_options: { link: notificationPresentation({category:message.category,documentId:message.documentId||"",destination:message.destination||""},DEFAULT_LINK).link },
      },
    } }),
  });
  const text = await response.text();
  return { ok: response.ok, status: response.status, text };
}

function chooseDevotional(devotionals: FireDoc[], localDate: string) {
  return devotionals
    .filter((doc) => bool(doc.fields || {}, "isApproved", true))
    .filter((doc) => { const date = str(doc.fields || {}, "date"); return !date || date <= localDate; })
    .sort((a, b) => str(b.fields || {}, "date").localeCompare(str(a.fields || {}, "date")))[0] || null;
}

function hiddenNewsIds(settings: FireDoc | null) {
  return new Set((settings?.fields?.hiddenIds?.arrayValue?.values || []).map((value) => Number(value.integerValue || value.stringValue || NaN)).filter(Number.isFinite));
}
function chooseNews(news: FireDoc[], hidden: Set<number>, localDate: string) {
  const visible = news.filter((doc) => {
    const fields = doc.fields || {};
    const rawId = fields.id?.integerValue || doc.name?.split("/").pop() || "";
    const id = Number(rawId);
    return clean(str(fields, "title")).length > 0 && clean(str(fields, "content")).length > 0 && !hidden.has(id);
  }).sort((a, b) => String(a.name).localeCompare(String(b.name)));
  if (!visible.length) return null;
  let hash = 0;
  for (const char of localDate) hash = (hash * 31 + char.charCodeAt(0)) >>> 0;
  return visible[hash % visible.length];
}

function parseServiceKeys(fields: Record<string, FireValue>) {
  try { const parsed = JSON.parse(str(fields, "servicePushKeys") || "[]"); return Array.isArray(parsed) ? parsed.map(String) : []; }
  catch { return []; }
}


// Firestore create-only claims are shared by concurrent cron invocations.
async function claimDelivery(projectId: string, accessToken: string, key: string) {
  const id = await eventDigest(key);
  const name = `projects/${projectId}/databases/(default)/documents/notification_deliveries/${id}`;
  const url = new URL(`https://firestore.googleapis.com/v1/${name}`);
  url.searchParams.set("currentDocument.exists", "false");
  const response = await fetch(url, { method: "PATCH", headers: { authorization: `Bearer ${accessToken}`, "content-type": "application/json" }, body: JSON.stringify({ fields: { eventKey: { stringValue: key }, createdAt: { timestampValue: new Date().toISOString() } } }) });
  if (response.status === 409 || response.status === 412) return null;
  if (!response.ok) throw new Error(`Reserva de envio falhou: ${response.status}`);
  return await response.json() as FireDoc;
}
async function releaseClaim(accessToken: string, claim: FireDoc) {
  const url = new URL(`https://firestore.googleapis.com/v1/${claim.name}`);
  if (claim.updateTime) url.searchParams.set("currentDocument.updateTime", claim.updateTime);
  await fetch(url, { method: "DELETE", headers: { authorization: `Bearer ${accessToken}` } });
}
async function drainOutbox(projectId: string, accessToken: string, now: Date) {
  const clock = fortalezaClock(now);
  const docs = await listCollection(projectId, accessToken, "notification_outbox");
  const pending = docs.filter(d => str(d.fields || {}, "state") === "pending" && Number(d.fields?.slot?.integerValue) === clock.hour);
  const groups = new Map<string, FireDoc[]>();
  for (const d of pending.sort((a, b) => (a.fields?.createdAt?.timestampValue || "").localeCompare(b.fields?.createdAt?.timestampValue || ""))) {
    const payload = JSON.parse(str(d.fields || {}, "payload"));
    if (payload.data.collection && payload.data.documentId) {
      const current = await getDocument(projectId, accessToken, `${payload.data.collection}/${encodeURIComponent(payload.data.documentId)}`);
      if (!current || current.fields?.isPublished?.booleanValue === false || current.fields?.isApproved?.booleanValue === false) {
        await patchStringFields(d.name!, accessToken, { state: "discarded", reason: "content_no_longer_public" });
        continue;
      }
    }
    const group = `${payload.topic}:${payload.data.collection || payload.data.category}`;
    groups.set(group, [...(groups.get(group) || []), d]);
  }
  let sent = 0;
  for (const [group, items] of groups) {
    // One batch per category per day. Later additions stay pending until tomorrow.
    const claim = await claimDelivery(projectId, accessToken, `digest:${clock.date}:${group}`);
    if (!claim) continue;
    try {
      const payloads = items.map(d => JSON.parse(str(d.fields || {}, "payload")));
      const latest = payloads[payloads.length - 1];
      const contentKeys = payloads.filter(p => p.data.collection?.startsWith("conteudos_") && p.data.documentId).map(p => `content:${p.data.collection}:${p.data.documentId}`);
      const batch = { ...latest, data: { ...latest.data, eventKey: `digest:${clock.date}:${group}`, contentKeys: JSON.stringify(contentKeys), digestTag: `micrhema-digest-${clock.date}-${clock.hour}`, expiresAt: String(Date.now() + 3600000) } };
      if (items.length > 1 && clock.hour !== 18) {
        batch.title = `${items.length} novidades no MIC Rhema`;
        batch.body = payloads.map(p => p.title).join(" • ").slice(0, 180);
        delete batch.data.documentId;
        batch.data.title = batch.title; batch.data.body = batch.body;
      }
      const response = await fetch(`${Deno.env.get("SUPABASE_URL")}/functions/v1/notify-fcm`, { method: "POST", headers: { "content-type": "application/json", apikey: Deno.env.get("SUPABASE_ANON_KEY") || "", "x-internal-scheduler": Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") || "" }, body: JSON.stringify(batch) });
      const result = await response.json();
      if (!response.ok || !result.ok) throw new Error(`Resumo não enviado: ${response.status}`);
      for (const d of items) await patchStringFields(d.name!, accessToken, { state: "sent", sentAt: now.toISOString() });
      sent++;
    } catch (error) { await releaseClaim(accessToken, claim); throw error; }
  }
  return sent;
}

Deno.serve(async (request) => {
  if (request.method === "OPTIONS") return new Response("ok", { headers: cors });
  if (request.method !== "POST") return json({ error: "Método não permitido." }, 405);
  try {
    const schedulerHash = await eventDigest(request.headers.get("x-scheduler-key") || "");
    if (schedulerHash !== "ba749b2eba2b9a00951b19d4efcf09970cbff65f5c93816ff8d5bb393dfe3a23") return json({ error: "Não autorizado" }, 401);
    const account = JSON.parse(Deno.env.get("FIREBASE_SERVICE_ACCOUNT_JSON") || "{}") as ServiceAccount;
    const projectId = account.project_id || Deno.env.get("FIREBASE_PROJECT_ID") || DEFAULT_PROJECT_ID;
    const accessToken = await googleAccessToken(account);
    const now = new Date();
    const input = await request.json().catch(() => ({}));
    if (input.action === "inspect") {
      const validation = await send(projectId, accessToken, "all_users", { title: "Verificação sem envio", body: "Validação", category: "devotional", link: DEFAULT_LINK, tag: "diagnostic" }, true);
      const tokens = await listCollection(projectId, accessToken, "pwa_push_tokens");
      let firebaseFunctions: unknown = { status: "not_checked" };
      let firebaseSchedules: unknown = { status: "not_checked" };
      try {
        const cloudToken = await googleAccessToken(account, "https://www.googleapis.com/auth/cloud-platform");
        const response = await fetch(`https://cloudfunctions.googleapis.com/v1/projects/${projectId}/locations/-/functions`, { headers: { authorization: `Bearer ${cloudToken}` } });
        const result = await response.json();
        firebaseFunctions = { status: response.status, functions: (result.functions || []).map((f: { name: string; status: string }) => ({ name: f.name, status: f.status })) };
        const jobs = await fetch(`https://cloudscheduler.googleapis.com/v1/projects/${projectId}/locations/us-central1/jobs`, { headers: { authorization: `Bearer ${cloudToken}` } });
        const jobData = await jobs.json();
        firebaseSchedules = { status: jobs.status, jobs: (jobData.jobs || []).map((j: { name: string; schedule: string; timeZone: string; state: string }) => ({ name: j.name, schedule: j.schedule, timeZone: j.timeZone, state: j.state })) };
      } catch { firebaseFunctions = { status: "unavailable" }; }
      return json({ firebaseFunctions, firebaseSchedules, ok: validation.ok, firebaseProject: projectId, firestoreReadable: true, tokenCount: tokens.length, fcmValidationStatus: validation.status, clock: fortalezaClock(now), slots: { devotional: 8, services: 10, news: 12, discipulado: 14, media: 15, updates: 18, ibr: 19, announcements: 17 } });
    }
    const digestSent = await drainOutbox(projectId, accessToken, now);

    const [tokenDocs, devotionals, news, services, newsSettings] = await Promise.all([
      listCollection(projectId, accessToken, "pwa_push_tokens"),
      listCollection(projectId, accessToken, "devocionais"),
      listCollection(projectId, accessToken, "bible_news"),
      listCollection(projectId, accessToken, "cultos_agenda"),
      getDocument(projectId, accessToken, "settings/bible_news_editorial"),
    ]);
    const tokens: PushToken[] = tokenDocs.map((doc) => ({ name: doc.name || "", token: str(doc.fields || {}, "token"), fields: doc.fields || {} })).filter((item) => item.name && item.token && bool(item.fields, "enabled", true));
    const hiddenIds = hiddenNewsIds(newsSettings);

    let sent = 0;
    let failed = 0;
    let due = 0;

    for (const item of tokens) {
      const clock = localClock(now, DEFAULT_TIME_ZONE);

      const sendOne = async (message: ScheduledMessage, dedupeField: string, dedupeValue: string) => {
        due += 1;
        const claim = await claimDelivery(projectId, accessToken, `${item.token}:${message.tag}`);
        if (!claim) return;
        const result = await send(projectId, accessToken, item.token, message);
        if (result.ok) {
          sent += 1;
          await patchStringFields(item.name, accessToken, { [dedupeField]: dedupeValue });
          item.fields[dedupeField] = { stringValue: dedupeValue };
        } else {
          await releaseClaim(accessToken, claim);
          failed += 1;
          console.error("FCM scheduled push failed", result.status, result.text.slice(0, 300));
          if (/UNREGISTERED|registration-token-not-registered/i.test(result.text)) await disableToken(item.name, accessToken);
        }
      };

      if (clock.hour === 8 && bool(item.fields, "prefDevotional", true) && str(item.fields, "lastDevotionalPushDate") !== clock.date) {
        const devotional = chooseDevotional(devotionals, clock.date);
        const fields = devotional?.fields || {};
        const devotionalTitle = clean(str(fields, "title"), "Uma palavra para hoje");
        const verse = clean(str(fields, "verse")) || clean(str(fields, "verseReference")) || "Separe alguns minutos para fortalecer sua fé com a Palavra de Deus.";
        await sendOne({
          title: `Devocional Diário: ${devotionalTitle}`,
          body: verse.slice(0, 180),
          category: "devotional",
          link: `${DEFAULT_LINK}?view=devotionals`,
          tag: `micrhema-devotional-${clock.date}`,
          documentId: devotional?.name?.split("/").pop() || "",
          destination: "devotionals",
        }, "lastDevotionalPushDate", clock.date);
      }

      if (clock.hour === 12 && bool(item.fields, "prefNews", true) && str(item.fields, "lastNewsPushDate") !== clock.date) {
        const selected = chooseNews(news, hiddenIds, clock.date);
        const fields = selected?.fields || {};
        const id = fields.id?.integerValue || selected?.name?.split("/").pop() || "";
        const title = clean(str(fields, "title"), "Confira a notícia bíblica de hoje");
        const summary = clean(str(fields, "summary")) || clean(str(fields, "content")) || "Abra o MIC Rhema e confira a notícia bíblica do dia.";
        await sendOne({
          title: "Notícia bíblica do dia",
          body: `${title} — ${summary}`.slice(0, 180),
          category: "news",
          link: `${DEFAULT_LINK}?view=news${id ? `&request=${encodeURIComponent(id)}` : ""}`,
          tag: `micrhema-news-${clock.date}`,
          documentId: String(id),
          destination: "news",
        }, "lastNewsPushDate", clock.date);
      }

      if (clock.hour === 10 && bool(item.fields, "prefService", true) && str(item.fields, "lastServiceDigestDate") !== clock.date) {
        const selected = services.filter(d => bool(d.fields || {}, "isApproved", true)).flatMap(d => {
          const f = d.fields || {};
          const when = serviceMatchesDate(f, clock.date) ? "Hoje" : serviceMatchesDate(f, addDays(clock.date, 1)) ? "Amanhã" : "";
          return when ? [`${when}: ${clean(str(f, "title"), "Culto")}${str(f, "time") ? ` às ${str(f, "time")}` : ""}`] : [];
        });
        if (selected.length) await sendOne({ title: "Programação dos cultos", body: selected.join(" • ").slice(0, 180), category: "service", link: DEFAULT_LINK, tag: `micrhema-service-${clock.date}`, destination: "services" }, "lastServiceDigestDate", clock.date);
      }
    }

    return json({ ok: true, checkedTokens: tokens.length, due, sent, failed, digestSent, checkedAt: now.toISOString() });
  } catch (error) {
    console.error("pwa-scheduled-notifications failed", error instanceof Error ? error.message : "unknown");
    return json({ error: error instanceof Error ? error.message : "Falha no agendador de notificações." }, 500);
  }
});
