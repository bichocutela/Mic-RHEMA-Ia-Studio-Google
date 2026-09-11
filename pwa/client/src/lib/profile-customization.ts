import { doc, getDoc, serverTimestamp, setDoc } from "firebase/firestore";
import { firestore } from "./firebase";

const supabaseUrl = import.meta.env.VITE_SUPABASE_URL || "https://cwphbkdtorfpgmnlafqb.supabase.co";
const publishableKey = import.meta.env.VITE_SUPABASE_ANON_KEY || "sb_publishable_Dv98hBnbJB2TzRCG6aJNwA_KMPHLZSw";

export type PwaProfileCosmetic = {
  id: string;
  kind: "distintivo" | "moldura";
  name: string;
  description: string;
  challenge: string;
  image_ref: string;
  active: boolean;
  purchasable: boolean;
  xp_cost: number;
  emblem_ids: string[];
  free_for_all: boolean;
};

export type PwaLightEffect = {
  id: string;
  name: string;
  description: string;
  effect_type: string;
  tone: "suave" | "medio" | "forte" | string;
  color_hex: string;
  purchasable: boolean;
  xp_cost: number;
  emblem_ids: string[];
  active: boolean;
  free_for_all: boolean;
};

export type PwaProfileSelections = {
  primaryDistinctiveId: string;
  featuredDistinctiveIds: string[];
  selectedProfileFrameId: string;
  selectedProfileEffectId: string;
};

export type PwaCustomizationCatalog = {
  distinctives: PwaProfileCosmetic[];
  frames: PwaProfileCosmetic[];
  effects: PwaLightEffect[];
};

type FunctionPayload = { ok?: boolean; error?: string; items?: unknown[] };
let catalogCache: { value: PwaCustomizationCatalog; at: number } | null = null;

async function publicFunction(endpoint: string, body: Record<string, unknown>): Promise<FunctionPayload> {
  const response = await fetch(`${supabaseUrl}/functions/v1/${endpoint}`, {
    method: "POST",
    headers: {
      "content-type": "application/json",
      apikey: publishableKey,
      authorization: `Bearer ${publishableKey}`,
    },
    body: JSON.stringify(body),
  });
  const payload = await response.json().catch(() => ({})) as FunctionPayload;
  if (!response.ok || payload.ok !== true) throw new Error(payload.error || "Não foi possível carregar as personalizações agora.");
  return payload;
}

function stringList(value: unknown): string[] {
  return Array.isArray(value) ? value.map(String).filter(Boolean) : [];
}

function parseCosmetic(value: unknown): PwaProfileCosmetic | null {
  if (!value || typeof value !== "object") return null;
  const item = value as Record<string, unknown>;
  const kind = String(item.kind || "");
  if (kind !== "distintivo" && kind !== "moldura") return null;
  return {
    id: String(item.id || ""),
    kind,
    name: String(item.name || ""),
    description: String(item.description || ""),
    challenge: String(item.challenge || ""),
    image_ref: String(item.image_ref || ""),
    active: item.active !== false,
    purchasable: item.purchasable === true,
    xp_cost: Math.max(0, Number(item.xp_cost || 0)),
    emblem_ids: stringList(item.emblem_ids),
    free_for_all: item.free_for_all === true,
  };
}

function parseEffect(value: unknown): PwaLightEffect | null {
  if (!value || typeof value !== "object") return null;
  const item = value as Record<string, unknown>;
  const id = String(item.id || "");
  if (!id) return null;
  return {
    id,
    name: String(item.name || ""),
    description: String(item.description || ""),
    effect_type: String(item.effect_type || "orbit"),
    tone: String(item.tone || "medio"),
    color_hex: String(item.color_hex || "#FFD54F"),
    purchasable: item.purchasable === true,
    xp_cost: Math.max(0, Number(item.xp_cost || 0)),
    emblem_ids: stringList(item.emblem_ids),
    active: item.active !== false,
    free_for_all: item.free_for_all === true,
  };
}

export async function loadPwaCustomizationCatalog(force = false): Promise<PwaCustomizationCatalog> {
  if (!force && catalogCache && Date.now() - catalogCache.at < 60_000) return catalogCache.value;
  const [distinctivePayload, framePayload, effectPayload] = await Promise.all([
    publicFunction("xp-shop-admin-simple", { action: "cosmetics_catalog", kind: "distintivo", includeBuiltins: true }),
    publicFunction("xp-shop-admin-simple", { action: "cosmetics_catalog", kind: "moldura", includeBuiltins: true }),
    publicFunction("xp-light-effects-admin", { action: "list" }),
  ]);
  const value: PwaCustomizationCatalog = {
    distinctives: (distinctivePayload.items || []).map(parseCosmetic).filter((item): item is PwaProfileCosmetic => Boolean(item)),
    frames: (framePayload.items || []).map(parseCosmetic).filter((item): item is PwaProfileCosmetic => Boolean(item)),
    effects: (effectPayload.items || []).map(parseEffect).filter((item): item is PwaLightEffect => Boolean(item)),
  };
  catalogCache = { value, at: Date.now() };
  return value;
}

export function invalidatePwaCustomizationCatalog() {
  catalogCache = null;
}

export async function loadPwaProfileSelections(memberId: string): Promise<PwaProfileSelections> {
  if (!firestore || !memberId) return emptyPwaProfileSelections();
  const snapshot = await getDoc(doc(firestore, "users", memberId));
  const value = snapshot.exists() ? snapshot.data() : {};
  return {
    primaryDistinctiveId: typeof value.primaryDistinctiveId === "string" ? value.primaryDistinctiveId : "",
    featuredDistinctiveIds: stringList(value.featuredDistinctiveIds).slice(0, 4),
    selectedProfileFrameId: typeof value.selectedProfileFrameId === "string" ? value.selectedProfileFrameId : "",
    selectedProfileEffectId: typeof value.selectedProfileEffectId === "string" ? value.selectedProfileEffectId : "",
  };
}

export async function savePwaProfileSelections(memberId: string, value: PwaProfileSelections) {
  if (!firestore || !memberId) throw new Error("Entre novamente para salvar a personalização.");
  await setDoc(doc(firestore, "users", memberId), {
    primaryDistinctiveId: value.primaryDistinctiveId,
    featuredDistinctiveIds: value.featuredDistinctiveIds.filter(Boolean).filter((id, index, all) => all.indexOf(id) === index).slice(0, 4),
    selectedProfileFrameId: value.selectedProfileFrameId,
    selectedProfileEffectId: value.selectedProfileEffectId,
    updatedAt: Date.now(),
    updatedAtServer: serverTimestamp(),
  }, { merge: true });
}

export function emptyPwaProfileSelections(): PwaProfileSelections {
  return { primaryDistinctiveId: "", featuredDistinctiveIds: [], selectedProfileFrameId: "", selectedProfileEffectId: "" };
}

export function cosmeticRewardId(item: PwaProfileCosmetic) {
  return item.id === "moldura_luz_promessa" || item.id === "badge_leitor_palavra" ? item.id : `cosmetic:${item.id}`;
}

export function availablePwaCosmetics(
  items: PwaProfileCosmetic[],
  kind: "distintivo" | "moldura",
  badgeId: string,
  ownedIds: Set<string>,
) {
  return items.filter((item) => item.active && item.kind === kind && (() => {
    if (item.purchasable) return ownedIds.has(cosmeticRewardId(item)) && (!item.emblem_ids.length || item.emblem_ids.includes(badgeId));
    if (item.free_for_all) return true;
    return item.emblem_ids.includes(badgeId);
  })());
}

export function availablePwaEffects(items: PwaLightEffect[], badgeId: string, ownedIds: Set<string>) {
  return items.filter((item) => item.active && (() => {
    if (item.purchasable) return ownedIds.has(item.id) && (!item.emblem_ids.length || item.emblem_ids.includes(badgeId));
    if (item.free_for_all) return true;
    return item.emblem_ids.includes(badgeId);
  })());
}

export function normalizePwaSelections(
  value: PwaProfileSelections,
  distinctives: PwaProfileCosmetic[],
  frames: PwaProfileCosmetic[],
  effects: PwaLightEffect[],
): PwaProfileSelections {
  const distinctiveIds = new Set(distinctives.map((item) => item.id));
  const frameIds = new Set(frames.map((item) => item.id));
  const effectIds = new Set(effects.map((item) => item.id));
  return {
    primaryDistinctiveId: distinctiveIds.has(value.primaryDistinctiveId) ? value.primaryDistinctiveId : "",
    featuredDistinctiveIds: value.featuredDistinctiveIds.filter((id) => distinctiveIds.has(id)).filter((id, index, all) => all.indexOf(id) === index).slice(0, 4),
    selectedProfileFrameId: frameIds.has(value.selectedProfileFrameId) ? value.selectedProfileFrameId : "",
    selectedProfileEffectId: effectIds.has(value.selectedProfileEffectId) ? value.selectedProfileEffectId : "",
  };
}

export function resolvePwaXpAssetUrl(value: string) {
  const raw = String(value || "").trim();
  if (!raw || raw.startsWith("builtin-emblem://")) return "";
  if (!raw.toLowerCase().startsWith("micrhema-xp://")) return raw;
  const rest = raw.slice("micrhema-xp://".length);
  const parts = rest.split("/").filter(Boolean);
  if (parts.length < 3) return "";
  const bucket = parts[1];
  let objectPath = parts.slice(2).join("/");
  if (objectPath.startsWith(`${bucket}/`)) objectPath = objectPath.slice(bucket.length + 1);
  if (!objectPath || objectPath.includes("..")) return "";
  return `${supabaseUrl}/storage/v1/object/public/${bucket}/${objectPath.split("/").map(encodeURIComponent).join("/")}`;
}
