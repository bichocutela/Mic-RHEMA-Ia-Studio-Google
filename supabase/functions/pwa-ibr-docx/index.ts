import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";
import { createRemoteJWKSet, jwtVerify } from "npm:jose@5.10.0";

const FIREBASE_PROJECT_ID = "mic-rhema";
const FIREBASE_ISSUER = `https://securetoken.google.com/${FIREBASE_PROJECT_ID}`;
const FIREBASE_JWKS_URL = "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com";
const DOCX_MIME = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
const MAX_BYTES = 50 * 1024 * 1024;
const cors = {
  "access-control-allow-origin": "*",
  "access-control-allow-headers": "authorization, content-type",
  "access-control-allow-methods": "POST, OPTIONS",
  "content-type": "application/json; charset=utf-8",
  "cache-control": "no-store",
};
const jwks = createRemoteJWKSet(new URL(FIREBASE_JWKS_URL));

function json(body: Record<string, unknown>, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: cors });
}

async function requireAdmin(request: Request) {
  const token = request.headers.get("authorization")?.replace(/^Bearer\s+/i, "").trim();
  if (!token) throw new Error("Acesso administrativo obrigatório.");
  const verified = await jwtVerify(token, jwks, {
    issuer: FIREBASE_ISSUER,
    audience: FIREBASE_PROJECT_ID,
    algorithms: ["RS256"],
  });
  if (verified.payload.isAdmin !== true) throw new Error("Acesso administrativo obrigatório.");
  return String(verified.payload.sub || "admin").replace(/[^a-zA-Z0-9_-]/g, "_").slice(0, 128) || "admin";
}

Deno.serve(async (request: Request) => {
  if (request.method === "OPTIONS") return json({ ok: true });
  if (request.method !== "POST") return json({ error: "Método não permitido." }, 405);
  try {
    const adminUid = await requireAdmin(request);
    const form = await request.formData();
    const file = form.get("file");
    if (!(file instanceof File)) return json({ error: "Arquivo não informado." }, 400);
    const mime = String(file.type || "").toLowerCase();
    if (mime !== DOCX_MIME || !file.name.toLowerCase().endsWith(".docx")) {
      return json({ error: "Selecione um arquivo Word no formato .docx." }, 415);
    }
    if (file.size <= 0 || file.size > MAX_BYTES) return json({ error: "O arquivo DOCX excede o limite de 50 MB." }, 413);

    const supabaseUrl = Deno.env.get("SUPABASE_URL");
    const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
    if (!supabaseUrl || !serviceRoleKey) return json({ error: "Storage do servidor não configurado." }, 500);
    const supabase = createClient(supabaseUrl, serviceRoleKey, { auth: { autoRefreshToken: false, persistSession: false } });
    const objectPath = `${adminUid}/ibr-docx-${crypto.randomUUID()}.docx`;
    const upload = await supabase.storage.from("media-assets").upload(objectPath, file, {
      contentType: DOCX_MIME,
      cacheControl: "900",
      upsert: false,
    });
    if (upload.error) throw upload.error;
    const url = `${supabaseUrl}/storage/v1/object/public/media-assets/${objectPath}`;
    return json({ ok: true, signed_url: url, storage_path: `media-assets/${objectPath}` });
  } catch (error) {
    console.error("pwa-ibr-docx failed", error instanceof Error ? error.message : "unknown");
    return json({ error: error instanceof Error ? error.message : "Não foi possível enviar o DOCX." }, 403);
  }
});
