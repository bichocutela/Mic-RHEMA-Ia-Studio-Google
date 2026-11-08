import { createRemoteJWKSet, jwtVerify, type JWTPayload } from "npm:jose@5.10.0";

const project = "mic-rhema";
const keys = createRemoteJWKSet(new URL("https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com"));

export class AccessError extends Error {
  constructor(public status: number, message: string) { super(message); }
}

export type Caller = JWTPayload & { sub: string; isAdmin?: boolean; phone_number?: string; auth_time?: number; firebase?: { sign_in_provider?: string } };

export function phoneDigits(value: unknown): string {
  const digits = String(value ?? "").replace(/\D/g, "");
  return digits.startsWith("55") && digits.length >= 12 && digits.length <= 13 ? digits.slice(2) : digits;
}

export async function caller(request: Request): Promise<Caller> {
  const token = request.headers.get("authorization")?.match(/^Bearer\s+(.+)$/i)?.[1];
  if (!token) throw new AccessError(401, "Entre novamente para confirmar sua identidade.");
  try {
    const { payload } = await jwtVerify(token, keys, { issuer: `https://securetoken.google.com/${project}`, audience: project, algorithms: ["RS256"] });
    if (!payload.sub || payload.sub.length > 128) throw new Error("uid");
    return payload as Caller;
  } catch { throw new AccessError(401, "Sessão inválida ou expirada. Entre novamente."); }
}

export async function administrator(request: Request, allowInternal = false): Promise<void> {
  const secret = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (allowInternal && secret && (request.headers.get("x-internal-scheduler") === secret || request.headers.get("authorization") === `Bearer ${secret}`)) return;
  if ((await caller(request)).isAdmin !== true) throw new AccessError(403, "Acesso administrativo obrigatório.");
}

export function verifiedPhone(identity: Caller, phone: string): boolean {
  const age = Math.floor(Date.now() / 1000) - Number(identity.auth_time ?? 0);
  return identity.firebase?.sign_in_provider === "phone" && phoneDigits(identity.phone_number) === phone && age >= 0 && age <= 900;
}

export function ownsMember(identity: Caller, memberId: string, member: Record<string, unknown>): boolean {
  if (identity.isAdmin === true) return true;
  if (member.isAdmin === true) return false;
  if (identity.firebase?.sign_in_provider === "anonymous") return false;
  return identity.sub === memberId || identity.sub === member.firebaseUid || verifiedPhone(identity, phoneDigits(member.phone));
}
