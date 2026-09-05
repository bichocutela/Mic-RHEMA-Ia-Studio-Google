import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "npm:@supabase/supabase-js@2.57.4";
import { createRemoteJWKSet, errors as joseErrors, importPKCS8, jwtVerify, SignJWT } from "npm:jose@5.10.0";

const FIREBASE_PROJECT_ID = "mic-rhema";
const FIREBASE_ISSUER = `https://securetoken.google.com/${FIREBASE_PROJECT_ID}`;
const FIREBASE_JWKS_URL = "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com";
const FIREBASE_JWKS = createRemoteJWKSet(new URL(FIREBASE_JWKS_URL));
const GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
const LEVEL_8_PLUS = new Set([
  "semente_da_fe", "caminho_da_promessa", "escudo_da_fe", "aguas_vivas", "videira_verdadeira",
  "luz_do_mundo", "armadura_de_deus", "leao_de_juda", "chama_do_espirito", "coroa_da_vida",
  "asas_da_promessa", "tabernaculo", "arca_da_alianca", "nova_jerusalem", "gloria_eterna",
]);
const CHAPTER_COUNTS: Record<string, number> = {
  "Gênesis":50,"Êxodo":40,"Levítico":27,"Números":36,"Deuteronômio":34,"Josué":24,"Juízes":21,"Rute":4,
  "1 Samuel":31,"2 Samuel":24,"1 Reis":22,"2 Reis":25,"1 Crônicas":29,"2 Crônicas":36,"Esdras":10,"Neemias":13,
  "Ester":10,"Jó":42,"Salmos":150,"Provérbios":31,"Eclesiastes":12,"Cânticos":8,"Isaías":66,"Jeremias":52,
  "Lamentações":5,"Ezequiel":48,"Daniel":12,"Oséias":14,"Joel":3,"Amós":9,"Obadias":1,"Jonas":4,"Miquéias":7,
  "Naum":3,"Habacuque":3,"Sofonias":3,"Ageu":2,"Zacarias":14,"Malaquias":4,"Mateus":28,"Marcos":16,"Lucas":24,
  "João":21,"Atos":28,"Romanos":16,"1 Coríntios":16,"2 Coríntios":13,"Gálatas":6,"Efésios":6,"Filipenses":4,
  "Colossenses":4,"1 Tessalonicenses":5,"2 Tessalonicenses":3,"1 Timóteo":6,"2 Timóteo":4,"Tito":3,"Filemom":1,
  "Hebreus":13,"Tiago":5,"1 Pedro":5,"2 Pedro":3,"1 João":5,"2 João":1,"3 João":1,"Judas":1,"Apocalipse":22,
};

type ServiceAccount = { project_id?: string; client_email?: string; private_key?: string };
type FirebaseClaims = { sub?: string; user_id?: string };
type FirestoreValue = {
  stringValue?: string; booleanValue?: boolean; integerValue?: string; doubleValue?: number; timestampValue?: string;
  arrayValue?: { values?: FirestoreValue[] }; mapValue?: { fields?: Record<string, FirestoreValue> }; nullValue?: null;
};
type FirestoreDocument = { name?: string; fields?: Record<string, FirestoreValue> };
type AwardRule = { amount: number; cap?: number; description: string };

class HttpError extends Error { constructor(public status: number, message: string) { super(message); } }

const cors = {
  "access-control-allow-origin": "*",
  "access-control-allow-headers": "authorization, content-type, apikey",
  "access-control-allow-methods": "POST, OPTIONS",
  "content-type": "application/json; charset=utf-8",
  "cache-control": "no-store",
};
const json = (body: Record<string, unknown>, status = 200) => new Response(JSON.stringify(body), { status, headers: cors });
const clean = (value: unknown, max = 1000) => String(value ?? "").trim().slice(0, max);

function bearer(request: Request) {
  const match = (request.headers.get("authorization") || "").match(/^Bearer\s+(.+)$/i);
  if (!match) throw new HttpError(401, "Sessão Firebase ausente.");
  return match[1];
}

async function verifyFirebaseToken(token: string): Promise<FirebaseClaims> {
  try {
    const result = await jwtVerify(token, FIREBASE_JWKS, {
      issuer: FIREBASE_ISSUER,
      audience: FIREBASE_PROJECT_ID,
      algorithms: ["RS256"],
    });
    const claims = result.payload as FirebaseClaims;
    if (!claims.sub) throw new HttpError(401, "Sessão Firebase inválida.");
    return claims;
  } catch (error) {
    if (error instanceof HttpError) throw error;
    if (error instanceof joseErrors.JWTExpired) throw new HttpError(401, "Sua sessão expirou. Entre novamente.");
    throw new HttpError(401, "Sessão Firebase inválida.");
  }
}

async function googleAccessToken(account: ServiceAccount) {
  if (!account.client_email || !account.private_key) throw new HttpError(500, "Credencial Firebase do servidor incompleta.");
  const now = Math.floor(Date.now() / 1000);
  const assertion = await new SignJWT({
    iss: account.client_email,
    scope: "https://www.googleapis.com/auth/datastore",
    aud: GOOGLE_TOKEN_URL,
  })
    .setProtectedHeader({ alg: "RS256", typ: "JWT" })
    .setIssuedAt(now)
    .setExpirationTime(now + 3600)
    .sign(await importPKCS8(account.private_key.replace(/\\n/g, "\n"), "RS256"));
  const response = await fetch(GOOGLE_TOKEN_URL, {
    method: "POST",
    headers: { "content-type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", assertion }),
  });
  const payload = await response.json();
  if (!response.ok || !payload.access_token) throw new HttpError(500, "Não foi possível autenticar o perfil no Firebase.");
  return String(payload.access_token);
}

function decodeValue(value?: FirestoreValue): unknown {
  if (!value) return undefined;
  if (value.stringValue !== undefined) return value.stringValue;
  if (value.booleanValue !== undefined) return value.booleanValue;
  if (value.integerValue !== undefined) return Number(value.integerValue);
  if (value.doubleValue !== undefined) return value.doubleValue;
  if (value.timestampValue !== undefined) return value.timestampValue;
  if (value.arrayValue !== undefined) return (value.arrayValue.values ?? []).map(decodeValue);
  if (value.mapValue !== undefined) return Object.fromEntries(Object.entries(value.mapValue.fields ?? {}).map(([key, item]) => [key, decodeValue(item)]));
  return undefined;
}

function documentData(document?: FirestoreDocument | null): Record<string, unknown> {
  if (!document) return {};
  return Object.fromEntries(Object.entries(document.fields ?? {}).map(([key, value]) => [key, decodeValue(value)]));
}

function firestoreBase(projectId: string) {
  return `https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents`;
}

async function getDocument(projectId: string, token: string, collection: string, id: string): Promise<FirestoreDocument | null> {
  const response = await fetch(`${firestoreBase(projectId)}/${collection}/${encodeURIComponent(id)}`, { headers: { authorization: `Bearer ${token}` } });
  if (response.status === 404) return null;
  if (!response.ok) throw new HttpError(502, `Não foi possível consultar ${collection} agora.`);
  return await response.json() as FirestoreDocument;
}

const stringList = (value: unknown): string[] => Array.isArray(value) ? value.map(String).filter(Boolean) : [];
function activityMap(value: unknown): Record<string, string[]> {
  if (!value || typeof value !== "object" || Array.isArray(value)) return {};
  return Object.fromEntries(Object.entries(value as Record<string, unknown>).map(([key, item]) => [key, stringList(item)]));
}
function parseLegacyXp(entry: string) {
  const value = Number(entry.slice(entry.lastIndexOf("=") + 1));
  return Number.isFinite(value) && value > 0 ? Math.floor(value) : 0;
}

async function loadMember(projectId: string, token: string, memberId: string) {
  const access = documentData(await getDocument(projectId, token, "acessos_pendentes", memberId));
  if (!Object.keys(access).length) throw new HttpError(404, "Perfil de membro não encontrado.");
  if (access.isApproved !== true && access.isAdmin !== true) throw new HttpError(403, "Seu acesso de membro ainda não está aprovado.");
  const user = documentData(await getDocument(projectId, token, "users", memberId));
  const unlocked = new Set([...stringList(access.unlockedBadgeIds), ...stringList(user.unlockedBadgeIds)]);
  const legacy = [...new Set([
    ...(activityMap(access.badgeActivityIds).journey_xp_awards ?? []),
    ...(activityMap(user.badgeActivityIds).journey_xp_awards ?? []),
  ])];
  return {
    name: clean(access.name || user.name || "Membro MIC Rhema", 120),
    xpUnlocked: [...unlocked].some((id) => LEVEL_8_PLUS.has(id)),
    legacyXp: legacy.reduce((sum, item) => sum + parseLegacyXp(item), 0),
  };
}

function todayRecife() {
  return new Intl.DateTimeFormat("en-CA", { timeZone: "America/Recife", year: "numeric", month: "2-digit", day: "2-digit" }).format(new Date());
}
function addDays(date: string, delta: number) {
  const value = new Date(`${date}T00:00:00Z`);
  value.setUTCDate(value.getUTCDate() + delta);
  return value.toISOString().slice(0, 10);
}
async function loadStreak(supabase: any, memberId: string) {
  const today = todayRecife();
  const cutoff = addDays(today, -45);
  const { data, error } = await supabase.from("xp_transactions")
    .select("activity,date_key,type")
    .eq("member_id", memberId).eq("type", "earn").gte("date_key", cutoff)
    .order("date_key", { ascending: false }).limit(2000);
  if (error) throw error;
  const ignored = new Set(["legacy_sync", "daily_mission", "streak_7", "streak_30"]);
  const dates = new Set((data ?? []).filter((row: any) => !ignored.has(String(row.activity ?? ""))).map((row: any) => String(row.date_key ?? "")).filter(Boolean));
  let cursor = dates.has(today) ? today : addDays(today, -1);
  let streak = 0;
  while (dates.has(cursor)) { streak++; cursor = addDays(cursor, -1); }
  return streak;
}
async function awardStreak(supabase: any, memberId: string, streak: number) {
  for (const item of [
    { days: 7, activity: "streak_7", amount: 25, description: "Sequência de 7 dias" },
    { days: 30, activity: "streak_30", amount: 100, description: "Sequência de 30 dias" },
  ]) {
    if (streak < item.days) continue;
    const { error } = await supabase.rpc("xp_award", {
      p_member_id: memberId, p_activity: item.activity, p_content_id: `milestone_${item.days}`,
      p_variant: "", p_receipt_id: `streak:${item.days}`, p_amount: item.amount,
      p_description: item.description, p_daily_cap: 0,
    });
    if (error) throw error;
  }
}

function availableNow(item: Record<string, unknown>) {
  const now = Date.now();
  const from = item.available_from ? Date.parse(String(item.available_from)) : NaN;
  const until = item.available_until ? Date.parse(String(item.available_until)) : NaN;
  return (!Number.isFinite(from) || from <= now) && (!Number.isFinite(until) || until >= now);
}

function bibleParts(contentId: string) {
  const parts = contentId.split(":");
  if (parts.length < 3) return null;
  const version = parts.shift() || "";
  const maybeVerse = parts.length >= 3 ? Number(parts.pop()) : null;
  const chapter = Number(parts.pop());
  const book = parts.join(":");
  if (!version || !book || !Number.isInteger(chapter) || chapter < 1 || chapter > (CHAPTER_COUNTS[book] ?? 0)) return null;
  return { version, book, chapter, verse: Number.isInteger(maybeVerse) && Number(maybeVerse) > 0 ? Number(maybeVerse) : null };
}

async function validateNews(projectId: string, token: string, contentId: string) {
  const document = await getDocument(projectId, token, "bible_news", contentId);
  if (!document) throw new HttpError(400, "Notícia não pertence ao catálogo oficial.");
  const data = documentData(document);
  if (data.approved === false || data.isApproved === false) throw new HttpError(400, "Notícia não está aprovada para XP.");
}

async function planDocument(projectId: string, token: string, category: string) {
  const document = await getDocument(projectId, token, "bible_plans", category);
  if (!document) throw new HttpError(400, "Plano não pertence ao catálogo oficial.");
  return documentData(document);
}

function planThemes(data: Record<string, unknown>): string[] {
  const themes = Array.isArray(data.themes) ? data.themes : [];
  return themes.map((item) => item && typeof item === "object" ? clean((item as Record<string, unknown>).title, 300) : "").filter(Boolean);
}

async function validateAward(
  supabase: any,
  projectId: string,
  googleToken: string,
  memberId: string,
  activity: string,
  contentId: string,
): Promise<AwardRule> {
  const rules: Record<string, AwardRule> = {
    active_5min: { amount: 1, cap: 20, description: "5 minutos ativos" },
    bible_verse: { amount: 1, cap: 10, description: "Versículo lido" },
    bible_chapter: { amount: 5, description: "Capítulo bíblico concluído" },
    news_read: { amount: 2, cap: 10, description: "Notícia lida" },
    plan_theme: { amount: 3, description: "Tema de plano concluído" },
    plan_day: { amount: 5, description: "Dia de plano concluído" },
    plan_complete: { amount: 25, description: "Plano concluído" },
  };
  const rule = rules[activity];
  if (!rule) throw new HttpError(400, "Atividade XP não permitida na PWA.");

  if (activity === "bible_chapter" || activity === "bible_verse") {
    const parsed = bibleParts(contentId);
    if (!parsed) throw new HttpError(400, "Referência bíblica inválida.");
    if (activity === "bible_verse" && !parsed.verse) throw new HttpError(400, "Versículo inválido.");
  } else if (activity === "news_read") {
    await validateNews(projectId, googleToken, contentId);
  } else if (activity === "plan_theme" || activity === "plan_day") {
    const separator = contentId.indexOf("::");
    if (separator <= 0) throw new HttpError(400, "Tema de plano inválido.");
    const category = contentId.slice(0, separator);
    const theme = contentId.slice(separator + 2);
    const data = await planDocument(projectId, googleToken, category);
    if (!planThemes(data).includes(theme)) throw new HttpError(400, "Tema não pertence ao plano oficial.");
  } else if (activity === "plan_complete") {
    const data = await planDocument(projectId, googleToken, contentId);
    const expected = planThemes(data).map((theme) => `${contentId}::${theme}`);
    if (!expected.length) throw new HttpError(400, "Plano não possui temas válidos.");
    const { data: completed, error } = await supabase.from("xp_transactions")
      .select("content_id").eq("member_id", memberId).eq("type", "earn").eq("activity", "plan_day");
    if (error) throw error;
    const done = new Set((completed ?? []).map((row: any) => String(row.content_id ?? "")));
    if (!expected.every((id) => done.has(id))) throw new HttpError(409, "Conclua todos os temas deste plano antes do bônus final.");
  }
  return rule;
}

Deno.serve(async (request: Request) => {
  if (request.method === "OPTIONS") return json({ ok: true });
  if (request.method !== "POST") return json({ error: "Método não permitido." }, 405);

  try {
    const claims = await verifyFirebaseToken(bearer(request));
    const memberId = clean(claims.user_id || claims.sub, 180);
    if (!memberId) throw new HttpError(401, "Sessão sem identificador de membro.");

    const serviceAccount = JSON.parse(Deno.env.get("FIREBASE_SERVICE_ACCOUNT_JSON") || "{}") as ServiceAccount;
    const projectId = serviceAccount.project_id || FIREBASE_PROJECT_ID;
    const googleToken = await googleAccessToken(serviceAccount);
    const member = await loadMember(projectId, googleToken, memberId);

    const supabaseUrl = Deno.env.get("SUPABASE_URL") || "";
    const serviceRole = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") || "";
    if (!supabaseUrl || !serviceRole) throw new HttpError(500, "Backend XP não configurado.");
    const supabase = createClient(supabaseUrl, serviceRole, { auth: { persistSession: false, autoRefreshToken: false } });

    const { data: ensured, error: ensureError } = await supabase.rpc("xp_ensure_account", { p_member_id: memberId, p_legacy_xp: member.legacyXp });
    if (ensureError) throw ensureError;
    let account = Array.isArray(ensured) ? ensured[0] : ensured;
    if (!account) throw new HttpError(500, "Conta XP não pôde ser inicializada.");

    const input = await request.json().catch(() => ({})) as Record<string, unknown>;
    const action = clean(input.action || "dashboard", 60);

    if (action === "dashboard") {
      const streak = await loadStreak(supabase, memberId);
      if (member.xpUnlocked) await awardStreak(supabase, memberId, streak);
      const { data: refreshed } = await supabase.from("xp_accounts").select("member_id,total_earned,total_spent,balance,migrated_legacy_xp,updated_at").eq("member_id", memberId).single();
      if (refreshed) account = refreshed;
      const [historyResult, catalogResult, redemptionResult, entitlementResult] = await Promise.all([
        supabase.from("xp_transactions").select("id,type,amount,activity,content_id,variant,receipt_id,description,date_key,created_at").eq("member_id", memberId).order("created_at", { ascending: false }).limit(100),
        supabase.from("xp_shop_items").select("id,name,description,cost,category,kind,image_url,stock,limit_per_member,active,available_from,available_until").eq("active", true).order("cost", { ascending: true }),
        supabase.from("xp_redemptions").select("id,item_id,item_name,cost,status,redemption_code,created_at,delivered_at").eq("member_id", memberId).order("created_at", { ascending: false }).limit(100),
        supabase.from("xp_entitlements").select("id,item_id,item_name,kind,unlocked_at").eq("member_id", memberId).order("unlocked_at", { ascending: false }).limit(100),
      ]);
      for (const result of [historyResult, catalogResult, redemptionResult, entitlementResult]) if (result.error) throw result.error;
      const catalog = (catalogResult.data ?? []).filter((item: any) => availableNow(item));
      return json({
        ok: true, unlocked: member.xpUnlocked, account, streak,
        transactions: historyResult.data ?? [], items: catalog,
        redemptions: redemptionResult.data ?? [], entitlements: entitlementResult.data ?? [],
      });
    }

    if (action === "award") {
      const activity = clean(input.activity, 60);
      const contentId = clean(input.contentId, 1000);
      if (!activity || !contentId) throw new HttpError(400, "Atividade incompleta.");
      if (!member.xpUnlocked) return json({ ok: true, unlocked: false, granted: 0, reason: "xp_locked", account });
      const rule = await validateAward(supabase, projectId, googleToken, memberId, activity, contentId);
      const { data, error } = await supabase.rpc("xp_award", {
        p_member_id: memberId,
        p_activity: activity,
        p_content_id: contentId,
        p_variant: "",
        p_receipt_id: `${activity}:${contentId}`,
        p_amount: rule.amount,
        p_description: rule.description,
        p_daily_cap: rule.cap ?? 0,
      });
      if (error) throw error;
      const result = Array.isArray(data) ? data[0] : data;
      if (!result) throw new HttpError(500, "Ledger XP não retornou resultado.");
      account = {
        member_id: memberId,
        total_earned: Number(result.total_earned ?? account.total_earned ?? 0),
        total_spent: Number(result.total_spent ?? account.total_spent ?? 0),
        balance: Number(result.balance ?? account.balance ?? 0),
        migrated_legacy_xp: Number(account.migrated_legacy_xp ?? 0),
        updated_at: new Date().toISOString(),
      };
      return json({ ok: true, unlocked: true, granted: Number(result.granted ?? 0), duplicate: Boolean(result.duplicate), reason: result.cap_reached ? "daily_cap" : "", account });
    }

    if (action === "redeem") {
      if (!member.xpUnlocked) throw new HttpError(403, "A Loja XP é liberada no Nível 8.");
      const itemId = clean(input.itemId, 100);
      const expectedCost = Math.floor(Number(input.expectedCost ?? 0));
      if (!itemId || !Number.isFinite(expectedCost) || expectedCost <= 0) throw new HttpError(400, "Resgate inválido.");
      const { data, error } = await supabase.rpc("xp_redeem_checked", { p_member_id: memberId, p_item_id: itemId, p_expected_cost: expectedCost });
      if (error) throw error;
      const row = Array.isArray(data) ? data[0] : data;
      if (!row) throw new HttpError(500, "O resgate não retornou resultado.");
      if (row.redemption_id) {
        await supabase.from("xp_redemptions").update({ member_name: member.name }).eq("id", row.redemption_id);
      }
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

    throw new HttpError(400, "Ação XP inválida.");
  } catch (error) {
    if (error instanceof HttpError) return json({ error: error.message }, error.status);
    const message = error instanceof Error ? error.message : "Falha na Jornada XP.";
    console.error("pwa-xp failed", message);
    const lowered = message.toLowerCase();
    const status = lowered.includes("saldo xp insuficiente") || lowered.includes("esgotada") || lowered.includes("limite de resgate") || lowered.includes("preço") ? 409 : 500;
    return json({ error: message }, status);
  }
});
