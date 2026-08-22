/* SANTUÁRIO EM MOVIMENTO — cache + receptor FCM Web Push exclusivo da PWA. */
importScripts("https://www.gstatic.com/firebasejs/12.18.0/firebase-app-compat.js");
importScripts("https://www.gstatic.com/firebasejs/12.18.0/firebase-messaging-compat.js");

firebase.initializeApp({
  apiKey: "AIzaSyD-GPqTLRFmOiNATJwzKUHGqJeTPQcf0E8",
  authDomain: "mic-rhema.firebaseapp.com",
  projectId: "mic-rhema",
  appId: "1:894363387794:web:f8010218d4f6c6e085234b",
  messagingSenderId: "894363387794",
});
firebase.messaging();

const CACHE = "mic-rhema-pwa-v3";
const APP_SHELL = ["./", "./manifest.webmanifest"];
self.addEventListener("install", (event) => event.waitUntil(caches.open(CACHE).then((cache) => cache.addAll(APP_SHELL)).then(() => self.skipWaiting())));
self.addEventListener("activate", (event) => event.waitUntil(
  caches.keys()
    .then((keys) => Promise.all(keys.filter((key) => key.startsWith("mic-rhema-pwa-") && key !== CACHE).map((key) => caches.delete(key))))
    .then(() => self.clients.claim()),
));
self.addEventListener("fetch", (event) => {
  if (event.request.method !== "GET") return;
  if (event.request.mode === "navigate") {
    event.respondWith(fetch(event.request).catch(() => caches.match("./")));
  }
});
