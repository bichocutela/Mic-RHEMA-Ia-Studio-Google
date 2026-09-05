import { firebaseAuth } from "./firebase";

const supabaseUrl = import.meta.env.VITE_SUPABASE_URL || "https://cwphbkdtorfpgmnlafqb.supabase.co";

export type PwaXpAccount = {
  member_id: string;
  total_earned: number;
  total_spent: number;
  balance: number;
  migrated_legacy_xp?: number;
  updated_at?: string;
};

export type PwaXpTransaction = {
  id: string;
  type: string;
  amount: number;
  activity: string;
  content_id: string;
  description: string;
  date_key: string;
  created_at: string;
};

export type PwaXpItem = {
  id: string;
  name: string;
  description: string;
  cost: number;
  category: string;
  kind: "digital" | "profile" | "physical" | string;
  image_url: string;
  stock: number | null;
  limit_per_member: number;
  active: boolean;
};

export type PwaXpRedemption = {
  id: string;
  item_id: string;
  item_name: string;
  cost: number;
  status: string;
  redemption_code: string;
  created_at: string;
  delivered_at?: string;
};

export type PwaXpEntitlement = {
  id: string;
  item_id: string;
  item_name: string;
  kind: string;
  unlocked_at: string;
};

export type PwaXpDashboard = {
  ok: true;
  unlocked: boolean;
  account: PwaXpAccount;
  streak: number;
  transactions: PwaXpTransaction[];
  items: PwaXpItem[];
  redemptions: PwaXpRedemption[];
  entitlements: PwaXpEntitlement[];
};

export type PwaXpAwardResult = {
  ok: true;
  unlocked: boolean;
  granted: number;
  duplicate?: boolean;
  reason?: string;
  account: PwaXpAccount;
};

export type PwaQuizDifficulty = "easy" | "medium" | "hard";
export type PwaQuizQuestion = {
  id: string;
  difficulty: PwaQuizDifficulty;
  baseXp: number;
  prompt: string;
  options: string[];
};
export type PwaQuizStatus = {
  ok: true;
  unlocked: boolean;
  difficulty: PwaQuizDifficulty;
  answered: number;
  total: number;
  question: PwaQuizQuestion | null;
};
export type PwaQuizHint = {
  ok: true;
  questionId: string;
  variant: "subtle_hint" | "easy_hint";
  hint: string;
  multiplier: number;
};
export type PwaQuizAnswer = {
  ok: true;
  unlocked: boolean;
  questionId: string;
  duplicate: boolean;
  granted: number;
  correct: boolean;
  selectedOptionIndex: number;
  correctOptionIndex: number;
  variant: "" | "subtle_hint" | "easy_hint";
  reference: string;
  explanation: string;
  account: PwaXpAccount;
};

async function authenticatedRequest<T>(endpoint: string, body: Record<string, unknown>, forceRefresh = false): Promise<T> {
  const user = firebaseAuth?.currentUser;
  if (!user) throw new Error("Entre novamente para acessar a Jornada XP.");
  const token = await user.getIdToken(forceRefresh);
  const response = await fetch(`${supabaseUrl}/functions/v1/${endpoint}`, {
    method: "POST",
    headers: { "content-type": "application/json", authorization: `Bearer ${token}` },
    body: JSON.stringify(body),
  });
  if (response.status === 401 && !forceRefresh) return authenticatedRequest<T>(endpoint, body, true);
  const payload = await response.json().catch(() => ({}));
  if (!response.ok || payload.ok !== true) {
    const error = new Error(payload.error || "Não foi possível sincronizar a Jornada XP agora.");
    (error as Error & { status?: number }).status = response.status;
    throw error;
  }
  return payload as T;
}

function request<T>(body: Record<string, unknown>, forceRefresh = false): Promise<T> {
  return authenticatedRequest<T>("pwa-xp", body, forceRefresh);
}
function quizRequest<T>(body: Record<string, unknown>, forceRefresh = false): Promise<T> {
  return authenticatedRequest<T>("xp-quiz", body, forceRefresh);
}

export function loadPwaXpDashboard() {
  return request<PwaXpDashboard>({ action: "dashboard" });
}

export function awardPwaXp(activity: string, contentId: string) {
  return request<PwaXpAwardResult>({ action: "award", activity, contentId });
}

export async function tryAwardPwaXp(activity: string, contentId: string) {
  try {
    return await awardPwaXp(activity, contentId);
  } catch (error) {
    const value = error as Error & { status?: number };
    if (value.status === 409) return null;
    throw error;
  }
}

export function redeemPwaXp(itemId: string, expectedCost: number) {
  return request<{ ok: true; unlocked: boolean; account: PwaXpAccount; redemption: Record<string, unknown> }>({
    action: "redeem",
    itemId,
    expectedCost,
  });
}

export function loadPwaQuiz(difficulty: PwaQuizDifficulty) {
  return quizRequest<PwaQuizStatus>({ action: "status", difficulty });
}

export function requestPwaQuizHint(questionId: string, hint: "subtle" | "easy") {
  return quizRequest<PwaQuizHint>({ action: "hint", questionId, hint });
}

export function submitPwaQuizAnswer(questionId: string, selectedOptionIndex: number) {
  return quizRequest<PwaQuizAnswer>({ action: "answer", questionId, selectedOptionIndex });
}

export function syncPwaQuizProfile() {
  return authenticatedRequest<{ ok: true; answered: number; correct: number; noEasyHint: number; noHint: number; hardCorrect: number }>("pwa-quiz-sync", {});
}
