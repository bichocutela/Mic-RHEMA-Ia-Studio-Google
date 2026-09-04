export type AcceptedDate = Date | null;

export function parseAppDate(raw?: string): AcceptedDate {
  const value = String(raw || "").trim();
  if (!value) return null;
  let match = value.match(/^(\d{4})-(\d{2})-(\d{2})$/);
  if (match) return validDate(Number(match[1]), Number(match[2]), Number(match[3]));
  match = value.match(/^(\d{2})[\/-](\d{2})[\/-](\d{4})$/);
  if (match) return validDate(Number(match[3]), Number(match[2]), Number(match[1]));
  return null;
}

function validDate(year: number, month: number, day: number) {
  const date = new Date(year, month - 1, day, 0, 0, 0, 0);
  return date.getFullYear() === year && date.getMonth() === month - 1 && date.getDate() === day ? date : null;
}

export function appDateKey(raw?: string) {
  const date = parseAppDate(raw);
  if (!date) return "";
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}

export function formatAppDate(raw?: string) {
  const date = parseAppDate(raw);
  return date ? date.toLocaleDateString("pt-BR") : String(raw || "");
}

export function todayKey(date = new Date()) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}

export function contentTimestamp(item: Record<string, unknown>) {
  for (const key of ["publishedAt", "createdAt", "timestamp", "updatedAt"]) {
    const value = Number(item[key] || 0);
    if (Number.isFinite(value) && value > 0) return value;
  }
  const numericId = Number(item.id || 0);
  return Number.isFinite(numericId) ? numericId : 0;
}

export function youtubeVideoId(value?: string) {
  const raw = String(value || "").trim();
  if (/^[A-Za-z0-9_-]{11}$/.test(raw)) return raw;
  if (!raw) return "";
  try {
    const url = new URL(raw);
    const host = url.hostname.replace(/^www\./, "").toLowerCase();
    const candidate = host === "youtu.be"
      ? url.pathname.split("/")[1]
      : host.endsWith("youtube.com")
        ? url.searchParams.get("v") || url.pathname.match(/^\/(?:embed|shorts|live)\/([^/?]+)/)?.[1]
        : "";
    return candidate && /^[A-Za-z0-9_-]{11}$/.test(candidate) ? candidate : "";
  } catch { return ""; }
}

export function youtubeThumbnail(value?: string) {
  const id = youtubeVideoId(value);
  return id ? `https://i.ytimg.com/vi/${id}/hqdefault.jpg` : "";
}

/**
 * Resolve formatos de imagem usados historicamente pelo Android/PWA para uma URL exibível no navegador.
 * - links assinados antigos do bucket público media-assets viram URLs públicas permanentes;
 * - storage_path do media-assets vira URL pública;
 * - links compartilhados/download do Google Drive viram thumbnail própria para <img>;
 * - gs:// antigo é convertido para o endpoint HTTP do Firebase Storage.
 */
export function resolveDisplayImageUrl(value?: string) {
  const raw = String(value || "").trim();
  if (!raw) return "";

  if (raw.startsWith("media-assets/")) {
    return `https://cwphbkdtorfpgmnlafqb.supabase.co/storage/v1/object/public/${raw}`;
  }

  const gsMatch = raw.match(/^gs:\/\/([^/]+)\/(.+)$/i);
  if (gsMatch) {
    return `https://firebasestorage.googleapis.com/v0/b/${encodeURIComponent(gsMatch[1])}/o/${encodeURIComponent(gsMatch[2])}?alt=media`;
  }

  try {
    const url = new URL(raw);
    const host = url.hostname.replace(/^www\./, "").toLowerCase();

    const signedMediaMarker = "/storage/v1/object/sign/media-assets/";
    const authenticatedMediaMarker = "/storage/v1/object/authenticated/media-assets/";
    const marker = url.pathname.includes(signedMediaMarker)
      ? signedMediaMarker
      : url.pathname.includes(authenticatedMediaMarker)
        ? authenticatedMediaMarker
        : "";
    if (marker) {
      const objectPath = url.pathname.slice(url.pathname.indexOf(marker) + marker.length);
      return `${url.origin}/storage/v1/object/public/media-assets/${objectPath}`;
    }

    if (host === "drive.google.com" || host === "docs.google.com") {
      const fileId = url.pathname.match(/\/d\/([A-Za-z0-9_-]+)/)?.[1] || url.searchParams.get("id") || "";
      if (fileId) return `https://drive.google.com/thumbnail?id=${encodeURIComponent(fileId)}&sz=w2000`;
    }

    return url.protocol === "http:" || url.protocol === "https:" ? url.toString() : "";
  } catch {
    return "";
  }
}

export function normalizeSearch(value: string) {
  return value.normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLowerCase().replace(/\s+/g, " ").trim();
}

export function stableSessionShuffle<T extends { id: string }>(items: T[], storageKey: string, limit = items.length): T[] {
  const ids = items.map((item) => item.id);
  try {
    const existing = JSON.parse(sessionStorage.getItem(storageKey) || "[]") as string[];
    if (Array.isArray(existing)) {
      const ordered = existing.map((id) => items.find((item) => item.id === id)).filter(Boolean) as T[];
      const missing = items.filter((item) => !existing.includes(item.id));
      if (ordered.length && missing.length === 0) return ordered.slice(0, limit);
    }
  } catch { /* gera novamente abaixo */ }
  const shuffled = items.slice();
  for (let index = shuffled.length - 1; index > 0; index--) {
    const swap = Math.floor(Math.random() * (index + 1));
    [shuffled[index], shuffled[swap]] = [shuffled[swap], shuffled[index]];
  }
  sessionStorage.setItem(storageKey, JSON.stringify(shuffled.map((item) => item.id)));
  return shuffled.slice(0, limit);
}

export function safeFilename(value: string, fallback = "mic-rhema") {
  const cleaned = normalizeSearch(value).replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");
  return cleaned || fallback;
}