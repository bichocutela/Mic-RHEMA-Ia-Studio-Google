import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "npm:@supabase/supabase-js@2.57.4";
import { createRemoteJWKSet, importPKCS8, jwtVerify, SignJWT } from "npm:jose@5.10.0";

const FIREBASE_PROJECT_ID = "mic-rhema";
const FIREBASE_ISSUER = `https://securetoken.google.com/${FIREBASE_PROJECT_ID}`;
const FIREBASE_JWKS = createRemoteJWKSet(new URL("https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com"));
const GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
const FIRESTORE_SCOPE = "https://www.googleapis.com/auth/datastore";
const CURRENT_PUBLISHABLE_KEY = "sb_publishable_Dv98hBnbJB2TzRCG6aJNwA_KMPHLZSw";
const SOURCE_URLS = [
  "https://raw.githubusercontent.com/bichocutela/Mic-RHEMA-Ia-Studio-Google/main/app/src/main/java/com/aistudio/micrhema/BibleQuizCatalog.kt",
  "https://raw.githubusercontent.com/bichocutela/Mic-RHEMA-Ia-Studio-Google/main/app/src/main/java/com/aistudio/micrhema/BibleQuizExpansion.kt",
];
const LEVEL_8_PLUS = new Set([
  "semente_da_fe","caminho_da_promessa","escudo_da_fe","aguas_vivas","videira_verdadeira",
  "luz_do_mundo","armadura_de_deus","leao_de_juda","chama_do_espirito","coroa_da_vida",
  "asas_da_promessa","tabernaculo","arca_da_alianca","nova_jerusalem","gloria_eterna",
]);

type ServiceAccount = { project_id?: string; client_email?: string; private_key?: string };
type FirestoreValue = { stringValue?: string; booleanValue?: boolean; integerValue?: string; doubleValue?: number; timestampValue?: string; arrayValue?: { values?: FirestoreValue[] }; mapValue?: { fields?: Record<string, FirestoreValue> }; nullValue?: null };
type FirestoreDocument = { fields?: Record<string, FirestoreValue> };
type Difficulty = "easy" | "medium" | "hard";
type Activity = "quiz_easy" | "quiz_medium" | "quiz_hard";
type QuizQuestion = { id: string; difficulty: Difficulty; activity: Activity; prompt: string; options: string[]; correctOptionIndex: number; hardHint: string; easyHint: string; reference: string; explanation: string };
type MemberAuth = { memberId: string; xpUnlocked: boolean; legacyXp: number; legacyQuizIds: string[] };

let catalogCache: Map<string, QuizQuestion> | null = null;
const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-client-info",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Content-Type": "application/json; charset=utf-8",
  "Cache-Control": "no-store",
};
const json = (body: Record<string, unknown>, status = 200) => new Response(JSON.stringify(body), { status, headers: cors });
const clean = (value: unknown, max = 1000) => String(value ?? "").trim().slice(0, max);
const normalizePhone = (value: unknown) => { const digits = String(value ?? "").replace(/\D/g, ""); return digits.length >= 12 && digits.length <= 13 && digits.startsWith("55") ? digits.slice(2) : digits; };

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
function docData(doc?: FirestoreDocument | null) { return Object.fromEntries(Object.entries(doc?.fields ?? {}).map(([k,v]) => [k, fromValue(v)])); }
function stringList(value: unknown) { return Array.isArray(value) ? value.map(String).filter(Boolean) : []; }
function activityMap(value: unknown): Record<string,string[]> {
  if (!value || typeof value !== "object" || Array.isArray(value)) return {};
  return Object.fromEntries(Object.entries(value as Record<string,unknown>).map(([k,v]) => [k,stringList(v)]));
}
function parseLegacyXp(entry: string) { const n = Number(entry.slice(entry.lastIndexOf("=") + 1)); return Number.isFinite(n) && n > 0 ? Math.floor(n) : 0; }
function parseLegacyQuizIds(entries: string[]) {
  return [...new Set(entries.map(entry => {
    const cut = entry.lastIndexOf("=");
    const receipt = (cut > 0 ? entry.slice(0, cut) : entry).trim();
    return receipt.startsWith("quiz:") ? receipt.slice(5).trim() : "";
  }).filter(Boolean))];
}

async function googleToken(account: ServiceAccount) {
  if (!account.client_email || !account.private_key) throw new Error("Credencial Firebase incompleta.");
  const now = Math.floor(Date.now()/1000);
  const assertion = await new SignJWT({ iss: account.client_email, scope: FIRESTORE_SCOPE, aud: GOOGLE_TOKEN_URL })
    .setProtectedHeader({ alg: "RS256", typ: "JWT" }).setIssuedAt(now).setExpirationTime(now + 3600)
    .sign(await importPKCS8(account.private_key.replace(/\\n/g,"\n"), "RS256"));
  const response = await fetch(GOOGLE_TOKEN_URL, { method:"POST", headers:{"Content-Type":"application/x-www-form-urlencoded"}, body:new URLSearchParams({grant_type:"urn:ietf:params:oauth2:grant-type:jwt-bearer", assertion}) });
  const body = await response.json();
  if (!response.ok || !body.access_token) throw new Error("Falha ao autenticar no Firebase.");
  return String(body.access_token);
}
async function getDocument(projectId: string, token: string, collection: string, id: string): Promise<FirestoreDocument|null> {
  const response = await fetch(`https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents/${collection}/${encodeURIComponent(id)}`, { headers:{Authorization:`Bearer ${token}`} });
  if (response.status === 404) return null;
  if (!response.ok) throw new Error(`Falha ao consultar ${collection}.`);
  return await response.json() as FirestoreDocument;
}

function difficultyFromToken(value: string): Difficulty | null {
  const v = value.toLowerCase();
  return v.includes("easy") ? "easy" : v.includes("medium") ? "medium" : v.includes("hard") ? "hard" : null;
}
function activityFor(d: Difficulty): Activity { return d === "easy" ? "quiz_easy" : d === "medium" ? "quiz_medium" : "quiz_hard"; }
function baseXp(d: Difficulty) { return d === "easy" ? 10 : d === "medium" ? 20 : 30; }
function splitTopLevel(value: string): string[] {
  const out: string[] = []; let current = ""; let depth = 0; let quoted = false; let escaped = false;
  for (const ch of value) {
    if (escaped) { current += ch; escaped = false; continue; }
    if (ch === "\\" && quoted) { current += ch; escaped = true; continue; }
    if (ch === '"') { quoted = !quoted; current += ch; continue; }
    if (!quoted) {
      if (ch === "(" || ch === "[") depth++;
      else if (ch === ")" || ch === "]") depth--;
      if (ch === "," && depth === 0) { out.push(current.trim()); current = ""; continue; }
    }
    current += ch;
  }
  if (current.trim()) out.push(current.trim());
  return out;
}
function unquote(value: string) {
  const trimmed = value.trim();
  if (!trimmed.startsWith('"') || !trimmed.endsWith('"')) return trimmed;
  try { return JSON.parse(trimmed); } catch { return trimmed.slice(1,-1).replace(/\\"/g,'"').replace(/\\n/g,'\n').replace(/\\\\/g,'\\'); }
}
function parseOptions(value: string) {
  const start = value.indexOf("listOf("); const end = value.lastIndexOf(")");
  if (start < 0 || end <= start) return [];
  return splitTopLevel(value.slice(start + 7, end)).map(unquote);
}
function parseCatalog(source: string, target: Map<string,QuizQuestion>) {
  for (const raw of source.split(/\r?\n/)) {
    const line = raw.trim(); if (!line.includes("quizQuestion(")) continue;
    const start = line.indexOf("quizQuestion(") + "quizQuestion(".length;
    const end = line.lastIndexOf(")"); if (end <= start) continue;
    const args = splitTopLevel(line.slice(start,end)); if (args.length < 9) continue;
    const id = unquote(args[0]); const difficulty = difficultyFromToken(args[1]); const options = parseOptions(args[3]); const correctOptionIndex = Number(args[4]);
    if (!id || !difficulty || options.length !== 4 || !Number.isInteger(correctOptionIndex) || correctOptionIndex < 0 || correctOptionIndex > 3) continue;
    target.set(id,{ id,difficulty,activity:activityFor(difficulty),prompt:unquote(args[2]),options,correctOptionIndex,hardHint:unquote(args[5]),easyHint:unquote(args[6]),reference:unquote(args[7]),explanation:unquote(args[8]) });
  }
}
function parseExpansion(source: string, target: Map<string,QuizQuestion>) {
  for (const raw of source.split(/\r?\n/)) {
    const line = raw.trim(); if (!/^(easy|medium|hard)_\d+\|/.test(line)) continue;
    const p = line.split("|"); if (p.length < 12) continue;
    const difficulty = p[0].startsWith("easy_") ? "easy" : p[0].startsWith("medium_") ? "medium" : "hard";
    const options = [p[2],p[3],p[4],p[5]]; const correctOptionIndex = Number(p[6]); const reference = p[7];
    if (!Number.isInteger(correctOptionIndex) || correctOptionIndex < 0 || correctOptionIndex > 3) continue;
    target.set(p[0],{ id:p[0],difficulty,activity:activityFor(difficulty),prompt:p[1],options,correctOptionIndex,hardHint:`Procure o detalhe no contexto de ${reference}.`,easyHint:`A resposta direta é: ${options[correctOptionIndex]}.`,reference,explanation:`${options[correctOptionIndex]} é a resposta indicada em ${reference}.` });
  }
}
async function catalog() {
  if (catalogCache) return catalogCache;
  const target = new Map<string,QuizQuestion>();
  for (const url of SOURCE_URLS) {
    const response = await fetch(url,{headers:{"User-Agent":"MIC-Rhema-Quiz-Authority"}});
    if (!response.ok) throw new Error(`Catálogo oficial indisponível (${response.status}).`);
    const source = await response.text();
    if (url.includes("Catalog")) parseCatalog(source,target); else parseExpansion(source,target);
  }
  if (target.size !== 300) throw new Error(`Catálogo oficial incompleto (${target.size}/300).`);
  catalogCache = target; return target;
}
function publicQuestion(q: QuizQuestion) { return { id:q.id,difficulty:q.difficulty,baseXp:baseXp(q.difficulty),prompt:q.prompt,options:q.options }; }

async function resolveMember(request: Request, input: Record<string,unknown>, projectId: string, fireToken: string): Promise<MemberAuth> {
  const authorization = request.headers.get("authorization") ?? "";
  const bearer = authorization.match(/^Bearer\s+(.+)$/i)?.[1] ?? "";
  const providedKey = request.headers.get("apikey") ?? "";
  const configuredKey = Deno.env.get("SUPABASE_ANON_KEY") ?? "";
  let memberId = ""; let phone = ""; let firebaseMode = false;

  const legacy = Boolean(configuredKey) && providedKey === configuredKey && bearer === configuredKey;
  const publishable = providedKey === CURRENT_PUBLISHABLE_KEY && bearer === CURRENT_PUBLISHABLE_KEY;
  if (legacy || publishable) {
    memberId = clean(input.memberId,200); phone = normalizePhone(input.phone);
    if (!memberId || phone.length < 10 || phone.length > 11) throw new Error("Membro inválido.");
  } else {
    if (!bearer) throw new Error("Sessão ausente.");
    const verified = await jwtVerify(bearer,FIREBASE_JWKS,{issuer:FIREBASE_ISSUER,audience:FIREBASE_PROJECT_ID,algorithms:["RS256"]});
    memberId = clean(verified.payload.sub,200); firebaseMode = true;
    if (!memberId) throw new Error("Sessão Firebase inválida.");
  }

  const access = docData(await getDocument(projectId,fireToken,"acessos_pendentes",memberId));
  if (!Object.keys(access).length) throw new Error("Cadastro do membro não encontrado.");
  if (firebaseMode && access.isApproved !== true && access.isAdmin !== true) throw new Error("Acesso do membro ainda não aprovado.");
  if (!firebaseMode && normalizePhone(access.phone) !== phone) throw new Error("Identidade do membro não confere.");
  const user = docData(await getDocument(projectId,fireToken,"users",memberId));
  const unlocked = new Set([...stringList(access.unlockedBadgeIds),...stringList(user.unlockedBadgeIds)]);
  const legacyEntries = [...new Set([...(activityMap(access.badgeActivityIds).journey_xp_awards ?? []),...(activityMap(user.badgeActivityIds).journey_xp_awards ?? [])])];
  return {
    memberId,
    xpUnlocked:[...unlocked].some(id=>LEVEL_8_PLUS.has(id)),
    legacyXp:legacyEntries.reduce((sum,item)=>sum+parseLegacyXp(item),0),
    legacyQuizIds:parseLegacyQuizIds(legacyEntries),
  };
}

Deno.serve(async (request) => {
  if (request.method === "OPTIONS") return new Response("ok",{headers:cors});
  if (request.method !== "POST") return json({error:"Método não permitido."},405);
  try {
    const input = await request.json() as Record<string,unknown>;
    const serviceAccount = JSON.parse(Deno.env.get("FIREBASE_SERVICE_ACCOUNT_JSON") ?? "{}") as ServiceAccount;
    const projectId = serviceAccount.project_id || Deno.env.get("FIREBASE_PROJECT_ID") || FIREBASE_PROJECT_ID;
    const fireToken = await googleToken(serviceAccount);
    const member = await resolveMember(request,input,projectId,fireToken);
    const url = Deno.env.get("SUPABASE_URL") ?? ""; const role = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
    if (!url || !role) throw new Error("Backend XP não configurado.");
    const supabase = createClient(url,role,{auth:{persistSession:false,autoRefreshToken:false}});
    const { data: ensured, error: ensureError } = await supabase.rpc("xp_ensure_account",{p_member_id:member.memberId,p_legacy_xp:member.legacyXp});
    if (ensureError) throw ensureError;
    const ensuredAccount = Array.isArray(ensured) ? ensured[0] : ensured;
    if (member.legacyQuizIds.length) {
      const rows = member.legacyQuizIds.map(question_id => ({member_id:member.memberId,question_id}));
      const { error } = await supabase.from("xp_legacy_quiz_receipts").upsert(rows,{onConflict:"member_id,question_id",ignoreDuplicates:true});
      if (error) throw error;
    }
    const action = clean(input.action,40);
    const questions = await catalog();

    if (action === "status" || action === "next_question") {
      const difficulty = difficultyFromToken(clean(input.difficulty,20)) ?? "easy";
      const ids = [...questions.values()].filter(q=>q.difficulty===difficulty).map(q=>q.id);
      const [{ data: attempts, error: attemptsError }, { data: legacy, error: legacyError }] = await Promise.all([
        supabase.from("xp_quiz_attempts").select("question_id").eq("member_id",member.memberId).in("question_id",ids),
        supabase.from("xp_legacy_quiz_receipts").select("question_id").eq("member_id",member.memberId).in("question_id",ids),
      ]);
      if (attemptsError) throw attemptsError;
      if (legacyError) throw legacyError;
      const answered = new Set([...(attempts ?? []),...(legacy ?? [])].map((x:any)=>String(x.question_id)));
      const next = ids.map(id=>questions.get(id)!).find(q=>!answered.has(q.id)) ?? null;
      return json({ok:true,unlocked:member.xpUnlocked,difficulty,answered:answered.size,total:ids.length,question:next ? publicQuestion(next) : null});
    }

    const questionId = clean(input.questionId,200);
    const question = questions.get(questionId);
    if (!question) return json({error:"Pergunta não pertence ao catálogo oficial do Quiz."},400);
    const { data: legacyReceipt, error: legacyReceiptError } = await supabase.from("xp_legacy_quiz_receipts")
      .select("question_id").eq("member_id",member.memberId).eq("question_id",question.id).maybeSingle();
    if (legacyReceiptError) throw legacyReceiptError;

    if (action === "hint") {
      if (legacyReceipt) return json({error:"Pergunta já concluída no progresso legado."},409);
      const requested = clean(input.hint,20);
      const rank = requested === "easy" || requested === "easy_hint" ? 2 : requested === "subtle" || requested === "subtle_hint" ? 1 : 0;
      if (!rank) return json({error:"Dica inválida."},400);
      const { data, error } = await supabase.rpc("xp_record_quiz_hint",{p_member_id:member.memberId,p_question_id:question.id,p_hint_rank:rank});
      if (error) throw error;
      const effectiveRank = Number(data ?? rank);
      const variant = effectiveRank >= 2 ? "easy_hint" : "subtle_hint";
      const hintText = effectiveRank >= 2 ? question.easyHint : question.hardHint;
      return json({ok:true,questionId:question.id,variant,hint:hintText,multiplier:effectiveRank >= 2 ? 0.7 : 0.9});
    }

    if (action === "answer") {
      const selected = Number(input.selectedOptionIndex);
      if (!Number.isInteger(selected) || selected < 0 || selected > 3) return json({error:"Alternativa inválida."},400);
      const correct = selected === question.correctOptionIndex;
      if (legacyReceipt) {
        return json({
          ok:true,unlocked:member.xpUnlocked,questionId:question.id,duplicate:true,legacy:true,granted:0,
          correct,selectedOptionIndex:selected,correctOptionIndex:question.correctOptionIndex,variant:"",
          reference:question.reference,explanation:question.explanation,
          account:{member_id:member.memberId,total_earned:Number(ensuredAccount?.total_earned ?? 0),total_spent:Number(ensuredAccount?.total_spent ?? 0),balance:Number(ensuredAccount?.balance ?? 0)},
        });
      }
      const { data, error } = await supabase.rpc("xp_submit_quiz",{
        p_member_id:member.memberId,p_question_id:question.id,p_selected_option:selected,p_activity:question.activity,
        p_variant:"",p_correct:correct,p_amount:baseXp(question.difficulty),p_description:`Pergunta ${question.difficulty === "easy" ? "fácil" : question.difficulty === "medium" ? "média" : "difícil"} correta`,
      });
      if (error) throw error;
      const result = Array.isArray(data) ? data[0] : data;
      const { data: attempt } = await supabase.from("xp_quiz_attempts").select("selected_option,variant,correct").eq("member_id",member.memberId).eq("question_id",question.id).single();
      return json({
        ok:true,unlocked:member.xpUnlocked,questionId:question.id,duplicate:Boolean(result?.duplicate),granted:Number(result?.granted ?? 0),
        correct:Boolean(attempt?.correct ?? result?.correct),selectedOptionIndex:Number(attempt?.selected_option ?? selected),correctOptionIndex:question.correctOptionIndex,
        variant:String(attempt?.variant ?? ""),reference:question.reference,explanation:question.explanation,
        account:{member_id:member.memberId,total_earned:Number(result?.total_earned ?? 0),total_spent:Number(result?.total_spent ?? 0),balance:Number(result?.balance ?? 0)},
      });
    }

    if (action === "claim_mission") {
      if (!member.xpUnlocked) return json({ok:true,unlocked:false,granted:0,reason:"xp_locked"});
      const missionId = clean(input.missionId,200);
      const activity: string | null = missionId.startsWith("easy_") ? "journey_mission_easy" : missionId.startsWith("medium_") ? "journey_mission_medium" : missionId.startsWith("hard_") ? "journey_mission_hard" : null;
      const amount = activity === "journey_mission_easy" ? 15 : activity === "journey_mission_medium" ? 35 : activity === "journey_mission_hard" ? 70 : 0;
      if (!activity || !amount) return json({error:"Missão da Jornada inválida."},400);
      const { data, error } = await supabase.rpc("xp_award",{p_member_id:member.memberId,p_activity:activity,p_content_id:missionId,p_variant:"",p_receipt_id:`mission:${missionId}`,p_amount:amount,p_description:"Missão da Jornada concluída",p_daily_cap:0});
      if (error) {
        const message = String(error.message ?? "");
        if (message.includes("ainda não foi concluída")) return json({ok:true,unlocked:true,granted:0,reason:"mission_incomplete"},409);
        throw error;
      }
      const result = Array.isArray(data) ? data[0] : data;
      return json({ok:true,unlocked:true,granted:Number(result?.granted ?? 0),duplicate:Boolean(result?.duplicate),reason:"",account:{member_id:member.memberId,total_earned:Number(result?.total_earned ?? 0),total_spent:Number(result?.total_spent ?? 0),balance:Number(result?.balance ?? 0)}});
    }

    return json({error:"Ação inválida."},400);
  } catch (error) {
    console.error("xp-quiz failed",error);
    const message = error instanceof Error ? error.message : "Falha no Quiz.";
    const status = message.includes("Sessão") || message.includes("Identidade") ? 401 : 500;
    return json({error:message},status);
  }
});