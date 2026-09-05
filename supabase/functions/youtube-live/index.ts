import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { importPKCS8, SignJWT } from "npm:jose@5.10.0";

const GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
const DATASTORE_SCOPE = "https://www.googleapis.com/auth/datastore";
const DEFAULT_HANDLE = "@micrhemaoficial";
const DEFAULT_TITLE = "Estamos ao vivo";
const CACHE_WINDOW_MS = 45_000;
const MAX_RECENT_UPLOADS = 12;

const cors = {
  "access-control-allow-origin": "*",
  "access-control-allow-headers": "authorization, apikey, content-type, x-client-info",
  "access-control-allow-methods": "GET, POST, OPTIONS",
  "content-type": "application/json; charset=utf-8",
  "cache-control": "no-store",
};

type ServiceAccount = { project_id?: string; client_email?: string; private_key?: string };
type FireField = { stringValue?: string; booleanValue?: boolean; integerValue?: string; timestampValue?: string };
type FireDocument = { fields?: Record<string, FireField> };
type LiveConfig = {
  autoEnabled: boolean;
  manualEnabled: boolean;
  youtubeHandle: string;
  manualUrl: string;
  manualTitle: string;
  channelId: string;
};

type YoutubeChannel = { id?: string; snippet?: { title?: string }; contentDetails?: { relatedPlaylists?: { uploads?: string } } };
type YoutubePlaylistItem = { contentDetails?: { videoId?: string } };
type YoutubeVideo = {
  id?: string;
  snippet?: { title?: string; liveBroadcastContent?: "live" | "upcoming" | "none"; thumbnails?: Record<string, { url?: string }> };
  liveStreamingDetails?: { actualStartTime?: string; actualEndTime?: string; scheduledStartTime?: string };
};

function json(body: Record<string, unknown>, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: cors });
}
function bool(field: FireField | undefined, fallback: boolean) {
  return typeof field?.booleanValue === "boolean" ? field.booleanValue : fallback;
}
function text(field: FireField | undefined, fallback = "") {
  return String(field?.stringValue ?? fallback).trim();
}
function timestamp(field: FireField | undefined) {
  const value = field?.timestampValue ? Date.parse(field.timestampValue) : 0;
  return Number.isFinite(value) ? value : 0;
}
function cleanHandle(value: string) {
  const trimmed = value.trim();
  if (!trimmed) return DEFAULT_HANDLE;
  const match = trimmed.match(/youtube\.com\/(?:@([^/?#]+)|channel\/([^/?#]+))/i);
  if (match?.[1]) return `@${match[1]}`;
  if (match?.[2]) return match[2];
  return trimmed.startsWith("@") ? trimmed : `@${trimmed.replace(/^@/, "")}`;
}
function youtubeVideoId(url: string) {
  const value = url.trim();
  if (!value) return "";
  const patterns = [/[?&]v=([A-Za-z0-9_-]{6,})/, /youtu\.be\/([A-Za-z0-9_-]{6,})/, /youtube\.com\/(?:live|shorts|embed)\/([A-Za-z0-9_-]{6,})/];
  for (const pattern of patterns) {
    const match = value.match(pattern);
    if (match?.[1]) return match[1];
  }
  return /^[A-Za-z0-9_-]{6,}$/.test(value) ? value : "";
}
function thumb(video?: YoutubeVideo) {
  const t = video?.snippet?.thumbnails || {};
  return t.maxres?.url || t.standard?.url || t.high?.url || t.medium?.url || t.default?.url || "";
}
function fireString(value: string): FireField { return { stringValue: value }; }
function fireBool(value: boolean): FireField { return { booleanValue: value }; }
function fireTimestamp(value = new Date().toISOString()): FireField { return { timestampValue: value }; }

async function googleAccessToken(account: ServiceAccount) {
  if (!account.client_email || !account.private_key) throw new Error("FIREBASE_SERVICE_ACCOUNT_JSON incompleto.");
  const now = Math.floor(Date.now() / 1000);
  const key = await importPKCS8(account.private_key.replace(/\\n/g, "\n"), "RS256");
  const assertion = await new SignJWT({ iss: account.client_email, scope: DATASTORE_SCOPE, aud: GOOGLE_TOKEN_URL })
    .setProtectedHeader({ alg: "RS256", typ: "JWT" })
    .setIssuedAt(now)
    .setExpirationTime(now + 3600)
    .sign(key);
  const response = await fetch(GOOGLE_TOKEN_URL, {
    method: "POST",
    headers: { "content-type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", assertion }),
  });
  const payload = await response.json();
  if (!response.ok || !payload.access_token) throw new Error("Não foi possível autenticar no Firestore.");
  return payload.access_token as string;
}

async function youtube<T>(path: string, key: string): Promise<T> {
  const separator = path.includes("?") ? "&" : "?";
  const response = await fetch(`https://www.googleapis.com/youtube/v3/${path}${separator}key=${encodeURIComponent(key)}`, { cache: "no-store" });
  const payload = await response.json();
  if (!response.ok) {
    const detail = payload?.error?.message || `YouTube API ${response.status}`;
    throw new Error(detail);
  }
  return payload as T;
}

async function firestoreGet(projectId: string, token: string) {
  const url = `https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents/settings/live_stream`;
  const response = await fetch(url, { headers: { authorization: `Bearer ${token}` }, cache: "no-store" });
  if (response.status === 404) return null;
  if (!response.ok) throw new Error(`Falha ao ler configuração da live: ${response.status}`);
  return await response.json() as FireDocument;
}

async function firestoreWrite(projectId: string, token: string, fields: Record<string, FireField>) {
  const url = `https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents/settings/live_stream`;
  const response = await fetch(url, {
    method: "PATCH",
    headers: { authorization: `Bearer ${token}`, "content-type": "application/json" },
    body: JSON.stringify({ fields }),
  });
  if (!response.ok) throw new Error(`Falha ao atualizar status da live: ${response.status}`);
  return await response.json();
}

function configFrom(doc: FireDocument | null): LiveConfig {
  const fields = doc?.fields || {};
  return {
    autoEnabled: bool(fields.autoEnabled, true),
    manualEnabled: bool(fields.manualEnabled, false),
    youtubeHandle: cleanHandle(text(fields.youtubeHandle, DEFAULT_HANDLE)),
    manualUrl: text(fields.manualUrl),
    manualTitle: text(fields.manualTitle, DEFAULT_TITLE),
    channelId: text(fields.channelId),
  };
}

async function resolveChannel(config: LiveConfig, apiKey: string) {
  if (config.channelId && config.youtubeHandle === config.channelId) {
    const payload = await youtube<{ items?: YoutubeChannel[] }>(`channels?part=snippet,contentDetails&id=${encodeURIComponent(config.channelId)}`, apiKey);
    return payload.items?.[0] || null;
  }
  if (/^UC[A-Za-z0-9_-]+$/.test(config.youtubeHandle)) {
    const payload = await youtube<{ items?: YoutubeChannel[] }>(`channels?part=snippet,contentDetails&id=${encodeURIComponent(config.youtubeHandle)}`, apiKey);
    return payload.items?.[0] || null;
  }
  const handle = config.youtubeHandle.replace(/^@/, "");
  const payload = await youtube<{ items?: YoutubeChannel[] }>(`channels?part=snippet,contentDetails&forHandle=${encodeURIComponent(handle)}`, apiKey);
  return payload.items?.[0] || null;
}

async function findLiveVideo(channel: YoutubeChannel, apiKey: string) {
  const playlist = channel.contentDetails?.relatedPlaylists?.uploads || "";
  if (!playlist) return { live: null as YoutubeVideo | null, upcoming: null as YoutubeVideo | null };
  const list = await youtube<{ items?: YoutubePlaylistItem[] }>(`playlistItems?part=contentDetails&maxResults=${MAX_RECENT_UPLOADS}&playlistId=${encodeURIComponent(playlist)}`, apiKey);
  const ids = (list.items || []).map((item) => item.contentDetails?.videoId || "").filter(Boolean);
  if (!ids.length) return { live: null as YoutubeVideo | null, upcoming: null as YoutubeVideo | null };
  const videos = await youtube<{ items?: YoutubeVideo[] }>(`videos?part=snippet,liveStreamingDetails&id=${encodeURIComponent(ids.join(","))}`, apiKey);
  const items = videos.items || [];
  const live = items.find((video) => video.snippet?.liveBroadcastContent === "live" || Boolean(video.liveStreamingDetails?.actualStartTime && !video.liveStreamingDetails?.actualEndTime)) || null;
  const upcoming = items
    .filter((video) => video.snippet?.liveBroadcastContent === "upcoming" || Boolean(video.liveStreamingDetails?.scheduledStartTime && !video.liveStreamingDetails?.actualStartTime))
    .sort((a, b) => Date.parse(a.liveStreamingDetails?.scheduledStartTime || "9999-12-31") - Date.parse(b.liveStreamingDetails?.scheduledStartTime || "9999-12-31"))[0] || null;
  return { live, upcoming };
}

Deno.serve(async (request) => {
  if (request.method === "OPTIONS") return new Response("ok", { headers: cors });
  if (!["GET", "POST"].includes(request.method)) return json({ error: "Método não permitido." }, 405);
  try {
    const apiKey = Deno.env.get("YOUTUBE_API_KEY")?.trim() || "";
    if (!apiKey) return json({ error: "YOUTUBE_API_KEY não configurada." }, 500);
    const account = JSON.parse(Deno.env.get("FIREBASE_SERVICE_ACCOUNT_JSON") || "{}") as ServiceAccount;
    const projectId = account.project_id || Deno.env.get("FIREBASE_PROJECT_ID") || "mic-rhema";
    const accessToken = await googleAccessToken(account);
    const current = await firestoreGet(projectId, accessToken);
    const fields = current?.fields || {};
    const config = configFrom(current);
    const force = request.method === "POST" || new URL(request.url).searchParams.get("force") === "1";
    const lastChecked = timestamp(fields.checkedAt);
    if (!force && lastChecked > 0 && Date.now() - lastChecked < CACHE_WINDOW_MS) {
      return json({
        ok: true,
        cached: true,
        isLive: bool(fields.isLive, false),
        source: text(fields.source, "none"),
        url: text(fields.url),
        title: text(fields.title),
        videoId: text(fields.videoId),
        checkedAt: fields.checkedAt?.timestampValue || "",
      });
    }

    let channel: YoutubeChannel | null = null;
    let live: YoutubeVideo | null = null;
    let upcoming: YoutubeVideo | null = null;
    let autoError = "";
    if (config.autoEnabled) {
      try {
        channel = await resolveChannel(config, apiKey);
        if (!channel?.id) throw new Error(`Canal ${config.youtubeHandle} não encontrado.`);
        const found = await findLiveVideo(channel, apiKey);
        live = found.live;
        upcoming = found.upcoming;
      } catch (error) {
        autoError = error instanceof Error ? error.message : "Falha ao consultar o YouTube.";
      }
    }

    const manualVideoId = youtubeVideoId(config.manualUrl);
    const manualActive = config.manualEnabled && Boolean(config.manualUrl);
    const autoActive = config.autoEnabled && Boolean(live?.id);
    const isLive = manualActive || autoActive;
    const source = manualActive ? "manual" : autoActive ? "automatic" : "none";
    const effectiveVideo = manualActive ? null : live;
    const videoId = manualActive ? manualVideoId : effectiveVideo?.id || "";
    const url = manualActive ? config.manualUrl : videoId ? `https://www.youtube.com/watch?v=${videoId}` : "";
    const title = manualActive ? config.manualTitle || DEFAULT_TITLE : effectiveVideo?.snippet?.title || DEFAULT_TITLE;
    const thumbnailUrl = manualActive
      ? (videoId ? `https://i.ytimg.com/vi/${videoId}/hqdefault.jpg` : "")
      : thumb(effectiveVideo || undefined);
    const startedAt = manualActive ? "" : effectiveVideo?.liveStreamingDetails?.actualStartTime || "";
    const channelId = channel?.id || config.channelId;

    const output: Record<string, FireField> = {
      autoEnabled: fireBool(config.autoEnabled),
      manualEnabled: fireBool(config.manualEnabled),
      youtubeHandle: fireString(config.youtubeHandle),
      manualUrl: fireString(config.manualUrl),
      manualTitle: fireString(config.manualTitle),
      channelId: fireString(channelId),
      isLive: fireBool(isLive),
      source: fireString(source),
      videoId: fireString(videoId),
      url: fireString(url),
      title: fireString(title),
      thumbnailUrl: fireString(thumbnailUrl),
      startedAt: fireString(startedAt),
      checkedAt: fireTimestamp(),
      autoError: fireString(autoError),
      upcomingVideoId: fireString(upcoming?.id || ""),
      upcomingTitle: fireString(upcoming?.snippet?.title || ""),
      upcomingThumbnailUrl: fireString(thumb(upcoming || undefined)),
      upcomingScheduledAt: fireString(upcoming?.liveStreamingDetails?.scheduledStartTime || ""),
    };
    await firestoreWrite(projectId, accessToken, output);

    return json({
      ok: true,
      cached: false,
      isLive,
      source,
      url,
      title,
      videoId,
      thumbnailUrl,
      startedAt,
      channelId,
      youtubeHandle: config.youtubeHandle,
      upcoming: upcoming?.id ? {
        videoId: upcoming.id,
        title: upcoming.snippet?.title || "",
        scheduledAt: upcoming.liveStreamingDetails?.scheduledStartTime || "",
      } : null,
      autoError: autoError || undefined,
    });
  } catch (error) {
    console.error("youtube-live failed", error instanceof Error ? error.message : error);
    return json({ error: error instanceof Error ? error.message : "Falha ao verificar transmissão." }, 500);
  }
});
