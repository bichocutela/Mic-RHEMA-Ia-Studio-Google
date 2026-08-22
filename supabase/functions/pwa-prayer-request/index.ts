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

function clean(value: unknown) {
  return String(value ?? "").trim();
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
  if (!response.ok || !payload.access_token) throw new Error("Não foi possível autenticar o envio do pedido.");
  return payload.access_token as string;
}

Deno.serve(async (request) => {
  if (request.method === "OPTIONS") return new Response("ok", { headers: cors });
  if (request.method !== "POST") return json({ error: "Método não permitido." }, 405);
  try {
    const input = await request.json() as { name?: unknown; request?: unknown };
    const name = clean(input.name);
    const prayer = clean(input.request);
    if (!name || !prayer) return json({ error: "Preencha seu nome e o pedido de oração." }, 400);
    if (name.length > 120 || prayer.length > 4000) return json({ error: "Seu pedido ultrapassa o tamanho permitido." }, 400);

    const account = JSON.parse(Deno.env.get("FIREBASE_SERVICE_ACCOUNT_JSON") || "{}") as ServiceAccount;
    const projectId = account.project_id || Deno.env.get("FIREBASE_PROJECT_ID") || "mic-rhema";
    const id = crypto.randomUUID();
    const response = await fetch(`https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents/prayer_requests/${id}`, {
      method: "PATCH",
      headers: { authorization: `Bearer ${await googleAccessToken(account)}`, "content-type": "application/json" },
      body: JSON.stringify({
        fields: {
          id: { stringValue: id },
          name: { stringValue: name },
          request: { stringValue: prayer },
          date: { stringValue: "Hoje" },
          createdAt: { integerValue: String(Date.now()) },
          createdAtServer: { timestampValue: new Date().toISOString() },
          source: { stringValue: "pwa-public" },
        },
      }),
    });
    if (!response.ok) throw new Error("Não foi possível salvar o pedido.");
    return json({ ok: true });
  } catch (error) {
    console.error("pwa-prayer-request failed", error instanceof Error ? error.message : "unknown");
    return json({ error: "Não foi possível enviar o pedido agora. Tente novamente." }, 500);
  }
});
