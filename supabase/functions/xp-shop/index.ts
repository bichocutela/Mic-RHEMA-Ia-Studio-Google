import { createClient } from "npm:@supabase/supabase-js@2.57.4";
import { importPKCS8, SignJWT } from "npm:jose@5.10.0";

const FIRESTORE_SCOPE = "https://www.googleapis.com/auth/datastore";
const TOKEN_URL = "https://oauth2.googleapis.com/token";
const DEFAULT_PROJECT_ID = "mic-rhema";
const CURRENT_PUBLISHABLE_KEY = "sb_publishable_Dv98hBnbJB2TzRCG6aJNwA_KMPHLZSw";
const LEVEL_8_PLUS = new Set([
  "semente_da_fe", "caminho_da_promessa", "escudo_da_fe", "aguas_vivas", "videira_verdadeira",
  "luz_do_mundo", "armadura_de_deus", "leao_de_juda", "chama_do_espirito", "coroa_da_vida",
  "asas_da_promessa", "tabernaculo", "arca_da_alianca", "nova_jerusalem", "gloria_eterna",
]);

type ServiceAccount = { project_id?: string; client_email?: string; private_key?: string };
type FirestoreValue = {
  stringValue?: string; booleanValue?: boolean; integerValue?: string; doubleValue?: number;
  timestampValue?: string; arrayValue?: { values?: FirestoreValue[] };
  mapValue?: { fields?: Record<string, FirestoreValue> }; nullValue?: null;
};
type FirestoreDocument = { fields?: Record<string, FirestoreValue> };

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-client-info",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Content-Type": "application/json; charset=utf-8",
};

function json(body: Record<string, unknown>, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: corsHeaders });
}

function normalizePhone(value: unknown): string {
  const digits = String(value ?? "").replace(/\D/g, "");
  return digits.length >= 12 && digits.length <= 13 && digits.startsWith("55") ? digits.slice(2) : digits;
}

function fromValue(value?: FirestoreValue): unknown {
  if (!value) return null;
  if (value.stringValue !== undefined) return value.stringValue;
  if (value.booleanValue !== undefined) return value.booleanValue;
  if (value.integerValue !== undefined) return Number(value.integerValue);
  if (value.doubleValue !== undefined) return value.doubleValue;
  if (value.timestampValue !== undefined) return value.timestampValue;
  if (value.arrayValue !== undefined) return (value.arrayValue.values ?? []).map(fromValue);
  if (value.mapValue !== undefined) return Object.fromEntries(Object.entries(value.mapValue.fields ?? {}).map(([k, v]) => [k, fromValue(v)]));
  return null;
}

function documentData(document?: FirestoreDocument | null): Record<string, unknown> {
  if (!document) return {};
  return Object.fromEntries(Object.entries(document.fields ?? {}).map(([k, v]) => [k, fromValue(v)]));
}

function stringList(value: unknown): string[] {
  return Array.isArray(value) ? value.map(String).filter(Boolean) : [];
}

function activityMap(value: unknown): Record<string, string[]> {
  if (!value || typeof value !== "object" || Array.isArray(value)) return {};
  return Object.fromEntries(Object.entries(value as Record<string, unknown>).map(([key, item]) => [key, stringList(item)]));
}

function parseLegacyXp(entry: string): number {
  const value = Number(entry.slice(entry.lastIndexOf("=") + 1));
  return Number.isFinite(value) && value > 0 ? Math.floor(value) : 0;
}

function baseUrl(projectId: string): string {
  return `https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents`;
}

async function googleAccessToken(account: ServiceAccount): Promise<string> {
  if (!account.client_email || !account.private_key) throw new Error("Conta de serviço Firebase incompleta.");
  const now = Math.floor(Date.now() / 1000);
  const assertion = await new SignJWT({ iss: account.client_email, scope: FIRESTORE_SCOPE, aud: TOKEN_URL })
    .setProtectedHeader({ alg: "RS256", typ: "JWT" })
    .setIssuedAt(now)
    .setExpirationTime(now + 3600)
    .sign(await importPKCS8(account.private_key.replace(/\\n/g, "\n"), "RS256"));
  const response = await fetch(TOKEN_URL, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", assertion }),
  });
  if (!response.ok) throw new Error(`Falha ao autenticar no Google: ${response.status}`);
  const payload = await response.json();
  if (!payload.access_token) throw new Error("Google não retornou access_token.");
  return payload.access_token;
}

async function getDocument(projectId: string, token: string, collection: string, id: string): Promise<FirestoreDocument | null> {
  const response = await fetch(`${baseUrl(projectId)}/${collection}/${encodeURIComponent(id)}`, { headers: { Authorization: `Bearer ${token}` } });
  if (response.status === 404) return null;
  if (!response.ok) throw new Error(`Falha ao ler ${collection}: ${response.status}`);
  return await response.json() as FirestoreDocument;
}

async function loadMember(projectId: string, token: string, memberId: string, phone: string) {
  const accessData = documentData(await getDocument(projectId, token, "acessos_pendentes", memberId));
  if (!Object.keys(accessData).length) throw new Error("Cadastro do membro não encontrado.");
  if (normalizePhone(accessData.phone) !== phone) throw new Error("Identidade do membro não confere.");
  const userData = documentData(await getDocument(projectId, token, "users", memberId));
  const unlocked = new Set([...stringList(accessData.unlockedBadgeIds), ...stringList(userData.unlockedBadgeIds)]);
  const legacyEntries = [...new Set([
    ...(activityMap(accessData.badgeActivityIds).journey_xp_awards ?? []),
    ...(activityMap(userData.badgeActivityIds).journey_xp_awards ?? []),
  ])];
  return {
    xpUnlocked: [...unlocked].some((id) => LEVEL_8_PLUS.has(id)),
    legacyXp: legacyEntries.reduce((sum, item) => sum + parseLegacyXp(item), 0),
  };
}

Deno.serve(async (request) => {
  if (request.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (request.method !== "POST") return json({ error: "Método não permitido." }, 405);

  try {
    const configuredLegacyKey = Deno.env.get("SUPABASE_ANON_KEY") ?? "";
    const providedKey = request.headers.get("apikey") ?? "";
    const authorization = request.headers.get("authorization") ?? "";
    const validLegacy = Boolean(configuredLegacyKey) && providedKey === configuredLegacyKey && authorization === `Bearer ${configuredLegacyKey}`;
    const validPublishable = providedKey === CURRENT_PUBLISHABLE_KEY && authorization === `Bearer ${CURRENT_PUBLISHABLE_KEY}`;
    if (!validLegacy && !validPublishable) return json({ error: "Cliente não autorizado." }, 401);

    const input = await request.json() as Record<string, unknown>;
    const action = String(input.action ?? "catalog");
    const memberId = String(input.memberId ?? "").trim();
    const phone = normalizePhone(input.phone);
    if (!memberId || phone.length < 10 || phone.length > 11) return json({ error: "Membro inválido." }, 400);

    const serviceAccount = JSON.parse(Deno.env.get("FIREBASE_SERVICE_ACCOUNT_JSON") ?? "{}") as ServiceAccount;
    const projectId = serviceAccount.project_id || Deno.env.get("FIREBASE_PROJECT_ID") || DEFAULT_PROJECT_ID;
    const googleToken = await googleAccessToken(serviceAccount);
    const member = await loadMember(projectId, googleToken, memberId, phone);

    const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
    const serviceRole = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
    if (!supabaseUrl || !serviceRole) throw new Error("Backend da Loja XP não configurado.");
    const supabase = createClient(supabaseUrl, serviceRole, { auth: { persistSession: false, autoRefreshToken: false } });

    const { data: accountRows, error: accountError } = await supabase.rpc("xp_ensure_account", {
      p_member_id: memberId,
      p_legacy_xp: member.legacyXp,
    });
    if (accountError) throw accountError;
    let account = Array.isArray(accountRows) ? accountRows[0] : accountRows;
    if (!account) throw new Error("Conta XP não pôde ser inicializada.");

    if (action === "catalog") {
      const { data, error } = await supabase
        .from("xp_shop_items")
        .select("id,name,description,cost,category,kind,image_url,stock,limit_per_member,active")
        .eq("active", true)
        .order("cost", { ascending: true });
      if (error) throw error;
      return json({ ok: true, unlocked: member.xpUnlocked, account, items: data ?? [] });
    }

    if (action === "my_redemptions") {
      const { data, error } = await supabase
        .from("xp_redemptions")
        .select("id,item_id,item_name,cost,status,redemption_code,created_at,delivered_at")
        .eq("member_id", memberId)
        .order("created_at", { ascending: false })
        .limit(100);
      if (error) throw error;
      return json({ ok: true, unlocked: member.xpUnlocked, account, redemptions: data ?? [] });
    }

    if (action === "redeem") {
      if (!member.xpUnlocked) return json({ error: "A Loja XP é liberada no Nível 8." }, 403);
      const itemId = String(input.itemId ?? "").trim();
      if (!itemId) return json({ error: "Recompensa inválida." }, 400);
      const { data, error } = await supabase.rpc("xp_redeem", { p_member_id: memberId, p_item_id: itemId });
      if (error) throw error;
      const row = Array.isArray(data) ? data[0] : data;
      if (!row) throw new Error("O resgate não retornou resultado.");
      account = {
        member_id: memberId,
        total_earned: Number(row.total_earned ?? account.total_earned ?? 0),
        total_spent: Number(row.total_spent ?? account.total_spent ?? 0),
        balance: Number(row.balance ?? account.balance ?? 0),
        migrated_legacy_xp: Number(account.migrated_legacy_xp ?? 0),
        updated_at: new Date().toISOString(),
      };
      return json({ ok: true, unlocked: true, account, redemption: row });
    }

    return json({ error: "Ação inválida." }, 400);
  } catch (error) {
    console.error("xp-shop failed", error);
    const message = error instanceof Error ? error.message : "Falha na Loja XP.";
    return json({ error: message }, message.includes("Saldo XP insuficiente") ? 409 : 500);
  }
});
