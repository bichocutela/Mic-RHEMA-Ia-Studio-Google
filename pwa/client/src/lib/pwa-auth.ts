/**
 * SANTUÁRIO EM MOVIMENTO — Sessão web protegida.
 * Nenhuma senha de administração ou chave de serviço é incluída no bundle publicado.
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

export async function signInPwa(input: { name: string; phone: string; password?: string }): Promise<PwaSession> {
  if (!firebaseAuth) {
    throw new Error("A conexão Firebase da PWA ainda não foi configurada para este ambiente.");
  }

  const response = await fetch(`${supabaseUrl}/functions/v1/pwa-auth`, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify(input),
  });
  const payload = await response.json().catch(() => ({}));
  if (!response.ok || !payload.token) {
    throw new Error(payload.error || "Não foi possível entrar. Confira seus dados ou aguarde a aprovação.");
  }

  const result = await signInWithCustomToken(firebaseAuth, payload.token);
  return {
    uid: result.user.uid,
    name: payload.member?.name || input.name,
    isAdmin: payload.member?.isAdmin === true,
    isIbr: payload.member?.isIbr === true,
  };
}
