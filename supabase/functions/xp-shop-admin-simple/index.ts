import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "npm:@supabase/supabase-js@2.57.4";

const ADMIN_PASSWORD = Deno.env.get("RHEMA_ADMIN_PASSWORD") || "igreja10";
const ITEM_KINDS = new Set(["digital", "profile", "physical"]);
const REDEMPTION_STATUSES = new Set(["pendente", "entregue", "cancelado"]);
const BADGE_DIFFICULTIES = new Set(["easy", "medium", "hard"]);

type BadgeChallenge = { difficulty: string; text: string; metric: string; target: number };
const BADGE_CHALLENGES: Record<string, BadgeChallenge[]> = {
  easy: [
    { difficulty: "easy", text: "Leia 2 capítulos da Bíblia.", metric: "bible_chapters", target: 2 },
    { difficulty: "easy", text: "Leia 1 devocional.", metric: "devotionals", target: 1 },
    { difficulty: "easy", text: "Conclua 1 tema de plano.", metric: "plan_themes", target: 1 },
    { difficulty: "easy", text: "Fique 10 minutos ativo no MIC Rhema.", metric: "active_minutes", target: 10 },
    { difficulty: "easy", text: "Acerte 3 perguntas no Quiz.", metric: "quiz_correct", target: 3 },
    { difficulty: "easy", text: "Ouça 1 áudio no MIC Rhema.", metric: "audios", target: 1 },
    { difficulty: "easy", text: "Assista 1 vídeo no MIC Rhema.", metric: "videos", target: 1 },
  ],
  medium: [
    { difficulty: "medium", text: "Leia 8 capítulos da Bíblia.", metric: "bible_chapters", target: 8 },
    { difficulty: "medium", text: "Leia 4 devocionais.", metric: "devotionals", target: 4 },
    { difficulty: "medium", text: "Conclua 3 temas de plano.", metric: "plan_themes", target: 3 },
    { difficulty: "medium", text: "Conclua 2 planos.", metric: "plans", target: 2 },
    { difficulty: "medium", text: "Fique 30 minutos ativo no MIC Rhema.", metric: "active_minutes", target: 30 },
    { difficulty: "medium", text: "Acerte 10 perguntas no Quiz.", metric: "quiz_correct", target: 10 },
    { difficulty: "medium", text: "Assista 2 vídeos no MIC Rhema.", metric: "videos", target: 2 },
    { difficulty: "medium", text: "Estude 2 livros no MIC Rhema.", metric: "books", target: 2 },
    { difficulty: "medium", text: "Leia 3 notícias bíblicas.", metric: "bible_news", target: 3 },
  ],
  hard: [
    { difficulty: "hard", text: "Leia 20 capítulos da Bíblia.", metric: "bible_chapters", target: 20 },
    { difficulty: "hard", text: "Fique 90 minutos ativo no MIC Rhema.", metric: "active_minutes", target: 90 },
    { difficulty: "hard", text: "Acerte 15 perguntas difíceis no Quiz.", metric: "quiz_hard_correct", target: 15 },
    { difficulty: "hard", text: "Acerte 20 perguntas sem usar nenhuma dica.", metric: "quiz_correct_no_hint", target: 20 },
    { difficulty: "hard", text: "Acerte 25 perguntas sem usar Dica Fácil.", metric: "quiz_correct_no_easy_hint", target: 25 },
    { difficulty: "hard", text: "Conclua 5 planos.", metric: "plans", target: 5 },
    { difficulty: "hard", text: "Estude 5 livros no MIC Rhema.", metric: "books", target: 5 },
    { difficulty: "hard", text: "Assista 5 vídeos no MIC Rhema.", metric: "videos", target: 5 },
    { difficulty: "hard", text: "Ouça 5 áudios no MIC Rhema.", metric: "audios", target: 5 },
    { difficulty: "hard", text: "Conquiste 1200 XP no MIC Rhema.", metric: "xp_total", target: 1200 },
  ],
};

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

    if (action === "admin_generate_badge_challenge") {
      const difficulty = clean(input.difficulty, 20).toLowerCase();
      if (!BADGE_DIFFICULTIES.has(difficulty)) return json({ error: "Escolha Fácil, Médio ou Difícil." }, 400);
      const exclude = clean(input.exclude, 300);
      const source = BADGE_CHALLENGES[difficulty] ?? [];
      const candidates = source.filter((item) => item.text !== exclude);
      const pool = candidates.length ? candidates : source;
      if (!pool.length) throw new Error("Nenhum desafio disponível para esta dificuldade.");
      const selected = pool[Math.floor(Math.random() * pool.length)];
      return json({ ok: true, challenge: selected });
    }

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
        .select("id,sequence_no,name,description,challenge,image_ref,special,active,challenge_metric,challenge_target,created_at,updated_at")
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
      if (!challenge) return json({ error: "Escolha um desafio para conquistar o emblema." }, 400);
      if (!imageRef || !imageRef.toLowerCase().startsWith("micrhema-xp://emblem/")) {
        return json({ error: "Envie um emblema PNG antes de salvar." }, 400);
      }

      const knownChallenge = Object.values(BADGE_CHALLENGES).flat().find((item) => item.text === challenge);
      if (!knownChallenge) return json({ error: "Esse desafio não pertence ao catálogo verificável. Busque outro desafio pelo seletor." }, 400);

      let sequenceNo: number | null = null;
      const suppliedId = clean(input.id, 90).toLowerCase().replace(/[^a-z0-9_:-]+/g, "_").replace(/^_+|_+$/g, "");
      if (suppliedId) {
        const { data: existing, error: existingError } = await sb.from("custom_profile_badges")
          .select("sequence_no,special").eq("id", suppliedId).maybeSingle();
        if (existingError) throw existingError;
        sequenceNo = existing?.sequence_no ?? null;
      }
      if (!special && sequenceNo === null) {
        const { data: lastRows, error: lastError } = await sb.from("custom_profile_badges")
          .select("sequence_no").not("sequence_no", "is", null).order("sequence_no", { ascending: false }).limit(1);
        if (lastError) throw lastError;
        const last = Number(lastRows?.[0]?.sequence_no ?? 22);
        sequenceNo = Math.max(23, Number.isFinite(last) ? last + 1 : 23);
      }

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
        challenge_metric: knownChallenge.metric,
        challenge_target: knownChallenge.target,
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
