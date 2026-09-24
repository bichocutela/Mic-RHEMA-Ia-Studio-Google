/**
 * MIC Rhema — sessão web usando a mesma identidade de membro do Android.
 * O telefone com DDD identifica o cadastro; nome completo só é obrigatório ao criar uma nova solicitação.
 */
import { signInWithCustomToken } from "firebase/auth";
import { firebaseAdminAuth, firebaseAuth } from "./firebase";

const supabaseUrl = import.meta.env.VITE_SUPABASE_URL || "https://cwphbkdtorfpgmnlafqb.supabase.co";
const supabasePublishableKey = import.meta.env.VITE_SUPABASE_ANON_KEY || "sb_publishable_Dv98hBnbJB2TzRCG6aJNwA_KMPHLZSw";

export type PwaSession = {
  uid: string;
  name: string;
  isAdmin: boolean;
  isIbr: boolean;
  isApproved?: boolean;
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

type RecoveryPayload = {
  ok?: boolean;
  found?: boolean;
  customToken?: string;
  duplicateCount?: number;
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

async function recoverAndroidMemberSession(phone: string): Promise<RecoveryPayload> {
  const response = await fetch(`${supabaseUrl}/functions/v1/member-session`, {
    method: "POST",
    headers: {
      apikey: supabasePublishableKey,
      authorization: `Bearer ${supabasePublishableKey}`,
      "content-type": "application/json",
    },
    body: JSON.stringify({ action: "recover", phone }),
  });
  const payload = await response.json().catch(() => ({})) as RecoveryPayload;
  if (!response.ok) throw new Error(payload.error || "Não foi possível verificar seu cadastro agora.");
  return payload;
}

async function sessionFromRecovery(payload: RecoveryPayload, fallbackName: string): Promise<PwaSession> {
  if (!firebaseAuth) throw new Error("A conexão Firebase da PWA ainda não foi configurada para este ambiente.");
  if (!payload.customToken) throw new Error(payload.error || "O servidor não retornou uma sessão válida.");
  const result = await signInWithCustomToken(firebaseAuth, payload.customToken);
  return {
    uid: result.user.uid,
    name: payload.member?.name || fallbackName || "Membro MIC Rhema",
    isAdmin: false,
    isIbr: payload.member?.isIbr === true,
    isApproved: payload.member?.isApproved === true,
  };
}

async function sessionFromPayload(payload: AuthPayload, fallbackName: string, admin = false): Promise<PwaSession> {
  const auth = admin ? firebaseAdminAuth : firebaseAuth;
  if (!auth) throw new Error("A conexão Firebase da PWA ainda não foi configurada para este ambiente.");
  if (!payload.token) throw new Error(payload.error || "O servidor não retornou uma sessão válida.");
  if (admin && payload.member?.isAdmin !== true) throw new Error("Acesso administrativo inválido.");
  const result = await signInWithCustomToken(auth, payload.token);
  return {
    uid: result.user.uid,
    name: payload.member?.name || fallbackName || "Membro MIC Rhema",
    isAdmin: admin,
    isIbr: payload.member?.isIbr === true,
    isApproved: payload.member?.isApproved === true || admin,
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

  // Primeiro usa exatamente a recuperação do Android. Isso preserva a identidade
  // pelo telefone, consolida cadastros duplicados e recupera também perfis pendentes.
  const recovery = await recoverAndroidMemberSession(phone);
  if (recovery.found) {
    const session = await sessionFromRecovery(recovery, completeName);
    return {
      session,
      pending: session.isApproved !== true,
      requested: false,
      message: recovery.duplicateCount && recovery.duplicateCount > 0
        ? "Acesso recuperado. Registros antigos duplicados foram ignorados."
        : "Acesso e progresso recuperados.",
    };
  }

  // Se não existe cadastro, a função PWA cria a mesma solicitação pendente usada
  // pelo app. Versões novas do backend já devolvem token também para o perfil pendente.
  const payload = await authRequest({ name: completeName, phone });
  if (payload.token) {
    const session = await sessionFromPayload(payload, completeName, false);
    return {
      session,
      pending: payload.pending === true || session.isApproved !== true,
      requested: payload.requested === true,
      message: payload.requested
        ? "Solicitação enviada. Aguarde a aprovação do administrador."
        : "Acesso e progresso recuperados.",
    };
  }

  // Compatibilidade durante uma eventual janela de deploy do backend.
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

  const session = await sessionFromPayload(payload, completeName, false);
  return { session, pending: false, requested: false, message: "Acesso e progresso recuperados." };
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
  return sessionFromPayload(payload, "Administrador", true);
}
