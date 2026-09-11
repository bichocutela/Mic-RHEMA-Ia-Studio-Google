/* SANTUÁRIO EM MOVIMENTO — cache resiliente + receptor FCM Web Push exclusivo da PWA. */
importScripts("https://www.gstatic.com/firebasejs/12.18.0/firebase-app-compat.js");
importScripts("https://www.gstatic.com/firebasejs/12.18.0/firebase-messaging-compat.js");

firebase.initializeApp({
  apiKey: "AIzaSyD-GPqTLRFmOiNATJwzKUHGqJeTPQcf0E8",
  authDomain: "mic-rhema.firebaseapp.com",
  projectId: "mic-rhema",
  appId: "1:894363387794:web:f8010218d4f6c6e085234b",
  messagingSenderId: "894363387794",
});

function pushCategory(payload) {
  return String(payload?.data?.category || payload?.category || "").trim().toLowerCase();
}

function isAndroidUpdatePayload(payload) {
  if (pushCategory(payload) === "app_update") return true;
  const title = String(payload?.notification?.title || payload?.data?.title || "").trim().toLowerCase();
  const body = String(payload?.notification?.body || payload?.data?.body || "").trim().toLowerCase();
  return title === "tem atualização nova!" || (body.includes("mic rhema") && body.includes("já está disponível"));
}

self.addEventListener("push", (event) => {
  try {
    const payload = event.data?.json?.();
    if (isAndroidUpdatePayload(payload)) event.stopImmediatePropagation();
  } catch {
    // Payloads que não sejam JSON seguem normalmente para o Firebase Messaging.
  }
});

const messaging = firebase.messaging();
messaging.onBackgroundMessage((payload) => {
  if (isAndroidUpdatePayload(payload)) return;
});

const CACHE_PREFIX = "mic-rhema-pwa-";
const CACHE = "mic-rhema-pwa-v8";
const SHELL_URL = "./";
const MANIFEST_URL = "./manifest.webmanifest";
const BASE_PATH = new URL("./", self.location.href).pathname;
const SHARE_DB = "mic-rhema-share-import";
const SHARE_STORE = "pending";
const SHARE_RECORD_ID = "latest";
const MAX_SHARED_BYTES = 50 * 1024 * 1024;

function shellAssetsFromHtml(html) {
  const urls = new Set();
  const matcher = /<(?:script|link)\b[^>]*(?:src|href)=["']([^"']+)["'][^>]*>/gi;
  let match;
  while ((match = matcher.exec(html))) {
    try {
      const url = new URL(match[1], self.location.href);
      if (url.origin === self.location.origin && url.pathname.startsWith(BASE_PATH) && url.pathname.includes("/assets/")) urls.add(url.href);
    } catch {
      // Ignora referências externas ou inválidas.
    }
  }
  return [...urls];
}

async function cacheCompleteShell(response) {
  if (!response || !response.ok) return;
  const html = await response.clone().text();
  const cache = await caches.open(CACHE);
  const assets = shellAssetsFromHtml(html);
  await Promise.all(assets.map(async (url) => {
    const assetResponse = await fetch(new Request(url, { cache: "reload" }));
    if (!assetResponse.ok) throw new Error(`Falha ao preparar ${url}`);
    await cache.put(url, assetResponse.clone());
  }));
  await cache.put(SHELL_URL, response.clone());
}

async function primeShell() {
  const response = await fetch(new Request(SHELL_URL, { cache: "reload" }));
  if (!response.ok) throw new Error("Não foi possível preparar a PWA.");
  await cacheCompleteShell(response);
  const manifest = await fetch(new Request(MANIFEST_URL, { cache: "reload" }));
  if (manifest.ok) {
    const cache = await caches.open(CACHE);
    await cache.put(MANIFEST_URL, manifest.clone());
  }
}

async function clearLegacyAndroidUpdateNotifications() {
  if (!self.registration?.getNotifications) return;
  const notifications = await self.registration.getNotifications();
  notifications.forEach((notification) => {
    const title = String(notification.title || "").trim().toLowerCase();
    const body = String(notification.body || "").trim().toLowerCase();
    const tag = String(notification.tag || "").trim().toLowerCase();
    if (title === "tem atualização nova!" || tag.includes("app_update") || (body.includes("mic rhema") && body.includes("já está disponível"))) notification.close();
  });
}

function sharedKind(name, type, fingerprint = "") {
  const value = `${name || ""} ${type || ""} ${fingerprint || ""}`.toLowerCase();
  if (value.includes("application/pdf") || /\.pdf(?:\b|$)/.test(value)) return "pdf";
  if (value.includes("application/epub+zip") || /\.epub(?:\b|$)/.test(value)) return "epub";
  if (value.includes("wordprocessingml") || value.includes("msword") || /\.docx(?:\b|$)/.test(value)) return "docx";
  return "unknown";
}

function firstSharedUrl(...values) {
  for (const value of values) {
    const match = String(value || "").match(/https?:\/\/[^\s<>"']+/i);
    if (match) return match[0];
  }
  return "";
}

function openShareDb() {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(SHARE_DB, 1);
    request.onupgradeneeded = () => {
      const db = request.result;
      if (!db.objectStoreNames.contains(SHARE_STORE)) db.createObjectStore(SHARE_STORE, { keyPath: "id" });
    };
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error || new Error("Falha ao abrir armazenamento de compartilhamento."));
  });
}

async function saveSharedRecord(record) {
  const db = await openShareDb();
  await new Promise((resolve, reject) => {
    const transaction = db.transaction(SHARE_STORE, "readwrite");
    transaction.objectStore(SHARE_STORE).put(record);
    transaction.oncomplete = resolve;
    transaction.onerror = () => reject(transaction.error || new Error("Falha ao salvar compartilhamento."));
  });
  db.close();
}

async function receiveShare(request) {
  const form = await request.formData();
  const files = form.getAll("documents").filter((value) => value instanceof File && value.size > 0);
  const title = String(form.get("title") || "");
  const text = String(form.get("text") || "");
  const url = String(form.get("url") || "");
  const file = files.find((candidate) => sharedKind(candidate.name, candidate.type) !== "unknown") || files[0] || null;
  if (file && file.size > MAX_SHARED_BYTES) {
    return new Response("Arquivo compartilhado maior que 50 MB.", { status: 413, headers: { "content-type": "text/plain; charset=utf-8" } });
  }
  const remoteUrl = firstSharedUrl(url, text, title);
  if (!file && !remoteUrl) return new Response("Nenhum arquivo ou link compatível foi compartilhado.", { status: 400 });
  const name = file?.name || title || remoteUrl.split("/").pop()?.split(/[?#]/)[0] || "material-compartilhado";
  const kind = sharedKind(name, file?.type || "", `${title} ${text} ${remoteUrl}`);
  await saveSharedRecord({
    id: SHARE_RECORD_ID,
    name,
    type: file?.type || "",
    size: file?.size || 0,
    blob: file ? file.slice(0, file.size, file.type) : null,
    kind,
    sharedTitle: title,
    sharedText: text,
    sharedUrl: remoteUrl,
    createdAt: Date.now(),
  });
  return Response.redirect(new URL("./?view=admin&section=ibr&shared=1", self.location.href), 303);
}

self.addEventListener("install", (event) => event.waitUntil(primeShell().then(() => self.skipWaiting())));

self.addEventListener("activate", (event) => event.waitUntil((async () => {
  const keys = (await caches.keys()).filter((key) => key.startsWith(CACHE_PREFIX));
  const version = (key) => Number(key.match(/v(\d+)$/)?.[1] || 0);
  const keep = new Set(keys.sort((a, b) => version(b) - version(a)).slice(0, 2));
  keep.add(CACHE);
  await Promise.all(keys.filter((key) => !keep.has(key)).map((key) => caches.delete(key)));
  await clearLegacyAndroidUpdateNotifications().catch(() => undefined);
  await self.clients.claim();
})()));

self.addEventListener("fetch", (event) => {
  const request = event.request;
  const url = new URL(request.url);

  if (request.method === "POST" && url.origin === self.location.origin && url.pathname === `${BASE_PATH}share-target`) {
    event.respondWith(receiveShare(request).catch(() => Response.redirect(new URL("./?view=admin&section=ibr&shared=error", self.location.href), 303)));
    return;
  }

  if (request.method !== "GET") return;

  if (request.mode === "navigate") {
    event.respondWith((async () => {
      try {
        const response = await fetch(request, { cache: "no-store" });
        if (response.ok) event.waitUntil(cacheCompleteShell(response.clone()).catch(() => undefined));
        return response;
      } catch {
        return (await caches.match(SHELL_URL)) || Response.error();
      }
    })());
    return;
  }

  const localStatic = url.origin === self.location.origin
    && url.pathname.startsWith(BASE_PATH)
    && ["script", "style", "font", "image"].includes(request.destination);
  if (!localStatic) return;

  event.respondWith((async () => {
    const cached = await caches.match(request);
    if (cached) return cached;
    const response = await fetch(request);
    if (response.ok) {
      const cache = await caches.open(CACHE);
      await cache.put(request, response.clone());
    }
    return response;
  })());
});
