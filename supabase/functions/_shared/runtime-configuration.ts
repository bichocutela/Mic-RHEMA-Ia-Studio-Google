/** Migrated credentials stay in Vault and are readable only by the server role. */
const allowed = new Set(["FIREBASE_SERVICE_ACCOUNT_JSON", "FIREBASE_PROJECT_ID", "YOUTUBE_API_KEY", "RHEMA_ADMIN_PASSWORD_SHA256", "FIREBASE_WEB_API_KEY"]);
let pending: Promise<void> | undefined;
const migrated: Record<string, string> = {};
export function runtimeEnv(name: string): string | undefined {
  return Deno.env.get(name) || migrated[name];
}
export function loadRuntimeConfiguration(): Promise<void> {
  if (pending) return pending;
  pending = (async () => {
    const url = Deno.env.get("SUPABASE_URL") || "";
    const key = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") || "";
    if (url.replace(/\/$/, "") !== "https://sinzscjscvgzkvkimcbd.supabase.co" || !key) return;
    const response = await fetch(`${url}/rest/v1/rpc/micrhema_runtime_configuration`, {
      method: "POST", headers: { apikey: key, authorization: `Bearer ${key}`, "content-type": "application/json" }, body: "{}",
    });
    if (!response.ok) throw new Error("A configuração privada do servidor está indisponível.");
    const values = await response.json() as Record<string, unknown>;
    for (const [name, value] of Object.entries(values)) {
      if (allowed.has(name) && typeof value === "string" && value) migrated[name] = value;
    }
  })().catch(error => { pending = undefined; throw error; });
  return pending;
}
