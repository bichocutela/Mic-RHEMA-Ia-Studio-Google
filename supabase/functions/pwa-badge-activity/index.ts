import { createRemoteJWKSet, errors as joseErrors, importPKCS8, jwtVerify, SignJWT } from "npm:jose@5.10.0";

const FIREBASE_PROJECT_ID = "mic-rhema";
const FIREBASE_ISSUER = `https://securetoken.google.com/${FIREBASE_PROJECT_ID}`;
const FIREBASE_JWKS = createRemoteJWKSet(new URL("https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com"));
const GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
const ALLOWED_ACTIVITIES = new Set(["plans", "plan_themes", "books", "videos", "bible_chapters", "bible_news", "devotionals", "audios", "active_minutes"]);
const cors = {
  "access-control-allow-origin": "*",
  "access-control-allow-headers": "authorization, content-type",
  "access-control-allow-methods": "POST, OPTIONS",
  "content-type": "application/json; charset=utf-8",
  "cache-control": "no-store",
};

type ServiceAccount = { project_id?: string; client_email?: string; private_key?: string };
class HttpError extends Error { constructor(public status: number, message: string) { super(message); } }

function json(body: Record<string, unknown>, status = 200) { return new Response(JSON.stringify(body), { status, headers: cors }); }
function bearerToken(request: Request) {
  const match = (request.headers.get("authorization") || "").match(/^Bearer\s+(.+)$/i);
  if (!match) throw new HttpError(401, "Sessão ausente.");
  return match[1];
}
async function verifiedMemberId(token: string) {
  try {
    const result = await jwtVerify(token, FIREBASE_JWKS, { issuer: FIREBASE_ISSUER, audience: FIREBASE_PROJECT_ID, algorithms: ["RS256"] });
    if (!result.payload.sub) throw new HttpError(401, "Sessão inválida.");
    return String(result.payload.sub);
  } catch (error) {
    if (error instanceof HttpError) throw error;
    if (error instanceof joseErrors.JWTExpired) throw new HttpError(401, "Sessão expirada.");
    throw new HttpError(401, "Sessão inválida.");
  }
}
async function firestoreAccessToken(account: ServiceAccount) {
  if (!account.client_email || !account.private_key) throw new HttpError(500, "Credencial Firebase incompleta.");
  const now = Math.floor(Date.now() / 1000);
  const key = await importPKCS8(account.private_key.replace(/\\n/g, "\n"), "RS256");
  const assertion = await new SignJWT({ iss: account.client_email, scope: "https://www.googleapis.com/auth/datastore", aud: GOOGLE_TOKEN_URL })
    .setProtectedHeader({ alg: "RS256", typ: "JWT" }).setIssuedAt(now).setExpirationTime(now + 3600).sign(key);
  const response = await fetch(GOOGLE_TOKEN_URL, {
    method: "POST",
    headers: { "content-type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth2:grant-type:jwt-bearer", assertion }),
  });
  const payload = await response.json();
  if (!response.ok || !payload.access_token) throw new HttpError(500, "Falha ao sincronizar progresso.");
  return String(payload.access_token);
}
function decodeValue(value: any): any {
  if (!value) return undefined;
  if ("stringValue" in value) return value.stringValue;
  if ("booleanValue" in value) return value.booleanValue;
  if ("integerValue" in value) return Number(value.integerValue);
  if ("doubleValue" in value) return Number(value.doubleValue);
  if ("arrayValue" in value) return (value.arrayValue?.values || []).map(decodeValue);
  if ("mapValue" in value) return Object.fromEntries(Object.entries(value.mapValue?.fields || {}).map(([key, entry]) => [key, decodeValue(entry)]));
  return undefined;
}
function encodeValue(value: any): any {
  if (typeof value === "boolean") return { booleanValue: value };
  if (typeof value === "number") return Number.isInteger(value) ? { integerValue: String(value) } : { doubleValue: value };
  if (Array.isArray(value)) return { arrayValue: { values: value.map(encodeValue) } };
  if (value && typeof value === "object") return { mapValue: { fields: Object.fromEntries(Object.entries(value).map(([key, entry]) => [key, encodeValue(entry)])) } };
  return { stringValue: String(value ?? "") };
}
function firestoreBase(projectId: string) { return `https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents`; }
async function readDocument(projectId: string, accessToken: string, path: string) {
  const response = await fetch(`${firestoreBase(projectId)}/${path}`, { headers: { authorization: `Bearer ${accessToken}` } });
  if (response.status === 404) return null;
  if (!response.ok) throw new HttpError(502, "Não foi possível ler o progresso.");
  return await response.json() as any;
}
async function listDocuments(projectId: string, accessToken: string, path: string) {
  const response = await fetch(`${firestoreBase(projectId)}/${path}?pageSize=500`, { headers: { authorization: `Bearer ${accessToken}` } });
  if (!response.ok) return [];
  const payload = await response.json() as any;
  return payload.documents || [];
}
async function patchDocument(projectId: string, accessToken: string, path: string, data: Record<string, unknown>) {
  const mask = Object.keys(data).map((key) => `updateMask.fieldPaths=${encodeURIComponent(key)}`).join("&");
  const response = await fetch(`${firestoreBase(projectId)}/${path}?${mask}`, {
    method: "PATCH",
    headers: { authorization: `Bearer ${accessToken}`, "content-type": "application/json" },
    body: JSON.stringify({ fields: Object.fromEntries(Object.entries(data).map(([key, value]) => [key, encodeValue(value)])) }),
  });
  if (!response.ok) throw new HttpError(502, "Não foi possível salvar o progresso.");
}
function fields(document: any) { return Object.fromEntries(Object.entries(document?.fields || {}).map(([key, value]) => [key, decodeValue(value)])) as Record<string, any>; }
function uniqueCount(map: Record<string, string[]>, key: string) { return new Set((map[key] || []).map(String)).size; }
function calculateBadges(existing: string[], activities: Record<string, string[]>, completedCourses: number) {
  const unlocked = new Set(existing.length ? existing : ["caminhante"]);
  const counts = {
    plans: uniqueCount(activities, "plans"), plan_themes: uniqueCount(activities, "plan_themes"), books: uniqueCount(activities, "books"),
    videos: uniqueCount(activities, "videos"), bible_chapters: uniqueCount(activities, "bible_chapters"), bible_news: uniqueCount(activities, "bible_news"),
    devotionals: uniqueCount(activities, "devotionals"), audios: uniqueCount(activities, "audios"),
  };
  const activeMinutes = uniqueCount(activities, "active_minutes");
  if (counts.devotionals >= 3 && counts.plan_themes >= 1) unlocked.add("semeador");
  if (counts.plans >= 1 && counts.plan_themes >= 3 && counts.bible_chapters >= 3) unlocked.add("discipulo");
  if (activeMinutes >= 60 && Object.values(counts).reduce((sum, value) => sum + value, 0) >= 10) unlocked.add("perseverante");
  if (counts.books >= 3 && counts.videos >= 3 && counts.audios >= 2) unlocked.add("estudante_rhema");
  if (completedCourses >= 1 && counts.bible_news >= 3 && counts.bible_chapters >= 10) unlocked.add("mestre_da_palavra");
  if (unlocked.has("mestre_da_palavra") && Object.values(counts).every((value) => value >= 1) && activeMinutes >= 180) unlocked.add("guardiao_da_fe");
  return [...unlocked];
}
async function countCompletedCourses(projectId: string, accessToken: string, memberId: string) {
  const courses = (await listDocuments(projectId, accessToken, "ibr_courses")).map(fields);
  const progress = (await listDocuments(projectId, accessToken, `users/${encodeURIComponent(memberId)}/ibrProgress`)).map(fields);
  return courses.filter((course: any) => Array.isArray(course.chapters) && course.chapters.length > 0 && course.chapters.every((chapter: any) => progress.some((entry: any) => String(entry.courseId) === String(course.id || course._id || "") && String(entry.chapterId) === String(chapter.id) && entry.isCompleted === true))).length;
}

Deno.serve(async (request: Request) => {
  if (request.method === "OPTIONS") return new Response("ok", { headers: cors });
  if (request.method !== "POST") return json({ error: "Método não permitido." }, 405);
  try {
    const memberId = await verifiedMemberId(bearerToken(request));
    const input = await request.json() as { activity?: unknown; itemId?: unknown; reconcile?: unknown };
    const reconcileOnly = input.reconcile === true;
    const activity = String(input.activity || "").trim();
    const itemId = String(input.itemId || "").trim().slice(0, 180);
    if (!reconcileOnly && (!ALLOWED_ACTIVITIES.has(activity) || !itemId)) return json({ error: "Atividade inválida." }, 400);
    const account = JSON.parse(Deno.env.get("FIREBASE_SERVICE_ACCOUNT_JSON") || "{}") as ServiceAccount;
    const projectId = account.project_id || FIREBASE_PROJECT_ID;
    const accessToken = await firestoreAccessToken(account);
    const memberPath = `acessos_pendentes/${encodeURIComponent(memberId)}`;
    const member = await readDocument(projectId, accessToken, memberPath);
    if (!member) throw new HttpError(404, "Perfil não encontrado.");
    const data = fields(member);
    if (data.isApproved !== true && data.isAdmin !== true) throw new HttpError(403, "Perfil ainda não aprovado.");
    const activities = (data.badgeActivityIds && typeof data.badgeActivityIds === "object" ? data.badgeActivityIds : {}) as Record<string, string[]>;
    if (!reconcileOnly) {
      const current = Array.isArray(activities[activity]) ? activities[activity].map(String) : [];
      if (!current.includes(itemId)) activities[activity] = [...current, itemId].slice(-6000);
    }
    const previousBadges = Array.isArray(data.unlockedBadgeIds) ? data.unlockedBadgeIds.map(String) : [];
    const completedCourses = await countCompletedCourses(projectId, accessToken, memberId);
    const unlockedBadgeIds = calculateBadges(previousBadges, activities, completedCourses);
    const update = { badgeActivityIds: activities, unlockedBadgeIds, updatedAt: Date.now() };
    await patchDocument(projectId, accessToken, memberPath, update);
    await patchDocument(projectId, accessToken, `users/${encodeURIComponent(memberId)}`, update).catch(() => undefined);
    return json({ ok: true, unlockedBadgeIds, newlyUnlocked: unlockedBadgeIds.filter((id) => !previousBadges.includes(id)) });
  } catch (error) {
    if (error instanceof HttpError) return json({ error: error.message }, error.status);
    console.error("pwa-badge-activity", error);
    return json({ error: "Não foi possível registrar o progresso agora." }, 500);
  }
});
