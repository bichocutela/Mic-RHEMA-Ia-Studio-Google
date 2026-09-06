import { createClient } from "npm:@supabase/supabase-js@2.57.4";
import { importPKCS8, SignJWT } from "npm:jose@5.10.0";

const FIRESTORE_SCOPE = "https://www.googleapis.com/auth/datastore";
const TOKEN_URL = "https://oauth2.googleapis.com/token";
const DEFAULT_PROJECT_ID = "mic-rhema";
const ADMIN_ACTIONS = new Set(["admin_catalog", "admin_upsert_item", "admin_redemptions", "admin_set_redemption_status"]);
const ITEM_KINDS = new Set(["digital", "profile", "physical"]);
const REDEMPTION_STATUSES = new Set(["pendente", "entregue", "cancelado"]);

type ServiceAccount = { project_id?: string; client_email?: string; private_key?: string };
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
type XpContext = {
  memberId: string;
  unlocked: boolean;
  account: Record<string, unknown>;
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

function documentData(document?: FirestoreDocument | null): Record<string, unknown> {
  return document
    ? Object.fromEntries(Object.entries(document.fields ?? {}).map(([key, value]) => [key, fromValue(value)]))
    : {};
}

function firestoreBase(projectId: string) {
  return `https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents`;
}

async function googleAccessToken(account: ServiceAccount) {
  if (!account.client_email || !account.private_key) {
    throw new Error("Conta de serviço Firebase incompleta.");
  }
  const now = Math.floor(Date.now() / 1000);
  const assertion = await new SignJWT({
    iss: account.client_email,
    scope: FIRESTORE_SCOPE,
    aud: TOKEN_URL,
  })
    .setProtectedHeader({ alg: "RS256", typ: "JWT" })
    .setIssuedAt(now)
    .setExpirationTime(now + 3600)
    .sign(await importPKCS8(account.private_key.replace(/\\n/g, "\n"), "RS256"));

  const response = await fetch(TOKEN_URL, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth2:grant-type:jwt-bearer",
      assertion,
    }),
  });
  const payload = await response.json();
  if (!response.ok || !payload.access_token) {
    throw new Error("Falha ao autenticar o serviço administrativo no Firebase.");
  }
  return String(payload.access_token);
}

async function getDocument(
  projectId: string,
  token: string,
  collection: string,
  id: string,
): Promise<FirestoreDocument | null> {
  const response = await fetch(
    `${firestoreBase(projectId)}/${collection}/${encodeURIComponent(id)}`,
    { headers: { Authorization: `Bearer ${token}` } },
  );
  if (response.status === 404) return null;
  if (!response.ok) throw new Error(`Falha ao ler ${collection}: ${response.status}`);
  return await response.json() as FirestoreDocument;
}

async function firebaseIdentity(idToken: string): Promise<FirebaseIdentity> {
  const apiKey = Deno.env.get("FIREBASE_WEB_API_KEY") ?? "";
  if (!apiKey) throw new Error("Firebase Web API Key não configurada.");
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
  if (!response.ok || !user?.localId) throw new Error("Sessão Firebase inválida.");
  let claims: Record<string, unknown> = {};
  try {
    claims = JSON.parse(user.customAttributes || "{}");
  } catch {
    claims = {};
  }
  return { uid: user.localId, claims };
}

async function assertAdmin(projectId: string, googleToken: string, authorization: string) {
  const idToken = authorization.replace(/^Bearer\s+/i, "").trim();
  if (!idToken) throw new Error("Acesso administrativo obrigatório.");
  const identity = await firebaseIdentity(idToken);
  if (identity.claims.isAdmin === true) return identity;
  for (const collection of ["acessos_pendentes", "users"]) {
    const data = documentData(await getDocument(projectId, googleToken, collection, identity.uid));
    if (data.isAdmin === true) return identity;
  }
  throw new Error("Acesso administrativo obrigatório.");
}

async function loadXpContext(
  request: Request,
  input: Record<string, unknown>,
  supabaseUrl: string,
): Promise<XpContext> {
  const headers: Record<string, string> = { "Content-Type": "application/json" };
  const authorization = request.headers.get("authorization") ?? "";
  const apiKey = request.headers.get("apikey") ?? "";
  if (authorization) headers.Authorization = authorization;
  if (apiKey) headers.apikey = apiKey;

  const response = await fetch(`${supabaseUrl.replace(/\/$/, "")}/functions/v1/xp-engine`, {
    method: "POST",
    headers,
    body: JSON.stringify({
      action: "get_account",
      memberId: input.memberId,
      phone: input.phone,
    }),
  });
  const raw = await response.text();
  let payload: Record<string, any> = {};
  try {
    payload = raw ? JSON.parse(raw) : {};
  } catch {
    payload = {};
  }
  if (!response.ok) {
    throw new ShopHttpError(
      response.status,
      clean(payload.error, 500) || `Falha ao sincronizar a conta XP (${response.status}).`,
    );
  }
  const account = payload.account && typeof payload.account === "object"
    ? payload.account as Record<string, unknown>
    : {};
  const memberId = clean(account.member_id || input.memberId, 180);
  if (!memberId) throw new ShopHttpError(401, "A conta XP não informou o membro autenticado.");
  return {
    memberId,
    unlocked: payload.unlocked === true,
    account,
  };
}

function firebaseBearer(request: Request): string {
  const authorization = request.headers.get("authorization") ?? "";
  const bearer = authorization.match(/^Bearer\s+(.+)$/i)?.[1]?.trim() ?? "";
  const apiKey = (request.headers.get("apikey") ?? "").trim();
  if (!bearer || bearer === apiKey) return "";
  return bearer;
}

async function tryLoadMemberName(projectId: string, idToken: string, memberId: string) {
  if (!idToken) return "Membro MIC Rhema";
  try {
    const user = documentData(await getDocument(projectId, idToken, "users", memberId));
    return clean(user.name || user.ibrCertificateName || "Membro MIC Rhema", 120) || "Membro MIC Rhema";
  } catch (error) {
    console.warn("xp-shop: nome do membro indisponível; resgate seguirá sem bloquear", error);
    return "Membro MIC Rhema";
  }
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
    const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
    const serviceRole = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
    if (!supabaseUrl || !serviceRole) throw new Error("Backend da Loja XP não configurado.");

    const supabase = createClient(supabaseUrl, serviceRole, {
      auth: { persistSession: false, autoRefreshToken: false },
    });

    if (ADMIN_ACTIONS.has(action)) {
      const authorization = request.headers.get("authorization") ?? "";
      const serviceAccount = JSON.parse(Deno.env.get("FIREBASE_SERVICE_ACCOUNT_JSON") ?? "{}") as ServiceAccount;
      const projectId = serviceAccount.project_id || Deno.env.get("FIREBASE_PROJECT_ID") || DEFAULT_PROJECT_ID;
      const googleToken = await googleAccessToken(serviceAccount);
      await assertAdmin(projectId, googleToken, authorization);

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

    // O fluxo do membro não cria mais um segundo token OAuth do Google.
    // A identidade, o desbloqueio da Jornada e a conta já são validados pelo xp-engine,
    // que é a autoridade central usada pelo restante do aplicativo.
    const context = await loadXpContext(request, input, supabaseUrl);
    let account = context.account;
    const memberId = context.memberId;

    if (action === "catalog") {
      const { data, error } = await supabase.from("xp_shop_items")
        .select("id,name,description,cost,category,kind,image_url,stock,limit_per_member,active,available_from,available_until")
        .eq("active", true)
        .order("cost", { ascending: true });
      if (error) throw error;
      const items = (data ?? []).filter((item) => isAvailableNow(item as Record<string, unknown>));
      return json({ ok: true, unlocked: context.unlocked, account, items });
    }

    if (action === "my_redemptions") {
      const [{ data, error }, entitlements] = await Promise.all([
        supabase.from("xp_redemptions")
          .select("id,item_id,item_name,cost,status,redemption_code,created_at,delivered_at")
          .eq("member_id", memberId)
          .order("created_at", { ascending: false })
          .limit(100),
        memberEntitlements(supabase, memberId),
      ]);
      if (error) throw error;
      return json({
        ok: true,
        unlocked: context.unlocked,
        account,
        redemptions: data ?? [],
        entitlements,
      });
    }

    if (action === "redeem") {
      const idToken = firebaseBearer(request);
      if (!idToken) {
        return json({ error: "Atualize o MIC Rhema e entre novamente para resgatar recompensas." }, 401);
      }
      if (!context.unlocked) return json({ error: "A Loja XP é liberada no Nível 8." }, 403);

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
        p_member_id: memberId,
        p_item_id: itemId,
        p_expected_cost: expectedCost,
      });
      if (error) throw error;
      const row = Array.isArray(data) ? data[0] : data;
      if (!row) throw new Error("O resgate não retornou resultado.");

      if (row.redemption_id) {
        const projectId = Deno.env.get("FIREBASE_PROJECT_ID") || DEFAULT_PROJECT_ID;
        const memberName = await tryLoadMemberName(projectId, idToken, memberId);
        const { error: nameError } = await supabase.from("xp_redemptions")
          .update({ member_name: memberName })
          .eq("id", row.redemption_id);
        if (nameError) {
          console.warn("Não foi possível registrar o nome do membro no resgate", nameError.message);
        }
      }

      account = {
        member_id: memberId,
        total_earned: Number(row.total_earned ?? account.total_earned ?? 0),
        total_spent: Number(row.total_spent ?? account.total_spent ?? 0),
        balance: Number(row.balance ?? account.balance ?? 0),
        migrated_legacy_xp: Number(account.migrated_legacy_xp ?? 0),
        updated_at: new Date().toISOString(),
      };
      const entitlements = await memberEntitlements(supabase, memberId);
      return json({ ok: true, unlocked: true, account, redemption: row, entitlements });
    }

    return json({ error: "Ação inválida." }, 400);
  } catch (error) {
    console.error("xp-shop failed", error);
    const message = error instanceof Error ? error.message : "Falha na Loja XP.";
    if (error instanceof ShopHttpError) return json({ error: message }, error.status);
    const lowered = message.toLowerCase();
    const status = lowered.includes("sessão") || lowered.includes("identidade") ? 401
      : lowered.includes("acesso administrativo") ? 403
      : lowered.includes("saldo xp insuficiente") || lowered.includes("esgotada") || lowered.includes("limite de resgate") || lowered.includes("indisponível") || lowered.includes("preço") ? 409
      : lowered.includes("inválid") || lowered.includes("não encontrado") || lowered.includes("não pode") ? 400
      : 500;
    return json({ error: message }, status);
  }
});
