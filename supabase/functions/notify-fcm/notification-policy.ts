export function notificationSlot(data: Record<string, string>): number | null {
  const category = (data.category || "").toLowerCase();
  const collection = data.collection || "";
  if (collection === "devocionais" || /devotional|devocional/.test(category)) return 8;
  if (collection === "bible_news" || /^(news|daily_news)$/.test(category)) return 12;
  if (collection === "cultos_agenda" || /^(service|next_service)$/.test(category)) return 10;
  if (collection === "discipulado_pdfs" || category === "discipulado") return 14;
  if (category === "app_update") return 18;
  if (collection === "ibr_courses" || /ibr|course|lesson/.test(category)) return 19;
  if (collection.startsWith("conteudos_") || /^(media|sermons|audio|video)$/.test(category)) return 15;
  if (collection === "events" || /^(events|event|content_updates)$/.test(category)) return 17;
  return null;
}

export function fortalezaClock(now = new Date()) {
  const parts = new Intl.DateTimeFormat("en-CA", { timeZone: "America/Fortaleza", year: "numeric", month: "2-digit", day: "2-digit", hour: "2-digit", hourCycle: "h23" }).formatToParts(now);
  const read = (key: string) => parts.find(p => p.type === key)?.value || "00";
  return { date: `${read("year")}-${read("month")}-${read("day")}`, hour: Number(read("hour")) };
}

// Creation of devotional documents is synchronization, never a second reminder.
export function isDailySlot(slot: number | null) { return slot === 8 || slot === 10 || slot === 12; }

export async function eventDigest(value: string) {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value));
  return Array.from(new Uint8Array(digest), b => b.toString(16).padStart(2, "0")).join("");
}
