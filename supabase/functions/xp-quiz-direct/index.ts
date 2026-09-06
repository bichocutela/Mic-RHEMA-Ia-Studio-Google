import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "npm:@supabase/supabase-js@2.57.4";
import { createRemoteJWKSet, jwtVerify } from "npm:jose@5.10.0";

const FIREBASE_PROJECT_ID = "mic-rhema";
const FIREBASE_ISSUER = `https://securetoken.google.com/${FIREBASE_PROJECT_ID}`;
const FIREBASE_JWKS = createRemoteJWKSet(new URL("https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com"));
const QUIZ_CATALOG_VERSION = "quiz-v1-20260905";
const QUIZ_CATALOG_COMMIT = "b058141542821fc97e1de64bdd5dbf912ef904a3";
const SOURCE_URLS = [
  `https://raw.githubusercontent.com/bichocutela/Mic-RHEMA-Ia-Studio-Google/${QUIZ_CATALOG_COMMIT}/app/src/main/java/com/aistudio/micrhema/BibleQuizCatalog.kt`,
  `https://raw.githubusercontent.com/bichocutela/Mic-RHEMA-Ia-Studio-Google/${QUIZ_CATALOG_COMMIT}/app/src/main/java/com/aistudio/micrhema/BibleQuizExpansion.kt`,
];

type Difficulty = "easy" | "medium" | "hard";
type Activity = "quiz_easy" | "quiz_medium" | "quiz_hard";
type QuizQuestion = {
  id: string;
  difficulty: Difficulty;
  activity: Activity;
  prompt: string;
  options: string[];
  correctOptionIndex: number;
  hardHint: string;
  easyHint: string;
  reference: string;
  explanation: string;
};

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
const list = (value: unknown): string[] => Array.isArray(value) ? [...new Set(value.map((item) => clean(item, 200)).filter(Boolean))].slice(0, 500) : [];

async function memberIdFromFirebase(request: Request): Promise<string> {
  const authorization = request.headers.get("authorization") ?? "";
  const token = authorization.match(/^Bearer\s+(.+)$/i)?.[1]?.trim() ?? "";
  if (!token) throw new Error("Sessão Firebase ausente. Entre novamente no MIC Rhema.");
  const verified = await jwtVerify(token, FIREBASE_JWKS, {
    issuer: FIREBASE_ISSUER,
    audience: FIREBASE_PROJECT_ID,
    algorithms: ["RS256"],
  });
  const memberId = clean(verified.payload.sub, 200);
  if (!memberId) throw new Error("Sessão Firebase inválida.");
  return memberId;
}

function difficultyFromToken(value: string): Difficulty | null {
  const normalized = value.toLowerCase();
  return normalized.includes("easy") ? "easy" : normalized.includes("medium") ? "medium" : normalized.includes("hard") ? "hard" : null;
}
function activityFor(difficulty: Difficulty): Activity {
  return difficulty === "easy" ? "quiz_easy" : difficulty === "medium" ? "quiz_medium" : "quiz_hard";
}
function baseXp(difficulty: Difficulty) {
  return difficulty === "easy" ? 10 : difficulty === "medium" ? 20 : 30;
}
function splitTopLevel(value: string) {
  const out: string[] = [];
  let current = "";
  let depth = 0;
  let quoted = false;
  let escaped = false;
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
  try { return JSON.parse(trimmed); } catch {
    return trimmed.slice(1, -1).replace(/\\"/g, '"').replace(/\\n/g, "\n").replace(/\\\\/g, "\\");
  }
}
function parseOptions(value: string) {
  const start = value.indexOf("listOf(");
  const end = value.lastIndexOf(")");
  if (start < 0 || end <= start) return [];
  return splitTopLevel(value.slice(start + 7, end)).map(unquote);
}
function parseCatalog(source: string, target: Map<string, QuizQuestion>) {
  for (const raw of source.split(/\r?\n/)) {
    const line = raw.trim();
    if (!line.includes("quizQuestion(")) continue;
    const start = line.indexOf("quizQuestion(") + 13;
    const end = line.lastIndexOf(")");
    if (end <= start) continue;
    const args = splitTopLevel(line.slice(start, end));
    if (args.length < 9) continue;
    const id = unquote(args[0]);
    const difficulty = difficultyFromToken(args[1]);
    const options = parseOptions(args[3]);
    const correctOptionIndex = Number(args[4]);
    if (!id || !difficulty || options.length !== 4 || !Number.isInteger(correctOptionIndex) || correctOptionIndex !in [0,1,2,3]) continue;
    target.set(id, {
      id, difficulty, activity: activityFor(difficulty), prompt: unquote(args[2]), options, correctOptionIndex,
      hardHint: unquote(args[5]), easyHint: unquote(args[6]), reference: unquote(args[7]), explanation: unquote(args[8]),
    });
  }
}
function parseExpansion(source: string, target: Map<string, QuizQuestion>) {
  for (const raw of source.split(/\r?\n/)) {
    const line = raw.trim();
    if (!/^(easy|medium|hard)_\d+\|/.test(line)) continue;
    const fields = line.split("|");
    if (fields.length < 12) continue;
    const difficulty: Difficulty = fields[0].startsWith("easy_") ? "easy" : fields[0].startsWith("medium_") ? "medium" : "hard";
    const options = [fields[2], fields[3], fields[4], fields[5]];
    const correctOptionIndex = Number(fields[6]);
    const reference = fields[7];
    if (!Number.isInteger(correctOptionIndex) || correctOptionIndex < 0 || correctOptionIndex > 3) continue;
    target.set(fields[0], {
      id: fields[0], difficulty, activity: activityFor(difficulty), prompt: fields[1], options, correctOptionIndex,
      hardHint: `Procure o detalhe no contexto de ${reference}.`, easyHint: `A resposta direta é: ${options[correctOptionIndex]}.`,
      reference, explanation: `${options[correctOptionIndex]} é a resposta indicada em ${reference}.`,
    });
  }
}
async function catalog() {
  if (catalogCache) return catalogCache;
  const target = new Map<string, QuizQuestion>();
  for (const url of SOURCE_URLS) {
    const response = await fetch(url, { headers: { "User-Agent": "MIC-Rhema-Quiz-Direct" } });
    if (!response.ok) throw new Error(`Catálogo oficial indisponível (${response.status}).`);
    const source = await response.text();
    if (url.includes("Catalog")) parseCatalog(source, target); else parseExpansion(source, target);
  }
  if (target.size !== 300) throw new Error(`Catálogo oficial incompleto (${target.size}/300).`);
  catalogCache = target;
  return target;
}
function randomFrom<T>(values: T[]): T | null {
  if (!values.length) return null;
  const random = new Uint32Array(1);
  crypto.getRandomValues(random);
  return values[random[0] % values.length] ?? null;
}
const publicQuestion = (question: QuizQuestion) => ({
  id: question.id,
  difficulty: question.difficulty,
  baseXp: baseXp(question.difficulty),
  prompt: question.prompt,
  options: question.options,
  catalogVersion: QUIZ_CATALOG_VERSION,
});

Deno.serve(async (request) => {
  if (request.method === "OPTIONS") return new Response("ok", { headers: cors });
  if (request.method !== "POST") return json({ error: "Método não permitido." }, 405);
  try {
    const input = await request.json() as Record<string, unknown>;
    const action = clean(input.action, 40);
    const memberId = await memberIdFromFirebase(request);
    const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
    const serviceRole = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
    if (!supabaseUrl || !serviceRole) throw new Error("Backend XP não configurado.");
    const sb = createClient(supabaseUrl, serviceRole, { auth: { persistSession: false, autoRefreshToken: false } });

    const { data: ensured, error: ensureError } = await sb.rpc("xp_ensure_account", { p_member_id: memberId, p_legacy_xp: 0 });
    if (ensureError) throw ensureError;
    const account = Array.isArray(ensured) ? ensured[0] : ensured;
    const questions = await catalog();

    if (action === "status" || action === "next_question") {
      const difficulty = difficultyFromToken(clean(input.difficulty, 20)) ?? "easy";
      const ids = [...questions.values()].filter((question) => question.difficulty === difficulty).map((question) => question.id);
      const localAnswered = new Set(list(input.answeredIds).filter((id) => ids.includes(id)));
      const [attempts, legacy] = await Promise.all([
        sb.from("xp_quiz_attempts").select("question_id").eq("member_id", memberId).in("question_id", ids),
        sb.from("xp_legacy_quiz_receipts").select("question_id").eq("member_id", memberId).in("question_id", ids),
      ]);
      if (attempts.error) throw attempts.error;
      if (legacy.error) throw legacy.error;
      const answered = new Set([
        ...localAnswered,
        ...(attempts.data ?? []).map((item: any) => String(item.question_id)),
        ...(legacy.data ?? []).map((item: any) => String(item.question_id)),
      ]);
      const unanswered = ids.filter((id) => !answered.has(id));
      const nextId = randomFrom(unanswered);
      const next = nextId ? questions.get(nextId) ?? null : null;
      return json({
        ok: true,
        catalogVersion: QUIZ_CATALOG_VERSION,
        difficulty,
        answered: answered.size,
        total: ids.length,
        remaining: unanswered.length,
        question: next ? publicQuestion(next) : null,
        account,
      });
    }

    const questionId = clean(input.questionId, 200);
    const question = questions.get(questionId);
    if (!question) return json({ error: "Pergunta não pertence ao catálogo oficial do Quiz." }, 400);

    const { data: legacyReceipt, error: legacyError } = await sb.from("xp_legacy_quiz_receipts")
      .select("question_id").eq("member_id", memberId).eq("question_id", question.id).maybeSingle();
    if (legacyError) throw legacyError;

    if (action === "hint") {
      if (legacyReceipt) return json({ error: "Pergunta já concluída anteriormente." }, 409);
      const requested = clean(input.hint, 20);
      const rank = requested === "easy" || requested === "easy_hint" ? 2 : requested === "subtle" || requested === "subtle_hint" ? 1 : 0;
      if (!rank) return json({ error: "Dica inválida." }, 400);
      const { data, error } = await sb.rpc("xp_record_quiz_hint", { p_member_id: memberId, p_question_id: question.id, p_hint_rank: rank });
      if (error) throw error;
      const effective = Number(data ?? rank);
      const variant = effective >= 2 ? "easy_hint" : "subtle_hint";
      return json({ ok: true, questionId: question.id, variant, hint: effective >= 2 ? question.easyHint : question.hardHint, multiplier: effective >= 2 ? 0.7 : 0.9 });
    }

    if (action === "answer") {
      const selected = Number(input.selectedOptionIndex);
      if (!Number.isInteger(selected) || selected < 0 || selected > 3) return json({ error: "Alternativa inválida." }, 400);
      const correct = selected === question.correctOptionIndex;
      if (legacyReceipt) {
        return json({ ok: true, questionId: question.id, duplicate: true, legacy: true, granted: 0, correct, selectedOptionIndex: selected, correctOptionIndex: question.correctOptionIndex, variant: "", reference: question.reference, explanation: question.explanation, account });
      }
      const { data, error } = await sb.rpc("xp_submit_quiz", {
        p_member_id: memberId,
        p_question_id: question.id,
        p_selected_option: selected,
        p_activity: question.activity,
        p_variant: "",
        p_correct: correct,
        p_amount: baseXp(question.difficulty),
        p_description: `Pergunta ${question.difficulty} correta`,
        p_auth_context: "firebase_member_v1",
      });
      if (error) throw error;
      const result = Array.isArray(data) ? data[0] : data;
      const { data: attempt } = await sb.from("xp_quiz_attempts").select("selected_option,variant,correct")
        .eq("member_id", memberId).eq("question_id", question.id).single();
      return json({
        ok: true,
        questionId: question.id,
        duplicate: Boolean(result?.duplicate),
        granted: Number(result?.granted ?? 0),
        correct: Boolean(attempt?.correct ?? result?.correct),
        selectedOptionIndex: Number(attempt?.selected_option ?? selected),
        correctOptionIndex: question.correctOptionIndex,
        variant: String(attempt?.variant ?? ""),
        reference: question.reference,
        explanation: question.explanation,
        account: {
          member_id: memberId,
          total_earned: Number(result?.total_earned ?? account?.total_earned ?? 0),
          total_spent: Number(result?.total_spent ?? account?.total_spent ?? 0),
          balance: Number(result?.balance ?? account?.balance ?? 0),
        },
      });
    }

    if (action === "claim_mission") {
      const missionId = clean(input.missionId, 200);
      const activity = missionId.startsWith("easy_") ? "journey_mission_easy" : missionId.startsWith("medium_") ? "journey_mission_medium" : missionId.startsWith("hard_") ? "journey_mission_hard" : "";
      const amount = activity === "journey_mission_easy" ? 15 : activity === "journey_mission_medium" ? 35 : activity === "journey_mission_hard" ? 70 : 0;
      if (!activity || !amount) return json({ error: "Missão da Jornada inválida." }, 400);
      const { data, error } = await sb.rpc("xp_award", {
        p_member_id: memberId,
        p_activity: activity,
        p_content_id: missionId,
        p_variant: "",
        p_receipt_id: `mission:${missionId}`,
        p_amount: amount,
        p_description: "Missão da Jornada concluída",
        p_daily_cap: 0,
      });
      if (error) {
        const message = String(error.message ?? "");
        if (message.includes("ainda não foi concluída")) return json({ ok: true, granted: 0, reason: "mission_incomplete" }, 409);
        throw error;
      }
      const result = Array.isArray(data) ? data[0] : data;
      return json({ ok: true, granted: Number(result?.granted ?? 0), duplicate: Boolean(result?.duplicate), reason: "", account: result });
    }

    return json({ error: "Ação inválida." }, 400);
  } catch (error) {
    console.error("xp-quiz-direct failed", error);
    const message = error instanceof Error ? error.message : "Falha no Quiz.";
    const status = message.toLowerCase().includes("sessão") ? 401 : 500;
    return json({ error: message }, status);
  }
});
