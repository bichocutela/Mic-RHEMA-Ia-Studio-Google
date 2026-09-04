import { importPKCS8, SignJWT } from "npm:jose@5.10.0";

const GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
const IDENTITY_TOOLKIT_AUDIENCE = "https://identitytoolkit.googleapis.com/google.identity.identitytoolkit.v1.IdentityToolkit";
const ADMIN_PASSWORD = Deno.env.get("RHEMA_ADMIN_PASSWORD") || "igreja10";
const ADMIN_EMAIL = "admin@micrhema.app";

type ServiceAccount = { project_id?: string; client_email?: string; private_key?: string };
type Member = { id: string; name: string; isApproved: boolean; isIbr: boolean; isAdmin: boolean };

const cors = {
  "access-control-allow-origin": "*",
  "access-control-allow-headers": "authorization, apikey, content-type, x-client-info",
  "access-control-allow-methods": "POST, OPTIONS",
  "content-type": "application/json; charset=utf-8",
  "cache-control": "no-store",
};

function json(body: Record<string, unknown>, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: cors });
}
function clean(value: unknown) { return String(value ?? "").trim(); }
function normalizedPhone(value: string) { return value.replace(/\D/g, ""); }

async function googleAccessToken(account: ServiceAccount) {
  if (!account.client_email || !account.private_key) throw new Error("Credencial Firebase incompleta.");
  const now = Math.floor(Date.now() / 1000);
  const privateKey = await importPKCS8(account.private_key.replace(/\\n/g, "\n"), "RS256");
  const assertion = await new SignJWT({ iss: account.client_email, scope: "https://www.googleapis.com/auth/datastore", aud: GOOGLE_TOKEN_URL })
    .setProtectedHeader({ alg: "RS256", typ: "JWT" }).setIssuedAt(now).setExpirationTime(now + 3600).sign(privateKey);
  const response = await fetch(GOOGLE_TOKEN_URL, {
    method: "POST", headers: { "content-type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth-type:jwt-bearer", assertion }),
  });
  if (!response.ok) {
    const retry = await fetch(GOOGLE_TOKEN_URL, {
      method: "POST", headers: { "content-type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", assertion }),
    });
    const retryPayload = await retry.json();
    if (!retry.ok || !retryPayload.access_token) throw new Error("Não foi possível autenticar a consulta de membros.");
    return retryPayload.access_token as string;
  }
  const payload = await response.json();
  if (!payload.access_token) throw new Error("Não foi possível autenticar a consulta de membros.");
  return payload.access_token as string;
}

function firebaseValue(field: Record<string, unknown> | undefined): unknown {
  if (!field) return undefined;
  return field.stringValue ?? field.booleanValue ?? field.integerValue ?? field.timestampValue;
}

function memberFromDocument(document: Record<string, any> | undefined): Member | null {
  if (!document) return null;
  const fields = document.fields as Record<string, Record<string, unknown>>;
  const id = String(document.name || "").split("/").pop() || "";
  if (!id) return null;
  return {
    id,
    name: String(firebaseValue(fields.name) || "Membro MIC Rhema"),
    isApproved: firebaseValue(fields.isApproved) === true,
    isIbr: firebaseValue(fields.isIbr) === true,
    isAdmin: firebaseValue(fields.isAdmin) === true,
  };
}

async function queryMember(account: ServiceAccount, fieldPath: string, value: Record<string, unknown>): Promise<Member | null> {
  const projectId = account.project_id || Deno.env.get("FIREBASE_PROJECT_ID") || "mic-rhema";
  const token = await googleAccessToken(account);
  const response = await fetch(`https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents:runQuery`, {
    method: "POST",
    headers: { authorization: `Bearer ${token}`, "content-type": "application/json" },
    body: JSON.stringify({ structuredQuery: { from: [{ collectionId: "acessos_pendentes" }], where: { fieldFilter: { field: { fieldPath }, op: "EQUAL", value } }, limit: 10 } }),
  });
  if (!response.ok) throw new Error("Não foi possível consultar as aprovações.");
  const rows = await response.json() as Array<Record<string, any>>;
  return memberFromDocument(rows.find((row) => row.document)?.document);
}

async function findMember(account: ServiceAccount, phone: string) {
  return queryMember(account, "phone", { stringValue: phone });
}

async function findAndroidAdmin(account: ServiceAccount) {
  const byEmail = await queryMember(account, "email", { stringValue: ADMIN_EMAIL }).catch(() => null);
  if (byEmail?.isAdmin) return byEmail;
  const byFlag = await queryMember(account, "isAdmin", { booleanValue: true }).catch(() => null);
  return byFlag?.isAdmin ? byFlag : null;
}

async function customFirebaseToken(account: ServiceAccount, uid: string, claims: Record<string, boolean | string>) {
  if (!account.client_email || !account.private_key) throw new Error("Credencial Firebase incompleta.");
  const now = Math.floor(Date.now() / 1000);
  const privateKey = await importPKCS8(account.private_key.replace(/\\n/g, "\n"), "RS256");
  return new SignJWT({ iss: account.client_email, sub: account.client_email, aud: IDENTITY_TOOLKIT_AUDIENCE, uid, claims })
    .setProtectedHeader({ alg: "RS256", typ: "JWT" }).setIssuedAt(now).setExpirationTime(now + 3600).sign(privateKey);
}

Deno.serve(async (request) => {
  if (request.method === "OPTIONS") return new Response("ok", { headers: cors });
  if (request.method !== "POST") return json({ error: "Método não permitido." }, 405);
  try {
    const input = await request.json() as { name?: string; phone?: string; password?: string };
    const name = clean(input.name); const phone = clean(input.phone);
    const account = JSON.parse(Deno.env.get("FIREBASE_SERVICE_ACCOUNT_JSON") || "{}") as ServiceAccount;
    const isAdministrator = name.toLowerCase() === "admin" && clean(input.password) === ADMIN_PASSWORD;

    let member: Member;
    if (isAdministrator) {
      const androidAdmin = await findAndroidAdmin(account);
      member = androidAdmin
        ? { ...androidAdmin, name: androidAdmin.name || "Administrador", isApproved: true, isAdmin: true, isIbr: true }
        : { id: "admin", name: "Administrador", isApproved: true, isIbr: true, isAdmin: true };
    } else {
      if (!name || normalizedPhone(phone).length < 8) return json({ error: "Informe o nome e o telefone cadastrados." }, 400);
      member = await findMember(account, phone) ?? { id: "", name: "", isApproved: false, isIbr: false, isAdmin: false };
      if (!member.id || !member.isApproved) return json({ error: "Seu acesso ainda está pendente de aprovação pela administração." }, 403);
      if (member.name.toLocaleLowerCase("pt-BR") !== name.toLocaleLowerCase("pt-BR")) return json({ error: "Nome ou telefone não correspondem ao cadastro aprovado." }, 403);
    }

    const token = await customFirebaseToken(account, member.id, { isAdmin: member.isAdmin, isIbr: member.isIbr, memberId: member.id });
    return json({ ok: true, token, member });
  } catch (error) {
    console.error("pwa-auth failed", error instanceof Error ? error.message : "unknown");
    return json({ error: "Não foi possível iniciar sua sessão agora." }, 500);
  }
});
