import { importPKCS8, SignJWT } from "npm:jose@5.10.0";

const GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
const IDENTITY_TOOLKIT_AUDIENCE = "https://identitytoolkit.googleapis.com/google.identity.identitytoolkit.v1.IdentityToolkit";
const FIREBASE_PROJECT_ID = "mic-rhema";
const ADMIN_PASSWORD_SHA256 = Deno.env.get("RHEMA_ADMIN_PASSWORD_SHA256") || "42fd905d0baa828374a8801ad5e12d730109b02fd0f16f0a8414cb8c40d0c329";
const ADMIN_EMAIL = "admin@micrhema.app";

type ServiceAccount = { project_id?: string; client_email?: string; private_key?: string };
type FirestoreField = { stringValue?: string; booleanValue?: boolean; integerValue?: string; timestampValue?: string };
type FirestoreDocument = { name?: string; fields?: Record<string, FirestoreField> };
type Member = { id: string; name: string; isApproved: boolean; isIbr: boolean; isAdmin: boolean };

const cors = {
  "access-control-allow-origin": "*",
  "access-control-allow-headers": "authorization, apikey, content-type, x-client-info",
  "access-control-allow-methods": "POST, OPTIONS",
  "content-type": "application/json; charset=utf-8",
  "cache-control": "no-store",
};

function json(body: Record<string, unknown>, status = 200) { return new Response(JSON.stringify(body), { status, headers: cors }); }
function clean(value: unknown) { return String(value ?? "").trim(); }
function normalizePhone(value: unknown) {
  const digits = String(value ?? "").replace(/\D/g, "");
  return digits.length >= 12 && digits.length <= 13 && digits.startsWith("55") ? digits.slice(2) : digits;
}
function shortMemberName(value: string) {
  return value.trim().split(/\s+/).filter(Boolean).slice(0, 2).join(" ");
}
async function sha256(value: string) {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value));
  return [...new Uint8Array(digest)].map((byte) => byte.toString(16).padStart(2, "0")).join("");
}

async function googleAccessToken(account: ServiceAccount) {
  if (!account.client_email || !account.private_key) throw new Error("Credencial Firebase incompleta.");
  const now = Math.floor(Date.now() / 1000);
  const privateKey = await importPKCS8(account.private_key.replace(/\\n/g, "\n"), "RS256");
  const assertion = await new SignJWT({ iss: account.client_email, scope: "https://www.googleapis.com/auth/datastore", aud: GOOGLE_TOKEN_URL })
    .setProtectedHeader({ alg: "RS256", typ: "JWT" }).setIssuedAt(now).setExpirationTime(now + 3600).sign(privateKey);
  const response = await fetch(GOOGLE_TOKEN_URL, {
    method: "POST",
    headers: { "content-type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", assertion }),
  });
  const payload = await response.json();
  if (!response.ok || !payload.access_token) throw new Error("Não foi possível autenticar a consulta de membros.");
  return String(payload.access_token);
}

function firebaseValue(field: FirestoreField | undefined): unknown {
  if (!field) return undefined;
  return field.stringValue ?? field.booleanValue ?? field.integerValue ?? field.timestampValue;
}

function memberFromDocument(document?: FirestoreDocument | null): Member | null {
  if (!document) return null;
  const fields = document.fields || {};
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

function baseUrl(projectId: string) {
  return `https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents`;
}

async function getMemberById(projectId: string, accessToken: string, id: string): Promise<FirestoreDocument | null> {
  const response = await fetch(`${baseUrl(projectId)}/acessos_pendentes/${encodeURIComponent(id)}`, {
    headers: { authorization: `Bearer ${accessToken}` },
  });
  if (response.status === 404) return null;
  if (!response.ok) throw new Error("Não foi possível consultar o cadastro do membro.");
  return await response.json() as FirestoreDocument;
}

async function queryMembers(projectId: string, accessToken: string, fieldPath: string, value: Record<string, unknown>): Promise<FirestoreDocument[]> {
  const response = await fetch(`${baseUrl(projectId)}:runQuery`, {
    method: "POST",
    headers: { authorization: `Bearer ${accessToken}`, "content-type": "application/json" },
    body: JSON.stringify({
      structuredQuery: {
        from: [{ collectionId: "acessos_pendentes" }],
        where: { fieldFilter: { field: { fieldPath }, op: "EQUAL", value } },
        limit: 20,
      },
    }),
  });
  if (!response.ok) throw new Error("Não foi possível consultar os cadastros.");
  const rows = await response.json() as Array<{ document?: FirestoreDocument }>;
  return rows.map((row) => row.document).filter((item): item is FirestoreDocument => Boolean(item));
}

function rankMember(document: FirestoreDocument) {
  const member = memberFromDocument(document);
  if (!member) return 0;
  return member.isAdmin ? 4 : member.isIbr ? 3 : member.isApproved ? 2 : 1;
}

async function findMember(projectId: string, accessToken: string, phone: string): Promise<Member | null> {
  const canonicalId = `phone_${phone}`;
  const canonical = await getMemberById(projectId, accessToken, canonicalId);
  const candidates: FirestoreDocument[] = canonical ? [canonical] : [];
  for (const variant of [phone, `55${phone}`]) {
    candidates.push(...await queryMembers(projectId, accessToken, "phone", { stringValue: variant }));
  }
  const unique = new Map<string, FirestoreDocument>();
  candidates.forEach((document) => {
    const id = String(document.name || "").split("/").pop() || "";
    if (id) unique.set(id, document);
  });
  const selected = [...unique.values()].sort((left, right) => rankMember(right) - rankMember(left))[0];
  return memberFromDocument(selected);
}

async function findAndroidAdmin(projectId: string, accessToken: string) {
  const byEmail = (await queryMembers(projectId, accessToken, "email", { stringValue: ADMIN_EMAIL })).map(memberFromDocument).find((item) => item?.isAdmin) || null;
  if (byEmail) return byEmail;
  return (await queryMembers(projectId, accessToken, "isAdmin", { booleanValue: true })).map(memberFromDocument).find((item) => item?.isAdmin) || null;
}

function field(value: unknown): FirestoreField {
  if (typeof value === "boolean") return { booleanValue: value };
  if (typeof value === "number") return { integerValue: String(Math.trunc(value)) };
  return { stringValue: String(value ?? "") };
}

async function createPendingMember(projectId: string, accessToken: string, fullName: string, phone: string): Promise<Member> {
  const id = `phone_${phone}`;
  const now = Date.now();
  const data = {
    name: shortMemberName(fullName),
    ibrCertificateName: fullName.trim(),
    phone,
    email: "",
    isApproved: false,
    isVip: false,
    isIbr: false,
    isAdmin: false,
    status: "pendente",
    type: "acesso",
    avatarId: "davi",
    equippedBadgeId: "caminhante",
    createdAt: now,
    updatedAt: now,
  };
  const response = await fetch(`${baseUrl(projectId)}/acessos_pendentes/${encodeURIComponent(id)}?currentDocument.exists=false`, {
    method: "PATCH",
    headers: { authorization: `Bearer ${accessToken}`, "content-type": "application/json" },
    body: JSON.stringify({ fields: Object.fromEntries(Object.entries(data).map(([key, value]) => [key, field(value)])) }),
  });
  if (!response.ok) {
    const existing = await findMember(projectId, accessToken, phone);
    if (existing) return existing;
    throw new Error("Não foi possível criar a solicitação de acesso.");
  }
  return { id, name: shortMemberName(fullName), isApproved: false, isIbr: false, isAdmin: false };
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
    const name = clean(input.name);
    const phone = normalizePhone(input.phone);
    const password = clean(input.password);
    const account = JSON.parse(Deno.env.get("FIREBASE_SERVICE_ACCOUNT_JSON") || "{}") as ServiceAccount;
    const projectId = account.project_id || Deno.env.get("FIREBASE_PROJECT_ID") || FIREBASE_PROJECT_ID;
    const accessToken = await googleAccessToken(account);
    const isAdministrator = name.toLowerCase() === "admin" && password.length > 0 && await sha256(password) === ADMIN_PASSWORD_SHA256;

    let member: Member;
    if (isAdministrator) {
      const androidAdmin = await findAndroidAdmin(projectId, accessToken);
      member = androidAdmin
        ? { ...androidAdmin, name: androidAdmin.name || "Administrador", isApproved: true, isAdmin: true, isIbr: true }
        : { id: "admin", name: "Administrador", isApproved: true, isIbr: true, isAdmin: true };
    } else {
      if (!name || phone.length < 10 || phone.length > 11) return json({ error: "Preencha seu nome completo e um telefone válido com DDD." }, 400);
      const existing = await findMember(projectId, accessToken, phone);
      if (!existing) {
        member = await createPendingMember(projectId, accessToken, name, phone);
        if (!member.isApproved) return json({ ok: true, pending: true, requested: true, member });
      } else {
        member = existing;
        if (!member.isApproved && !member.isAdmin) return json({ ok: true, pending: true, requested: false, member });
      }
    }

    const token = await customFirebaseToken(account, member.id, {
      isAdmin: member.isAdmin,
      isIbr: member.isIbr,
      memberId: member.id,
    });
    return json({ ok: true, token, pending: false, requested: false, member });
  } catch (error) {
    console.error("pwa-auth failed", error instanceof Error ? error.message : "unknown");
    return json({ error: error instanceof Error ? error.message : "Não foi possível iniciar sua sessão agora." }, 500);
  }
});
