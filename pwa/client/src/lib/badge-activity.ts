import { firebaseAuth } from "./firebase";

const supabaseUrl = import.meta.env.VITE_SUPABASE_URL || "https://cwphbkdtorfpgmnlafqb.supabase.co";

export type BadgeActivityKey = "plans" | "plan_themes" | "books" | "videos" | "bible_chapters" | "bible_news" | "devotionals" | "audios" | "active_minutes";

export const PWA_BADGE_UNLOCK_EVENT = "micrhema:pwa:badge-unlocked";

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
  const result = payload as { ok: true; unlockedBadgeIds: string[]; newlyUnlocked: string[] };
  if (result.newlyUnlocked.length && typeof window !== "undefined") {
    window.dispatchEvent(new CustomEvent(PWA_BADGE_UNLOCK_EVENT, { detail: { badgeIds: result.newlyUnlocked } }));
  }
  return result;
}

/**
 * Espelha a ideia do contador ativo do Android: um minuto só é creditado depois
 * de sessenta segundos reais com a PWA visível e com uma sessão de membro ativa.
 */
export function startPwaActiveMinuteTracker() {
  let visibleSeconds = 0;
  let lastTick = Date.now();
  let sequence = 0;
  const tick = () => {
    const now = Date.now();
    const elapsedSeconds = Math.max(0, Math.min(65, Math.floor((now - lastTick) / 1000)));
    lastTick = now;
    if (document.visibilityState !== "visible" || !firebaseAuth?.currentUser) return;
    visibleSeconds += elapsedSeconds;
    while (visibleSeconds >= 60) {
      visibleSeconds -= 60;
      const minute = `${Math.floor(now / 60_000)}:${sequence++}`;
      void recordPwaActivity("active_minutes", minute).catch(() => undefined);
    }
  };
  const timer = window.setInterval(tick, 10_000);
  const onVisibility = () => { lastTick = Date.now(); };
  document.addEventListener("visibilitychange", onVisibility);
  return () => {
    window.clearInterval(timer);
    document.removeEventListener("visibilitychange", onVisibility);
  };
}
