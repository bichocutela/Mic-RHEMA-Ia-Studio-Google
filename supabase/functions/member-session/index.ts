import { importPKCS8, SignJWT } from "npm:jose@5.10.0";

const FIRESTORE_SCOPE = "https://www.googleapis.com/auth/datastore";
const TOKEN_URL = "https://oauth2.googleapis.com/token";
const CUSTOM_TOKEN_AUD = "https://identitytoolkit.googleapis.com/google.identity.identitytoolkit.v1.IdentityToolkit";
const DEFAULT_PROJECT_ID = "mic-rhema";
// Chave publicável atual usada pelo APK. Ela é pública por definição; a validação
// exata evita aceitar qualquer texto com prefixo sb_publishable_.
const CURRENT_PUBLISHABLE_KEY = "sb_publishable_Dv98hBnbJB2TzRCG6aJNwA_KMPHLZSw";

type ServiceAccount = {
  project_id?: string;
  client_email?: string;
  private_key?: string;
};

type FirestoreDocument = {
  name?: string;
  fields?: Record<string, FirestoreValue>;
};

type FirestoreValue = {
  stringValue?: string;
  booleanValue?: boolean;
  integerValue?: string;
  doubleValue?: number;
  timestampValue?: string;
  arrayValue?: { values?: FirestoreValue[] };
  mapValue?: { fields?: Record<string, FirestoreValue> };
  nullValue?: null;
};

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-client-info",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Content-Type": "application/json",
};

function json(body: Record<string, unknown>, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: corsHeaders });
}

function normalizePhone(value: unknown): string {
  const digits = String(value ?? "").replace(/\D/g, "");
  return digits.length >= 12 && digits.length <= 13 && digits.startsWith("55") ? digits.slice(2) : digits;
}

function memberIdFromDocument(document: FirestoreDocument): string {
  return (document.name ?? "").split("/").pop() ?? "";
}

function fromValue(value?: FirestoreValue): unknown {
  if (!value) return null;
  if (value.stringValue !== undefined) return value.stringValue;
  if (value.booleanValue !== undefined) return value.booleanValue;
  if (value.integerValue !== undefined) return Number(value.integerValue);
  if (value.doubleValue !== undefined) return value.doubleValue;
  if (value.timestampValue !== undefined) return value.timestampValue;
  if (value.arrayValue !== undefined) return (value.arrayValue.values ?? []).map(fromValue);
  if (value.mapValue !== undefined) return Object.fromEntries(
    Object.entries(value.mapValue.fields ?? {}).map(([key, item]) => [key, fromValue(item)]),
  );
  return null;
}

function documentData(document?: FirestoreDocument | null): Record<string, unknown> {
  if (!document) return {};
  return Object.fromEntries(Object.entries(document.fields ?? {}).map(([key, value]) => [key, fromValue(value)]));
}

function toValue(value: unknown): FirestoreValue {
  if (value === null || value === undefined) return { nullValue: null };
  if (typeof value === "boolean") return { booleanValue: value };
  if (typeof value === "number") return Number.isInteger(value)
    ? { integerValue: String(value) }
    : { doubleValue: value };
  if (typeof value === "string") return { stringValue: value };
  if (Array.isArray(value)) return { arrayValue: { values: value.map(toValue) } };
  if (typeof value === "object") return {
    mapValue: {
      fields: Object.fromEntries(Object.entries(value as Record<string, unknown>).map(([key, item]) => [key, toValue(item)])),
    },
  };
  return { stringValue: String(value) };
}

function fields(data: Record<string, unknown>): Record<string, FirestoreValue> {
  return Object.fromEntries(Object.entries(data).map(([key, value]) => [key, toValue(value)]));
}

async function googleAccessToken(account: ServiceAccount): Promise<string> {
  if (!account.client_email || !account.private_key) throw new Error("Conta de serviço Firebase incompleta.");
  const now = Math.floor(Date.now() / 1000);
  const assertion = await new SignJWT({
    iss: account.client_email,
    scope: FIRESTORE_SCOPE,
    aud: TOKEN_URL,
  })
    .setProtectedHeader({ alg: "RS256", typ: "JWT" })
    .setIssuedAt(now)
    .setExpirationTime(now + 3600)
    .sign(await importPKCS8(account.private_key.replace(/\\n/g, "\n"), "RS256"));

  const response = await fetch(TOKEN_URL, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion,
    }),
  });
  if (!response.ok) throw new Error(`Falha ao autenticar no Google: ${response.status}`);
  const payload = await response.json();
  if (!payload.access_token) throw new Error("Google não retornou access_token.");
  return payload.access_token;
}

async function firebaseCustomToken(account: ServiceAccount, uid: string): Promise<string> {
  if (!account.client_email || !account.private_key) throw new Error("Conta de serviço Firebase incompleta.");
  const now = Math.floor(Date.now() / 1000);
  return await new SignJWT({ uid })
    .setProtectedHeader({ alg: "RS256", typ: "JWT" })
    .setIssuer(account.client_email)
    .setSubject(account.client_email)
    .setAudience(CUSTOM_TOKEN_AUD)
    .setIssuedAt(now)
    .setExpirationTime(now + 3600)
    .sign(await importPKCS8(account.private_key.replace(/\\n/g, "\n"), "RS256"));
}

function baseUrl(projectId: string): string {
  return `https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents`;
}

async function getDocument(projectId: string, token: string, collection: string, id: string): Promise<FirestoreDocument | null> {
  const response = await fetch(`${baseUrl(projectId)}/${collection}/${encodeURIComponent(id)}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (response.status === 404) return null;
  if (!response.ok) throw new Error(`Falha ao ler ${collection}: ${response.status}`);
  return await response.json() as FirestoreDocument;
}

async function queryByPhone(projectId: string, token: string, phone: string): Promise<FirestoreDocument[]> {
  const response = await fetch(`${baseUrl(projectId)}:runQuery`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
    body: JSON.stringify({
      structuredQuery: {
        from: [{ collectionId: "acessos_pendentes" }],
        where: {
          fieldFilter: {
            field: { fieldPath: "phone" },
            op: "EQUAL",
            value: { stringValue: phone },
          },
        },
        limit: 20,
      },
    }),
  });
  if (!response.ok) throw new Error(`Falha ao localizar membro: ${response.status}`);
  const rows = await response.json() as Array<{ document?: FirestoreDocument }>;
  return rows.map((row) => row.document).filter((doc): doc is FirestoreDocument => Boolean(doc));
}

async function findMembers(projectId: string, token: string, phone: string): Promise<FirestoreDocument[]> {
  const canonicalId = `phone_${phone}`;
  const exact = await getDocument(projectId, token, "acessos_pendentes", canonicalId);
  const candidates = [exact].filter((doc): doc is FirestoreDocument => Boolean(doc));
  const variants = [phone, `55${phone}`];
  for (const variant of variants) candidates.push(...await queryByPhone(projectId, token, variant));
  const unique = new Map<string, FirestoreDocument>();
  candidates.forEach((doc) => unique.set(memberIdFromDocument(doc), doc));
  return [...unique.values()].filter((doc) => normalizePhone(documentData(doc).phone) === phone);
}

function rankMember(document: FirestoreDocument): number {
  const data = documentData(document);
  const privilege = data.isAdmin === true ? 4 : data.isIbr === true ? 3 : data.isApproved === true ? 2 : 1;
  const updated = Number(data.updatedAt ?? data.createdAt ?? 0);
  return privilege * 10_000_000_000_000 + updated;
}

async function patchDocument(
  projectId: string,
  token: string,
  collection: string,
  id: string,
  data: Record<string, unknown>,
): Promise<void> {
  const masks = Object.keys(data).map((key) => `updateMask.fieldPaths=${encodeURIComponent(key)}`).join("&");
  const response = await fetch(`${baseUrl(projectId)}/${collection}/${encodeURIComponent(id)}?${masks}`, {
    method: "PATCH",
    headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
    body: JSON.stringify({ fields: fields(data) }),
  });
  if (!response.ok) {
    const details = await response.text();
    throw new Error(`Falha ao sincronizar ${collection}: ${response.status} ${details.slice(0, 180)}`);
  }
}

function stringList(value: unknown): string[] {
  return Array.isArray(value) ? value.map(String).filter(Boolean) : [];
}

function activityMap(value: unknown): Record<string, string[]> {
  if (!value || typeof value !== "object" || Array.isArray(value)) return {};
  return Object.fromEntries(
    Object.entries(value as Record<string, unknown>).map(([key, item]) => [key, stringList(item)]),
  );
}

function unionActivities(a: unknown, b: unknown): Record<string, string[]> {
  const first = activityMap(a);
  const second = activityMap(b);
  const keys = new Set([...Object.keys(first), ...Object.keys(second)]);
  return Object.fromEntries([...keys].map((key) => [key, [...new Set([...(first[key] ?? []), ...(second[key] ?? [])])]]));
}

function mergeMember(accessData: Record<string, unknown>, userData: Record<string, unknown>): Record<string, unknown> {
  const badgeActivityIds = unionActivities(accessData.badgeActivityIds, userData.badgeActivityIds);
  const unlockedBadgeIds = [...new Set([
    ...stringList(accessData.unlockedBadgeIds),
    ...stringList(userData.unlockedBadgeIds),
  ])];
  return {
    ...accessData,
    name: String(userData.name ?? accessData.name ?? ""),
    email: String(userData.email ?? accessData.email ?? ""),
    address: String(userData.address ?? accessData.address ?? ""),
    birthDate: String(userData.birthDate ?? accessData.birthDate ?? ""),
    avatarId: String(userData.avatarId ?? accessData.avatarId ?? ""),
    equippedBadgeId: String(userData.equippedBadgeId ?? accessData.equippedBadgeId ?? ""),
    unlockedBadgeIds,
    badgeActivityIds,
    profilePhotoUrl: String(userData.profilePhotoUrl ?? accessData.profilePhotoUrl ?? ""),
    supabaseStoragePath: String(userData.supabaseStoragePath ?? accessData.supabaseStoragePath ?? ""),
  };
}

Deno.serve(async (request) => {
  if (request.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (request.method !== "POST") return json({ error: "Método não permitido." }, 405);

  try {
    const configuredLegacyKey = Deno.env.get("SUPABASE_ANON_KEY") ?? "";
    const providedKey = request.headers.get("apikey") ?? "";
    const authorization = request.headers.get("authorization") ?? "";
    const validLegacy = Boolean(configuredLegacyKey) && providedKey === configuredLegacyKey && authorization === `Bearer ${configuredLegacyKey}`;
    const validPublishable = providedKey === CURRENT_PUBLISHABLE_KEY && authorization === `Bearer ${CURRENT_PUBLISHABLE_KEY}`;
    if (!validLegacy && !validPublishable) {
      return json({ error: "Cliente não autorizado." }, 401);
    }

    const input = await request.json() as Record<string, unknown>;
    const action = String(input.action ?? "recover");
    const phone = normalizePhone(input.phone);
    if (phone.length < 10 || phone.length > 11) return json({ error: "Telefone inválido." }, 400);

    const account = JSON.parse(Deno.env.get("FIREBASE_SERVICE_ACCOUNT_JSON") ?? "{}") as ServiceAccount;
    const projectId = account.project_id || Deno.env.get("FIREBASE_PROJECT_ID") || DEFAULT_PROJECT_ID;
    const token = await googleAccessToken(account);
    const matches = await findMembers(projectId, token, phone);

    if (action === "recover") {
      if (matches.length === 0) return json({ ok: true, found: false });
      const selected = [...matches].sort((a, b) => rankMember(b) - rankMember(a))[0];
      const memberId = memberIdFromDocument(selected);
      const accessData = documentData(selected);
      const userDocument = await getDocument(projectId, token, "users", memberId);
      const merged = mergeMember(accessData, documentData(userDocument));
      const customToken = await firebaseCustomToken(account, memberId);
      return json({
        ok: true,
        found: true,
        memberId,
        customToken,
        member: { ...merged, id: memberId, phone: normalizePhone(merged.phone) },
        duplicateCount: Math.max(0, matches.length - 1),
      });
    }

    if (action === "sync_state") {
      const memberId = String(input.memberId ?? "").trim();
      const identityPhone = normalizePhone(input.identityPhone ?? phone);
      if (!memberId) return json({ error: "Membro inválido." }, 400);
      const current = await getDocument(projectId, token, "acessos_pendentes", memberId);
      if (!current) return json({ error: "Cadastro não encontrado." }, 404);
      const currentData = documentData(current);
      if (normalizePhone(currentData.phone) !== identityPhone) return json({ error: "Identidade do membro não confere." }, 403);

      const newPhone = normalizePhone(input.phone ?? identityPhone);
      if (newPhone !== identityPhone) {
        const collisions = await findMembers(projectId, token, newPhone);
        if (collisions.some((doc) => memberIdFromDocument(doc) !== memberId)) {
          return json({ error: "Este telefone já pertence a outro cadastro." }, 409);
        }
      }

      const safe = {
        phone: newPhone,
        name: String(input.name ?? currentData.name ?? "").trim(),
        email: String(input.email ?? currentData.email ?? "").trim(),
        address: String(input.address ?? currentData.address ?? "").trim(),
        birthDate: String(input.birthDate ?? currentData.birthDate ?? "").trim(),
        avatarId: String(input.avatarId ?? currentData.avatarId ?? "").trim(),
        equippedBadgeId: String(input.equippedBadgeId ?? currentData.equippedBadgeId ?? "").trim(),
        unlockedBadgeIds: stringList(input.unlockedBadgeIds ?? currentData.unlockedBadgeIds),
        badgeActivityIds: activityMap(input.badgeActivityIds ?? currentData.badgeActivityIds),
        profilePhotoUrl: String(input.profilePhotoUrl ?? currentData.profilePhotoUrl ?? "").trim(),
        supabaseStoragePath: String(input.supabaseStoragePath ?? currentData.supabaseStoragePath ?? "").trim(),
        updatedAt: Date.now(),
      };
      await patchDocument(projectId, token, "acessos_pendentes", memberId, safe);
      await patchDocument(projectId, token, "users", memberId, safe);
      return json({ ok: true, memberId, phone: newPhone });
    }

    return json({ error: "Ação inválida." }, 400);
  } catch (error) {
    console.error("member-session failed", error);
    return json({ error: error instanceof Error ? error.message : "Erro ao recuperar a conta." }, 500);
  }
});
