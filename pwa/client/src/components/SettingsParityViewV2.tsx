import { useEffect, useMemo, useRef, useState } from "react";
import { signOut } from "firebase/auth";
import { ChevronDown, ChevronUp, LogOut, RefreshCcw, Settings, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { firebaseAuth } from "@/lib/firebase";
import { syncPwaPushPreferences } from "@/lib/push";
import type { PwaSessionLike } from "./AndroidParityViews";
import "./SettingsParityViewV2.css";

type Theme = "system" | "light" | "dark";
type Accent = "blue" | "green" | "purple" | "gold" | "mono";
type PwaSettingsV2 = {
  notifications: boolean;
  vibration: boolean;
  animations: boolean;
  readingMode: boolean;
  fontSize: number;
  theme: Theme;
  accent: Accent;
  readingFont: string;
  keepScreenOn: boolean;
  autoSavePosition: boolean;
  autoScroll: boolean;
  playbackSpeed: number;
  skipTime: number;
  backgroundAudio: boolean;
  autoStartLastPlayback: boolean;
  sleepTimer: number;
  wifiOnlyDownloads: boolean;
  autoCleanDownloads: boolean;
  notifCourses: boolean;
  notifDevotional: boolean;
  notifEvents: boolean;
  notifService: boolean;
  notifNews: boolean;
  notifMedia: boolean;
  notifIbr: boolean;
  notifSermons: boolean;
  preloadImages: boolean;
  saveMobileData: boolean;
  autoUpdateContent: boolean;
  syncFavorites: boolean;
  autoBackup: boolean;
  trackPlaybackHistory: boolean;
};

const defaults: PwaSettingsV2 = {
  notifications: true, vibration: true, animations: true, readingMode: false, fontSize: 1,
  theme: "system", accent: "gold", readingFont: "Roboto", keepScreenOn: false,
  autoSavePosition: true, autoScroll: false, playbackSpeed: 1, skipTime: 15,
  backgroundAudio: true, autoStartLastPlayback: false, sleepTimer: 0, wifiOnlyDownloads: false,
  autoCleanDownloads: false, notifCourses: true, notifDevotional: true, notifEvents: true,
  notifService: true, notifNews: true, notifMedia: true, notifIbr: true, notifSermons: true,
  preloadImages: true, saveMobileData: false, autoUpdateContent: true, syncFavorites: true,
  autoBackup: true, trackPlaybackHistory: true,
};

const storageKey = "micrhema:pwa:settings";
const sections = ["appearance", "reading", "audio", "downloads", "notifications", "internet", "favorites", "maintenance", "account"] as const;
type SectionId = typeof sections[number];

function loadSettings(): PwaSettingsV2 {
  try { return { ...defaults, ...JSON.parse(localStorage.getItem(storageKey) || "{}") }; }
  catch { return defaults; }
}

function accentValue(accent: Accent, dark: boolean) {
  if (accent === "blue") return "#2563eb";
  if (accent === "green") return "#15803d";
  if (accent === "purple") return "#7e22ce";
  if (accent === "mono") return dark ? "#ffffff" : "#111827";
  return "#8a6500";
}

function applySettings(settings: PwaSettingsV2) {
  localStorage.setItem(storageKey, JSON.stringify(settings));
  const root = document.documentElement;
  const dark = settings.theme === "dark" || (settings.theme === "system" && window.matchMedia?.("(prefers-color-scheme: dark)").matches);
  root.dataset.pwaTheme = dark ? "dark" : "light";
  root.dataset.pwaAnimations = settings.animations ? "on" : "off";
  root.dataset.pwaReadingMode = settings.readingMode ? "on" : "off";
  root.style.setProperty("--pwa-font-scale", String([0.9, 1, 1.1, 1.2, 1.3][Math.max(0, Math.min(4, settings.fontSize))]));
  root.style.setProperty("--pwa-reading-font", settings.readingFont === "Serif" ? "Georgia, serif" : settings.readingFont === "Open Sans" ? "'Open Sans', sans-serif" : settings.readingFont === "Inter" ? "Inter, sans-serif" : "Roboto, sans-serif");
  root.style.setProperty("--pwa-primary", accentValue(settings.accent, dark));
}

function SettingToggle({ label, description, checked, onChange }: { label: string; description?: string; checked: boolean; onChange: (value: boolean) => void }) {
  return <label className="settings-v2-toggle"><span><strong>{label}</strong>{description && <small>{description}</small>}</span><input type="checkbox" checked={checked} onChange={(event) => onChange(event.target.checked)}/></label>;
}

function SettingSection({ id, title, description, open, onToggle, children }: { id: SectionId; title: string; description: string; open: boolean; onToggle: (id: SectionId) => void; children: React.ReactNode }) {
  return <section className="settings-v2-section"><button className="settings-v2-summary" onClick={() => onToggle(id)}><span><strong>{title}</strong><small>{description}</small></span>{open ? <ChevronUp size={20}/> : <ChevronDown size={20}/>}</button>{open && <div className="settings-v2-content">{children}</div>}</section>;
}

export function SettingsParityViewV2({ session, onProfile, onNotifications }: { session: PwaSessionLike; onProfile: () => void; onNotifications: () => void }) {
  const [settings, setSettings] = useState<PwaSettingsV2>(() => loadSettings());
  const [expanded, setExpanded] = useState<Set<SectionId>>(() => new Set());
  const [storageText, setStorageText] = useState("Calculando…");
  const wakeLock = useRef<any>(null);
  const availableSections = useMemo(() => sections.filter((id) => id !== "account" || Boolean(session)), [session]);

  const refreshStorage = () => navigator.storage?.estimate?.().then(({ usage = 0 }) => setStorageText(`${(usage / 1024 / 1024).toFixed(1)} MB usados pela PWA`)).catch(() => setStorageText("Uso indisponível neste navegador"));
  useEffect(() => {
    applySettings(settings);
    const timer = window.setTimeout(() => { void syncPwaPushPreferences(); }, 350);
    return () => window.clearTimeout(timer);
  }, [settings]);
  useEffect(() => { void refreshStorage(); }, []);
  useEffect(() => {
    const manageWakeLock = async () => {
      try {
        if (settings.keepScreenOn && "wakeLock" in navigator) wakeLock.current = await (navigator as any).wakeLock.request("screen");
        else if (wakeLock.current) { await wakeLock.current.release(); wakeLock.current = null; }
      } catch { /* iOS/Safari pode negar fora de uma interação ativa. */ }
    };
    void manageWakeLock();
    return () => { if (wakeLock.current) void wakeLock.current.release().catch(() => undefined); wakeLock.current = null; };
  }, [settings.keepScreenOn]);

  const patch = <K extends keyof PwaSettingsV2>(key: K, value: PwaSettingsV2[K]) => setSettings((current) => ({ ...current, [key]: value }));
  const toggleSection = (id: SectionId) => setExpanded((current) => { const next = new Set(current); next.has(id) ? next.delete(id) : next.add(id); return next; });
  const clearCache = async () => { if ("caches" in window) { const keys = await caches.keys(); await Promise.all(keys.map((key) => caches.delete(key))); } await refreshStorage(); toast.success("Cache seguro limpo."); };
  const clearDownloads = async () => { if ("caches" in window) { const keys = await caches.keys(); const downloadKeys = keys.filter((key) => /download|media|offline/i.test(key)); await Promise.all(downloadKeys.map((key) => caches.delete(key))); } await refreshStorage(); toast.success("Downloads armazenados pela PWA foram limpos."); };
  const clearHistory = () => { ["micrhema:recently-viewed", "micrhema:pwa:history", "micrhema:playback-history"].forEach((key) => localStorage.removeItem(key)); toast.success("Histórico limpo."); };
  const restore = () => { setSettings(defaults); toast.success("Configurações restauradas."); };
  const reloadLocal = () => { setSettings(loadSettings()); void refreshStorage(); toast.success("Dados locais recarregados."); };
  const logout = async () => { if (firebaseAuth) await signOut(firebaseAuth).catch(() => undefined); localStorage.removeItem("mic-rhema-pwa-session"); window.location.reload(); };

  return <section className="parity-page settings-v2-root">
    <header className="settings-v2-hero"><div className="settings-v2-icon"><Settings size={24}/></div><div><h1>Configurações</h1><p>Ajuste o MIC Rhema ao seu ritmo.</p></div></header>
    <div className="settings-v2-sync"><strong>{session ? "Preferências sincronizadas" : "Preferências neste aparelho"}</strong><span>{session ? `As escolhas de ${session.name} ficam disponíveis nesta PWA.` : "Entre na sua conta para usar os recursos de membro."}</span></div>
    <div className="settings-v2-expand"><button onClick={() => setExpanded(new Set(availableSections))}>Expandir tudo</button><button onClick={() => setExpanded(new Set())}>Minimizar tudo</button></div>

    <SettingSection id="appearance" title="Aparência" description="Tema, legibilidade e modo de leitura." open={expanded.has("appearance")} onToggle={toggleSection}>
      <SettingToggle label="Notificações" description="Permite os avisos escolhidos abaixo." checked={settings.notifications} onChange={(v) => patch("notifications", v)}/>
      <SettingToggle label="Vibração ao tocar nas abas" description="Usada quando o navegador e o aparelho permitirem." checked={settings.vibration} onChange={(v) => patch("vibration", v)}/>
      <SettingToggle label="Animações" checked={settings.animations} onChange={(v) => patch("animations", v)}/>
      <SettingToggle label="Modo leitura" checked={settings.readingMode} onChange={(v) => patch("readingMode", v)}/>
      <label>Tamanho da fonte<input type="range" min="0" max="4" step="1" value={settings.fontSize} onChange={(e) => patch("fontSize", Number(e.target.value))}/></label>
      <label>Tema<select value={settings.theme} onChange={(e) => patch("theme", e.target.value as Theme)}><option value="system">Sistema</option><option value="light">Claro</option><option value="dark">Escuro</option></select></label>
      <label>Cor de destaque<select value={settings.accent} onChange={(e) => patch("accent", e.target.value as Accent)}><option value="blue">Azul</option><option value="green">Verde</option><option value="purple">Roxo</option><option value="gold">Dourado</option><option value="mono">Branco/Preto</option></select></label>
      <label>Fonte para leitura<select value={settings.readingFont} onChange={(e) => patch("readingFont", e.target.value)}><option>Roboto</option><option>Inter</option><option>Open Sans</option><option>Serif</option></select></label>
    </SettingSection>

    <SettingSection id="reading" title="Leitura bíblica" description="Preferências aplicadas diretamente ao leitor." open={expanded.has("reading")} onToggle={toggleSection}>
      <SettingToggle label="Manter a tela ligada" description="Usa Wake Lock quando o navegador permitir." checked={settings.keepScreenOn} onChange={(v) => patch("keepScreenOn", v)}/>
      <SettingToggle label="Salvar posição da leitura" checked={settings.autoSavePosition} onChange={(v) => patch("autoSavePosition", v)}/>
      <SettingToggle label="Rolagem automática" checked={settings.autoScroll} onChange={(v) => patch("autoScroll", v)}/>
    </SettingSection>

    <SettingSection id="audio" title="Áudio" description="Retomada, velocidade e controles da reprodução." open={expanded.has("audio")} onToggle={toggleSection}>
      <label>Velocidade<select value={settings.playbackSpeed} onChange={(e) => patch("playbackSpeed", Number(e.target.value))}><option value="0.75">0.75x</option><option value="1">1.0x</option><option value="1.25">1.25x</option><option value="1.5">1.5x</option><option value="2">2.0x</option></select></label>
      <label>Pular<select value={settings.skipTime} onChange={(e) => patch("skipTime", Number(e.target.value))}><option value="10">10s</option><option value="15">15s</option><option value="30">30s</option></select></label>
      <SettingToggle label="Continuar com tela bloqueada" description="Depende das permissões do navegador." checked={settings.backgroundAudio} onChange={(v) => patch("backgroundAudio", v)}/>
      <SettingToggle label="Iniciar última pregação" checked={settings.autoStartLastPlayback} onChange={(v) => patch("autoStartLastPlayback", v)}/>
      <label>Temporizador para desligar<select value={settings.sleepTimer} onChange={(e) => patch("sleepTimer", Number(e.target.value))}><option value="0">Desativado</option><option value="15">15 min</option><option value="30">30 min</option><option value="60">60 min</option></select></label>
    </SettingSection>

    <SettingSection id="downloads" title="Downloads" description="Rede, espaço ocupado e limpeza de arquivos." open={expanded.has("downloads")} onToggle={toggleSection}>
      <SettingToggle label="Apenas no Wi‑Fi" checked={settings.wifiOnlyDownloads} onChange={(v) => patch("wifiOnlyDownloads", v)}/>
      <SettingToggle label="Limpar downloads antigos" checked={settings.autoCleanDownloads} onChange={(v) => patch("autoCleanDownloads", v)}/>
      <div className="settings-v2-action"><span><strong>Espaço ocupado</strong><small>{storageText}</small></span><button onClick={() => void refreshStorage()}><RefreshCcw size={18}/></button></div>
      <button className="settings-v2-danger" onClick={() => void clearDownloads()}><Trash2 size={18}/> Limpar downloads</button>
      <small className="settings-v2-note">A PWA não permite escolher “Cartão SD”; o navegador controla a pasta de armazenamento.</small>
    </SettingSection>

    <SettingSection id="notifications" title="Notificações" description="Escolha quais avisos deseja receber." open={expanded.has("notifications")} onToggle={toggleSection}>
      <SettingToggle label="Novos cursos" checked={settings.notifCourses} onChange={(v) => patch("notifCourses", v)}/><SettingToggle label="Devocional diário às 8h" checked={settings.notifDevotional} onChange={(v) => patch("notifDevotional", v)}/><SettingToggle label="Avisos de eventos e cultos" checked={settings.notifEvents} onChange={(v) => patch("notifEvents", v)}/><SettingToggle label="Próximo culto" checked={settings.notifService} onChange={(v) => patch("notifService", v)}/><SettingToggle label="Notícia do meio-dia" checked={settings.notifNews} onChange={(v) => patch("notifNews", v)}/><SettingToggle label="Novas mídias" checked={settings.notifMedia} onChange={(v) => patch("notifMedia", v)}/><SettingToggle label="Novas aulas e módulos IBR" checked={settings.notifIbr} onChange={(v) => patch("notifIbr", v)}/><SettingToggle label="Novas pregações" checked={settings.notifSermons} onChange={(v) => patch("notifSermons", v)}/><button onClick={onNotifications}>Ativar permissão de notificações neste aparelho</button>
    </SettingSection>

    <SettingSection id="internet" title="Internet e dados" description="Controle atualização e pré-carregamento." open={expanded.has("internet")} onToggle={toggleSection}>
      <SettingToggle label="Pré-carregar imagens" checked={settings.preloadImages} onChange={(v) => patch("preloadImages", v)}/><SettingToggle label="Economizar dados móveis" checked={settings.saveMobileData} onChange={(v) => patch("saveMobileData", v)}/><SettingToggle label="Atualizar automaticamente" checked={settings.autoUpdateContent} onChange={(v) => patch("autoUpdateContent", v)}/><button onClick={() => void clearCache()}>Limpar cache</button>
    </SettingSection>

    <SettingSection id="favorites" title="Favoritos e histórico" description="Acompanhe conteúdos e preserve seus dados." open={expanded.has("favorites")} onToggle={toggleSection}>
      <SettingToggle label="Sincronizar favoritos" checked={settings.syncFavorites} onChange={(v) => patch("syncFavorites", v)}/><SettingToggle label="Backup automático" checked={settings.autoBackup} onChange={(v) => patch("autoBackup", v)}/><SettingToggle label="Histórico de reprodução" checked={settings.trackPlaybackHistory} onChange={(v) => patch("trackPlaybackHistory", v)}/><button className="settings-v2-danger" onClick={clearHistory}><Trash2 size={18}/> Limpar histórico</button>
    </SettingSection>

    <SettingSection id="maintenance" title="Manutenção" description="Ações de recuperação e redefinição." open={expanded.has("maintenance")} onToggle={toggleSection}>
      <button onClick={reloadLocal}><RefreshCcw size={18}/> Recarregar dados locais</button><button className="settings-v2-danger" onClick={restore}><RefreshCcw size={18}/> Restaurar configurações</button>
    </SettingSection>

    {session && <SettingSection id="account" title="Conta" description="Dados da sua sessão no MIC Rhema." open={expanded.has("account")} onToggle={toggleSection}><div className="settings-v2-account"><strong>{session.name}</strong><small>{session.isIbr ? "Membro · Aluno IBR" : "Membro"}</small></div><button onClick={onProfile}>Alterar número de contato / abrir perfil</button><button className="settings-v2-danger" onClick={() => void logout()}><LogOut size={18}/> Sair da conta</button></SettingSection>}
  </section>;
}
