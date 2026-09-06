import { createClient } from "npm:@supabase/supabase-js@2.57.4";
import { createRemoteJWKSet, importPKCS8, jwtVerify, SignJWT } from "npm:jose@5.10.0";

const FIRESTORE_SCOPE = "https://www.googleapis.com/auth/datastore";
const TOKEN_URL = "https://oauth2.googleapis.com/token";
const FIREBASE_PROJECT_ID = "mic-rhema";
const FIREBASE_ISSUER = `https://securetoken.google.com/${FIREBASE_PROJECT_ID}`;
const FIREBASE_JWKS = createRemoteJWKSet(new URL("https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com"));
const DEFAULT_PROJECT_ID = "mic-rhema";
const CURRENT_PUBLISHABLE_KEY = "sb_publishable_Dv98hBnbJB2TzRCG6aJNwA_KMPHLZSw";
const STATIC_CATALOG_COMMIT = "62744bb0e22b98a4dff276ed974e006dfbd332f6";
const STREAK_IGNORED = new Set(["legacy_sync", "daily_mission", "streak_7", "streak_30"]);
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
type FirestoreValue = {
  stringValue?: string; booleanValue?: boolean; integerValue?: string; doubleValue?: number;
  timestampValue?: string; arrayValue?: { values?: FirestoreValue[] };
  mapValue?: { fields?: Record<string, FirestoreValue> }; nullValue?: null;
};
type FirestoreDocument = { name?: string; fields?: Record<string, FirestoreValue> };
type AwardRule = { amount: number; description: string; dailyCapXp?: number };
type Identity = { memberId: string; firebase: boolean; phone: string };

type MemberInfo = { name: string; xpUnlocked: boolean; legacyXp: number };

const AWARD_RULES: Record<string, AwardRule> = {
  active_5min: { amount: 1, description: "5 minutos ativos", dailyCapXp: 20 },
  bible_verse: { amount: 1, description: "Versículo lido", dailyCapXp: 10 },
  bible_chapter: { amount: 5, description: "Capítulo bíblico concluído" },
  devotional: { amount: 5, description: "Devocional concluído" },
  news_read: { amount: 2, description: "Notícia lida", dailyCapXp: 10 },
  plan_theme: { amount: 3, description: "Tema de plano concluído" },
  plan_day: { amount: 5, description: "Dia de plano concluído" },
  plan_complete: { amount: 25, description: "Plano concluído" },
  ibr_lesson: { amount: 10, description: "Aula IBR concluída" },
  prayer_sent: { amount: 5, description: "Pedido de oração enviado", dailyCapXp: 5 },
};

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-client-info",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Content-Type": "application/json; charset=utf-8",
  "Cache-Control": "no-store",
};
const json = (body: Record<string, unknown>, status = 200) => new Response(JSON.stringify(body), { status, headers: corsHeaders });
const clean = (value: unknown, max = 1000) => String(value ?? "").trim().slice(0, max);
const normalizePhone = (value: unknown) => {
  const digits = String(value ?? "").replace(/\D/g, "");
  return digits.length >= 12 && digits.length <= 13 && digits.startsWith("55") ? digits.slice(2) : digits;
};

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
  return document ? Object.fromEntries(Object.entries(document.fields ?? {}).map(([k, v]) => [k, fromValue(v)])) : {};
}
const stringList = (value: unknown): string[] => Array.isArray(value) ? value.map(String).filter(Boolean) : [];
function activityMap(value: unknown): Record<string, string[]> {
  if (!value || typeof value !== "object" || Array.isArray(value)) return {};
  return Object.fromEntries(Object.entries(value as Record<string, unknown>).map(([k, v]) => [k, stringList(v)]));
}
function parseLegacyXp(entry: string) {
  const value = Number(entry.slice(entry.lastIndexOf("=") + 1));
  return Number.isFinite(value) && value > 0 ? Math.floor(value) : 0;
}
function firestoreBase(projectId: string) {
  return `https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents`;
}
async function googleAccessToken(account: ServiceAccount) {
  if (!account.client_email || !account.private_key) throw new Error("Conta de serviço Firebase incompleta.");
  const now = Math.floor(Date.now() / 1000);
  const assertion = await new SignJWT({ iss: account.client_email, scope: FIRESTORE_SCOPE, aud: TOKEN_URL })
    .setProtectedHeader({ alg: "RS256", typ: "JWT" }).setIssuedAt(now).setExpirationTime(now + 3600)
    .sign(await importPKCS8(account.private_key.replace(/\\n/g, "\n"), "RS256"));
  const response = await fetch(TOKEN_URL, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth2:grant-type:jwt-bearer", assertion }),
  });
  const payload = await response.json();
  if (!response.ok || !payload.access_token) throw new Error("Falha ao autenticar no Firebase.");
  return String(payload.access_token);
}
async function getDocumentPath(projectId: string, token: string, path: string): Promise<FirestoreDocument | null> {
  const encoded = path.split("/").map((part) => encodeURIComponent(part)).join("/");
  const response = await fetch(`${firestoreBase(projectId)}/${encoded}`, { headers: { Authorization: `Bearer ${token}` } });
  if (response.status === 404) return null;
  if (!response.ok) throw new Error(`Falha ao consultar catálogo (${response.status}).`);
  return await response.json() as FirestoreDocument;
}
async function getDocument(projectId: string, token: string, collection: string, id: string) {
  return getDocumentPath(projectId, token, `${collection}/${id}`);
}

async function resolveIdentity(request: Request, input: Record<string, unknown>): Promise<Identity> {
  const auth = request.headers.get("authorization") ?? "";
  const bearer = auth.match(/^Bearer\s+(.+)$/i)?.[1] ?? "";
  const providedKey = request.headers.get("apikey") ?? "";
  const legacyKey = Deno.env.get("SUPABASE_ANON_KEY") ?? "";
  const legacy = Boolean(legacyKey) && providedKey === legacyKey && bearer === legacyKey;
  const publishable = providedKey === CURRENT_PUBLISHABLE_KEY && bearer === CURRENT_PUBLISHABLE_KEY;
  if (legacy || publishable) {
    const memberId = clean(input.memberId, 200);
    const phone = normalizePhone(input.phone);
    if (!memberId || phone.length < 10 || phone.length > 11) throw new Error("Membro inválido.");
    return { memberId, firebase: false, phone };
  }
  if (!bearer) throw new Error("Sessão Firebase ausente.");
  const verified = await jwtVerify(bearer, FIREBASE_JWKS, {
    issuer: FIREBASE_ISSUER,
    audience: FIREBASE_PROJECT_ID,
    algorithms: ["RS256"],
  });
  const memberId = clean(verified.payload.sub, 200);
  if (!memberId) throw new Error("Sessão Firebase inválida.");
  return { memberId, firebase: true, phone: "" };
}

async function loadMember(projectId: string, token: string, identity: Identity): Promise<MemberInfo> {
  const access = documentData(await getDocument(projectId, token, "acessos_pendentes", identity.memberId));
  if (!Object.keys(access).length) throw new Error("Cadastro do membro não encontrado.");
  if (identity.firebase) {
    if (access.isApproved !== true && access.isAdmin !== true) throw new Error("Acesso do membro ainda não aprovado.");
  } else if (normalizePhone(access.phone) !== identity.phone) {
    throw new Error("Identidade do membro não confere.");
  }
  const user = documentData(await getDocument(projectId, token, "users", identity.memberId));
  const unlocked = new Set([...stringList(access.unlockedBadgeIds), ...stringList(user.unlockedBadgeIds)]);
  const legacyEntries = [...new Set([
    ...(activityMap(access.badgeActivityIds).journey_xp_awards ?? []),
    ...(activityMap(user.badgeActivityIds).journey_xp_awards ?? []),
  ])];
  return {
    name: clean(access.name || user.name || "Membro MIC Rhema", 120),
    xpUnlocked: [...unlocked].some((id) => LEVEL_8_PLUS.has(id)),
    legacyXp: legacyEntries.reduce((sum, item) => sum + parseLegacyXp(item), 0),
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
    .select("activity,date_key,type").eq("member_id", memberId).eq("type", "earn")
    .gte("date_key", cutoff).order("date_key", { ascending: false }).limit(2000);
  if (error) throw error;
  const dates = new Set((data ?? [])
    .filter((row: any) => !STREAK_IGNORED.has(String(row.activity ?? "")))
    .map((row: any) => String(row.date_key ?? "")).filter(Boolean));
  let cursor = dates.has(today) ? today : addDays(today, -1);
  let streak = 0;
  while (dates.has(cursor)) { streak++; cursor = addDays(cursor, -1); }
  return streak;
}
async function awardStreakMilestones(supabase: any, memberId: string, streak: number) {
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
async function dailyMissionState(supabase: any, memberId: string) {
  const { data, error } = await supabase.rpc("xp_daily_mission_state", { p_member_id: memberId });
  if (error) throw error;
  return (data ?? {}) as Record<string, unknown>;
}
async function refreshDailyMission(supabase: any, memberId: string) {
  let state = await dailyMissionState(supabase, memberId);
  if (state.complete === true && state.bonusGranted !== true) {
    const date = clean(state.date, 20) || todayRecife();
    const { error } = await supabase.rpc("xp_award", {
      p_member_id: memberId,
      p_activity: "daily_mission",
      p_content_id: date,
      p_variant: "",
      p_receipt_id: `daily_mission:${date}`,
      p_amount: 10,
      p_description: "Jornada diária concluída",
      p_daily_cap: 10,
    });
    if (error) throw error;
    state = await dailyMissionState(supabase, memberId);
  }
  return state;
}

function bibleParts(contentId: string) {
  const parts = contentId.split(":");
  if (parts.length < 3) return null;
  const version = parts.shift() ?? "";
  const maybeVerse = parts.length >= 3 ? Number(parts.pop()) : null;
  const chapter = Number(parts.pop());
  const book = parts.join(":");
  if (!version || !book || !Number.isInteger(chapter) || chapter < 1 || chapter > (CHAPTER_COUNTS[book] ?? 0)) return null;
  const verse = Number.isInteger(maybeVerse) && Number(maybeVerse) > 0 && Number(maybeVerse) <= 200 ? Number(maybeVerse) : null;
  return { version, book, chapter, verse };
}

let staticPlans: Map<string, Set<string>> | null = null;
async function loadStaticPlans() {
  if (staticPlans) return staticPlans;
  const url = `https://raw.githubusercontent.com/bichocutela/Mic-RHEMA-Ia-Studio-Google/${STATIC_CATALOG_COMMIT}/app/src/main/java/com/aistudio/micrhema/PlansData.kt`;
  const response = await fetch(url, { headers: { "User-Agent": "MIC-Rhema-XP-Content" } });
  if (!response.ok) throw new Error("Catálogo estático de planos indisponível.");
  const map = new Map<string, Set<string>>();
  let category = "";
  for (const raw of (await response.text()).split(/\r?\n/)) {
    const line = raw.trim();
    const categoryMatch = line.match(/^PlanCategory\("((?:\\.|[^"\\])+)"/);
    if (categoryMatch) {
      category = categoryMatch[1].replace(/\\"/g, '"').replace(/\\\\/g, "\\");
      if (!map.has(category)) map.set(category, new Set());
      continue;
    }
    const themeMatch = line.match(/^PlanTheme\("((?:\\.|[^"\\])+)"/);
    if (category && themeMatch) {
      map.get(category)?.add(themeMatch[1].replace(/\\"/g, '"').replace(/\\\\/g, "\\"));
    }
  }
  if (!map.size) throw new Error("Catálogo estático de planos vazio.");
  staticPlans = map;
  return map;
}

let staticNewsIds: Set<string> | null = null;
async function loadStaticNewsIds() {
  if (staticNewsIds) return staticNewsIds;
  const paths = ["BibleNewsData.kt", "BibleNewsEditorialCatalog.kt"];
  const ids = new Set<string>();
  for (const path of paths) {
    const url = `https://raw.githubusercontent.com/bichocutela/Mic-RHEMA-Ia-Studio-Google/${STATIC_CATALOG_COMMIT}/app/src/main/java/com/aistudio/micrhema/${path}`;
    const response = await fetch(url, { headers: { "User-Agent": "MIC-Rhema-XP-Content" } });
    if (!response.ok) continue;
    const source = await response.text();
    for (const match of source.matchAll(/BibleNews\(\s*(\d+)/g)) ids.add(match[1]);
    for (const match of source.matchAll(/\bid\s*=\s*(\d+)/g)) ids.add(match[1]);
  }
  staticNewsIds = ids;
  return ids;
}

function planThemes(data: Record<string, unknown>) {
  return (Array.isArray(data.themes) ? data.themes : [])
    .map((item) => item && typeof item === "object" ? clean((item as Record<string, unknown>).title, 300) : "")
    .filter(Boolean);
}
async function validatePlan(projectId: string, token: string, activity: string, contentId: string) {
  const separator = contentId.indexOf("::");
  const category = activity === "plan_complete" ? contentId : separator > 0 ? contentId.slice(0, separator) : "";
  const theme = activity === "plan_complete" ? "" : separator > 0 ? contentId.slice(separator + 2) : "";
  if (!category || (activity !== "plan_complete" && !theme)) throw new Error("Identificador de plano inválido.");
  const remote = documentData(await getDocument(projectId, token, "bible_plans", category));
  if (Object.keys(remote).length) {
    if (activity === "plan_complete" || planThemes(remote).includes(theme)) return;
    throw new Error("Tema não pertence ao plano oficial.");
  }
  const local = await loadStaticPlans();
  const themes = local.get(category);
  if (!themes || (activity !== "plan_complete" && !themes.has(theme))) throw new Error("Plano não pertence ao catálogo oficial.");
}
async function validateDevotional(projectId: string, token: string, contentId: string) {
  for (const collection of ["devotionals", "devocionais"]) {
    const document = await getDocument(projectId, token, collection, contentId);
    if (document) {
      const data = documentData(document);
      if (data.isApproved !== false && data.approved !== false) return;
    }
  }
  if (["1", "2", "3", "4"].includes(contentId)) return;
  const auto = contentId.match(/^auto-2027-(2027-\d{2}-\d{2})$/);
  if (auto && Number.isFinite(Date.parse(`${auto[1]}T00:00:00Z`))) return;
  throw new Error("Devocional não pertence ao catálogo oficial.");
}
async function validateNews(projectId: string, token: string, contentId: string) {
  const remote = await getDocument(projectId, token, "bible_news", contentId);
  if (remote) {
    const data = documentData(remote);
    if (data.isApproved !== false && data.approved !== false) return;
  }
  const ids = await loadStaticNewsIds();
  if (ids.has(contentId)) return;
  throw new Error("Notícia não pertence ao catálogo oficial.");
}
async function validateIbr(projectId: string, token: string, memberId: string, contentId: string) {
  const separator = contentId.indexOf(":");
  if (separator <= 0 || separator >= contentId.length - 1) throw new Error("Aula IBR inválida.");
  const courseId = contentId.slice(0, separator);
  const chapterId = contentId.slice(separator + 1);
  const progress = documentData(await getDocumentPath(projectId, token, `users/${memberId}/ibrProgress/${courseId}_${chapterId}`));
  if (!Object.keys(progress).length || progress.isCompleted !== true || clean(progress.courseId, 300) !== courseId || clean(progress.chapterId, 300) !== chapterId) {
    throw new Error("Aula IBR ainda não está concluída no progresso sincronizado.");
  }
}
async function validatePrayer(projectId: string, token: string, memberId: string, contentId: string) {
  const request = documentData(await getDocument(projectId, token, "prayer_requests", contentId));
  if (!Object.keys(request).length || clean(request.requesterUid, 200) !== memberId) throw new Error("Pedido de oração não pertence ao membro autenticado.");
}
async function validateAward(projectId: string, token: string, memberId: string, activity: string, contentId: string) {
  const rule = AWARD_RULES[activity];
  if (!rule) throw new Error("Atividade XP não permitida neste motor.");
  if (activity === "active_5min") {
    const match = contentId.match(/^(\d{4}-\d{2}-\d{2}):(\d{1,3})$/);
    if (!match || match[1] !== todayRecife() || Number(match[2]) < 1 || Number(match[2]) > 100) throw new Error("Bloco de tempo ativo inválido.");
  } else if (activity === "bible_chapter" || activity === "bible_verse") {
    const parsed = bibleParts(contentId);
    if (!parsed) throw new Error("Referência bíblica inválida.");
    if (activity === "bible_chapter" && parsed.verse !== null) throw new Error("Capítulo bíblico inválido.");
    if (activity === "bible_verse" && parsed.verse === null) throw new Error("Versículo bíblico inválido.");
  } else if (activity === "devotional") {
    await validateDevotional(projectId, token, contentId);
  } else if (activity === "news_read") {
    await validateNews(projectId, token, contentId);
  } else if (activity === "plan_theme" || activity === "plan_day" || activity === "plan_complete") {
    await validatePlan(projectId, token, activity, contentId);
  } else if (activity === "ibr_lesson") {
    await validateIbr(projectId, token, memberId, contentId);
  } else if (activity === "prayer_sent") {
    await validatePrayer(projectId, token, memberId, contentId);
  }
  return rule;
}

Deno.serve(async (request) => {
  if (request.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (request.method !== "POST") return json({ error: "Método não permitido." }, 405);
  try {
    const input = await request.json() as Record<string, unknown>;
    const action = clean(input.action || "get_account", 40);
    const identity = await resolveIdentity(request, input);
    if ((action === "award" || action === "journey_state") && !identity.firebase) {
      return json({ error: "Atualize o MIC Rhema e entre novamente para sincronizar a Jornada XP com segurança." }, 401);
    }

    const serviceAccount = JSON.parse(Deno.env.get("FIREBASE_SERVICE_ACCOUNT_JSON") ?? "{}") as ServiceAccount;
    const projectId = serviceAccount.project_id || Deno.env.get("FIREBASE_PROJECT_ID") || DEFAULT_PROJECT_ID;
    const googleToken = await googleAccessToken(serviceAccount);
    const member = await loadMember(projectId, googleToken, identity);

    const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
    const serviceRole = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
    if (!supabaseUrl || !serviceRole) throw new Error("Backend do XP não configurado.");
    const supabase = createClient(supabaseUrl, serviceRole, { auth: { persistSession: false, autoRefreshToken: false } });

    const { data: ensured, error: ensureError } = await supabase.rpc("xp_ensure_account", {
      p_member_id: identity.memberId,
      p_legacy_xp: member.legacyXp,
    });
    if (ensureError) throw ensureError;
    let account = Array.isArray(ensured) ? ensured[0] : ensured;
    if (!account) throw new Error("Conta XP não pôde ser inicializada.");

    if (identity.firebase && member.xpUnlocked) {
      const { error } = await supabase.rpc("xp_capture_journey_baseline", { p_member_id: identity.memberId });
      if (error) throw error;
    }

    if (action === "get_account") return json({ ok: true, unlocked: member.xpUnlocked, account });

    if (action === "history") {
      const limit = Math.min(100, Math.max(1, Number(input.limit ?? 50)));
      const { data, error } = await supabase.from("xp_transactions")
        .select("id,type,amount,activity,content_id,variant,receipt_id,description,date_key,created_at")
        .eq("member_id", identity.memberId).order("created_at", { ascending: false }).limit(limit);
      if (error) throw error;
      return json({ ok: true, unlocked: member.xpUnlocked, account, transactions: data ?? [] });
    }

    if (action === "journey_state") {
      const streak = await loadStreak(supabase, identity.memberId);
      if (member.xpUnlocked) await awardStreakMilestones(supabase, identity.memberId, streak);
      const dailyMission = member.xpUnlocked
        ? await refreshDailyMission(supabase, identity.memberId)
        : await dailyMissionState(supabase, identity.memberId);
      const { data, error } = await supabase.from("xp_accounts")
        .select("member_id,total_earned,total_spent,balance,migrated_legacy_xp,updated_at")
        .eq("member_id", identity.memberId).single();
      if (error) throw error;
      return json({ ok: true, unlocked: member.xpUnlocked, account: data, streak, dailyMission });
    }

    if (action !== "award") return json({ error: "Ação inválida." }, 400);
    const activity = clean(input.activity, 80);
    const contentId = clean(input.contentId, 1000);
    const variant = clean(input.variant, 40);
    if (!activity || !contentId) return json({ error: "Atividade incompleta." }, 400);
    if (variant) return json({ error: "Variação não permitida para esta atividade." }, 400);
    if (!member.xpUnlocked) return json({ ok: true, unlocked: false, granted: 0, reason: "xp_locked", account });

    const rule = await validateAward(projectId, googleToken, identity.memberId, activity, contentId);
    const receiptId = `${activity}:${contentId}`;
    const { data, error } = await supabase.rpc("xp_award", {
      p_member_id: identity.memberId,
      p_activity: activity,
      p_content_id: contentId,
      p_variant: "",
      p_receipt_id: receiptId,
      p_amount: rule.amount,
      p_description: rule.description,
      p_daily_cap: rule.dailyCapXp ?? 0,
    });
    if (error) throw error;
    const result = Array.isArray(data) ? data[0] : data;
    if (!result) throw new Error("O ledger não retornou o resultado do XP.");
    account = {
      member_id: identity.memberId,
      total_earned: Number(result.total_earned ?? account.total_earned ?? 0),
      total_spent: Number(result.total_spent ?? account.total_spent ?? 0),
      balance: Number(result.balance ?? account.balance ?? 0),
      migrated_legacy_xp: Number(account.migrated_legacy_xp ?? 0),
      updated_at: new Date().toISOString(),
    };
    return json({
      ok: true,
      unlocked: true,
      granted: Number(result.granted ?? 0),
      duplicate: Boolean(result.duplicate),
      reason: result.cap_reached ? "daily_cap" : "",
      receiptId,
      account,
    });
  } catch (error) {
    console.error("xp-engine failed", error);
    const message = error instanceof Error ? error.message : "Falha no motor de XP.";
    const lowered = message.toLowerCase();
    const status = lowered.includes("sessão") || lowered.includes("identidade") ? 401
      : lowered.includes("inválid") || lowered.includes("não pertence") || lowered.includes("ainda não") ? 400
      : 500;
    return json({ error: message }, status);
  }
});
