/**
 * SANTUÁRIO EM MOVIMENTO — consentimento explícito e inscrição Web Push da PWA.
 * Funciona somente no site instalado/compatível; não compartilha código com o aplicativo Android.
 */
import { getMessaging, getToken, isSupported, onMessage, type MessagePayload } from "firebase/messaging";
import { firebaseApp, firebaseAuth } from "./firebase";

const VAPID_PUBLIC_KEY = "BHkibd35bzMzP9t3If0K32xxrgMlulTQXvevAe370icbBosqINSM1WDL_TEi3k6Ja7LhHHqn6ec7NiCyArEjSkM";
const supabaseUrl = import.meta.env.VITE_SUPABASE_URL || "https://cwphbkdtorfpgmnlafqb.supabase.co";

function isStandalonePwa() {
  return window.matchMedia?.("(display-mode: standalone)").matches || Boolean((navigator as Navigator & { standalone?: boolean }).standalone);
}

function isAppleMobile() {
  return /iPad|iPhone|iPod/.test(navigator.userAgent);
}

export async function subscribeToPwaPush() {
  if (!firebaseApp || !("serviceWorker" in navigator) || !("Notification" in window)) {
    throw new Error("Este navegador não oferece suporte a notificações.");
  }
  if (isAppleMobile() && !isStandalonePwa()) {
    throw new Error("No iPhone, instale o MIC Rhema na Tela de Início antes de ativar os avisos.");
  }
  if (!(await isSupported())) throw new Error("Este navegador ainda não oferece suporte a avisos do MIC Rhema.");

  const permission = Notification.permission === "default" ? await Notification.requestPermission() : Notification.permission;
  if (permission !== "granted") throw new Error("As notificações não foram autorizadas. Você pode ativá-las nas configurações do dispositivo.");

  const registration = await navigator.serviceWorker.ready;
  const token = await getToken(getMessaging(firebaseApp), {
    vapidKey: VAPID_PUBLIC_KEY,
    serviceWorkerRegistration: registration,
  });
  if (!token) throw new Error("Não foi possível registrar este dispositivo para receber avisos.");

  const response = await fetch(`${supabaseUrl}/functions/v1/pwa-push-subscribe`, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ token, platform: isAppleMobile() ? "ios-web" : "web" }),
  });
  const payload = await response.json().catch(() => ({}));
  if (!response.ok || payload.ok !== true) throw new Error(payload.error || "Não foi possível concluir a inscrição de avisos.");
  return { token };
}

export async function listenToForegroundPush(onPayload: (payload: MessagePayload) => void) {
  if (!firebaseApp || !(await isSupported())) return () => undefined;
  return onMessage(getMessaging(firebaseApp), onPayload);
}

export async function sendPwaPush(input: { title: string; body: string; link?: string }) {
  const idToken = await firebaseAuth?.currentUser?.getIdToken();
  if (!idToken) throw new Error("Entre novamente como administrador para enviar o aviso.");
  const response = await fetch(`${supabaseUrl}/functions/v1/pwa-push-send`, {
    method: "POST",
    headers: { "content-type": "application/json", authorization: `Bearer ${idToken}` },
    body: JSON.stringify(input),
  });
  const payload = await response.json().catch(() => ({}));
  if (!response.ok || payload.ok !== true) throw new Error(payload.error || "Não foi possível enviar o aviso da PWA.");
  return payload as { recipients: number; sent: number };
}
