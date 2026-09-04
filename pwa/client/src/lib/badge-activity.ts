import { firebaseAuth } from "./firebase";

const supabaseUrl = import.meta.env.VITE_SUPABASE_URL || "https://cwphbkdtorfpgmnlafqb.supabase.co";

export type BadgeActivityKey = "plans" | "plan_themes" | "books" | "videos" | "bible_chapters" | "bible_news" | "devotionals" | "audios" | "active_minutes";

export async function recordPwaActivity(activity: BadgeActivityKey, itemId: string) {
  const user = firebaseAuth?.currentUser;
  const normalized = String(itemId || "").trim();
  if (!user || !normalized) return { ok: false, skipped: true, newlyUnlocked: [] as string[] };
  const token = await user.getIdToken();
  const response = await fetch(`${supabaseUrl}/functions/v1/pwa-badge-activity`, {
    method: "POST",
    headers: { "content-type": "application/json", authorization: `Bearer ${token}` },
    body: JSON.stringify({ activity, itemId: normalized }),
  });
  const payload = await response.json().catch(() => ({}));
  if (!response.ok || payload.ok !== true) throw new Error(payload.error || "Não foi possível sincronizar a atividade agora.");
  return payload as { ok: true; unlockedBadgeIds: string[]; newlyUnlocked: string[] };
}

/** Registra minutos ativos reais e deduplicados enquanto a PWA está visível. */
export function startPwaActiveMinuteTracker() {
  let lastMinute = "";
  const tick = () => {
    if (document.visibilityState !== "visible" || !firebaseAuth?.currentUser) return;
    const minute = String(Math.floor(Date.now() / 60_000));
    if (minute === lastMinute) return;
    lastMinute = minute;
    void recordPwaActivity("active_minutes", minute).catch(() => undefined);
  };
  const timer = window.setInterval(tick, 60_000);
  const onVisibility = () => tick();
  document.addEventListener("visibilitychange", onVisibility);
  tick();
  return () => { window.clearInterval(timer); document.removeEventListener("visibilitychange", onVisibility); };
}
