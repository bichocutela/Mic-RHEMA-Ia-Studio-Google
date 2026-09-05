import { firebaseAuth } from "./firebase";
import { awardPwaXp } from "./xp";

const supabaseUrl = import.meta.env.VITE_SUPABASE_URL || "https://cwphbkdtorfpgmnlafqb.supabase.co";

export type BadgeActivityKey = "plans" | "plan_themes" | "books" | "videos" | "bible_chapters" | "bible_news" | "devotionals" | "audios" | "active_minutes";

export const PWA_BADGE_UNLOCK_EVENT = "micrhema:pwa:badge-unlocked";

type BadgeActivityResult = { ok: true; unlockedBadgeIds: string[]; newlyUnlocked: string[] };
const VERIFIED_ONLY = new Set<BadgeActivityKey>(["books", "videos", "devotionals", "audios"]);

function announceBadgeUnlock(result: BadgeActivityResult) {
  if (result.newlyUnlocked.length && typeof window !== "undefined") {
    window.dispatchEvent(new CustomEvent(PWA_BADGE_UNLOCK_EVENT, { detail: { badgeIds: result.newlyUnlocked } }));
  }
  return result;
}

async function postBadgeProgress(body: Record<string, unknown>) {
  const user = firebaseAuth?.currentUser;
  if (!user) return { ok: false, skipped: true, newlyUnlocked: [] as string[] };
  const token = await user.getIdToken();
  const response = await fetch(`${supabaseUrl}/functions/v1/pwa-badge-activity`, {
    method: "POST",
    headers: { "content-type": "application/json", authorization: `Bearer ${token}` },
    body: JSON.stringify(body),
  });
  const payload = await response.json().catch(() => ({}));
  if (!response.ok || payload.ok !== true) throw new Error(payload.error || "Não foi possível sincronizar o progresso agora.");
  return announceBadgeUnlock(payload as BadgeActivityResult);
}

export async function recordPwaActivity(activity: BadgeActivityKey, itemId: string) {
  const normalized = String(itemId || "").trim();
  if (!normalized || VERIFIED_ONLY.has(activity)) return { ok: false, skipped: true, newlyUnlocked: [] as string[] };
  return postBadgeProgress({ activity, itemId: normalized });
}

/** Conteúdos protegidos só entram no perfil após o ledger/servidor confirmar uso real. */
export async function recordVerifiedPwaActivity(activity: BadgeActivityKey, itemId: string) {
  const normalized = String(itemId || "").trim();
  if (!normalized) return { ok: false, skipped: true, newlyUnlocked: [] as string[] };
  return postBadgeProgress({ activity, itemId: normalized });
}

/** Recalcula níveis sem criar uma atividade artificial, usado após progresso do IBR. */
export async function reconcilePwaBadges() {
  return postBadgeProgress({ reconcile: true });
}

function recifeDate() {
  return new Intl.DateTimeFormat("en-CA", { timeZone: "America/Recife", year: "numeric", month: "2-digit", day: "2-digit" }).format(new Date());
}

/**
 * Espelha o contador ativo do Android. Cada minuto visível continua alimentando
 * missões/emblemas; a cada cinco minutos reais a PWA envia um receipt persistente
 * ao mesmo ledger central, preservando o limite diário de +20 XP no backend.
 */
export function startPwaActiveMinuteTracker() {
  let visibleSeconds = 0;
  let lastTick = Date.now();
  let localSequence = 0;

  const activeState = () => {
    const uid = firebaseAuth?.currentUser?.uid || "guest";
    const key = `micrhema:pwa:xp-active:${uid}`;
    const today = recifeDate();
    try {
      const parsed = JSON.parse(localStorage.getItem(key) || "null") as { date?: string; remainder?: number; sequence?: number } | null;
      if (parsed?.date === today) return { key, date: today, remainder: Math.max(0, Number(parsed.remainder) || 0), sequence: Math.max(0, Number(parsed.sequence) || 0) };
    } catch { /* reinicia o contador persistente */ }
    return { key, date: today, remainder: 0, sequence: 0 };
  };

  const tick = () => {
    const now = Date.now();
    const elapsedSeconds = Math.max(0, Math.min(65, Math.floor((now - lastTick) / 1000)));
    lastTick = now;
    if (document.visibilityState !== "visible" || !firebaseAuth?.currentUser) return;
    visibleSeconds += elapsedSeconds;
    while (visibleSeconds >= 60) {
      visibleSeconds -= 60;
      const minute = `${Math.floor(now / 60_000)}:${localSequence++}`;
      void recordPwaActivity("active_minutes", minute).catch(() => undefined);

      const state = activeState();
      state.remainder += 1;
      while (state.remainder >= 5) {
        state.remainder -= 5;
        state.sequence += 1;
        const receipt = `${state.date}:${state.sequence}`;
        void awardPwaXp("active_5min", receipt).catch(() => undefined);
      }
      localStorage.setItem(state.key, JSON.stringify({ date: state.date, remainder: state.remainder, sequence: state.sequence }));
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
