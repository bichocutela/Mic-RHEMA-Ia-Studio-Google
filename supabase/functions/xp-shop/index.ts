import { createClient } from "npm:@supabase/supabase-js@2.57.4";

const DEFAULT_PROJECT_ID = "mic-rhema";
const DEFAULT_FIREBASE_WEB_API_KEY = "AIzaSyD-GPqTLRFmOiNATJwzKUHGqJeTPQcf0E8";
const LEVEL_8_PLUS = new Set([
  "semente_da_fe", "caminho_da_promessa", "escudo_da_fe", "aguas_vivas", "videira_verdadeira",
  "luz_do_mundo", "armadura_de_deus", "leao_de_juda", "chama_do_espirito", "coroa_da_vida",
  "asas_da_promessa", "tabernaculo", "arca_da_alianca", "nova_jerusalem", "gloria_eterna",
]);
const ADMIN_ACTIONS = new Set(["admin_catalog", "admin_upsert_item", "admin_redemptions", "admin_set_redemption_status"]);
const ITEM_KINDS = new Set(["digital", "profile", "physical"]);
const REDEMPTION_STATUSES = new Set(["pendente", "entregue", "cancelado"]);

type FirestoreValue = {
  stringValue?: string;
  booleanValue?: boolean;
  integerValue?: string;
  doubleValue?: number;
  timestampValue?: string;
  arrayValue?: { values?: FirestoreValue[] };
  mapValue?: { fields?: Record<string, FirestoreValue> };
  nullValue?: null;
};
type FirestoreDocument = { fields?: Record<string, FirestoreValue> };
type FirebaseIdentity = { uid: string; claims: Record<string, unknown> };
type MemberContext = {
  memberId: string;
  name: string;
  unlocked: boolean;
  legacyXp: number;
  account: Record<string, any>;
};

class ShopHttpError extends Error {
  constructor(public status: number, message: string) {
    super(message);
  }
}

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-client-info",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Content-Type": "application/json; charset=utf-8",
  "Cache-Control": "no-store",
};

const json = (body: Record<string, unknown>, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: corsHeaders });
const clean = (value: unknown, max = 500) => String(value ?? "").trim().slice(0, max);

function fromValue(value?: FirestoreValue): unknown {
  if (!value) return null;
  if (value.stringValue !== undefined) return value.stringValue;
  if (value.booleanValue !== undefined) return value.booleanValue;
  if (value.integerValue !== undefined) return Number(value.integerValue);
  if (value.doubleValue !== undefined) return value.doubleValue;
  if (value.timestampValue !== undefined) return value.timestampValue;
  if (value.arrayValue !== undefined) return (value.arrayValue.values ?? []).map(fromValue);
  if (value.mapValue !== undefined) {
    return Object.fromEntries(Object.entries(value.mapValue.fields ?? {}).map(([key, item]) => [key, fromValue(item)]));
  }
  return null;
}

function documentData(document?: FirestoreDocument | null): Record<string, any> {
  return document
    ? Object.fromEntries(Object.entries(document.fields ?? {}).map(([key, value]) => [key, fromValue(value)]))
    : {};
}

const stringList = (value: unknown): string[] => Array.isArray(value)
  ? value.map(String).filter(Boolean)
  : [];

function activityMap(value: unknown): Record<string, string[]> {
  if (!value || typeof value !== "object" || Array.isArray(value)) return {};
  return Object.fromEntries(
    Object.entries(value as Record<string, unknown>).map(([key, item]) => [key, stringList(item)]),
  );
}

function parseLegacyXp(entry: string) {
  const value = Number(entry.slice(entry.lastIndexOf("=") + 1));
  return Number.isFinite(value) && value > 0 ? Math.floor(value) : 0;
}

function firestoreBase(projectId: string) {
  return `https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents`;
}

async function getDocument(
  projectId: string,
  idToken: string,
  collection: string,
  id: string,
): Promise<FirestoreDocument | null> {
  const response = await fetch(
    `${firestoreBase(projectId)}/${collection}/${encodeURIComponent(id)}`,
    { headers: { Authorization: `Bearer ${idToken}` } },
  );
  if (response.status === 404) return null;
  if (!response.ok) throw new ShopHttpError(response.status, `Falha ao ler ${collection}: ${response.status}`);
  return await response.json() as FirestoreDocument;
}

async function firebaseIdentity(idToken: string): Promise<FirebaseIdentity> {
  const apiKey = Deno.env.get("FIREBASE_WEB_API_KEY") || DEFAULT_FIREBASE_WEB_API_KEY;
  const response = await fetch(
    `https://identitytoolkit.googleapis.com/v1/accounts:lookup?key=${encodeURIComponent(apiKey)}`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ idToken }),
    },
  );
  const payload = await response.json() as {
    users?: Array<{ localId?: string; customAttributes?: string }>;
  };
  const user = payload.users?.[0];
  if (!response.ok || !user?.localId) throw new ShopHttpError(401, "Sessão Firebase inválida.");
  let claims: Record<string, unknown> = {};
  try {
    claims = JSON.parse(user.customAttributes || "{}");
  } catch {
    claims = {};
  }
  return { uid: user.localId, claims };
}

function firebaseBearer(request: Request): string {
  const authorization = request.headers.get("authorization") ?? "";
  const bearer = authorization.match(/^Bearer\s+(.+)$/i)?.[1]?.trim() ?? "";
  const apiKey = (request.headers.get("apikey") ?? "").trim();
  if (!bearer || bearer === apiKey) return "";
  return bearer;
}

async function assertAdmin(request: Request, projectId: string) {
  const idToken = firebaseBearer(request);
  if (!idToken) throw new ShopHttpError(403, "Acesso administrativo obrigatório.");
  const identity = await firebaseIdentity(idToken);
  if (identity.claims.isAdmin === true) return identity;

  for (const collection of ["users", "acessos_pendentes"]) {
    try {
      const data = documentData(await getDocument(projectId, idToken, collection, identity.uid));
      if (data.isAdmin === true) return identity;
    } catch (error) {
      if (error instanceof ShopHttpError && error.status === 403) continue;
      throw error;
    }
  }
  throw new ShopHttpError(403, "Acesso administrativo obrigatório.");
}

async function loadMemberContext(
  request: Request,
  input: Record<string, unknown>,
  projectId: string,
  supabase: any,
): Promise<MemberContext> {
  const idToken = firebaseBearer(request);
  if (!idToken) throw new ShopHttpError(401, "Sua sessão de membro expirou. Entre novamente no MIC Rhema.");

  const identity = await firebaseIdentity(idToken);
  const requestedMemberId = clean(input.memberId, 180);
  if (requestedMemberId && requestedMemberId !== identity.uid) {
    throw new ShopHttpError(401, "A sessão Firebase não pertence ao membro ativo. Entre novamente.");
  }

  const user = documentData(await getDocument(projectId, idToken, "users", identity.uid));
  if (!Object.keys(user).length) throw new ShopHttpError(404, "Cadastro do membro não encontrado.");
  if (user.isApproved !== true && user.isAdmin !== true && user.isIbr !== true) {
    throw new ShopHttpError(403, "Acesso do membro ainda não aprovado.");
  }

  const unlockedIds = stringList(user.unlockedBadgeIds);
  const unlocked = unlockedIds.some((id) => LEVEL_8_PLUS.has(id));
  const legacyEntries = activityMap(user.badgeActivityIds).journey_xp_awards ?? [];
  const legacyXp = [...new Set(legacyEntries)].reduce((sum, entry) => sum + parseLegacyXp(entry), 0);

  const { data: ensured, error } = await supabase.rpc("xp_ensure_account", {
    p_member_id: identity.uid,
    p_legacy_xp: legacyXp,
  });
  if (error) throw error;
  const account = Array.isArray(ensured) ? ensured[0] : ensured;
  if (!account) throw new Error("Conta XP não pôde ser inicializada.");

  return {
    memberId: identity.uid,
    name: clean(user.name || user.ibrCertificateName || "Membro MIC Rhema", 120) || "Membro MIC Rhema",
    unlocked,
    legacyXp,
    account,
  };
}

function parseNullableDate(value: unknown): string | null {
  const text = clean(value, 80);
  if (!text) return null;
  const time = Date.parse(text);
  if (!Number.isFinite(time)) throw new Error("Data da recompensa inválida.");
  return new Date(time).toISOString();
}

function isAvailableNow(item: Record<string, unknown>, now = Date.now()) {
  const from = item.available_from ? Date.parse(String(item.available_from)) : NaN;
  const until = item.available_until ? Date.parse(String(item.available_until)) : NaN;
  return (!Number.isFinite(from) || from <= now) && (!Number.isFinite(until) || until >= now);
}

async function memberEntitlements(supabase: any, memberId: string) {
  const { data, error } = await supabase.from("xp_entitlements")
    .select("id,item_id,item_name,kind,unlocked_at")
    .eq("member_id", memberId)
    .order("unlocked_at", { ascending: false });
  if (error) throw error;
  return data ?? [];
}

Deno.serve(async (request) => {
  if (request.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (request.method !== "POST") return json({ error: "Método não permitido." }, 405);

  try {
    const input = await request.json() as Record<string, unknown>;
    const action = clean(input.action || "catalog", 60);
    const projectId = Deno.env.get("FIREBASE_PROJECT_ID") || DEFAULT_PROJECT_ID;
    const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
    const serviceRole = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
    if (!supabaseUrl || !serviceRole) throw new Error("Backend da Loja XP não configurado.");

    const supabase = createClient(supabaseUrl, serviceRole, {
      auth: { persistSession: false, autoRefreshToken: false },
    });

    if (ADMIN_ACTIONS.has(action)) {
      await assertAdmin(request, projectId);

      if (action === "admin_catalog") {
        const { data, error } = await supabase.from("xp_shop_items")
          .select("id,name,description,cost,category,kind,image_url,stock,limit_per_member,active,available_from,available_until,created_at,updated_at")
          .order("active", { ascending: false })
          .order("cost", { ascending: true });
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
        const stock = stockValue === null || stockValue === undefined || clean(stockValue, 40) === ""
          ? null
          : Math.floor(Number(stockValue));
        const active = input.active !== false;
        const availableFrom = parseNullableDate(input.availableFrom);
        const availableUntil = parseNullableDate(input.availableUntil);

        if (!id || id.length < 3) return json({ error: "Informe um identificador válido para a recompensa." }, 400);
        if (!name) return json({ error: "Informe o nome da recompensa." }, 400);
        if (!Number.isFinite(cost) || cost <= 0) return json({ error: "O preço em XP deve ser maior que zero." }, 400);
        if (!ITEM_KINDS.has(kind)) return json({ error: "Tipo de recompensa inválido." }, 400);
        if (!Number.isFinite(limitPerMember) || limitPerMember < 1) return json({ error: "Limite por membro inválido." }, 400);
        if (kind !== "physical" && limitPerMember !== 1) {
          return json({ error: "Recompensas digitais e de perfil só podem ser resgatadas uma vez por membro." }, 400);
        }
        if (stock !== null && (!Number.isFinite(stock) || stock < 0)) return json({ error: "Estoque inválido." }, 400);
        if (availableFrom && availableUntil && Date.parse(availableUntil) <= Date.parse(availableFrom)) {
          return json({ error: "A data final precisa ser posterior à data inicial." }, 400);
        }

        const row = {
          id,
          name,
          description,
          cost,
          category,
          kind,
          image_url: imageUrl,
          stock,
          limit_per_member: limitPerMember,
          active,
          available_from: availableFrom,
          available_until: availableUntil,
          updated_at: new Date().toISOString(),
        };
        const { data, error } = await supabase.from("xp_shop_items")
          .upsert(row, { onConflict: "id" })
          .select()
          .single();
        if (error) throw error;
        return json({ ok: true, item: data });
      }

      if (action === "admin_redemptions") {
        const status = clean(input.status, 30);
        let query = supabase.from("xp_redemptions")
          .select("id,member_id,member_name,item_id,item_name,cost,status,redemption_code,created_at,delivered_at,stock_consumed")
          .order("created_at", { ascending: false })
          .limit(200);
        if (status && status !== "todos") query = query.eq("status", status);
        const { data, error } = await query;
        if (error) throw error;
        return json({ ok: true, redemptions: data ?? [] });
      }

      if (action === "admin_set_redemption_status") {
        const redemptionId = clean(input.redemptionId, 80);
        const status = clean(input.status, 30);
        if (!redemptionId || !REDEMPTION_STATUSES.has(status)) {
          return json({ error: "Alteração de resgate inválida." }, 400);
        }
        const { data, error } = await supabase.rpc("xp_admin_update_redemption", {
          p_redemption_id: redemptionId,
          p_status: status,
        });
        if (error) throw error;
        const row = Array.isArray(data) ? data[0] : data;
        if (!row) throw new Error("O resgate não retornou resultado.");
        return json({ ok: true, redemption: row });
      }
    }

    const member = await loadMemberContext(request, input, projectId, supabase);
    let account = member.account;

    if (action === "catalog") {
      const { data, error } = await supabase.from("xp_shop_items")
        .select("id,name,description,cost,category,kind,image_url,stock,limit_per_member,active,available_from,available_until")
        .eq("active", true)
        .order("cost", { ascending: true });
      if (error) throw error;
      const items = (data ?? []).filter((item) => isAvailableNow(item as Record<string, unknown>));
      return json({ ok: true, unlocked: member.unlocked, account, items });
    }

    if (action === "my_redemptions") {
      const [{ data, error }, entitlements] = await Promise.all([
        supabase.from("xp_redemptions")
          .select("id,item_id,item_name,cost,status,redemption_code,created_at,delivered_at")
          .eq("member_id", member.memberId)
          .order("created_at", { ascending: false })
          .limit(100),
        memberEntitlements(supabase, member.memberId),
      ]);
      if (error) throw error;
      return json({
        ok: true,
        unlocked: member.unlocked,
        account,
        redemptions: data ?? [],
        entitlements,
      });
    }

    if (action === "redeem") {
      if (!member.unlocked) return json({ error: "A Loja XP é liberada no Nível 8." }, 403);
      const itemId = clean(input.itemId, 100);
      const expectedCost = Math.floor(Number(input.expectedCost ?? 0));
      if (!itemId || !Number.isFinite(expectedCost) || expectedCost <= 0) {
        return json({ error: "Resgate inválido." }, 400);
      }

      const { data: availabilityItem, error: availabilityError } = await supabase.from("xp_shop_items")
        .select("id,cost,active,available_from,available_until")
        .eq("id", itemId)
        .maybeSingle();
      if (availabilityError) throw availabilityError;
      if (!availabilityItem || availabilityItem.active !== true || !isAvailableNow(availabilityItem as Record<string, unknown>)) {
        return json({ error: "Recompensa indisponível neste período." }, 409);
      }
      if (Number(availabilityItem.cost) !== expectedCost) {
        return json({ error: "O preço da recompensa foi atualizado. Atualize a Loja e confirme novamente." }, 409);
      }

      const { data, error } = await supabase.rpc("xp_redeem_checked", {
        p_member_id: member.memberId,
        p_item_id: itemId,
        p_expected_cost: expectedCost,
      });
      if (error) throw error;
      const row = Array.isArray(data) ? data[0] : data;
      if (!row) throw new Error("O resgate não retornou resultado.");

      if (row.redemption_id) {
        const { error: nameError } = await supabase.from("xp_redemptions")
          .update({ member_name: member.name })
          .eq("id", row.redemption_id);
        if (nameError) {
          console.warn("Não foi possível registrar o nome do membro no resgate", nameError.message);
        }
      }

      account = {
        member_id: member.memberId,
        total_earned: Number(row.total_earned ?? account.total_earned ?? 0),
        total_spent: Number(row.total_spent ?? account.total_spent ?? 0),
        balance: Number(row.balance ?? account.balance ?? 0),
        migrated_legacy_xp: Number(account.migrated_legacy_xp ?? 0),
        updated_at: new Date().toISOString(),
      };
      const entitlements = await memberEntitlements(supabase, member.memberId);
      return json({ ok: true, unlocked: true, account, redemption: row, entitlements });
    }

    return json({ error: "Ação inválida." }, 400);
  } catch (error) {
    console.error("xp-shop failed", error);
    const message = error instanceof Error ? error.message : "Falha na Loja XP.";
    if (error instanceof ShopHttpError) return json({ error: message }, error.status);
    const lowered = message.toLowerCase();
    const status = lowered.includes("sessão") || lowered.includes("identidade") ? 401
      : lowered.includes("acesso administrativo") || lowered.includes("ainda não aprovado") ? 403
      : lowered.includes("saldo xp insuficiente") || lowered.includes("esgotada") || lowered.includes("limite de resgate") || lowered.includes("indisponível") || lowered.includes("preço") ? 409
      : lowered.includes("inválid") || lowered.includes("não encontrado") || lowered.includes("não pode") ? 400
      : 500;
    return json({ error: message }, status);
  }
});
