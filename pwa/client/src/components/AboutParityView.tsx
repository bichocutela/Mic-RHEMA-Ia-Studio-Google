import { useEffect, useMemo, useState } from "react";
import { CircleUserRound, Copy, Info, Mail, MapPin, Phone, Smartphone, X } from "lucide-react";
import { listenToDocument } from "@/lib/firebase";

type AboutSettings = {
  pastorName?: string;
  pastorTitle?: string;
  missionTagline?: string;
  rhemaMeaning?: string;
};

type ShareTarget = {
  platform: "Android" | "iPhone";
  url: string;
  helper: string;
};

const PWA_URL = "https://bichocutela.github.io/Mic-RHEMA-Ia-Studio-Google/";
const LATEST_RELEASE_URL = "https://github.com/bichocutela/Mic-RHEMA-Ia-Studio-Google/releases/latest";
const LATEST_RELEASE_API = "https://api.github.com/repos/bichocutela/Mic-RHEMA-Ia-Studio-Google/releases/latest";

function AndroidIcon({ size = 24 }: { size?: number }) {
  return <svg width={size} height={size} viewBox="0 0 24 24" aria-hidden="true" fill="currentColor">
    <path d="M17.6 9.48 19.44 6.3c.16-.31.04-.69-.26-.85-.29-.15-.65-.06-.83.22l-1.88 3.24a11.43 11.43 0 0 0-8.94 0L5.65 5.67a.615.615 0 0 0-.83-.22c-.3.16-.42.54-.26.85L6.4 9.48C3.3 11.25 1.28 14.44 1 18h22c-.28-3.56-2.3-6.75-5.4-8.52ZM7 15.25A1.25 1.25 0 1 1 7 12.75a1.25 1.25 0 0 1 0 2.5Zm10 0a1.25 1.25 0 1 1 0-2.5 1.25 1.25 0 0 1 0 2.5Z"/>
  </svg>;
}

function qrImageUrl(value: string) {
  return `https://api.qrserver.com/v1/create-qr-code/?size=320x320&margin=12&data=${encodeURIComponent(value)}`;
}

export function AboutParityView() {
  const [about, setAbout] = useState<(AboutSettings & { id: string }) | null>(null);
  const [androidUrl, setAndroidUrl] = useState(LATEST_RELEASE_URL);
  const [shareTarget, setShareTarget] = useState<ShareTarget | null>(null);
  const [copied, setCopied] = useState(false);

  useEffect(() => listenToDocument<AboutSettings>("settings", "about", setAbout, () => setAbout(null)), []);

  useEffect(() => {
    const controller = new AbortController();
    fetch(LATEST_RELEASE_API, {
      signal: controller.signal,
      headers: { Accept: "application/vnd.github+json" },
    })
      .then(response => response.ok ? response.json() : Promise.reject(new Error("release unavailable")))
      .then((release: { assets?: Array<{ name?: string; browser_download_url?: string }> }) => {
        const apk = release.assets?.find(asset => asset.name?.toLowerCase().endsWith(".apk"));
        if (apk?.browser_download_url) setAndroidUrl(apk.browser_download_url);
      })
      .catch(() => undefined);
    return () => controller.abort();
  }, []);

  useEffect(() => {
    if (!shareTarget) setCopied(false);
  }, [shareTarget]);

  const pastorName = about?.pastorName?.trim() || "Pastor Evaldo Leôncio";
  const pastorTitle = about?.pastorTitle?.trim() || "Liderança";
  const mission = about?.missionTagline?.trim() || "Conectando Pessoas e Transformando Vidas.";
  const rhemaMeaning = about?.rhemaMeaning?.trim() || "Rhema é a palavra revelada de Deus para um momento específico.";
  const qrUrl = useMemo(() => shareTarget ? qrImageUrl(shareTarget.url) : "", [shareTarget]);

  const copyShareLink = async () => {
    if (!shareTarget) return;
    try {
      await navigator.clipboard.writeText(shareTarget.url);
      setCopied(true);
      window.setTimeout(() => setCopied(false), 1800);
    } catch {
      window.prompt("Copie o link:", shareTarget.url);
    }
  };

  return <>
    <section className="page-pad android-module">
      <div className="android-section-heading"><div><p>SOBRE</p><h2>MIC Rhema</h2></div></div>
      <div className="android-list-cards">
        <article className="android-module-card"><CircleUserRound size={29}/><div><strong>{pastorTitle}</strong><small>{pastorName}</small></div></article>
        <article className="android-module-card"><Info size={29}/><div><strong>Nossa Missão</strong><small>{mission}</small></div></article>
        <article className="android-module-card"><Info size={29}/><div><strong>O que significa Rhema</strong><small>{rhemaMeaning}</small></div></article>
        <article className="android-module-card"><MapPin size={29}/><div><strong>Localização</strong><small>Rua Todos os Santos – Natal/RN</small></div></article>
        <a className="android-module-card" href="tel:+5584988041804"><Phone size={29}/><div><strong>Telefone</strong><small>84 98804 1804</small></div></a>
        <a className="android-module-card" href="mailto:micrhema2@gmail.com"><Mail size={29}/><div><strong>E-mail</strong><small>micrhema2@gmail.com</small></div></a>
      </div>

      <section style={{ marginTop: 22 }} aria-label="Compartilhar Via">
        <div style={{ marginBottom: 12 }}>
          <strong style={{ display: "block", fontSize: "1rem" }}>Compartilhar Via</strong>
          <small style={{ display: "block", marginTop: 5, opacity: .72 }}>Use o QR Code para baixar no Android ou abrir no iPhone.</small>
        </div>
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10 }}>
          <button
            type="button"
            onClick={() => setShareTarget({ platform: "Android", url: androidUrl, helper: "Escaneie para baixar a APK mais recente do MIC Rhema." })}
            style={{ minHeight: 54, borderRadius: 18, border: "1px solid currentColor", background: "transparent", color: "inherit", display: "flex", alignItems: "center", justifyContent: "center", gap: 9, fontWeight: 800 }}
          >
            <AndroidIcon size={22}/>
            Android
          </button>
          <button
            type="button"
            onClick={() => setShareTarget({ platform: "iPhone", url: PWA_URL, helper: "Escaneie para abrir o MIC Rhema no iPhone e adicionar à Tela de Início." })}
            style={{ minHeight: 54, borderRadius: 18, border: "1px solid currentColor", background: "transparent", color: "inherit", display: "flex", alignItems: "center", justifyContent: "center", gap: 9, fontWeight: 800 }}
          >
            <Smartphone size={22}/>
            iPhone
          </button>
        </div>
      </section>
    </section>

    {shareTarget && <div
      role="dialog"
      aria-modal="true"
      aria-label={`QR Code ${shareTarget.platform}`}
      onClick={() => setShareTarget(null)}
      style={{ position: "fixed", inset: 0, zIndex: 1200, background: "rgba(0,0,0,.62)", display: "grid", placeItems: "center", padding: 20 }}
    >
      <div
        onClick={event => event.stopPropagation()}
        style={{ width: "min(92vw, 390px)", borderRadius: 24, background: "var(--card, #fffdf7)", color: "var(--foreground, #1d1b20)", padding: 20, boxShadow: "0 24px 70px rgba(0,0,0,.32)" }}
      >
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 12 }}>
          <div>
            <strong style={{ display: "block", fontSize: "1.15rem" }}>MIC Rhema no {shareTarget.platform}</strong>
            <small style={{ opacity: .72 }}>Compartilhar via QR Code</small>
          </div>
          <button type="button" aria-label="Fechar" onClick={() => setShareTarget(null)} style={{ width: 40, height: 40, borderRadius: 999, border: 0, background: "rgba(127,127,127,.12)", color: "inherit", display: "grid", placeItems: "center" }}><X size={20}/></button>
        </div>

        <div style={{ margin: "18px auto 14px", width: "min(76vw, 290px)", aspectRatio: "1", background: "#fff", borderRadius: 18, padding: 12, display: "grid", placeItems: "center" }}>
          <img src={qrUrl} alt={`QR Code para ${shareTarget.platform}`} style={{ width: "100%", height: "100%", objectFit: "contain" }}/>
        </div>

        <p style={{ margin: "0 0 14px", textAlign: "center", lineHeight: 1.45, opacity: .78 }}>{shareTarget.helper}</p>
        <button
          type="button"
          onClick={copyShareLink}
          style={{ width: "100%", minHeight: 48, borderRadius: 16, border: 0, background: "#3b82f6", color: "white", display: "flex", alignItems: "center", justifyContent: "center", gap: 8, fontWeight: 800 }}
        >
          <Copy size={18}/>
          {copied ? "Link copiado!" : "Copiar link"}
        </button>
      </div>
    </div>}
  </>;
}
