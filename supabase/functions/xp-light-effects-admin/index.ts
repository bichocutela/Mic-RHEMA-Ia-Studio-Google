import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "npm:@supabase/supabase-js@2.57.4";

const TONES = new Set(["suave", "medio", "forte"]);
const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-client-info",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Content-Type": "application/json; charset=utf-8",
  "Cache-Control": "no-store",
};
const json = (body: Record<string, unknown>, status = 200) => new Response(JSON.stringify(body), { status, headers: cors });
const clean = (value: unknown, max = 1000) => String(value ?? "").trim().slice(0, max);

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
      return json({ ok: true, items: data ?? [] });
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
      return json({ ok: true, item: data });
    }

    if (action === "delete") {
      const id = clean(input.id, 90).toLowerCase().replace(/[^a-z0-9_:-]+/g, "_").replace(/^_+|_+$/g, "");
      if (!id) return json({ error: "Identificador inválido." }, 400);
      const { error } = await sb.from("profile_light_effects").delete().eq("id", id);
      if (error) throw error;
      return json({ ok: true });
    }

    return json({ error: "Ação inválida." }, 400);
  } catch (error) {
    console.error("xp-light-effects-admin failed", error);
    return json({ error: error instanceof Error ? error.message : "Falha na sincronização dos efeitos de luz." }, 500);
  }
});
