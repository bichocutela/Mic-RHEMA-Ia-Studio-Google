import { useEffect, useMemo, useRef, useState } from "react";
import { signOut } from "firebase/auth";
import { toast } from "sonner";
import {
  BadgeCheck, BookOpen, ChevronLeft, ChevronRight, CircleUserRound, Headphones, Heart,
  LogOut, Mail, MapPin, Moon, Play, RefreshCcw, Save, Search, Settings, ShieldCheck,
  Smartphone, Sun, UserRound, Video, Volume2,
} from "lucide-react";
import { bibleBookChapters, bibleBooks } from "@/lib/pwa-data";
import {
  firebaseAuth, listenToCollection, loadPwaMemberProfile, savePwaMemberProfile,
  type PwaMemberProfile,
} from "@/lib/firebase";

export type PwaSessionLike = { uid: string; name: string; isAdmin: boolean; isIbr?: boolean } | null;

type BibleVerse = { verse: number; text: string };
type MediaItem = {
  id: string; title?: string; description?: string; imageUrl?: string; thumbnailUrl?: string; coverUrl?: string;
  videoUrl?: string; audioUrl?: string; bookUrl?: string; mediaUrl?: string; artist?: string; author?: string;
  isApproved?: boolean; approved?: boolean;
};

type Translation = { code: string; name: string; apiCode: string };
const translations: Translation[] = [
  { code: "ARA", name: "Almeida Revista e Atualizada", apiCode: "ARA" },
  { code: "NVI", name: "Nova Versão Internacional", apiCode: "NVIPT" },
  { code: "NTLH", name: "Nova Tradução na Linguagem de Hoje", apiCode: "NTLH" },
  { code: "NAA", name: "Nova Almeida Atualizada 2017", apiCode: "NAA" },
  { code: "ARC", name: "Almeida Revista e Corrigida 2009", apiCode: "ARC09" },
  { code: "ACF", name: "Almeida Corrigida Fiel 2011", apiCode: "ACF11" },
  { code: "NVT", name: "Nova Versão Transformadora 2016", apiCode: "NVT" },
  { code: "NBV", name: "Nova Bíblia Viva 2007", apiCode: "NBV07" },
  { code: "KJA", name: "King James Atualizada 2001", apiCode: "KJA" },
];

const avatars = [
  ["davi", "Davi"], ["ester", "Ester"], ["daniel", "Daniel"], ["rute", "Rute"], ["moises", "Moisés"],
  ["noe", "Noé"], ["maria", "Maria"], ["paulo", "Paulo"], ["josue", "Josué"], ["abraao", "Abraão"],
  ["sara", "Sara"], ["rebeca", "Rebeca"], ["jaco", "Jacó"], ["jose", "José"], ["samuel", "Samuel"],
  ["elias", "Elias"], ["isaias", "Isaías"], ["jeremias", "Jeremias"], ["joao_batista", "João Batista"],
  ["timoteo", "Timóteo"], ["priscila", "Priscila"], ["lidia", "Lídia"],
] as const;

const badgeCatalog = [
  ["caminhante", "Caminhante", "O início de uma jornada de fé e conhecimento."],
  ["semeador", "Semeador", "Quem planta a Palavra no coração todos os dias."],
  ["discipulo", "Discípulo", "Um passo firme no aprendizado da Palavra."],
  ["perseverante", "Perseverante", "Constância para continuar mesmo nos dias difíceis."],
  ["estudante_rhema", "Estudante Rhema", "Dedicação reconhecida ao estudo no Instituto Bíblico Rhema."],
  ["mestre_da_palavra", "Mestre da Palavra", "Conhecimento construído com disciplina e compromisso."],
  ["guardiao_da_fe", "Guardião da Fé", "Um testemunho de perseverança, serviço e maturidade."],
  ["primeira_oracao", "Primeira Oração", "Um primeiro momento separado para falar com Deus."],
  ["leitor_da_palavra", "Leitor da Palavra", "A Bíblia aberta e o coração disposto a aprender."],
  ["coracao_grato", "Coração Grato", "Reconhecimento pelas bênçãos recebidas."],
  ["constante", "Constante", "Pequenos passos repetidos com fidelidade."],
  ["certificado_ibr", "Certificado IBR", "Uma conquista acadêmica no Instituto Bíblico Rhema."],
] as const;

function cleanBibleText(text: string) {
  return text.replace(/<[^>]*>/g, " ").replace(/&nbsp;/gi, " ").replace(/\s+/g, " ").trim();
}

function bibleCacheKey(apiCode: string, bookId: number, chapter: number) {
  return `micrhema:bible:${apiCode}:${bookId}:${chapter}`;
}

export function BibleParityView() {
  const [book, setBook] = useState(() => localStorage.getItem("micrhema:bible:book") || "Gênesis");
  const [chapter, setChapter] = useState(() => Number(localStorage.getItem("micrhema:bible:chapter") || 1));
  const [version, setVersion] = useState(() => localStorage.getItem("micrhema:bible:version") || "ARA");
  const [verses, setVerses] = useState<BibleVerse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [reload, setReload] = useState(0);
  const [saved, setSaved] = useState(false);
  const translation = translations.find((item) => item.code === version) || translations[0];
  const bookId = bibleBooks.indexOf(book) + 1;
  const maxChapter = bibleBookChapters[book as keyof typeof bibleBookChapters] || 1;

  useEffect(() => {
    localStorage.setItem("micrhema:bible:book", book);
    localStorage.setItem("micrhema:bible:chapter", String(chapter));
    localStorage.setItem("micrhema:bible:version", translation.code);
    const favorites = JSON.parse(localStorage.getItem("micrhema:bible:favorites") || "[]") as string[];
    setSaved(favorites.includes(`${translation.code}:${book}:${chapter}`));
  }, [book, chapter, translation.code]);

  useEffect(() => {
    const controller = new AbortController();
    const key = bibleCacheKey(translation.apiCode, bookId, chapter);
    const cached = localStorage.getItem(key);
    setLoading(true); setError("");
    fetch(`https://bolls.life/get-text/${translation.apiCode}/${bookId}/${chapter}/`, { signal: controller.signal, headers: { Accept: "application/json" } })
      .then(async (response) => {
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        const payload = await response.json() as Array<{ verse?: unknown; text?: unknown }>;
        const normalized = payload.map((item) => ({ verse: Number(item.verse), text: cleanBibleText(String(item.text || "")) })).filter((item) => item.verse > 0 && item.text);
        if (!normalized.length) throw new Error("Capítulo sem conteúdo.");
        localStorage.setItem(key, JSON.stringify(normalized));
        setVerses(normalized);
      })
      .catch((requestError: unknown) => {
        if ((requestError as { name?: string })?.name === "AbortError") return;
        if (cached) {
          try { setVerses(JSON.parse(cached) as BibleVerse[]); setError("Sem conexão: exibindo o capítulo salvo no aparelho."); }
          catch { setError("Não foi possível carregar este capítulo."); }
        } else setError("Não foi possível carregar este capítulo. Verifique a conexão e tente novamente.");
      })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [bookId, chapter, translation.apiCode, reload]);

  const toggleSaved = () => {
    const id = `${translation.code}:${book}:${chapter}`;
    const favorites = new Set(JSON.parse(localStorage.getItem("micrhema:bible:favorites") || "[]") as string[]);
    if (favorites.has(id)) favorites.delete(id); else favorites.add(id);
    localStorage.setItem("micrhema:bible:favorites", JSON.stringify(Array.from(favorites)));
    setSaved(favorites.has(id));
    toast.success(favorites.has(id) ? "Capítulo salvo." : "Capítulo removido dos salvos.");
  };

  return <section className="parity-page bible-parity">
    <div className="parity-title"><div><p>BÍBLIA</p><h1>{book} {chapter}</h1><span>{translation.name}</span></div><BookOpen size={30}/></div>
    <div className="parity-toolbar">
      <label>Tradução<select value={translation.code} onChange={(event) => setVersion(event.target.value)}>{translations.map((item) => <option key={item.code} value={item.code}>{item.code} — {item.name}</option>)}</select></label>
      <label>Livro<select value={book} onChange={(event) => { setBook(event.target.value); setChapter(1); }}>{bibleBooks.map((name) => <option key={name}>{name}</option>)}</select></label>
      <label>Capítulo<select value={chapter} onChange={(event) => setChapter(Number(event.target.value))}>{Array.from({ length: maxChapter }, (_, index) => index + 1).map((number) => <option key={number} value={number}>{number}</option>)}</select></label>
    </div>
    <div className="parity-reader">
      {loading && <p className="parity-status">Carregando {translation.code}…</p>}
      {!loading && error && <div className="parity-warning"><span>{error}</span><button onClick={() => setReload((value) => value + 1)}><RefreshCcw size={16}/> Tentar novamente</button></div>}
      {!loading && verses.map((verse) => <p key={verse.verse}><sup>{verse.verse}</sup>{verse.text}</p>)}
    </div>
    <div className="parity-bottom-actions"><button disabled={chapter <= 1} onClick={() => setChapter((value) => Math.max(1, value - 1))}><ChevronLeft size={18}/> Anterior</button><button onClick={toggleSaved}><Heart size={18} fill={saved ? "currentColor" : "none"}/> {saved ? "Salvo" : "Salvar"}</button><button disabled={chapter >= maxChapter} onClick={() => setChapter((value) => Math.min(maxChapter, value + 1))}>Próximo <ChevronRight size={18}/></button></div>
  </section>;
}

function activityCount(profile: PwaMemberProfile | null) {
  if (!profile) return 0;
  return Object.values(profile.badgeActivityIds || {}).reduce((total, ids) => total + (Array.isArray(ids) ? ids.length : 0), 0);
}

function ProfileLoader({ children }: { children: (profile: PwaMemberProfile, reload: () => Promise<void>) => React.ReactNode }) {
  const [profile, setProfile] = useState<PwaMemberProfile | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const reload = async () => {
    setLoading(true); setError("");
    try { setProfile(await loadPwaMemberProfile()); }
    catch (requestError) { setError(requestError instanceof Error ? requestError.message : "Não foi possível carregar o perfil."); }
    finally { setLoading(false); }
  };
  useEffect(() => { void reload(); }, []);
  if (loading) return <section className="parity-page"><p className="parity-status">Sincronizando seu perfil…</p></section>;
  if (!profile || error) return <section className="parity-page"><div className="parity-warning"><span>{error || "Perfil indisponível."}</span><button onClick={() => void reload()}><RefreshCcw size={16}/> Tentar novamente</button></div></section>;
  return <>{children(profile, reload)}</>;
}

export function ProfileParityView({ session, onNavigateHome }: { session: PwaSessionLike; onNavigateHome: () => void }) {
  if (!session) return <section className="parity-page"><div className="parity-empty"><CircleUserRound size={46}/><h1>Meu Perfil</h1><p>Entre para acessar seus dados e conquistas.</p></div></section>;
  return <ProfileLoader>{(profile, reload) => <ProfileEditor profile={profile} reload={reload} onNavigateHome={onNavigateHome} />}</ProfileLoader>;
}

function ProfileEditor({ profile, reload, onNavigateHome }: { profile: PwaMemberProfile; reload: () => Promise<void>; onNavigateHome: () => void }) {
  const [draft, setDraft] = useState(profile);
  const [saving, setSaving] = useState(false);
  const [showAvatars, setShowAvatars] = useState(false);
  const [showBadges, setShowBadges] = useState(false);
  useEffect(() => setDraft(profile), [profile]);
  const avatarName = avatars.find(([id]) => id === draft.avatarId)?.[1] || "Davi";
  const badge = badgeCatalog.find(([id]) => id === draft.equippedBadgeId) || badgeCatalog[0];

  const save = async () => {
    setSaving(true);
    try {
      const updated = await savePwaMemberProfile({ name: draft.name, phone: draft.phone, address: draft.address, birthDate: draft.birthDate, email: draft.email, avatarId: draft.avatarId, equippedBadgeId: draft.equippedBadgeId });
      setDraft(updated);
      const stored = localStorage.getItem("mic-rhema-pwa-session");
      if (stored) { const parsed = JSON.parse(stored); parsed.name = updated.name; localStorage.setItem("mic-rhema-pwa-session", JSON.stringify(parsed)); }
      toast.success("Perfil atualizado também para o Android.");
      await reload();
    } catch (requestError) { toast.error(requestError instanceof Error ? requestError.message : "Não foi possível salvar."); }
    finally { setSaving(false); }
  };

  const logout = async () => {
    if (firebaseAuth) await signOut(firebaseAuth).catch(() => undefined);
    localStorage.removeItem("mic-rhema-pwa-session");
    onNavigateHome();
    window.location.reload();
  };

  return <section className="parity-page profile-parity">
    <div className="profile-parity-hero"><div className="profile-avatar-large">{avatarName.slice(0, 1)}</div><div><p>SEU AVATAR BÍBLICO</p><h1>{draft.name}</h1><span>{avatarName} · {badge[1]}</span></div></div>
    <div className="profile-achievement"><BadgeCheck size={24}/><div><strong>{badge[1]}</strong><span>{badge[2]}</span><small>{activityCount(profile)} atividades registradas · {profile.unlockedBadgeIds.length} emblema(s) liberado(s)</small></div></div>
    <div className="profile-fields">
      <label>Nome completo<input value={draft.name} onChange={(event) => setDraft({ ...draft, name: event.target.value })}/></label>
      <label>Telefone<input inputMode="tel" value={draft.phone} onChange={(event) => setDraft({ ...draft, phone: event.target.value.replace(/\D/g, "").slice(0, 15) })}/></label>
      <label>Endereço<input value={draft.address} onChange={(event) => setDraft({ ...draft, address: event.target.value })}/></label>
      <label>Data de nascimento<input inputMode="numeric" placeholder="dd/mm/aa" value={draft.birthDate} onChange={(event) => setDraft({ ...draft, birthDate: event.target.value.replace(/[^0-9/]/g, "").slice(0, 10) })}/></label>
      <label>E-mail para certificado IBR<input type="email" value={draft.email} onChange={(event) => setDraft({ ...draft, email: event.target.value })}/></label>
    </div>
    <div className="profile-choice-actions"><button onClick={() => setShowAvatars((value) => !value)}><UserRound size={18}/> Trocar avatar</button><button onClick={() => setShowBadges((value) => !value)}><BadgeCheck size={18}/> Emblemas e níveis</button></div>
    {showAvatars && <div className="avatar-grid">{avatars.map(([id, name]) => <button className={draft.avatarId === id ? "selected" : ""} key={id} onClick={() => { setDraft({ ...draft, avatarId: id }); setShowAvatars(false); }}><span>{name.slice(0, 1)}</span><small>{name}</small></button>)}</div>}
    {showBadges && <div className="badge-list">{badgeCatalog.map(([id, name, description]) => { const unlocked = profile.unlockedBadgeIds.includes(id) || id === "caminhante"; return <button disabled={!unlocked} className={draft.equippedBadgeId === id ? "selected" : ""} key={id} onClick={() => { if (unlocked) setDraft({ ...draft, equippedBadgeId: id }); }}><BadgeCheck size={18}/><span><strong>{name}</strong><small>{unlocked ? description : "Bloqueado"}</small></span></button>; })}</div>}
    <button className="parity-primary" disabled={saving} onClick={() => void save()}><Save size={18}/>{saving ? "Salvando…" : "Salvar alterações"}</button>
    <button className="parity-danger" onClick={() => void logout()}><LogOut size={18}/> Sair da conta</button>
  </section>;
}

export function MembersParityView({ session, onProfile }: { session: PwaSessionLike; onProfile: () => void }) {
  if (!session) return <section className="parity-page"><div className="parity-empty"><ShieldCheck size={46}/><h1>Área de Membros</h1><p>Entre com o mesmo nome e telefone aprovados no aplicativo ou solicite seu acesso.</p><button className="parity-primary" onClick={onProfile}>Entrar ou solicitar acesso</button></div></section>;
  return <ProfileLoader>{(profile) => <section className="parity-page members-parity"><div className="parity-title"><div><p>MEMBROS</p><h1>{profile.name}</h1><span>Acesso aprovado</span></div><BadgeCheck size={30}/></div><div className="android-list-cards"><article className="android-module-card"><Smartphone size={26}/><div><strong>{profile.phone || "Telefone não informado"}</strong><small>Telefone do cadastro</small></div></article><article className="android-module-card"><Mail size={26}/><div><strong>{profile.email || "E-mail não informado"}</strong><small>Certificado IBR</small></div></article><article className="android-module-card"><MapPin size={26}/><div><strong>{profile.address || "Endereço não informado"}</strong><small>Endereço</small></div></article><article className="android-module-card"><BookOpen size={26}/><div><strong>{profile.isIbr ? "Aluno IBR" : "Membro"}</strong><small>{profile.isIbr ? "Cursos IBR liberados" : "Acesso comunitário"}</small></div></article></div><button className="parity-primary" onClick={onProfile}>Abrir Meu Perfil <ChevronRight size={18}/></button></section>}</ProfileLoader>;
}

type PwaSettings = {
  theme: "system" | "light" | "dark"; fontSize: number; readingFont: string; readingMode: boolean; animations: boolean;
  autoSavePosition: boolean; autoScroll: boolean; playbackSpeed: number; skipTime: number; backgroundAudio: boolean;
  wifiOnlyDownloads: boolean; autoCleanDownloads: boolean; notifications: boolean; notifCourses: boolean; notifDevotional: boolean;
  notifEvents: boolean; notifService: boolean; notifNews: boolean; notifMedia: boolean; notifIbr: boolean; preloadImages: boolean;
  saveMobileData: boolean; autoUpdateContent: boolean;
};
const defaultSettings: PwaSettings = { theme: "system", fontSize: 1, readingFont: "Roboto", readingMode: false, animations: true, autoSavePosition: true, autoScroll: false, playbackSpeed: 1, skipTime: 15, backgroundAudio: true, wifiOnlyDownloads: false, autoCleanDownloads: false, notifications: true, notifCourses: true, notifDevotional: true, notifEvents: true, notifService: true, notifNews: true, notifMedia: true, notifIbr: true, preloadImages: true, saveMobileData: false, autoUpdateContent: true };

function loadSettings(): PwaSettings {
  try { return { ...defaultSettings, ...JSON.parse(localStorage.getItem("micrhema:pwa:settings") || "{}") }; } catch { return defaultSettings; }
}

function applySettings(settings: PwaSettings) {
  localStorage.setItem("micrhema:pwa:settings", JSON.stringify(settings));
  const root = document.documentElement;
  const dark = settings.theme === "dark" || (settings.theme === "system" && window.matchMedia?.("(prefers-color-scheme: dark)").matches);
  root.dataset.pwaTheme = dark ? "dark" : "light";
  root.style.setProperty("--pwa-font-scale", String([0.9, 1, 1.1, 1.2, 1.3][Math.max(0, Math.min(4, settings.fontSize))]));
  root.style.setProperty("--pwa-reading-font", settings.readingFont === "Serif" ? "Georgia, serif" : settings.readingFont === "Open Sans" ? "'Open Sans', sans-serif" : settings.readingFont === "Inter" ? "Inter, sans-serif" : "Roboto, sans-serif");
  root.dataset.pwaAnimations = settings.animations ? "on" : "off";
}

export function SettingsParityView({ session, onProfile, onNotifications }: { session: PwaSessionLike; onProfile: () => void; onNotifications: () => void }) {
  const [settings, setSettings] = useState<PwaSettings>(() => loadSettings());
  const [storageText, setStorageText] = useState("Calculando…");
  useEffect(() => { applySettings(settings); }, [settings]);
  useEffect(() => { navigator.storage?.estimate?.().then(({ usage = 0 }) => setStorageText(`${(usage / 1024 / 1024).toFixed(1)} MB usados pela PWA`)).catch(() => setStorageText("Uso indisponível neste navegador")); }, []);
  const patch = <K extends keyof PwaSettings>(key: K, value: PwaSettings[K]) => setSettings((current) => ({ ...current, [key]: value }));
  const clearCache = async () => { if ("caches" in window) { const names = await caches.keys(); await Promise.all(names.map((name) => caches.delete(name))); } toast.success("Cache da PWA limpo."); };
  const Toggle = ({ label, field }: { label: string; field: keyof PwaSettings }) => <label className="settings-toggle"><span>{label}</span><input type="checkbox" checked={Boolean(settings[field])} onChange={(event) => patch(field as any, event.target.checked as any)}/></label>;
  return <section className="parity-page settings-parity"><div className="parity-title"><div><p>CONFIGURAÇÕES</p><h1>Do seu jeito</h1><span>Preferências equivalentes às do Android, adaptadas ao navegador.</span></div><Settings size={30}/></div>
    <details open><summary>Aparência</summary><div className="settings-grid"><label>Tema<select value={settings.theme} onChange={(event) => patch("theme", event.target.value as PwaSettings["theme"])}><option value="system">Sistema</option><option value="light">Claro</option><option value="dark">Escuro</option></select></label><label>Tamanho da fonte<input type="range" min="0" max="4" step="1" value={settings.fontSize} onChange={(event) => patch("fontSize", Number(event.target.value))}/></label><label>Fonte para leitura<select value={settings.readingFont} onChange={(event) => patch("readingFont", event.target.value)}><option>Roboto</option><option>Inter</option><option>Open Sans</option><option>Serif</option></select></label><Toggle label="Modo leitura" field="readingMode"/><Toggle label="Animações" field="animations"/></div></details>
    <details><summary>Leitura bíblica</summary><div className="settings-grid"><Toggle label="Salvar posição da leitura" field="autoSavePosition"/><Toggle label="Rolagem automática" field="autoScroll"/></div></details>
    <details><summary>Áudio</summary><div className="settings-grid"><label>Velocidade<select value={settings.playbackSpeed} onChange={(event) => patch("playbackSpeed", Number(event.target.value))}><option value="0.75">0.75x</option><option value="1">1.0x</option><option value="1.25">1.25x</option><option value="1.5">1.5x</option><option value="2">2.0x</option></select></label><label>Pular<select value={settings.skipTime} onChange={(event) => patch("skipTime", Number(event.target.value))}><option value="10">10s</option><option value="15">15s</option><option value="30">30s</option></select></label><Toggle label="Continuar com tela bloqueada quando o navegador permitir" field="backgroundAudio"/></div></details>
    <details><summary>Downloads e armazenamento</summary><div className="settings-grid"><Toggle label="Apenas no Wi-Fi" field="wifiOnlyDownloads"/><Toggle label="Limpar downloads antigos" field="autoCleanDownloads"/><div className="settings-action"><span>Espaço ocupado</span><strong>{storageText}</strong></div><button onClick={() => void clearCache()}>Limpar cache da PWA</button></div></details>
    <details><summary>Notificações</summary><div className="settings-grid"><Toggle label="Notificações" field="notifications"/><Toggle label="Novos cursos" field="notifCourses"/><Toggle label="Devocional diário" field="notifDevotional"/><Toggle label="Eventos e cultos" field="notifEvents"/><Toggle label="Próximo culto" field="notifService"/><Toggle label="Notícia do dia" field="notifNews"/><Toggle label="Novas mídias" field="notifMedia"/><Toggle label="Novas aulas IBR" field="notifIbr"/><button onClick={onNotifications}>Ativar permissão de notificações neste aparelho</button></div></details>
    <details><summary>Internet e dados</summary><div className="settings-grid"><Toggle label="Pré-carregar imagens" field="preloadImages"/><Toggle label="Economizar dados móveis" field="saveMobileData"/><Toggle label="Atualizar automaticamente" field="autoUpdateContent"/></div></details>
    {session && <details><summary>Conta</summary><div className="settings-grid"><button onClick={onProfile}>Abrir Meu Perfil</button></div></details>}
  </section>;
}

function youtubeId(value?: string) {
  if (!value) return "";
  try {
    const url = new URL(value); const host = url.hostname.replace(/^www\./, "");
    const id = host === "youtu.be" ? url.pathname.split("/")[1] : url.searchParams.get("v") || url.pathname.match(/^\/(?:embed|shorts|live)\/([^/?]+)/)?.[1] || "";
    return /^[A-Za-z0-9_-]{11}$/.test(id) ? id : "";
  } catch { return ""; }
}
function itemUrl(item: MediaItem) { return item.videoUrl || item.audioUrl || item.bookUrl || item.mediaUrl || ""; }
function itemImage(item: MediaItem) { const id = youtubeId(item.videoUrl || item.mediaUrl); return item.imageUrl || item.thumbnailUrl || item.coverUrl || (id ? `https://i.ytimg.com/vi/${id}/hqdefault.jpg` : ""); }

export function MediaParityView() {
  const [videos, setVideos] = useState<MediaItem[]>([]); const [audios, setAudios] = useState<MediaItem[]>([]); const [books, setBooks] = useState<MediaItem[]>([]);
  const [filter, setFilter] = useState("Tudo"); const [selected, setSelected] = useState<(MediaItem & { kind: string }) | null>(null);
  useEffect(() => listenToCollection<MediaItem>("conteudos_videos", setVideos, () => setVideos([])), []);
  useEffect(() => listenToCollection<MediaItem>("conteudos_audios", setAudios, () => setAudios([])), []);
  useEffect(() => listenToCollection<MediaItem>("conteudos_books", setBooks, () => setBooks([])), []);
  const all = useMemo(() => [
    ...videos.filter((item) => item.approved !== false && item.isApproved !== false).map((item) => ({ ...item, kind: "Vídeo" })),
    ...audios.filter((item) => item.approved !== false && item.isApproved !== false).map((item) => ({ ...item, kind: "Áudio" })),
    ...books.filter((item) => item.approved !== false && item.isApproved !== false).map((item) => ({ ...item, kind: "Livro" })),
  ], [videos, audios, books]);
  const visible = filter === "Tudo" ? all : all.filter((item) => item.kind === filter);
  if (selected) return <MediaReader item={selected} onBack={() => setSelected(null)}/>;
  return <section className="parity-page media-parity"><div className="parity-title"><div><p>MÍDIA</p><h1>Conteúdo da igreja</h1><span>Vídeos, áudios e livros sincronizados em tempo real.</span></div><Play size={30}/></div><div className="filter-pills">{["Tudo", "Vídeo", "Áudio", "Livro"].map((kind) => <button key={kind} className={filter === kind ? "selected" : ""} onClick={() => setFilter(kind)}>{kind}</button>)}</div><div className="media-parity-grid">{visible.map((item) => <button key={`${item.kind}-${item.id}`} onClick={() => setSelected(item)}><div className="media-cover">{itemImage(item) ? <img src={itemImage(item)} alt=""/> : item.kind === "Áudio" ? <Headphones/> : item.kind === "Livro" ? <BookOpen/> : <Video/>}</div><span>{item.kind}</span><strong>{item.title || "Conteúdo MIC Rhema"}</strong><small>{item.description || item.artist || item.author || "Abrir conteúdo"}</small></button>)}</div></section>;
}

function MediaReader({ item, onBack }: { item: MediaItem & { kind: string }; onBack: () => void }) {
  const audioRef = useRef<HTMLAudioElement | null>(null);
  const settings = loadSettings();
  const url = itemUrl(item); const video = youtubeId(url);
  useEffect(() => { if (audioRef.current) audioRef.current.playbackRate = settings.playbackSpeed; }, [settings.playbackSpeed]);
  return <section className="parity-page media-reader"><button className="back-link" onClick={onBack}><ChevronLeft size={18}/> Voltar à mídia</button><div className="parity-title"><div><p>{item.kind.toUpperCase()}</p><h1>{item.title || "Conteúdo"}</h1><span>{item.description || item.artist || item.author || "MIC Rhema"}</span></div></div>
    {!url ? <p className="parity-warning">Este conteúdo ainda não possui arquivo ou link.</p> : item.kind === "Vídeo" ? (video ? <iframe className="parity-video" title={item.title || "Vídeo"} src={`https://www.youtube-nocookie.com/embed/${video}?rel=0`} allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowFullScreen/> : <video className="parity-video" controls src={url}/>) : item.kind === "Áudio" ? <div className="audio-reader"><Headphones size={42}/><audio ref={audioRef} controls src={url}/><div className="audio-skip"><button onClick={() => { if (audioRef.current) audioRef.current.currentTime = Math.max(0, audioRef.current.currentTime - settings.skipTime); }}>−{settings.skipTime}s</button><span>{settings.playbackSpeed}x</span><button onClick={() => { if (audioRef.current) audioRef.current.currentTime += settings.skipTime; }}>+{settings.skipTime}s</button></div></div> : <iframe className="parity-document" title={item.title || "Livro/PDF"} src={url}/>} 
  </section>;
}
