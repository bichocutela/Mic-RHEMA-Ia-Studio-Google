import { loadRuntimeConfiguration, runtimeEnv } from "./runtime-configuration.ts";
let calls = 0;
Deno.env.set("SUPABASE_URL", "https://sinzscjscvgzkvkimcbd.supabase.co");
Deno.env.set("SUPABASE_SERVICE_ROLE_KEY", "server-role-test-only");
globalThis.fetch = (async (url: RequestInfo | URL, init?: RequestInit) => {
  if (String(url) !== "https://sinzscjscvgzkvkimcbd.supabase.co/rest/v1/rpc/micrhema_runtime_configuration") throw new Error("Wrong configuration destination");
  const headers = new Headers(init?.headers);
  if (headers.get("authorization") !== "Bearer server-role-test-only" || headers.get("apikey") !== "server-role-test-only") throw new Error("Missing server authorization");
  calls++;
  return Response.json({ FIREBASE_PROJECT_ID: "mic-rhema", YOUTUBE_API_KEY: "migrated-test-key", SUPABASE_SERVICE_ROLE_KEY: "must-be-ignored", UNEXPECTED: "must-be-ignored" });
}) as typeof fetch;
Deno.test("runtime configuration uses the server role, caches one read and ignores unapproved names", async () => {
  await Promise.all([loadRuntimeConfiguration(), loadRuntimeConfiguration()]);
  if (calls !== 1 || runtimeEnv("FIREBASE_PROJECT_ID") !== "mic-rhema") throw new Error("Configuration was not loaded once");
  if (runtimeEnv("SUPABASE_SERVICE_ROLE_KEY") !== "server-role-test-only" || runtimeEnv("UNEXPECTED")) throw new Error("Protected environment was overridden");
  Deno.env.set("YOUTUBE_API_KEY", "environment-has-priority");
  if (runtimeEnv("YOUTUBE_API_KEY") !== "environment-has-priority") throw new Error("Native configuration lost priority");
});
