import { useEffect, useMemo, useState } from "react";
import { ExternalLink, Radio, X } from "lucide-react";
import { listenToDocument } from "@/lib/firebase";
import "./LiveStreamSurface.css";

export type LiveStreamSettings = {
  id?: string;
  autoEnabled?: boolean;
  manualEnabled?: boolean;
  youtubeHandle?: string;
  manualUrl?: string;
  manualTitle?: string;
  channelId?: string;
  isLive?: boolean;
  source?: "manual" | "automatic" | "none" | string;
  videoId?: string;
  url?: string;
  title?: string;
  thumbnailUrl?: string;
  startedAt?: string;
  checkedAt?: unknown;
  autoError?: string;
  upcomingVideoId?: string;
  upcomingTitle?: string;
  upcomingScheduledAt?: string;
};

const endpoint = "https://cwphbkdtorfpgmnlafqb.supabase.co/functions/v1/youtube-live";

export async function refreshLiveStream(force = false) {
  const response = await fetch(force ? `${endpoint}?force=1` : endpoint, {
    method: force ? "POST" : "GET",
    headers: { "content-type": "application/json" },
    cache: "no-store",
  });
  const payload = await response.json().catch(() => ({}));
  if (!response.ok || payload?.error) throw new Error(payload?.error || "Não foi possível verificar a transmissão.");
  return payload;
}

function youtubeVideoId(url?: string) {
  const value = String(url || "").trim();
  const patterns = [/[?&]v=([A-Za-z0-9_-]{6,})/, /youtu\.be\/([A-Za-z0-9_-]{6,})/, /youtube\.com\/(?:live|shorts|embed)\/([A-Za-z0-9_-]{6,})/];
  for (const pattern of patterns) {
    const match = value.match(pattern);
    if (match?.[1]) return match[1];
  }
  return "";
}

export function LiveStreamSurface({ visible = true }: { visible?: boolean }) {
  const [live, setLive] = useState<LiveStreamSettings | null>(null);
  const [open, setOpen] = useState(false);

  useEffect(() => listenToDocument<LiveStreamSettings>("settings", "live_stream", setLive, () => setLive(null)), []);
  useEffect(() => {
    if (!visible) return;
    void refreshLiveStream(false).catch(() => undefined);
    const onVisible = () => { if (document.visibilityState === "visible") void refreshLiveStream(false).catch(() => undefined); };
    document.addEventListener("visibilitychange", onVisible);
    return () => document.removeEventListener("visibilitychange", onVisible);
  }, [visible]);

  const videoId = useMemo(() => live?.videoId?.trim() || youtubeVideoId(live?.url), [live?.videoId, live?.url]);
  if (!visible || live?.isLive !== true || (!videoId && !live?.url)) return null;

  return <>
    <button className="live-stream-signal" onClick={() => setOpen(true)} aria-label="Abrir transmissão ao vivo">
      <span className="live-stream-dot" aria-hidden="true"/>
      <span className="live-stream-copy"><small>ESTAMOS AO VIVO</small><strong>{live.title?.trim() || "Transmissão MIC Rhema"}</strong></span>
      <span className="live-stream-watch"><Radio size={18}/> Assistir agora</span>
    </button>
    {open && <div className="live-stream-backdrop" onMouseDown={() => setOpen(false)}>
      <section className="live-stream-player" onMouseDown={(event) => event.stopPropagation()}>
        <header><div><small>● AO VIVO</small><h2>{live.title?.trim() || "Transmissão MIC Rhema"}</h2></div><button onClick={() => setOpen(false)} aria-label="Fechar"><X size={21}/></button></header>
        {videoId ? <iframe
          src={`https://www.youtube-nocookie.com/embed/${videoId}?autoplay=1&playsinline=1&rel=0`}
          title={live.title || "Transmissão ao vivo"}
          allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
          allowFullScreen
        /> : <div className="live-stream-embed-fallback">Esta transmissão não oferece player incorporado.</div>}
        <footer><span>{live.source === "manual" ? "Transmissão ativada manualmente" : "Transmissão detectada automaticamente no YouTube"}</span>{live.url && <a href={live.url} target="_blank" rel="noreferrer"><ExternalLink size={16}/> Abrir no YouTube</a>}</footer>
      </section>
    </div>}
  </>;
}
