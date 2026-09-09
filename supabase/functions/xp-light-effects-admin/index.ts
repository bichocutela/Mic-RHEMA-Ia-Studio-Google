import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "npm:@supabase/supabase-js@2.57.4";

const TONES = new Set(["suave", "medio", "forte"]);
const SHOP_CATEGORY = "Efeitos de Luz";
const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-client-info",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Content-Type": "application/json; charset=utf-8",
  "Cache-Control": "no-store",
};
const json = (body: Record<string, unknown>, status = 200) => new Response(JSON.stringify(body), { status, headers: cors });
const clean = (value: unknown, max = 1000) => String(value ?? "").trim().slice(0, max);

type LightEffectRow = {
  id: string;
  name: string;
  description: string;
  effect_type: string;
  tone: string;
  color_hex: string;
  purchasable: boolean;
  xp_cost: number;
  emblem_ids: string[];
  active: boolean;
};

async function syncEffectToShop(sb: any, item: LightEffectRow) {
  const shouldSell = item.active === true && item.purchasable === true && Number(item.xp_cost) > 0;
  if (!shouldSell) {
    const { error } = await sb.from("xp_shop_items")
      .update({ active: false, updated_at: new Date().toISOString() })
      .eq("id", item.id)
      .eq("category", SHOP_CATEGORY);
    if (error) throw error;
    return;
  }

  const row = {
    id: item.id,
    name: item.name,
    description: item.description,
    cost: Math.floor(Number(item.xp_cost)),
    category: SHOP_CATEGORY,
    kind: "profile",
    image_url: "",
    stock: null,
    limit_per_member: 1,
    active: true,
    available_from: null,
    available_until: null,
    updated_at: new Date().toISOString(),
  };
  const { error } = await sb.from("xp_shop_items").upsert(row, { onConflict: "id" });
  if (error) throw error;
}

async function syncCatalogToShop(sb: any, items: LightEffectRow[]) {
  await Promise.all(items.map((item) => syncEffectToShop(sb, item)));
}

Deno.serve(async (request) => {
  if (request.method === "OPTIONS") return new Response("ok", { headers: cors });
  if (request.method !== "POST") return json({ error: "Método não permitido." }, 405);

  try {
    const input = await request.json() as Record<string, unknown>;
    const action = clean(input.action, 30).toLowerCase();
    const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
    const serviceRole = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
    if (!supabaseUrl || !serviceRole) throw new Error("Backend dos efeitos de luz não configurado.");
    const sb = createClient(supabaseUrl, serviceRole, { auth: { persistSession: false, autoRefreshToken: false } });

    if (action === "list") {
      const { data, error } = await sb.from("profile_light_effects")
        .select("id,name,description,effect_type,tone,color_hex,purchasable,xp_cost,emblem_ids,active,created_at,updated_at")
        .order("created_at", { ascending: true });
      if (error) throw error;
      const items = (data ?? []) as LightEffectRow[];
      await syncCatalogToShop(sb, items);
      return json({ ok: true, items });
    }

    if (action === "upsert") {
      const id = clean(input.id, 90).toLowerCase().replace(/[^a-z0-9_:-]+/g, "_").replace(/^_+|_+$/g, "");
      const name = clean(input.name, 120);
      const description = clean(input.description, 1200);
      const effectType = clean(input.effectType, 50).toLowerCase();
      const tone = clean(input.tone, 20).toLowerCase();
      const colorHex = clean(input.colorHex, 20).toUpperCase();
      const purchasable = input.purchasable === true;
      const xpCost = Math.max(0, Math.floor(Number(input.xpCost ?? 0)));
      const active = input.active !== false;
      const emblemIds = Array.isArray(input.emblemIds)
        ? input.emblemIds.map((value) => clean(value, 90)).filter(Boolean).slice(0, 300)
        : [];

      if (!id) return json({ error: "Identificador inválido." }, 400);
      if (!name) return json({ error: "Informe o nome do efeito." }, 400);
      if (!effectType) return json({ error: "Informe o tipo do efeito." }, 400);
      if (!TONES.has(tone)) return json({ error: "Tonalidade inválida." }, 400);
      if (!/^#[0-9A-F]{6}$/.test(colorHex)) return json({ error: "Cor inválida. Use #RRGGBB." }, 400);
      if (purchasable && xpCost <= 0) return json({ error: "Informe um valor em XP maior que zero." }, 400);

      const row = {
        id,
        name,
        description,
        effect_type: effectType,
        tone,
        color_hex: colorHex,
        purchasable,
        xp_cost: purchasable ? xpCost : 0,
        emblem_ids: emblemIds,
        active,
        updated_at: new Date().toISOString(),
      };
      const { data, error } = await sb.from("profile_light_effects").upsert(row, { onConflict: "id" }).select().single();
      if (error) throw error;
      await syncEffectToShop(sb, data as LightEffectRow);
      return json({ ok: true, item: data });
    }

    if (action === "delete") {
      const id = clean(input.id, 90).toLowerCase().replace(/[^a-z0-9_:-]+/g, "_").replace(/^_+|_+$/g, "");
      if (!id) return json({ error: "Identificador inválido." }, 400);
      const { error } = await sb.from("profile_light_effects").delete().eq("id", id);
      if (error) throw error;
      const { error: shopError } = await sb.from("xp_shop_items")
        .update({ active: false, updated_at: new Date().toISOString() })
        .eq("id", id)
        .eq("category", SHOP_CATEGORY);
      if (shopError) throw shopError;
      return json({ ok: true });
    }

    return json({ error: "Ação inválida." }, 400);
  } catch (error) {
    console.error("xp-light-effects-admin failed", error);
    return json({ error: error instanceof Error ? error.message : "Falha na sincronização dos efeitos de luz." }, 500);
  }
});
