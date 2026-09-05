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

async function request<T>(body: Record<string, unknown>, forceRefresh = false): Promise<T> {
  const user = firebaseAuth?.currentUser;
  if (!user) throw new Error("Entre novamente para acessar a Jornada XP.");
  const token = await user.getIdToken(forceRefresh);
  const response = await fetch(`${supabaseUrl}/functions/v1/pwa-xp`, {
    method: "POST",
    headers: { "content-type": "application/json", authorization: `Bearer ${token}` },
    body: JSON.stringify(body),
  });
  if (response.status === 401 && !forceRefresh) return request<T>(body, true);
  const payload = await response.json().catch(() => ({}));
  if (!response.ok || payload.ok !== true) {
    const error = new Error(payload.error || "Não foi possível sincronizar a Jornada XP agora.");
    (error as Error & { status?: number }).status = response.status;
    throw error;
  }
  return payload as T;
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
