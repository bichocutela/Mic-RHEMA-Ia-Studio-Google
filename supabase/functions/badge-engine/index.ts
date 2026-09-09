import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "npm:@supabase/supabase-js@2.57.4";

const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-client-info",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Content-Type": "application/json; charset=utf-8",
  "Cache-Control": "no-store",
};

const json = (body: Record<string, unknown>, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: cors });
const clean = (value: unknown, max = 1000) => String(value ?? "").trim().slice(0, max);

Deno.serve(async (request) => {
  if (request.method === "OPTIONS") return new Response("ok", { headers: cors });
  if (request.method !== "POST") return json({ error: "Método não permitido." }, 405);

  try {
    const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
    const serviceRole = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
    if (!supabaseUrl || !serviceRole) throw new Error("Backend de emblemas não configurado.");
    const sb = createClient(supabaseUrl, serviceRole, {
      auth: { persistSession: false, autoRefreshToken: false },
    });

    const input = await request.json() as Record<string, unknown>;
    const action = clean(input.action, 40);

    if (action === "catalog") {
      const { data, error } = await sb.from("custom_profile_badges")
        .select("id,sequence_no,name,description,challenge,image_ref,special,active,challenge_metric,challenge_target,created_at,updated_at")
        .eq("active", true)
        .order("special", { ascending: true })
        .order("sequence_no", { ascending: true, nullsFirst: false })
        .order("created_at", { ascending: true });
      if (error) throw error;
      return json({ ok: true, badges: data ?? [] });
    }

    if (action === "reconcile") {
      const memberId = clean(input.memberId, 120);
      if (!memberId) return json({ error: "Membro inválido." }, 400);

      const { data: progress, error: progressError } = await sb.rpc("xp_reconcile_custom_badges", {
        p_member_id: memberId,
      });
      if (progressError) throw progressError;

      const { data: unlockRows, error: unlockError } = await sb.from("custom_badge_unlocks")
        .select("badge_id,source,unlocked_at")
        .eq("member_id", memberId)
        .order("unlocked_at", { ascending: true });
      if (unlockError) throw unlockError;

      const newlyUnlockedIds = (progress ?? [])
        .filter((row: Record<string, unknown>) => row.newly_unlocked === true)
        .map((row: Record<string, unknown>) => clean(row.badge_id, 120))
        .filter(Boolean);
      const unlockedBadgeIds = (unlockRows ?? [])
        .map((row: Record<string, unknown>) => clean(row.badge_id, 120))
        .filter(Boolean);

      return json({
        ok: true,
        unlockedBadgeIds,
        newlyUnlockedIds,
        progress: progress ?? [],
      });
    }

    return json({ error: "Ação inválida." }, 400);
  } catch (error) {
    console.error("badge-engine failed", error);
    return json({ error: error instanceof Error ? error.message : "Falha no motor remoto de emblemas." }, 500);
  }
});
