/**
 * MIC Rhema — sessão web usando a mesma identidade de membro do Android.
 * O telefone com DDD identifica o cadastro; nome completo só é obrigatório ao criar uma nova solicitação.
 */
import { signInWithCustomToken } from "firebase/auth";
import { firebaseAuth } from "./firebase";

const supabaseUrl = import.meta.env.VITE_SUPABASE_URL || "https://cwphbkdtorfpgmnlafqb.supabase.co";

export type PwaSession = {
  uid: string;
  name: string;
  isAdmin: boolean;
  isIbr: boolean;
};

export type PwaMemberAccessResult = {
  session: PwaSession | null;
  pending: boolean;
  requested: boolean;
  message?: string;
};

type AuthPayload = {
  ok?: boolean;
  token?: string;
  pending?: boolean;
  requested?: boolean;
  error?: string;
  member?: {
    id?: string;
    name?: string;
    isAdmin?: boolean;
    isIbr?: boolean;
    isApproved?: boolean;
  };
};

export function normalizeMemberPhone(value: string) {
  const digits = String(value || "").replace(/\D/g, "");
  return digits.length >= 12 && digits.length <= 13 && digits.startsWith("55") ? digits.slice(2) : digits;
}

async function authRequest(input: { name: string; phone: string; password?: string }): Promise<AuthPayload> {
  const response = await fetch(`${supabaseUrl}/functions/v1/pwa-auth`, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify(input),
  });
  const payload = await response.json().catch(() => ({})) as AuthPayload;
  if (!response.ok) throw new Error(payload.error || "Não foi possível acessar sua conta agora.");
  return payload;
}

async function sessionFromPayload(payload: AuthPayload, fallbackName: string): Promise<PwaSession> {
  if (!firebaseAuth) throw new Error("A conexão Firebase da PWA ainda não foi configurada para este ambiente.");
  if (!payload.token) throw new Error(payload.error || "O servidor não retornou uma sessão válida.");
  const result = await signInWithCustomToken(firebaseAuth, payload.token);
  return {
    uid: result.user.uid,
    name: payload.member?.name || fallbackName || "Membro MIC Rhema",
    isAdmin: payload.member?.isAdmin === true,
    isIbr: payload.member?.isIbr === true,
  };
}

/**
 * Fluxo idêntico ao Android para membros: primeiro recupera pelo telefone.
 * Se o telefone não existir, o backend cria a solicitação canônica phone_{DDD+numero}.
 */
export async function signInOrRequestPwa(input: { name: string; phone: string }): Promise<PwaMemberAccessResult> {
  const completeName = input.name.trim();
  const phone = normalizeMemberPhone(input.phone);
  if (!completeName || phone.length < 10 || phone.length > 11) {
    throw new Error("Preencha seu nome completo e um telefone válido com DDD.");
  }

  const payload = await authRequest({ name: completeName, phone });
  if (payload.pending || payload.requested) {
    return {
      session: null,
      pending: true,
      requested: payload.requested === true,
      message: payload.requested
        ? "Solicitação enviada. Aguarde a aprovação do administrador."
        : "Seu cadastro já existe e ainda está aguardando aprovação do administrador.",
    };
  }

  const session = await sessionFromPayload(payload, completeName);
  return { session, pending: false, requested: false };
}

/** Mantém o fluxo administrativo e compatibilidade com chamadas antigas. */
export async function signInPwa(input: { name: string; phone: string; password?: string }): Promise<PwaSession> {
  const isAdmin = input.name.trim().toLowerCase() === "admin";
  if (!isAdmin) {
    const result = await signInOrRequestPwa({ name: input.name, phone: input.phone });
    if (!result.session) throw new Error(result.message || "Seu acesso ainda está pendente de aprovação.");
    return result.session;
  }

  const payload = await authRequest({ name: "admin", phone: "admin", password: input.password || "" });
  return sessionFromPayload(payload, "Administrador");
}
