import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "npm:@supabase/supabase-js@2.57.4";
import { createRemoteJWKSet, jwtVerify } from "npm:jose@5.10.0";

const FIREBASE_PROJECT_ID = "mic-rhema";
const FIREBASE_ISSUER = `https://securetoken.google.com/${FIREBASE_PROJECT_ID}`;
const FIREBASE_JWKS = createRemoteJWKSet(new URL("https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com"));
const ADMIN_EMAIL = "admin@micrhema.app";
const MAX_DB_INTEGER = 2_147_483_647;

const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-client-info",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Content-Type": "application/json; charset=utf-8",
  "Cache-Control": "no-store",
};

const json = (body: Record<string, unknown>, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: cors });
const clean = (value: unknown, max = 500) => String(value ?? "").trim().slice(0, max);

async function requireAdmin(request: Request): Promise<void> {
  const auth = request.headers.get("authorization") ?? "";
  const bearer = auth.match(/^Bearer\s+(.+)$/i)?.[1] ?? "";
  if (!bearer) throw new Error("Sessão administrativa ausente.");

  const verified = await jwtVerify(bearer, FIREBASE_JWKS, {
    issuer: FIREBASE_ISSUER,
    audience: FIREBASE_PROJECT_ID,
    algorithms: ["RS256"],
  });

  const uid = clean(verified.payload.sub, 200);
  if (!uid) throw new Error("Sessão administrativa inválida.");

  const email = clean(verified.payload.email, 320).toLowerCase();
  const isAdminClaim = verified.payload.isAdmin === true;

  if (isAdminClaim) return;
  if (email === ADMIN_EMAIL) return;

  throw new Error("Acesso administrativo obrigatório.");
}

Deno.serve(async (request) => {
  if (request.method === "OPTIONS") return new Response("ok", { headers: cors });
  if (request.method !== "POST") return json({ error: "Método não permitido." }, 405);

  try {
    await requireAdmin(request);

    const input = await request.json() as Record<string, unknown>;
    const action = clean(input.action, 60);
    const memberId = clean(input.memberId, 100);
    if (!memberId) return json({ error: "Membro inválido." }, 400);

    const url = Deno.env.get("SUPABASE_URL") ?? "";
    const role = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
    if (!url || !role) throw new Error("Backend administrativo de XP não configurado.");

    const sb = createClient(url, role, {
      auth: { persistSession: false, autoRefreshToken: false },
    });

    const ensureAccount = async () => {
      const { error } = await sb.rpc("xp_ensure_account", {
        p_member_id: memberId,
        p_legacy_xp: 0,
      });
      if (error) throw error;
    };

    const snapshot = async () => {
      const { data: account, error: accountError } = await sb
        .from("xp_accounts")
        .select("member_id,total_earned,total_spent,balance,migrated_legacy_xp,updated_at")
        .eq("member_id", memberId)
        .single();
      if (accountError) throw accountError;

      const { data: transactions, error: historyError } = await sb
        .from("xp_transactions")
        .select("id,type,amount,activity,content_id,variant,receipt_id,description,date_key,created_at")
        .eq("member_id", memberId)
        .order("created_at", { ascending: false })
        .limit(100);
      if (historyError) throw historyError;

      return { account, transactions: transactions ?? [] };
    };

    await ensureAccount();

    if (action === "member_xp") {
      const current = await snapshot();
      return json({ ok: true, memberId, ...current });
    }

    if (action === "add_extra") {
      const amount = Math.trunc(Number(input.amount ?? 0));
      if (!Number.isSafeInteger(amount) || amount <= 0 || amount > MAX_DB_INTEGER) {
        return json({ error: "Informe uma quantidade de XP maior que zero." }, 400);
      }

      const { data: before, error: beforeError } = await sb
        .from("xp_accounts")
        .select("total_earned,balance")
        .eq("member_id", memberId)
        .single();
      if (beforeError) throw beforeError;

      if (
        Number(before.total_earned ?? 0) + amount > MAX_DB_INTEGER ||
        Number(before.balance ?? 0) + amount > MAX_DB_INTEGER
      ) {
        return json({ error: "Esse valor ultrapassa o limite técnico da conta XP." }, 400);
      }

      const receiptId = `admin_extra:${memberId}:${crypto.randomUUID()}`;
      const { data: awarded, error: awardError } = await sb.rpc("xp_award", {
        p_member_id: memberId,
        p_activity: "admin_extra",
        p_content_id: receiptId,
        p_variant: "admin_manual",
        p_receipt_id: receiptId,
        p_amount: amount,
        p_description: "XP Extra concedido pelo ADM",
        p_daily_cap: 0,
      });
      if (awardError) throw awardError;

      const result = Array.isArray(awarded) ? awarded[0] : awarded;
      if (!result || Number(result.granted ?? 0) !== amount) {
        throw new Error("O crédito de XP não foi confirmado pelo ledger central.");
      }

      const current = await snapshot();
      return json({ ok: true, memberId, granted: amount, receiptId, ...current });
    }

    return json({ error: "Ação inválida." }, 400);
  } catch (error) {
    console.error("xp-member-admin failed", error);
    const message = error instanceof Error ? error.message : "Falha na administração do XP do membro.";
    const lowered = message.toLowerCase();
    const status = lowered.includes("sessão") ? 401 : lowered.includes("administrativ") ? 403 : lowered.includes("inválid") ? 400 : 500;
    return json({ error: message }, status);
  }
});
