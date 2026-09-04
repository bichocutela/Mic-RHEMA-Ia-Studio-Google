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
type FireDoc = { name?: string; fields?: Record<string, FireValue> };
type PushToken = { name: string; token: string; fields: Record<string, FireValue> };
type LocalClock = { date: string; hour: number; minute: number; timeZone: string };
type ScheduledMessage = { title: string; body: string; category: string; link: string; tag: string; documentId?: string; destination?: string };

function json(body: Record<string, unknown>, status = 200) { return new Response(JSON.stringify(body), { status, headers: cors }); }
function str(fields: Record<string, FireValue>, key: string) { return fields[key]?.stringValue || ""; }
function bool(fields: Record<string, FireValue>, key: string, fallback = true) { const value = fields[key]?.booleanValue; return typeof value === "boolean" ? value : fallback; }
function clean(value: string, fallback = "") { return String(value || fallback).replace(/\s+/g, " ").trim(); }
function safeTimeZone(value: string) { const candidate = value || DEFAULT_TIME_ZONE; try { new Intl.DateTimeFormat("en", { timeZone: candidate }).format(new Date()); return candidate; } catch { return DEFAULT_TIME_ZONE; } }

async function googleAccessToken(account: ServiceAccount) {
  if (!account.client_email || !account.private_key) throw new Error("Credencial Firebase incompleta.");
  const now = Math.floor(Date.now() / 1000);
  const privateKey = await importPKCS8(account.private_key.replace(/\\n/g, "\n"), "RS256");
  const assertion = await new SignJWT({ iss: account.client_email, scope: GOOGLE_SCOPE, aud: GOOGLE_TOKEN_URL })
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

async function send(projectId: string, accessToken: string, registrationToken: string, message: ScheduledMessage) {
  const response = await fetch(`https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`, {
    method: "POST",
    headers: { authorization: `Bearer ${accessToken}`, "content-type": "application/json; UTF-8" },
    body: JSON.stringify({ message: {
      token: registrationToken,
      notification: { title: message.title, body: message.body },
      data: {
        category: message.category, title: message.title, body: message.body,
        ...(message.documentId ? { documentId: message.documentId } : {}),
        ...(message.destination ? { destination: message.destination } : {}),
      },
      webpush: {
        headers: { Urgency: "high" },
        notification: { title: message.title, body: message.body, icon: `${DEFAULT_LINK}icons/icon-192.png`, tag: message.tag },
        fcm_options: { link: message.link },
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

Deno.serve(async (request) => {
  if (request.method === "OPTIONS") return new Response("ok", { headers: cors });
  if (request.method !== "POST") return json({ error: "Método não permitido." }, 405);
  try {
    const account = JSON.parse(Deno.env.get("FIREBASE_SERVICE_ACCOUNT_JSON") || "{}") as ServiceAccount;
    const projectId = account.project_id || Deno.env.get("FIREBASE_PROJECT_ID") || DEFAULT_PROJECT_ID;
    const accessToken = await googleAccessToken(account);
    const now = new Date();

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
      const clock = localClock(now, str(item.fields, "timeZone") || DEFAULT_TIME_ZONE);

      const sendOne = async (message: ScheduledMessage, dedupeField: string, dedupeValue: string) => {
        due += 1;
        const result = await send(projectId, accessToken, item.token, message);
        if (result.ok) {
          sent += 1;
          await patchStringFields(item.name, accessToken, { [dedupeField]: dedupeValue });
          item.fields[dedupeField] = { stringValue: dedupeValue };
        } else {
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

      if (clock.hour === 10 && bool(item.fields, "prefService", true)) {
        const today = clock.date;
        const tomorrow = addDays(today, 1);
        const previousKeys = parseServiceKeys(item.fields);
        const nextKeys = [...previousKeys];
        for (const service of services.filter((doc) => bool(doc.fields || {}, "isApproved", true))) {
          const fields = service.fields || {};
          const id = service.name?.split("/").pop() || clean(str(fields, "title"), "culto");
          const title = clean(str(fields, "title"), "Culto");
          const time = clean(str(fields, "time"));
          const timeText = time ? ` às ${time}` : "";
          let kind = "";
          let keyDate = "";
          let notificationTitle = "";
          let body = "";
          if (serviceMatchesDate(fields, today)) {
            kind = "today"; keyDate = today; notificationTitle = `Hoje tem culto: ${title}`; body = `Hoje é dia de ${title}${timeText}. Esperamos você!`;
          } else if (serviceMatchesDate(fields, tomorrow)) {
            kind = "day_before"; keyDate = today; notificationTitle = `Amanhã tem culto: ${title}`; body = `Prepare-se: amanhã teremos ${title}${timeText}.`;
          } else continue;
          const dedupe = `${keyDate}:${kind}:${id}`;
          if (previousKeys.includes(dedupe)) continue;
          due += 1;
          const result = await send(projectId, accessToken, item.token, {
            title: notificationTitle, body, category: "service", link: `${DEFAULT_LINK}?view=cultos`,
            tag: `micrhema-service-${dedupe}`, documentId: id, destination: "services",
          });
          if (result.ok) { sent += 1; nextKeys.push(dedupe); }
          else {
            failed += 1;
            console.error("FCM service push failed", result.status, result.text.slice(0, 300));
            if (/UNREGISTERED|registration-token-not-registered/i.test(result.text)) await disableToken(item.name, accessToken);
          }
        }
        if (nextKeys.length !== previousKeys.length) {
          const keep = nextKeys.slice(-30);
          await patchStringFields(item.name, accessToken, { servicePushKeys: JSON.stringify(keep) });
          item.fields.servicePushKeys = { stringValue: JSON.stringify(keep) };
        }
      }
    }

    return json({ ok: true, checkedTokens: tokens.length, due, sent, failed, checkedAt: now.toISOString() });
  } catch (error) {
    console.error("pwa-scheduled-notifications failed", error instanceof Error ? error.message : "unknown");
    return json({ error: error instanceof Error ? error.message : "Falha no agendador de notificações." }, 500);
  }
});
