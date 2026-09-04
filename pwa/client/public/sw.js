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

// Bloqueio no nível mais baixo do Service Worker. Este listener é registrado antes do
// Firebase Messaging, então até tokens Web antigos que ainda estejam em algum tópico
// legado têm o evento interrompido antes que o SDK possa exibir uma notificação.
self.addEventListener("push", (event) => {
  try {
    const payload = event.data?.json?.();
    if (isAndroidUpdatePayload(payload)) {
      event.stopImmediatePropagation();
    }
  } catch {
    // Payloads que não sejam JSON seguem normalmente para o Firebase Messaging.
  }
});

const messaging = firebase.messaging();

// Segunda barreira para mensagens de dados processadas pelo SDK.
messaging.onBackgroundMessage((payload) => {
  if (isAndroidUpdatePayload(payload)) return;
});

const CACHE_PREFIX = "mic-rhema-pwa-";
const CACHE = "mic-rhema-pwa-v7";
const SHELL_URL = "./";
const MANIFEST_URL = "./manifest.webmanifest";
const BASE_PATH = new URL("./", self.location.href).pathname;

function shellAssetsFromHtml(html) {
  const urls = new Set();
  const matcher = /<(?:script|link)\b[^>]*(?:src|href)=["']([^"']+)["'][^>]*>/gi;
  let match;
  while ((match = matcher.exec(html))) {
    try {
      const url = new URL(match[1], self.location.href);
      if (url.origin === self.location.origin && url.pathname.startsWith(BASE_PATH) && url.pathname.includes("/assets/")) {
        urls.add(url.href);
      }
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

  // Primeiro garante os arquivos da versão; só depois troca o HTML offline.
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
    if (title === "tem atualização nova!" || tag.includes("app_update") || (body.includes("mic rhema") && body.includes("já está disponível"))) {
      notification.close();
    }
  });
}

self.addEventListener("install", (event) => event.waitUntil(
  primeShell().then(() => self.skipWaiting()),
));

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
  if (request.method !== "GET") return;

  if (request.mode === "navigate") {
    event.respondWith((async () => {
      try {
        // HTML sempre vem primeiro da rede. A cópia offline só é trocada depois que
        // todos os assets referenciados por ela estiverem disponíveis no cache.
        const response = await fetch(request, { cache: "no-store" });
        if (response.ok) event.waitUntil(cacheCompleteShell(response.clone()).catch(() => undefined));
        return response;
      } catch {
        return (await caches.match(SHELL_URL)) || Response.error();
      }
    })());
    return;
  }

  const url = new URL(request.url);
  const localStatic = url.origin === self.location.origin
    && url.pathname.startsWith(BASE_PATH)
    && ["script", "style", "font", "image"].includes(request.destination);
  if (!localStatic) return;

  event.respondWith((async () => {
    // Hashes do Vite são imutáveis: se existe em qualquer uma das duas versões
    // mantidas, pode ser usado com segurança. Isso também protege uma tela que
    // permaneceu aberta durante uma atualização.
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
