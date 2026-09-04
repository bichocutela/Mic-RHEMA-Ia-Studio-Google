import "jsr:@supabase/functions-js/edge-runtime.d.ts";

const SUPABASE_MEDIA_BASE = "https://cwphbkdtorfpgmnlafqb.supabase.co/storage/v1/object/public/media-assets/";
const ALLOWED_HOSTS = new Set([
  "cwphbkdtorfpgmnlafqb.supabase.co",
  "drive.google.com",
  "docs.google.com",
  "drive.usercontent.google.com",
  "firebasestorage.googleapis.com",
]);

const cors = {
  "access-control-allow-origin": "*",
  "access-control-allow-headers": "range, content-type",
  "access-control-expose-headers": "accept-ranges, content-length, content-range, content-type",
  "access-control-allow-methods": "GET, HEAD, OPTIONS",
};

function driveFileId(raw: string) {
  try {
    const url = new URL(raw);
    const host = url.hostname.replace(/^www\./, "").toLowerCase();
    if (!host.includes("drive.google.com") && host !== "docs.google.com" && host !== "drive.usercontent.google.com") return "";
    return url.pathname.match(/\/d\/([A-Za-z0-9_-]+)/)?.[1] || url.searchParams.get("id") || "";
  } catch { return ""; }
}

function normalizeSource(rawValue: string) {
  const raw = rawValue.trim();
  if (!raw) throw new Error("Arquivo não informado.");
  if (raw.startsWith("media-assets/")) return `${SUPABASE_MEDIA_BASE}${raw.slice("media-assets/".length)}`;

  const gs = raw.match(/^gs:\/\/([^/]+)\/(.+)$/i);
  if (gs) {
    return `https://firebasestorage.googleapis.com/v0/b/${encodeURIComponent(gs[1])}/o/${encodeURIComponent(gs[2])}?alt=media`;
  }

  const driveId = driveFileId(raw);
  if (driveId) return `https://drive.usercontent.google.com/download?id=${encodeURIComponent(driveId)}&export=download&confirm=t`;

  const url = new URL(raw);
  const host = url.hostname.replace(/^www\./, "").toLowerCase();
  if (!ALLOWED_HOSTS.has(host)) throw new Error("Origem de mídia não permitida.");

  const markers = [
    "/storage/v1/object/sign/media-assets/",
    "/storage/v1/object/authenticated/media-assets/",
    "/storage/v1/object/public/media-assets/",
  ];
  const marker = markers.find((candidate) => url.pathname.includes(candidate));
  if (marker) {
    const objectPath = url.pathname.slice(url.pathname.indexOf(marker) + marker.length);
    return `${url.origin}/storage/v1/object/public/media-assets/${objectPath}`;
  }
  return url.toString();
}

Deno.serve(async (request: Request) => {
  if (request.method === "OPTIONS") return new Response(null, { status: 204, headers: cors });
  if (request.method !== "GET" && request.method !== "HEAD") {
    return new Response("Método não permitido.", { status: 405, headers: { ...cors, "content-type": "text/plain; charset=utf-8" } });
  }

  try {
    const incoming = new URL(request.url);
    const source = normalizeSource(incoming.searchParams.get("url") || "");
    const headers = new Headers({
      "accept": "*/*",
      "user-agent": "MIC-Rhema-PWA/1.0",
    });
    const range = request.headers.get("range");
    if (range) headers.set("range", range);

    const upstream = await fetch(source, {
      method: request.method,
      headers,
      redirect: "follow",
    });

    if (!upstream.ok && upstream.status !== 206) {
      const text = await upstream.text().catch(() => "");
      console.error("pwa-media-stream upstream", upstream.status, text.slice(0, 160));
      return new Response("Não foi possível carregar este áudio.", { status: 502, headers: { ...cors, "content-type": "text/plain; charset=utf-8" } });
    }

    const responseHeaders = new Headers(cors);
    for (const name of ["content-type", "content-length", "content-range", "accept-ranges", "etag", "last-modified"]) {
      const value = upstream.headers.get(name);
      if (value) responseHeaders.set(name, value);
    }
    if (!responseHeaders.has("content-type")) responseHeaders.set("content-type", "audio/mpeg");
    if (!responseHeaders.has("accept-ranges")) responseHeaders.set("accept-ranges", "bytes");
    responseHeaders.set("cache-control", "public, max-age=3600");

    return new Response(request.method === "HEAD" ? null : upstream.body, {
      status: upstream.status,
      headers: responseHeaders,
    });
  } catch (error) {
    console.error("pwa-media-stream", error instanceof Error ? error.message : "unknown");
    return new Response("Arquivo inválido ou indisponível.", { status: 400, headers: { ...cors, "content-type": "text/plain; charset=utf-8" } });
  }
});
