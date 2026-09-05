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
type FirestoreDocument = { name?: string; fields?: Record<string, FirestoreValue> };
type AwardRule = { amount: number; description: string; dailyCapXp?: number };

const AWARD_RULES: Record<string, AwardRule> = {
  active_5min: { amount: 1, description: "5 minutos ativos", dailyCapXp: 20 },
  bible_verse: { amount: 1, description: "Versículo lido", dailyCapXp: 10 },
  bible_chapter: { amount: 5, description: "Capítulo bíblico concluído" },
  devotional: { amount: 5, description: "Devocional concluído" },
  news_read: { amount: 2, description: "Notícia lida", dailyCapXp: 10 },
  plan_theme: { amount: 3, description: "Tema de plano concluído" },
  plan_day: { amount: 5, description: "Dia de plano concluído" },
  plan_complete: { amount: 25, description: "Plano concluído" },
  book_10: { amount: 3, description: "Progresso de leitura no livro" },
  book_complete: { amount: 25, description: "Livro concluído" },
  audio_10min: { amount: 3, description: "10 minutos de áudio" },
  audio_90: { amount: 8, description: "Áudio concluído" },
  video_10min: { amount: 3, description: "10 minutos de vídeo" },
  video_90: { amount: 8, description: "Vídeo concluído" },
  ibr_lesson: { amount: 10, description: "Aula IBR concluída" },
  prayer_sent: { amount: 5, description: "Pedido de oração enviado", dailyCapXp: 5 },
  quiz_easy: { amount: 10, description: "Pergunta fácil correta" },
  quiz_medium: { amount: 20, description: "Pergunta média correta" },
  quiz_hard: { amount: 30, description: "Pergunta difícil correta" },
  daily_mission: { amount: 10, description: "Missão diária concluída", dailyCapXp: 40 },
  streak_7: { amount: 25, description: "Sequência de 7 dias" },
  streak_30: { amount: 100, description: "Sequência de 30 dias" },
};

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

function effectiveRule(activity: string, variant: string): AwardRule | null {
  const base = AWARD_RULES[activity];
  if (!base) return null;
  if (!activity.startsWith("quiz_")) return base;
  const multiplier = variant === "easy_hint" ? 0.7 : variant === "subtle_hint" ? 0.9 : 1;
  return { ...base, amount: Math.round(base.amount * multiplier) };
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
    const action = String(input.action ?? "get_account");
    const memberId = String(input.memberId ?? "").trim();
    const phone = normalizePhone(input.phone);
    if (!memberId || phone.length < 10 || phone.length > 11) return json({ error: "Membro inválido." }, 400);

    const serviceAccount = JSON.parse(Deno.env.get("FIREBASE_SERVICE_ACCOUNT_JSON") ?? "{}") as ServiceAccount;
    const projectId = serviceAccount.project_id || Deno.env.get("FIREBASE_PROJECT_ID") || DEFAULT_PROJECT_ID;
    const googleToken = await googleAccessToken(serviceAccount);
    const member = await loadMember(projectId, googleToken, memberId, phone);

    const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
    const serviceRole = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
    if (!supabaseUrl || !serviceRole) throw new Error("Backend do XP não configurado.");
    const supabase = createClient(supabaseUrl, serviceRole, { auth: { persistSession: false, autoRefreshToken: false } });

    const { data: accountRows, error: accountError } = await supabase.rpc("xp_ensure_account", {
      p_member_id: memberId,
      p_legacy_xp: member.legacyXp,
    });
    if (accountError) throw accountError;
    let account = Array.isArray(accountRows) ? accountRows[0] : accountRows;
    if (!account) throw new Error("Conta XP não pôde ser inicializada.");

    if (action === "get_account") return json({ ok: true, unlocked: member.xpUnlocked, account });

    if (action === "history") {
      const limit = Math.min(100, Math.max(1, Number(input.limit ?? 50)));
      const { data, error } = await supabase
        .from("xp_transactions")
        .select("id,type,amount,activity,content_id,variant,receipt_id,description,date_key,created_at")
        .eq("member_id", memberId)
        .order("created_at", { ascending: false })
        .limit(limit);
      if (error) throw error;
      return json({ ok: true, unlocked: member.xpUnlocked, account, transactions: data ?? [] });
    }

    if (action === "award") {
      if (!member.xpUnlocked) return json({ ok: true, unlocked: false, granted: 0, reason: "xp_locked", account });
      const activity = String(input.activity ?? "").trim();
      const contentId = String(input.contentId ?? "").trim().slice(0, 220);
      const variant = String(input.variant ?? "").trim().slice(0, 40);
      if (!activity || !contentId) return json({ error: "Atividade incompleta." }, 400);
      const rule = effectiveRule(activity, variant);
      if (!rule) return json({ error: "Atividade de XP não reconhecida." }, 400);
      const receiptId = `${activity}:${contentId}:${variant || "base"}`;
      const { data, error } = await supabase.rpc("xp_award", {
        p_member_id: memberId,
        p_activity: activity,
        p_content_id: contentId,
        p_variant: variant,
        p_receipt_id: receiptId,
        p_amount: rule.amount,
        p_description: rule.description,
        p_daily_cap: rule.dailyCapXp ?? 0,
      });
      if (error) throw error;
      const result = Array.isArray(data) ? data[0] : data;
      if (!result) throw new Error("O ledger não retornou o resultado do XP.");
      account = {
        member_id: memberId,
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
    }

    return json({ error: "Ação inválida." }, 400);
  } catch (error) {
    console.error("xp-engine failed", error);
    return json({ error: error instanceof Error ? error.message : "Falha no motor de XP." }, 500);
  }
});
