import { AccessError } from "./migration-auth.ts";
import { firebaseAdministratorUid } from "./firebase-password-auth.ts";
let response: { status: number; body: unknown };
let calls = 0;
globalThis.fetch = (async (input: RequestInfo | URL, init?: RequestInit) => {
  if (!String(input).startsWith("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?")) throw new Error("Wrong authentication endpoint");
  const body = JSON.parse(String(init?.body));
  if (body.email !== "admin@micrhema.app" || body.returnSecureToken !== true) throw new Error("Wrong admin identity");
  calls++;
  return Response.json(response.body, { status: response.status });
}) as typeof fetch;
async function rejects(run: () => Promise<unknown>) { try { await run(); } catch (error) { if (error instanceof AccessError && error.status === 403) return; throw error; } throw new Error("Access unexpectedly allowed"); }
Deno.test("administrator password is validated by Firebase, not a bundled default", async () => {
  response = { status: 200, body: { localId: "existing-admin", email: "admin@micrhema.app" } };
  if (await firebaseAdministratorUid("user-supplied-password", "public-web-api-key") !== "existing-admin") throw new Error("Wrong UID");
  response = { status: 400, body: { error: { message: "INVALID_LOGIN_CREDENTIALS" } } };
  await rejects(() => firebaseAdministratorUid("invalid", "public-web-api-key"));
  response = { status: 200, body: { localId: "other-user", email: "other@example.test" } };
  await rejects(() => firebaseAdministratorUid("password", "public-web-api-key"));
  const before = calls;
  await rejects(() => firebaseAdministratorUid("", "public-web-api-key"));
  if (calls !== before) throw new Error("Empty password reached Firebase");
});
