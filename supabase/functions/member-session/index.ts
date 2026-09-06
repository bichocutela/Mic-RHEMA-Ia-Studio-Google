import { importPKCS8, SignJWT } from "npm:jose@5.10.0";

const FIRESTORE_SCOPE = "https://www.googleapis.com/auth/datastore";
const TOKEN_URL = "https://oauth2.googleapis.com/token";
const CUSTOM_TOKEN_AUD = "https://identitytoolkit.googleapis.com/google.identity.identitytoolkit.v1.IdentityToolkit";
const DEFAULT_PROJECT_ID = "mic-rhema";
const CURRENT_PUBLISHABLE_KEY = "sb_publishable_Dv98hBnbJB2TzRCG6aJNwA_KMPHLZSw";
const LEVEL_BADGES = [
  "caminhante", "semeador", "discipulo", "perseverante", "estudante_rhema", "mestre_da_palavra", "guardiao_da_fe",
  "semente_da_fe", "caminho_da_promessa", "escudo_da_fe", "aguas_vivas", "videira_verdadeira", "luz_do_mundo",
  "armadura_de_deus", "leao_de_juda", "chama_do_espirito", "coroa_da_vida", "asas_da_promessa", "tabernaculo",
  "arca_da_alianca", "nova_jerusalem", "gloria_eterna",
];

type ServiceAccount = { project_id?: string; client_email?: string; private_key?: string };
type FirestoreDocument = { name?: string; fields?: Record<string, FirestoreValue> };
type FirestoreValue = {
  stringValue?: string; booleanValue?: boolean; integerValue?: string; doubleValue?: number;
  timestampValue?: string; arrayValue?: { values?: FirestoreValue[] };
  mapValue?: { fields?: Record<string, FirestoreValue> }; nullValue?: null;
};

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-client-info",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Content-Type": "application/json",
};

function json(body: Record<string, unknown>, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: corsHeaders });
}
function normalizePhone(value: unknown) {
  const digits = String(value ?? "").replace(/\D/g, "");
  return digits.length >= 12 && digits.length <= 13 && digits.startsWith("55") ? digits.slice(2) : digits;
}
function memberIdFromDocument(document: FirestoreDocument) {
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
  if (value.mapValue !== undefined) return Object.fromEntries(Object.entries(value.mapValue.fields ?? {}).map(([k, v]) => [k, fromValue(v)]));
  return null;
}
function documentData(document?: FirestoreDocument | null): Record<string, unknown> {
  if (!document) return {};
  return Object.fromEntries(Object.entries(document.fields ?? {}).map(([key, value]) => [key, fromValue(value)]));
}
function toValue(value: unknown): FirestoreValue {
  if (value === null || value === undefined) return { nullValue: null };
  if (typeof value === "boolean") return { booleanValue: value };
  if (typeof value === "number") return Number.isInteger(value) ? { integerValue: String(value) } : { doubleValue: value };
  if (typeof value === "string") return { stringValue: value };
  if (Array.isArray(value)) return { arrayValue: { values: value.map(toValue) } };
  if (typeof value === "object") return { mapValue: { fields: Object.fromEntries(Object.entries(value as Record<string, unknown>).map(([k, v]) => [k, toValue(v)])) } };
  return { stringValue: String(value) };
}
function fields(data: Record<string, unknown>) {
  return Object.fromEntries(Object.entries(data).filter(([, value]) => value !== undefined).map(([key, value]) => [key, toValue(value)]));
}
function stringList(value: unknown): string[] {
  return Array.isArray(value) ? value.map(String).filter(Boolean) : [];
}
function activityMap(value: unknown): Record<string, string[]> {
  if (!value || typeof value !== "object" || Array.isArray(value)) return {};
  return Object.fromEntries(Object.entries(value as Record<string, unknown>).map(([key, item]) => [key, stringList(item)]));
}
function unionActivitySources(...sources: unknown[]): Record<string, string[]> {
  const merged: Record<string, string[]> = {};
  for (const source of sources) {
    for (const [key, items] of Object.entries(activityMap(source))) {
      merged[key] = [...new Set([...(merged[key] ?? []), ...items])];
    }
  }
  return merged;
}
function unionStringLists(...sources: unknown[]) {
  return [...new Set(sources.flatMap((source) => stringList(source)))];
}
function firstNonBlank(...values: unknown[]) {
  return values.map((value) => String(value ?? "").trim()).find(Boolean) ?? "";
}

async function googleAccessToken(account: ServiceAccount) {
  if (!account.client_email || !account.private_key) throw new Error("Conta de serviço Firebase incompleta.");
  const now = Math.floor(Date.now() / 1000);
  const assertion = await new SignJWT({ iss: account.client_email, scope: FIRESTORE_SCOPE, aud: TOKEN_URL })
    .setProtectedHeader({ alg: "RS256", typ: "JWT" }).setIssuedAt(now).setExpirationTime(now + 3600)
    .sign(await importPKCS8(account.private_key.replace(/\\n/g, "\n"), "RS256"));
  const response = await fetch(TOKEN_URL, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", assertion }),
  });
  const payload = await response.json();
  if (!response.ok || !payload.access_token) throw new Error("Falha ao autenticar no Google.");
  return String(payload.access_token);
}
async function firebaseCustomToken(account: ServiceAccount, uid: string) {
  if (!account.client_email || !account.private_key) throw new Error("Conta de serviço Firebase incompleta.");
  const now = Math.floor(Date.now() / 1000);
  return await new SignJWT({ uid })
    .setProtectedHeader({ alg: "RS256", typ: "JWT" })
    .setIssuer(account.client_email).setSubject(account.client_email).setAudience(CUSTOM_TOKEN_AUD)
    .setIssuedAt(now).setExpirationTime(now + 3600)
    .sign(await importPKCS8(account.private_key.replace(/\\n/g, "\n"), "RS256"));
}
function baseUrl(projectId: string) {
  return `https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents`;
}
async function getDocument(projectId: string, token: string, collection: string, id: string): Promise<FirestoreDocument | null> {
  const response = await fetch(`${baseUrl(projectId)}/${collection}/${encodeURIComponent(id)}`, { headers: { Authorization: `Bearer ${token}` } });
  if (response.status === 404) return null;
  if (!response.ok) throw new Error(`Falha ao ler ${collection}: ${response.status}`);
  return await response.json() as FirestoreDocument;
}
async function queryByPhone(projectId: string, token: string, phone: string): Promise<FirestoreDocument[]> {
  const response = await fetch(`${baseUrl(projectId)}:runQuery`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
    body: JSON.stringify({ structuredQuery: { from: [{ collectionId: "acessos_pendentes" }], where: { fieldFilter: { field: { fieldPath: "phone" }, op: "EQUAL", value: { stringValue: phone } } }, limit: 20 } }),
  });
  if (!response.ok) throw new Error(`Falha ao localizar membro: ${response.status}`);
  const rows = await response.json() as Array<{ document?: FirestoreDocument }>;
  return rows.map((row) => row.document).filter((doc): doc is FirestoreDocument => Boolean(doc));
}
async function findMembers(projectId: string, token: string, phone: string) {
  const candidates: FirestoreDocument[] = [];
  const exact = await getDocument(projectId, token, "acessos_pendentes", `phone_${phone}`);
  if (exact) candidates.push(exact);
  for (const variant of [phone, `55${phone}`]) candidates.push(...await queryByPhone(projectId, token, variant));
  const unique = new Map<string, FirestoreDocument>();
  candidates.forEach((doc) => unique.set(memberIdFromDocument(doc), doc));
  return [...unique.values()].filter((doc) => normalizePhone(documentData(doc).phone) === phone);
}
function rankMember(document: FirestoreDocument) {
  const data = documentData(document);
  const privilege = data.isAdmin === true ? 4 : data.isIbr === true ? 3 : data.isApproved === true ? 2 : 1;
  const updated = Number(data.updatedAt ?? data.createdAt ?? 0) || 0;
  return privilege * 10_000_000_000_000 + updated;
}
async function patchDocument(projectId: string, token: string, collection: string, id: string, data: Record<string, unknown>) {
  const keys = Object.keys(data).filter((key) => data[key] !== undefined);
  const masks = keys.map((key) => `updateMask.fieldPaths=${encodeURIComponent(key)}`).join("&");
  const response = await fetch(`${baseUrl(projectId)}/${collection}/${encodeURIComponent(id)}?${masks}`, {
    method: "PATCH",
    headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
    body: JSON.stringify({ fields: fields(data) }),
  });
  if (!response.ok) throw new Error(`Falha ao sincronizar ${collection}: ${response.status} ${(await response.text()).slice(0, 160)}`);
}
async function deleteDocument(projectId: string, token: string, collection: string, id: string) {
  const response = await fetch(`${baseUrl(projectId)}/${collection}/${encodeURIComponent(id)}`, { method: "DELETE", headers: { Authorization: `Bearer ${token}` } });
  if (!response.ok && response.status !== 404) throw new Error(`Falha ao remover duplicado em ${collection}: ${response.status}`);
}

async function consolidateMatches(projectId: string, token: string, phone: string, matches: FirestoreDocument[]) {
  const sorted = [...matches].sort((a, b) => rankMember(b) - rankMember(a));
  const selected = sorted[0];
  const memberId = memberIdFromDocument(selected);
  const accessRows = sorted.map(documentData);
  const userDocs = await Promise.all(sorted.map((doc) => getDocument(projectId, token, "users", memberIdFromDocument(doc))));
  const userRows = userDocs.map(documentData);
  const selectedAccess = accessRows[0] ?? {};
  const selectedUser = userRows[0] ?? {};
  const allRows = [...accessRows, ...userRows];
  const unlockedBadgeIds = unionStringLists(...allRows.map((row) => row.unlockedBadgeIds));
  if (!unlockedBadgeIds.includes("caminhante")) unlockedBadgeIds.unshift("caminhante");
  const badgeActivityIds = unionActivitySources(...allRows.map((row) => row.badgeActivityIds));
  const highestLevel = [...LEVEL_BADGES].reverse().find((badge) => unlockedBadgeIds.includes(badge));
  const createdCandidates = allRows.map((row) => Number(row.createdAt ?? 0)).filter((value) => Number.isFinite(value) && value > 0);
  const approved = allRows.some((row) => row.isApproved === true || row.isAdmin === true);
  const merged: Record<string, unknown> = {
    phone,
    name: firstNonBlank(selectedUser.name, selectedAccess.name, ...allRows.map((row) => row.name)),
    email: firstNonBlank(selectedUser.email, selectedAccess.email, ...allRows.map((row) => row.email)),
    address: firstNonBlank(selectedUser.address, selectedAccess.address, ...allRows.map((row) => row.address)),
    birthDate: firstNonBlank(selectedUser.birthDate, selectedAccess.birthDate, ...allRows.map((row) => row.birthDate)),
    avatarId: firstNonBlank(selectedUser.avatarId, selectedAccess.avatarId, ...allRows.map((row) => row.avatarId), "davi"),
    equippedBadgeId: highestLevel ?? firstNonBlank(selectedUser.equippedBadgeId, selectedAccess.equippedBadgeId, "caminhante"),
    unlockedBadgeIds,
    badgeActivityIds,
    profilePhotoUrl: firstNonBlank(selectedUser.profilePhotoUrl, selectedAccess.profilePhotoUrl, ...allRows.map((row) => row.profilePhotoUrl)),
    supabaseStoragePath: firstNonBlank(selectedUser.supabaseStoragePath, selectedAccess.supabaseStoragePath, ...allRows.map((row) => row.supabaseStoragePath)),
    ibrCertificateName: firstNonBlank(selectedUser.ibrCertificateName, selectedAccess.ibrCertificateName, ...allRows.map((row) => row.ibrCertificateName)),
    ibrCertificateUrl: firstNonBlank(selectedUser.ibrCertificateUrl, selectedAccess.ibrCertificateUrl, ...allRows.map((row) => row.ibrCertificateUrl)),
    ibrCertificateStoragePath: firstNonBlank(selectedUser.ibrCertificateStoragePath, selectedAccess.ibrCertificateStoragePath, ...allRows.map((row) => row.ibrCertificateStoragePath)),
    isAdmin: allRows.some((row) => row.isAdmin === true),
    isIbr: allRows.some((row) => row.isIbr === true),
    isApproved: approved,
    isVip: allRows.some((row) => row.isVip === true),
    status: approved ? "aprovado" : "pendente",
    type: firstNonBlank(selectedAccess.type, "acesso"),
    firebaseUid: memberId,
    createdAt: createdCandidates.length ? Math.min(...createdCandidates) : Date.now(),
    updatedAt: Date.now(),
  };
  await patchDocument(projectId, token, "acessos_pendentes", memberId, merged);
  await patchDocument(projectId, token, "users", memberId, merged);
  const duplicateIds = sorted.slice(1).map(memberIdFromDocument);
  for (const duplicateId of duplicateIds) {
    await deleteDocument(projectId, token, "acessos_pendentes", duplicateId);
    await deleteDocument(projectId, token, "users", duplicateId);
  }
  return { memberId, merged, duplicateIds };
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
    if (!validLegacy && !validPublishable) return json({ error: "Cliente não autorizado." }, 401);

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
      const beforeDuplicateCount = Math.max(0, matches.length - 1);
      const consolidated = await consolidateMatches(projectId, token, phone, matches);
      const customToken = await firebaseCustomToken(account, consolidated.memberId);
      return json({
        ok: true,
        found: true,
        memberId: consolidated.memberId,
        customToken,
        member: { ...consolidated.merged, id: consolidated.memberId, phone },
        duplicateCount: beforeDuplicateCount,
        duplicatesRemoved: consolidated.duplicateIds,
      });
    }

    if (action === "sync_avatar") {
      const memberId = String(input.memberId ?? "").trim();
      const identityPhone = normalizePhone(input.identityPhone ?? phone);
      const avatarId = String(input.avatarId ?? "").trim();
      if (!memberId) return json({ error: "Membro inválido." }, 400);
      if (!/^[a-z0-9_:-]{1,180}$/i.test(avatarId)) return json({ error: "Avatar inválido." }, 400);
      const current = await getDocument(projectId, token, "acessos_pendentes", memberId);
      if (!current) return json({ error: "Cadastro não encontrado." }, 404);
      if (normalizePhone(documentData(current).phone) !== identityPhone) {
        return json({ error: "Identidade do membro não confere." }, 403);
      }
      if (avatarId.startsWith("profile_photo::") && !avatarId.startsWith(`profile_photo::${memberId}::`)) {
        return json({ error: "A foto selecionada não pertence a este membro." }, 403);
      }
      const update = { avatarId, updatedAt: Date.now() };
      await patchDocument(projectId, token, "acessos_pendentes", memberId, update);
      await patchDocument(projectId, token, "users", memberId, update);
      return json({ ok: true, memberId, avatarId });
    }

    if (action === "sync_state") {
      const memberId = String(input.memberId ?? "").trim();
      const identityPhone = normalizePhone(input.identityPhone ?? phone);
      if (!memberId) return json({ error: "Membro inválido." }, 400);
      const current = await getDocument(projectId, token, "acessos_pendentes", memberId);
      if (!current) return json({ error: "Cadastro não encontrado." }, 404);
      const currentData = documentData(current);
      if (normalizePhone(currentData.phone) !== identityPhone) return json({ error: "Identidade do membro não confere." }, 403);
      const userDocument = await getDocument(projectId, token, "users", memberId);
      const userData = documentData(userDocument);
      // O telefone identifica a conta e só pode ser alterado pelo painel ADM.
      // APKs antigos ainda enviam esse campo no salvamento geral; ignoramos o valor
      // recebido e preservamos o telefone confirmado na sessão.
      const newPhone = identityPhone;
      const profileSafe = {
        phone: newPhone,
        name: String(input.name ?? currentData.name ?? userData.name ?? "").trim(),
        email: String(input.email ?? currentData.email ?? userData.email ?? "").trim(),
        address: String(input.address ?? currentData.address ?? userData.address ?? "").trim(),
        birthDate: String(input.birthDate ?? currentData.birthDate ?? userData.birthDate ?? "").trim(),
        avatarId: String(input.avatarId ?? currentData.avatarId ?? userData.avatarId ?? "").trim(),
        equippedBadgeId: String(input.equippedBadgeId ?? currentData.equippedBadgeId ?? userData.equippedBadgeId ?? "").trim(),
        profilePhotoUrl: String(input.profilePhotoUrl ?? currentData.profilePhotoUrl ?? userData.profilePhotoUrl ?? "").trim(),
        supabaseStoragePath: String(input.supabaseStoragePath ?? currentData.supabaseStoragePath ?? userData.supabaseStoragePath ?? "").trim(),
        updatedAt: Date.now(),
      };
      // Perfis de versões antigas reenviam também todo o histórico ao trocar o avatar.
      // Gravamos primeiro os campos pequenos para que uma atividade legada inválida
      // nunca faça "Minha foto" desaparecer nem bloqueie nome/endereço.
      await patchDocument(projectId, token, "acessos_pendentes", memberId, profileSafe);
      await patchDocument(projectId, token, "users", memberId, profileSafe);

      const progressSafe = {
        unlockedBadgeIds: unionStringLists(currentData.unlockedBadgeIds, userData.unlockedBadgeIds, input.unlockedBadgeIds),
        badgeActivityIds: unionActivitySources(currentData.badgeActivityIds, userData.badgeActivityIds, input.badgeActivityIds),
      };
      try {
        await patchDocument(projectId, token, "acessos_pendentes", memberId, progressSafe);
        await patchDocument(projectId, token, "users", memberId, progressSafe);
      } catch (progressError) {
        console.error("member-session legacy progress sync skipped", progressError);
      }
      return json({ ok: true, memberId, phone: newPhone });
    }

    return json({ error: "Ação inválida." }, 400);
  } catch (error) {
    console.error("member-session failed", error);
    return json({ error: error instanceof Error ? error.message : "Erro ao recuperar a conta." }, 500);
  }
});
