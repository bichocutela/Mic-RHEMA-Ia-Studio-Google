import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "npm:@supabase/supabase-js@2.57.4";

const ADMIN_PASSWORD = Deno.env.get("RHEMA_ADMIN_PASSWORD") || "igreja10";
const ITEM_KINDS = new Set(["digital", "profile", "physical"]);
const REDEMPTION_STATUSES = new Set(["pendente", "entregue", "cancelado"]);
const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-client-info, x-rhema-admin-password",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Content-Type": "application/json; charset=utf-8",
  "Cache-Control": "no-store",
};
const json = (body: Record<string, unknown>, status = 200) => new Response(JSON.stringify(body), { status, headers: cors });
const clean = (value: unknown, max = 1000) => String(value ?? "").trim().slice(0, max);
function parseNullableDate(value: unknown): string | null {
  const text = clean(value, 80);
  if (!text) return null;
  const time = Date.parse(text);
  if (!Number.isFinite(time)) throw new Error("Data da recompensa inválida.");
  return new Date(time).toISOString();
}

Deno.serve(async (request) => {
  if (request.method === "OPTIONS") return new Response("ok", { headers: cors });
  if (request.method !== "POST") return json({ error: "Método não permitido." }, 405);
  try {
    if ((request.headers.get("x-rhema-admin-password") ?? "") !== ADMIN_PASSWORD) {
      return json({ error: "Acesso administrativo obrigatório." }, 403);
    }
    const input = await request.json() as Record<string, unknown>;
    const action = clean(input.action, 60);
    const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
    const serviceRole = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
    if (!supabaseUrl || !serviceRole) throw new Error("Backend da Loja XP não configurado.");
    const sb = createClient(supabaseUrl, serviceRole, { auth: { persistSession: false, autoRefreshToken: false } });

    if (action === "admin_catalog") {
      const { data, error } = await sb.from("xp_shop_items")
        .select("id,name,description,cost,category,kind,image_url,stock,limit_per_member,active,available_from,available_until,created_at,updated_at")
        .order("active", { ascending: false }).order("cost", { ascending: true });
      if (error) throw error;
      return json({ ok: true, items: data ?? [] });
    }

    if (action === "admin_upsert_item") {
      const rawId = clean(input.id, 80).toLowerCase();
      const id = rawId.replace(/[^a-z0-9_:-]+/g, "_").replace(/^_+|_+$/g, "");
      const name = clean(input.name, 120);
      const description = clean(input.description, 1500);
      const category = clean(input.category || "Recompensas", 80);
      const kind = clean(input.kind || "digital", 20);
      const imageUrl = clean(input.imageUrl, 1000);
      const cost = Math.floor(Number(input.cost ?? 0));
      const limitPerMember = Math.floor(Number(input.limitPerMember ?? 1));
      const stockValue = input.stock;
      const stock = stockValue === null || stockValue === undefined || clean(stockValue, 40) === "" ? null : Math.floor(Number(stockValue));
      const active = input.active !== false;
      const availableFrom = parseNullableDate(input.availableFrom);
      const availableUntil = parseNullableDate(input.availableUntil);

      if (!id || id.length < 3) return json({ error: "Informe um identificador válido para a recompensa." }, 400);
      if (!name) return json({ error: "Informe o nome da recompensa." }, 400);
      if (!Number.isFinite(cost) || cost <= 0) return json({ error: "O preço em XP deve ser maior que zero." }, 400);
      if (!ITEM_KINDS.has(kind)) return json({ error: "Tipo de recompensa inválido." }, 400);
      if (!Number.isFinite(limitPerMember) || limitPerMember < 1) return json({ error: "Limite por membro inválido." }, 400);
      if (kind !== "physical" && limitPerMember !== 1) return json({ error: "Recompensas digitais e de perfil só podem ser resgatadas uma vez por membro." }, 400);
      if (stock !== null && (!Number.isFinite(stock) || stock < 0)) return json({ error: "Estoque inválido." }, 400);
      if (availableFrom && availableUntil && Date.parse(availableUntil) <= Date.parse(availableFrom)) {
        return json({ error: "A data final precisa ser posterior à data inicial." }, 400);
      }

      const row = {
        id, name, description, cost, category, kind,
        image_url: imageUrl, stock, limit_per_member: limitPerMember, active,
        available_from: availableFrom, available_until: availableUntil,
        updated_at: new Date().toISOString(),
      };
      const { data, error } = await sb.from("xp_shop_items").upsert(row, { onConflict: "id" }).select().single();
      if (error) throw error;
      return json({ ok: true, item: data });
    }

    if (action === "admin_badges") {
      const { data, error } = await sb.from("custom_profile_badges")
        .select("id,sequence_no,name,description,challenge,image_ref,special,active,created_at,updated_at")
        .order("special", { ascending: true })
        .order("sequence_no", { ascending: true, nullsFirst: false })
        .order("created_at", { ascending: true });
      if (error) throw error;
      return json({ ok: true, badges: data ?? [] });
    }

    if (action === "admin_upsert_badge") {
      const name = clean(input.name, 120);
      const description = clean(input.description, 1200);
      const challenge = clean(input.challenge, 1500);
      const imageRef = clean(input.imageRef, 1200);
      const special = input.special === true;
      const active = input.active !== false;
      if (!name) return json({ error: "Informe o nome do emblema." }, 400);
      if (!challenge) return json({ error: "Informe o desafio para conquistar o emblema." }, 400);
      if (!imageRef || !imageRef.toLowerCase().startsWith("micrhema-xp://emblem/")) {
        return json({ error: "Envie um emblema PNG antes de salvar." }, 400);
      }

      let sequenceNo: number | null = null;
      if (!special) {
        const { data: lastRows, error: lastError } = await sb.from("custom_profile_badges")
          .select("sequence_no").not("sequence_no", "is", null).order("sequence_no", { ascending: false }).limit(1);
        if (lastError) throw lastError;
        const last = Number(lastRows?.[0]?.sequence_no ?? 22);
        sequenceNo = Math.max(23, Number.isFinite(last) ? last + 1 : 23);
      }

      const suppliedId = clean(input.id, 90).toLowerCase().replace(/[^a-z0-9_:-]+/g, "_").replace(/^_+|_+$/g, "");
      const generatedId = special
        ? `special_${name.toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/[^a-z0-9]+/g, "_").replace(/^_+|_+$/g, "").slice(0, 50)}_${Date.now()}`
        : `custom_level_${sequenceNo}`;
      const id = suppliedId || generatedId;

      const row = {
        id,
        sequence_no: special ? null : sequenceNo,
        name,
        description,
        challenge,
        image_ref: imageRef,
        special,
        active,
        updated_at: new Date().toISOString(),
      };
      const { data, error } = await sb.from("custom_profile_badges").upsert(row, { onConflict: "id" }).select().single();
      if (error) throw error;
      return json({ ok: true, badge: data });
    }

    if (action === "admin_redemptions") {
      const status = clean(input.status, 30);
      let query = sb.from("xp_redemptions")
        .select("id,member_id,member_name,item_id,item_name,cost,status,redemption_code,created_at,delivered_at,stock_consumed")
        .order("created_at", { ascending: false }).limit(200);
      if (status && status !== "todos") query = query.eq("status", status);
      const { data, error } = await query;
      if (error) throw error;
      return json({ ok: true, redemptions: data ?? [] });
    }

    if (action === "admin_set_redemption_status") {
      const redemptionId = clean(input.redemptionId, 80);
      const status = clean(input.status, 30);
      if (!redemptionId || !REDEMPTION_STATUSES.has(status)) return json({ error: "Alteração de resgate inválida." }, 400);
      const { data, error } = await sb.rpc("xp_admin_update_redemption", { p_redemption_id: redemptionId, p_status: status });
      if (error) throw error;
      const row = Array.isArray(data) ? data[0] : data;
      if (!row) throw new Error("O resgate não retornou resultado.");
      return json({ ok: true, redemption: row });
    }

    return json({ error: "Ação inválida." }, 400);
  } catch (error) {
    console.error("xp-shop-admin-simple failed", error);
    return json({ error: error instanceof Error ? error.message : "Falha na administração da Loja XP." }, 500);
  }
});
