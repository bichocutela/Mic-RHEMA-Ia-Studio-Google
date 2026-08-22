import { importPKCS8, SignJWT } from "npm:jose@5.10.0";

const GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
const DATASTORE_SCOPE = "https://www.googleapis.com/auth/datastore";
const cors = {
  "access-control-allow-origin": "*",
  "access-control-allow-headers": "authorization, apikey, content-type, x-client-info",
  "access-control-allow-methods": "POST, OPTIONS",
  "content-type": "application/json; charset=utf-8",
  "cache-control": "no-store",
};

type ServiceAccount = { project_id?: string; client_email?: string; private_key?: string };

function json(body: Record<string, unknown>, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: cors });
}

async function googleAccessToken(account: ServiceAccount) {
  if (!account.client_email || !account.private_key) throw new Error("Credencial Firebase incompleta.");
  const now = Math.floor(Date.now() / 1000);
  const privateKey = await importPKCS8(account.private_key.replace(/\\n/g, "\n"), "RS256");
  const assertion = await new SignJWT({ iss: account.client_email, scope: DATASTORE_SCOPE, aud: GOOGLE_TOKEN_URL })
    .setProtectedHeader({ alg: "RS256", typ: "JWT" })
    .setIssuedAt(now)
    .setExpirationTime(now + 3600)
    .sign(privateKey);
  const response = await fetch(GOOGLE_TOKEN_URL, {
    method: "POST",
    headers: { "content-type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", assertion }),
  });
  const payload = await response.json();
  if (!response.ok || !payload.access_token) throw new Error("Não foi possível registrar notificações agora.");
  return payload.access_token as string;
}

async function tokenId(token: string) {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(token));
  return [...new Uint8Array(digest)].map((byte) => byte.toString(16).padStart(2, "0")).join("");
}

Deno.serve(async (request) => {
  if (request.method === "OPTIONS") return new Response("ok", { headers: cors });
  if (request.method !== "POST") return json({ error: "Método não permitido." }, 405);
  try {
    const input = await request.json() as { token?: unknown; platform?: unknown };
    const token = String(input.token || "").trim();
    if (token.length < 80 || token.length > 4096) return json({ error: "Token de notificação inválido." }, 400);

    const account = JSON.parse(Deno.env.get("FIREBASE_SERVICE_ACCOUNT_JSON") || "{}") as ServiceAccount;
    const projectId = account.project_id || Deno.env.get("FIREBASE_PROJECT_ID") || "mic-rhema";
    const documentId = await tokenId(token);
    const response = await fetch(`https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents/pwa_push_tokens/${documentId}`, {
      method: "PATCH",
      headers: { authorization: `Bearer ${await googleAccessToken(account)}`, "content-type": "application/json" },
      body: JSON.stringify({
        fields: {
          token: { stringValue: token },
          platform: { stringValue: String(input.platform || "web").slice(0, 32) },
          source: { stringValue: "pwa" },
          updatedAt: { timestampValue: new Date().toISOString() },
        },
      }),
    });
    if (!response.ok) throw new Error("Não foi possível salvar a inscrição de avisos.");
    return json({ ok: true });
  } catch (error) {
    console.error("pwa-push-subscribe failed", error instanceof Error ? error.message : "unknown");
    return json({ error: "Não foi possível ativar notificações agora." }, 500);
  }
});
