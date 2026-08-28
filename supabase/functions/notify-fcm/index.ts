import { importPKCS8, SignJWT } from "npm:jose@5.10.0";

const FCM_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
const FCM_TOKEN_URL = "https://oauth2.googleapis.com/token";
const DEFAULT_PROJECT_ID = "mic-rhema";
const MAX_TEXT_LENGTH = 180;

type NotificationRequest = {
  topic?: string;
  title?: string;
  body?: string;
  data?: Record<string, string | number | boolean>;
};

type ServiceAccount = {
  project_id?: string;
  client_email?: string;
  private_key?: string;
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

function cleanText(value: unknown, fallback: string): string {
  const text = String(value ?? fallback).trim();
  return text.slice(0, MAX_TEXT_LENGTH) || fallback;
}

async function accessToken(account: ServiceAccount): Promise<string> {
  if (!account.client_email || !account.private_key) {
    throw new Error("FIREBASE_SERVICE_ACCOUNT_JSON sem client_email ou private_key.");
  }
  const now = Math.floor(Date.now() / 1000);
  const assertion = await new SignJWT({
    iss: account.client_email,
    scope: FCM_SCOPE,
    aud: FCM_TOKEN_URL,
  })
    .setProtectedHeader({ alg: "RS256", typ: "JWT" })
    .setIssuedAt(now)
    .setExpirationTime(now + 3600)
    .sign(await importPKCS8(account.private_key.replace(/\\n/g, "\n"), "RS256"));

  const response = await fetch(FCM_TOKEN_URL, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion,
    }),
  });
  if (!response.ok) throw new Error(`Falha ao autenticar no FCM: ${response.status}`);
  const payload = await response.json();
  if (!payload.access_token) throw new Error("O Google não retornou um access_token.");
  return payload.access_token;
}

Deno.serve(async (request) => {
  if (request.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (request.method !== "POST") return json({ error: "Método não permitido." }, 405);

  try {
    const configuredKey = Deno.env.get("SUPABASE_ANON_KEY");
    const providedKey = request.headers.get("apikey");
    if (configuredKey && providedKey && providedKey !== configuredKey) {
      return json({ error: "Chave pública do Supabase inválida." }, 401);
    }

    const input = await request.json() as NotificationRequest;
    const topic = cleanText(input.topic, "all_users").replace(/[^a-zA-Z0-9_.-]/g, "");
    const title = cleanText(input.title, "Nova atualização disponível");
    const body = cleanText(input.body, "Confira as novidades no MIC Rhema.");
    const account = JSON.parse(Deno.env.get("FIREBASE_SERVICE_ACCOUNT_JSON") ?? "{}") as ServiceAccount;
    const projectId = account.project_id || Deno.env.get("FIREBASE_PROJECT_ID") || DEFAULT_PROJECT_ID;
    const token = await accessToken(account);
    const data = Object.fromEntries(
      Object.entries(input.data ?? {}).map(([key, value]) => [key, String(value)]),
    );

    data.title = data.title || title;
    data.body = data.body || body;
    data.category = data.category || "content_updates";

    // Data-only + prioridade alta: garante que o FCMService processe a mensagem
    // também em segundo plano e aplique as preferências individuais do usuário.
    const response = await fetch(`https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json; UTF-8",
      },
      body: JSON.stringify({
        message: {
          topic,
          data,
          android: {
            priority: "high",
            ttl: "86400s",
          },
        },
      }),
    });
    const responseBody = await response.text();
    if (!response.ok) {
      console.error("FCM rejected notification", response.status, responseBody);
      return json({ error: "FCM rejeitou a notificação.", details: responseBody.slice(0, 500) }, 502);
    }
    return json({ ok: true, topic, message: responseBody });
  } catch (error) {
    console.error("notify-fcm failed", error);
    return json({ error: error instanceof Error ? error.message : "Erro ao enviar notificação." }, 500);
  }
});
