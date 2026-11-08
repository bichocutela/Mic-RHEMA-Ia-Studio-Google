import { exportJWK, generateKeyPair, SignJWT } from "npm:jose@5.10.0";
import { AccessError, administrator, caller, ownsMember, verifiedPhone, type Caller } from "./migration-auth.ts";

function assert(value: unknown, message = "Assertion failed"): asserts value { if (!value) throw new Error(message); }
const { privateKey, publicKey } = await generateKeyPair("RS256", { extractable: true });
const jwk = { ...await exportJWK(publicKey), kid: "test", alg: "RS256", use: "sig" };
const originalFetch = globalThis.fetch;
globalThis.fetch = ((input: RequestInfo | URL, init?: RequestInit) => {
  if (String(input).includes("www.googleapis.com/service_accounts/v1/jwk/")) return Promise.resolve(Response.json({ keys: [jwk] }));
  throw new Error(`Unexpected network access: ${input}`);
}) as typeof fetch;
async function token(claims: Record<string, unknown> = {}, audience = "mic-rhema", expires = "5m") {
  return await new SignJWT({ sub: "member-a", ...claims }).setProtectedHeader({ alg: "RS256", kid: "test" }).setIssuer("https://securetoken.google.com/mic-rhema").setAudience(audience).setIssuedAt().setExpirationTime(expires).sign(privateKey);
}
const request = (bearer?: string, extra: Record<string, string> = {}) => new Request("https://example.test", { headers: { ...extra, ...(bearer ? { authorization: `Bearer ${bearer}` } : {}) } });
async function denied(run: () => Promise<unknown>, status: number) { try { await run(); throw new Error("Access unexpectedly allowed"); } catch (error) { assert(error instanceof AccessError && error.status === status, String(error)); } }

Deno.test("validates signature, audience, expiry and required bearer", async () => {
  assert((await caller(request(await token()))).sub === "member-a");
  await denied(() => caller(request()), 401);
  await denied(() => caller(request("sb_publishable_public")), 401);
  await denied(() => caller(request("a.b.c")), 401);
  await denied(async () => caller(request(await token({}, "other-project"))), 401);
  await denied(async () => caller(request(await token({}, "mic-rhema", "-1m"))), 401);
  const good = await token(); const parts = good.split("."); parts[1] = btoa(JSON.stringify({ sub: "attacker", isAdmin: true }));
  await denied(() => caller(request(parts.join("."))), 401);
});
Deno.test("admin actions require verified admin claim or explicit internal secret", async () => {
  await denied(async () => administrator(request(await token())), 403);
  await administrator(request(await token({ isAdmin: true })));
  Deno.env.set("SUPABASE_SERVICE_ROLE_KEY", "test-only-internal-key");
  await administrator(request(undefined, { "x-internal-scheduler": "test-only-internal-key" }), true);
  await denied(() => administrator(request(undefined, { "x-internal-scheduler": "wrong" }), true), 401);
  await denied(() => administrator(request(undefined, { "x-internal-scheduler": "test-only-internal-key" })), 401);
  Deno.env.delete("SUPABASE_SERVICE_ROLE_KEY");
  await denied(() => administrator(request(undefined, { "x-internal-scheduler": "" }), true), 401);
});
Deno.test("member ownership cannot be claimed by changing member ID or phone", () => {
  const identity = { sub: "member-a", firebase: { sign_in_provider: "custom" } } as Caller;
  assert(ownsMember(identity, "member-a", {}));
  assert(!ownsMember(identity, "member-b", { phone: "84999999999" }));
  assert(!ownsMember({ ...identity, firebase: { sign_in_provider: "anonymous" } }, "member-a", {}));
  assert(!ownsMember(identity, "member-a", { isAdmin: true }));
  assert(ownsMember({ ...identity, isAdmin: true }, "member-b", { isAdmin: true }));
});
Deno.test("phone recovery requires recent provider-verified phone and cannot claim admin accounts", () => {
  const identity = { sub: "phone-uid", phone_number: "+5584999999999", auth_time: Math.floor(Date.now() / 1000), firebase: { sign_in_provider: "phone" } } as Caller;
  assert(verifiedPhone(identity, "84999999999"));
  assert(ownsMember(identity, "legacy-id", { phone: "84999999999" }));
  assert(!ownsMember(identity, "admin-id", { phone: "84999999999", isAdmin: true }));
  assert(!verifiedPhone({ ...identity, auth_time: identity.auth_time! - 901 }, "84999999999"));
  assert(!verifiedPhone({ ...identity, firebase: { sign_in_provider: "custom" } }, "84999999999"));
  assert(!verifiedPhone(identity, "84988888888"));
});

const handlers: Array<(req: Request) => Response | Promise<Response>> = [];
const serve = Deno.serve;
Object.defineProperty(Deno, "serve", { configurable: true, value: (handler: typeof handlers[number]) => { handlers.push(handler); return {}; } });
for (const slug of ["pwa-auth", "member-session", "notify-fcm", "xp-shop-admin-simple", "badge-engine", "xp-light-effects-admin"]) await import(`../${slug}/index.ts`);
Object.defineProperty(Deno, "serve", { configurable: true, value: serve });
Deno.env.set("SUPABASE_URL", "https://project.supabase.co");
Deno.env.set("SUPABASE_SERVICE_ROLE_KEY", "test-only-no-database-access");
const cases = [
  { name: "pwa-auth", body: { name: "Membro", phone: "84999999999" } },
  { name: "member-session", body: { action: "recover", phone: "84999999999" } },
  { name: "notify-fcm", body: { title: "Unauthorized broadcast" } },
  { name: "xp-shop-admin-simple", body: { action: "admin_upsert_item" } },
  { name: "badge-engine", body: { action: "reconcile", memberId: "member-b" } },
  { name: "xp-light-effects-admin", body: { action: "upsert" } },
];
for (const [index, item] of cases.entries()) Deno.test(`${item.name}: rejects unauthenticated requests before external writes; accepts preflight`, async () => {
  const response = await handlers[index](new Request("https://example.test", { method: "POST", headers: { "content-type": "application/json" }, body: JSON.stringify(item.body) }));
  assert(response.status === 401, `${item.name}: ${response.status} ${await response.text()}`);
  const preflight = await handlers[index](new Request("https://example.test", { method: "OPTIONS" }));
  assert(preflight.status === 200);
});
Deno.test("badge reconciliation rejects another member's signed session", async () => {
  const response = await handlers[4](new Request("https://example.test", { method: "POST", headers: { authorization: `Bearer ${await token()}`, "content-type": "application/json" }, body: JSON.stringify({ action: "reconcile", memberId: "member-b" }) }));
  assert(response.status === 403);
});
