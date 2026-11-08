import { AccessError } from "./migration-auth.ts";

/** Validate the existing Firebase account; never create an admin from a default. */
export async function firebaseAdministratorUid(password: string, apiKey: string): Promise<string> {
  if (!password) throw new AccessError(403, "Credenciais administrativas inválidas.");
  const response = await fetch(`https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=${encodeURIComponent(apiKey)}`, {
    method: "POST", headers: { "content-type": "application/json" },
    body: JSON.stringify({ email: "admin@micrhema.app", password, returnSecureToken: true }),
  });
  const payload = await response.json() as { localId?: string; email?: string };
  if (!response.ok || !payload.localId || payload.email?.toLowerCase() !== "admin@micrhema.app") {
    throw new AccessError(403, "Credenciais administrativas inválidas.");
  }
  return payload.localId;
}
