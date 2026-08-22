/* SANTUÁRIO EM MOVIMENTO — cache de navegação da PWA; dados dinâmicos continuam online. */
const CACHE = "mic-rhema-pwa-v1";
const APP_SHELL = ["./", "./manifest.webmanifest"];
self.addEventListener("install", (event) => event.waitUntil(caches.open(CACHE).then((cache) => cache.addAll(APP_SHELL)).then(() => self.skipWaiting())));
self.addEventListener("activate", (event) => event.waitUntil(self.clients.claim()));
self.addEventListener("fetch", (event) => {
  if (event.request.method !== "GET") return;
  if (event.request.mode === "navigate") {
    event.respondWith(fetch(event.request).catch(() => caches.match("./")));
  }
});
