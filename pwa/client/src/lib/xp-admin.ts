import { firebaseAdminAuth } from "./firebase";

const supabaseUrl = import.meta.env.VITE_SUPABASE_URL || "https://cwphbkdtorfpgmnlafqb.supabase.co";
const publishableKey = import.meta.env.VITE_SUPABASE_ANON_KEY || "sb_publishable_Dv98hBnbJB2TzRCG6aJNwA_KMPHLZSw";

export type AdminXpShopItem = {
  id: string;
  name: string;
  description: string;
  cost: number;
  category: string;
  kind: string;
  imageUrl: string;
  stock: number | null;
  limitPerMember: number;
  active: boolean;
  availableFrom: string;
  availableUntil: string;
};

export type AdminXpRedemption = {
  id: string;
  memberId: string;
  memberName: string;
  itemId: string;
  itemName: string;
  cost: number;
  status: string;
  code: string;
  createdAt: string;
  deliveredAt: string;
};

export type AdminCustomBadge = {
  id: string;
  sequenceNo: number | null;
  name: string;
  description: string;
  challenge: string;
  imageRef: string;
  special: boolean;
  active: boolean;
};

export type AdminProfileCosmetic = {
  id: string;
  kind: "distintivo" | "moldura";
  name: string;
  description: string;
  challenge: string;
  imageRef: string;
  active: boolean;
  purchasable: boolean;
  xpCost: number;
  emblemIds: string[];
  freeForAll: boolean;
};

export type GeneratedBadgeChallenge = {
  difficulty: "easy" | "medium" | "hard" | string;
  text: string;
  metric: string;
  target: number;
};

export type AdminLightEffect = {
  id: string;
  name: string;
  description: string;
  effectType: string;
  tone: "suave" | "medio" | "forte";
  colorHex: string;
  purchasable: boolean;
  xpCost: number;
  emblemIds: string[];
  active: boolean;
  freeForAll: boolean;
};

export type ReleaseMode = "shop" | "free" | "emblem";

type JsonObject = Record<string, unknown>;

function requireAdminSession() {
  if (!firebaseAdminAuth?.currentUser) throw new Error("Abra novamente a Área Administrativa antes de editar a Loja XP.");
}

async function callFunction(endpoint: string, body: JsonObject, admin = true): Promise<JsonObject> {
  if (admin) requireAdminSession();
  const response = await fetch(`${supabaseUrl}/functions/v1/${endpoint}`, {
    method: "POST",
    headers: {
      "content-type": "application/json",
      apikey: publishableKey,
      authorization: `Bearer ${publishableKey}`,
    },
    body: JSON.stringify(body),
  });
  const payload = await response.json().catch(() => ({})) as JsonObject;
  if (!response.ok || payload.ok === false || payload.error) throw new Error(String(payload.error || `Falha na Loja XP (${response.status}).`));
  return payload;
}

function rows(payload: JsonObject, key: string) {
  return Array.isArray(payload[key]) ? payload[key] as JsonObject[] : [];
}

function strings(value: unknown): string[] {
  return Array.isArray(value) ? value.map(String).filter(Boolean) : [];
}

function parseShopItem(item: JsonObject): AdminXpShopItem {
  return {
    id: String(item.id || ""), name: String(item.name || ""), description: String(item.description || ""),
    cost: Math.max(0, Number(item.cost || 0)), category: String(item.category || ""), kind: String(item.kind || "digital"),
    imageUrl: String(item.image_url || ""), stock: item.stock === null || item.stock === undefined ? null : Math.max(0, Number(item.stock || 0)),
    limitPerMember: Math.max(1, Number(item.limit_per_member || 1)), active: item.active !== false,
    availableFrom: String(item.available_from || ""), availableUntil: String(item.available_until || ""),
  };
}

function parseBadge(item: JsonObject): AdminCustomBadge {
  return {
    id: String(item.id || ""), sequenceNo: item.sequence_no === null || item.sequence_no === undefined ? null : Number(item.sequence_no),
    name: String(item.name || ""), description: String(item.description || ""), challenge: String(item.challenge || ""),
    imageRef: String(item.image_ref || ""), special: item.special === true, active: item.active !== false,
  };
}

function parseCosmetic(item: JsonObject): AdminProfileCosmetic {
  return {
    id: String(item.id || ""), kind: String(item.kind || "distintivo") === "moldura" ? "moldura" : "distintivo",
    name: String(item.name || ""), description: String(item.description || ""), challenge: String(item.challenge || ""),
    imageRef: String(item.image_ref || ""), active: item.active !== false, purchasable: item.purchasable === true,
    xpCost: Math.max(0, Number(item.xp_cost || 0)), emblemIds: strings(item.emblem_ids), freeForAll: item.free_for_all === true,
  };
}

function parseEffect(item: JsonObject): AdminLightEffect {
  const tone = String(item.tone || "medio");
  return {
    id: String(item.id || ""), name: String(item.name || ""), description: String(item.description || ""), effectType: String(item.effect_type || "orbit"),
    tone: tone === "suave" || tone === "forte" ? tone : "medio", colorHex: String(item.color_hex || "#FFD54F"),
    purchasable: item.purchasable === true, xpCost: Math.max(0, Number(item.xp_cost || 0)), emblemIds: strings(item.emblem_ids),
    active: item.active !== false, freeForAll: item.free_for_all === true,
  };
}

export async function loadAdminXpCatalog() {
  return rows(await callFunction("xp-shop-admin-simple", { action: "admin_catalog" }), "items").map(parseShopItem);
}

export async function saveAdminXpItem(item: AdminXpShopItem) {
  const payload = await callFunction("xp-shop-admin-simple", {
    action: "admin_upsert_item", id: item.id, name: item.name, description: item.description, cost: item.cost,
    category: item.category, kind: item.kind, imageUrl: item.imageUrl, stock: item.stock, limitPerMember: item.limitPerMember,
    active: item.active, availableFrom: item.availableFrom, availableUntil: item.availableUntil,
  });
  return parseShopItem((payload.item || {}) as JsonObject);
}

export async function loadAdminXpRedemptions(status = "todos") {
  const payload = await callFunction("xp-shop-admin-simple", { action: "admin_redemptions", status });
  return rows(payload, "redemptions").map((item): AdminXpRedemption => ({
    id: String(item.id || ""), memberId: String(item.member_id || ""), memberName: String(item.member_name || ""),
    itemId: String(item.item_id || ""), itemName: String(item.item_name || ""), cost: Number(item.cost || 0), status: String(item.status || ""),
    code: String(item.redemption_code || ""), createdAt: String(item.created_at || ""), deliveredAt: String(item.delivered_at || ""),
  }));
}

export function setAdminXpRedemptionStatus(redemptionId: string, status: "entregue" | "cancelado") {
  return callFunction("xp-shop-admin-simple", { action: "admin_set_redemption_status", redemptionId, status });
}

export async function loadAdminBadges() {
  return rows(await callFunction("xp-shop-admin-simple", { action: "admin_badges" }), "badges").map(parseBadge);
}

export async function generateAdminBadgeChallenge(difficulty: "easy" | "medium" | "hard", exclude = "") {
  const payload = await callFunction("xp-shop-admin-simple", { action: "admin_generate_badge_challenge", difficulty, exclude });
  const item = (payload.challenge || {}) as JsonObject;
  return { difficulty: String(item.difficulty || difficulty), text: String(item.text || ""), metric: String(item.metric || ""), target: Number(item.target || 0) } as GeneratedBadgeChallenge;
}

export async function saveAdminBadge(item: AdminCustomBadge) {
  const payload = await callFunction("xp-shop-admin-simple", {
    action: "admin_upsert_badge", id: item.id, sequenceNo: item.sequenceNo, name: item.name, description: item.description,
    challenge: item.challenge, imageRef: item.imageRef, special: item.special, active: item.active,
  });
  return parseBadge((payload.badge || {}) as JsonObject);
}

export async function loadAdminCosmetics(kind: "distintivo" | "moldura") {
  return rows(await callFunction("xp-shop-admin-simple", { action: "admin_cosmetics", kind, includeBuiltins: true }), "items").map(parseCosmetic);
}

export async function saveAdminCosmetic(item: AdminProfileCosmetic) {
  const payload = await callFunction("xp-shop-admin-simple", {
    action: "admin_upsert_cosmetic", id: item.id, kind: item.kind, name: item.name, description: item.description,
    challenge: item.challenge, imageRef: item.imageRef, active: item.active, purchasable: item.purchasable,
    xpCost: item.xpCost, freeForAll: item.freeForAll, emblemIds: item.emblemIds,
  });
  return parseCosmetic((payload.item || {}) as JsonObject);
}

export async function loadAdminLightEffects() {
  return rows(await callFunction("xp-light-effects-admin", { action: "list" }), "items").map(parseEffect);
}

export async function saveAdminLightEffect(item: AdminLightEffect) {
  const payload = await callFunction("xp-light-effects-admin", {
    action: "upsert", id: item.id, name: item.name, description: item.description, effectType: item.effectType,
    tone: item.tone, colorHex: item.colorHex, purchasable: item.purchasable, xpCost: item.xpCost,
    emblemIds: item.emblemIds, active: item.active, freeForAll: item.freeForAll,
  });
  return parseEffect((payload.item || {}) as JsonObject);
}

export function deleteAdminLightEffect(id: string) {
  return callFunction("xp-light-effects-admin", { action: "delete", id });
}

export function releaseMode(item: { purchasable: boolean; freeForAll: boolean }): ReleaseMode {
  return item.purchasable ? "shop" : item.freeForAll ? "free" : "emblem";
}

export function applyReleaseMode<T extends { purchasable: boolean; freeForAll: boolean; xpCost: number; emblemIds: string[] }>(item: T, mode: ReleaseMode): T {
  return {
    ...item,
    purchasable: mode === "shop",
    freeForAll: mode === "free",
    xpCost: mode === "shop" ? Math.max(1, item.xpCost || 1) : 0,
    emblemIds: mode === "emblem" ? item.emblemIds : [],
  };
}

export function buildAdminXpAssetRef(type: "image" | "video" | "audio" | "emblem" | "pdf" | "frame" | "badge", storagePath: string) {
  const raw = storagePath.trim().replace(/^media-assets\//i, "").replace(/^\/+/, "");
  if (!raw || raw.includes("..")) throw new Error("Caminho do arquivo enviado é inválido.");
  return `micrhema-xp://${type}/media-assets/${raw}`;
}

export function isBuiltinCosmeticId(id: string) {
  return id === "moldura_luz_promessa" || id === "badge_leitor_palavra";
}
