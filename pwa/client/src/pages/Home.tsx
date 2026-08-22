/**
 * PARIDADE ANDROID — Fluxos PWA preservados na casca Material 3 do aplicativo Android.
 * Firebase, Supabase e Web Push permanecem web-only; o aplicativo Android não é alterado aqui.
 */
import { useEffect, useMemo, useState } from "react";
import { toast } from "sonner";
import {
  ArrowRight, BadgeCheck, Bell, BookHeart, BookOpen, CalendarDays, ChevronDown, ChevronLeft,
  ChevronRight, Church, CircleUserRound, Crown, FileText, Headphones, Heart, Image as ImageIcon,
  LayoutDashboard, LockKeyhole, Menu, Newspaper, Play, Plus, Search, School, Settings2, ShieldCheck,
  Sparkles, Users, Video, Volume2, X,
} from "lucide-react";
import { PwaShell, SectionHeading, type AppView } from "@/components/PwaShell";
import { InstallCard } from "@/components/InstallCard";
import { ASSETS, bibleBooks, genesisVerses, sampleMedia, sampleNews, settingsSections, type ContentCard } from "@/lib/pwa-data";
import { approveMemberRequest, createAdminContent, firebaseAuth, firebaseEnabled, listenToCollection, listenToDocument, savePwaProfile, submitPendingAccessRequest } from "@/lib/firebase";
import { onAuthStateChanged, signOut } from "firebase/auth";
import { signInPwa, type PwaSession } from "@/lib/pwa-auth";
import { listenToForegroundPush, sendPwaPush, subscribeToPwaPush } from "@/lib/push";
import androidPlans from "@/data/android-plans.json";

type CollectionItem = ContentCard & { description?: string; imageUrl?: string; thumbnailUrl?: string; coverUrl?: string; name?: string; isApproved?: boolean; approved?: boolean; isIbr?: boolean; content?: string; summary?: string; book?: string; chapter?: number; verse?: string | number; verseReference?: string; category?: string; intensity?: number; featured?: boolean; publishedAt?: number; timestamp?: number; date?: string; day?: string; dayShort?: string; time?: string; videoUrl?: string; audioUrl?: string; bookUrl?: string; mediaUrl?: string; author?: string; artist?: string; mediaType?: "Vídeo" | "Áudio" | "Livro" };
type CarouselBanner = { id: string; imageUrl?: string; title?: string; description?: string; tag?: string; eventDate?: string; eventInfo?: string };
type HomeBannersFallback = { urls?: string[] };
type AppBannerSettings = { bannerRotationSeconds?: number };

const menuItems: Array<{ id: AppView; label: string; note: string; icon: typeof Church }> = [
  { id: "discipulado", label: "Discipulado", note: "PDFs e estudos públicos", icon: BookHeart },
  { id: "cultos", label: "Cultos", note: "Agenda e detalhes", icon: CalendarDays },
  { id: "plans", label: "Planos", note: "Caminhos de leitura", icon: Sparkles },
  { id: "profile", label: "Meu perfil", note: "Avatar, emblemas e acesso", icon: CircleUserRound },
  { id: "settings", label: "Configurações", note: "Suas escolhas no aplicativo", icon: Settings2 },
];

function contentImage(item: CollectionItem) {
  return item.imageUrl || item.thumbnailUrl || item.coverUrl || item.image || ASSETS.media;
}

function newsReference(item: CollectionItem) {
  if (!item.book) return item.category || item.tag || "Notícia bíblica";
  const chapter = item.chapter ? ` ${item.chapter}` : "";
  const verse = item.verse ? `:${item.verse}` : "";
  return `${item.book}${chapter}${verse}`;
}

const serviceWeekdays: Record<string, number> = { domingo: 0, "segunda-feira": 1, segunda: 1, "terça-feira": 2, terça: 2, "quarta-feira": 3, quarta: 3, "quinta-feira": 4, quinta: 4, "sexta-feira": 5, sexta: 5, sábado: 6, sabado: 6 };

function serviceScheduleDate(item: CollectionItem) {
  const time = (item.time || "23:59").replace("h", ":").replace(/[^0-9:]/g, "");
  const [hour = "23", minute = "59"] = time.split(":");
  if (/^\d{4}-\d{2}-\d{2}$/.test(item.date || "")) {
    const parsed = new Date(`${item.date}T${hour.padStart(2, "0")}:${minute.slice(0, 2).padStart(2, "0")}:00`);
    if (!Number.isNaN(parsed.getTime())) return parsed;
  }
  const now = new Date();
  const targetDay = serviceWeekdays[(item.day || "domingo").toLowerCase()] ?? 0;
  const parsed = new Date(now);
  parsed.setHours(Number(hour) || 23, Number(minute) || 59, 0, 0);
  let difference = targetDay - now.getDay();
  if (difference < 0 || (difference === 0 && parsed.getTime() < now.getTime())) difference += 7;
  parsed.setDate(now.getDate() + difference);
  return parsed;
}

function formatServiceDate(item: CollectionItem) {
  const date = serviceScheduleDate(item);
  return { day: item.dayShort || date.toLocaleDateString("pt-BR", { weekday: "short" }).replace(".", "").toUpperCase(), number: String(date.getDate()).padStart(2, "0"), complete: date.toLocaleDateString("pt-BR", { day: "2-digit", month: "long" }) };
}

function nextServices(items: CollectionItem[]) {
  return items.filter((item) => item.approved !== false && item.isApproved !== false).slice().sort((left, right) => serviceScheduleDate(left).getTime() - serviceScheduleDate(right).getTime()).slice(0, 3);
}

function mediaUrl(item: CollectionItem) {
  return item.videoUrl || item.audioUrl || item.bookUrl || item.mediaUrl || "";
}

function isApprovedMedia(item: CollectionItem) {
  return item.approved !== false && item.isApproved !== false;
}

function latestMedia(videos: CollectionItem[], audios: CollectionItem[], books: CollectionItem[]) {
  const byIdDesc = (items: CollectionItem[]) => items.filter(isApprovedMedia).slice().sort((left, right) => Number(right.id) - Number(left.id)).slice(0, 3);
  return [
    ...byIdDesc(videos).map((item) => ({ ...item, tag: "Vídeo", mediaType: "Vídeo" as const })),
    ...byIdDesc(audios).map((item) => ({ ...item, tag: "Áudio", mediaType: "Áudio" as const })),
    ...byIdDesc(books).map((item) => ({ ...item, tag: "Livro", mediaType: "Livro" as const })),
  ];
}

function dailyDevotional(items: CollectionItem[]) {
  const approved = items.filter((item) => item.approved !== false && item.isApproved !== false).slice().sort((left, right) => (right.timestamp || 0) - (left.timestamp || 0));
  const now = new Date();
  const today = `${String(now.getDate()).padStart(2, "0")}/${String(now.getMonth() + 1).padStart(2, "0")}/${now.getFullYear()}`;
  return approved.find((item) => item.date?.trim() === today) || approved[0] || null;
}

function openMedia(item: CollectionItem) {
  const url = mediaUrl(item);
  if (!url) { toast.message("Este conteúdo ainda não possui um link de abertura."); return; }
  window.open(url, "_blank", "noopener,noreferrer");
}

function useLiveCollection(collectionName: string, fallback: ContentCard[]) {
  const [items, setItems] = useState<CollectionItem[]>(fallback);
  useEffect(() => {
    if (!firebaseEnabled) return;
    return listenToCollection<CollectionItem>(collectionName, (remote) => {
      if (remote.length) setItems(remote);
    }, () => undefined);
  }, [collectionName]);
  return items;
}

function useLiveDocument<T extends Record<string, unknown>>(collectionName: string, documentName: string) {
  const [item, setItem] = useState<(T & { id: string }) | null>(null);
  useEffect(() => listenToDocument<T>(collectionName, documentName, setItem, () => undefined), [collectionName, documentName]);
  return item;
}

function AppHeader({ onMenu, onNotifications, session, onProfile }: { onMenu: () => void; onNotifications: () => void; session: PwaSession | null; onProfile: () => void }) {
  return (
    <header className="app-header">
      <button className="brand-lockup" onClick={() => window.scrollTo({ top: 0, behavior: "smooth" })} aria-label="MIC Rhema — início">
        <img src={ASSETS.logo} alt="Símbolo MIC Rhema" />
        <span><b>MIC</b> RHEMA<small>IGREJA EM MOVIMENTO</small></span>
      </button>
      <div className="header-actions">
        <button className="icon-button" aria-label="Ativar notificações" onClick={onNotifications}><Bell size={19} /></button>
        <button className="avatar-button" aria-label="Abrir perfil" onClick={onProfile}>{session ? session.name.slice(0, 1).toUpperCase() : <Menu size={19} />}</button>
        <button className="mobile-menu-button" aria-label="Abrir menu" onClick={onMenu}><Menu size={20} /></button>
      </div>
    </header>
  );
}

function Hero({ onBible }: { onBible: () => void }) {
  return (
    <section className="hero-panel">
      <img src={ASSETS.hero} alt="Bíblia aberta em um santuário iluminado" />
      <div className="hero-shade" />
      <div className="hero-content">
        <p className="eyebrow text-white/70">PALAVRA PARA HOJE</p>
        <h1>Há luz para o próximo passo.</h1>
        <p>Comece onde a história começa e continue no ritmo da sua semana.</p>
        <button className="hero-cta" onClick={onBible}>Ler Gênesis 1 <ArrowRight size={18} /></button>
      </div>
      <div className="hero-marker"><span>01</span><i /></div>
    </section>
  );
}

function HorizontalCards({ items, onOpen, compact = false }: { items: CollectionItem[]; onOpen: (item: CollectionItem) => void; compact?: boolean }) {
  return <div className={`horizontal-cards ${compact ? "compact" : ""}`}>{items.slice(0, 6).map((item) => (
    <button className="content-card" key={item.id} onClick={() => onOpen(item)}>
      <img src={contentImage(item)} alt="" />
      <span className="image-vignette" />
      <div><em>{item.tag || "MIC RHEMA"}</em><strong>{item.title || item.name}</strong><small>{item.subtitle || item.description || "Conteúdo para você"}</small></div>
    </button>
  ))}</div>;
}

function HomeView({ onNavigate }: { onNavigate: (view: AppView) => void }) {
  const news = useLiveCollection("bible_news", sampleNews);
  const devotionals = useLiveCollection("devocionais", []);
  const videos = useLiveCollection("conteudos_videos", sampleMedia);
  const audios = useLiveCollection("conteudos_audios", []);
  const books = useLiveCollection("conteudos_books", []);
  const services = useLiveCollection("cultos_agenda", []);
  const [liveBanners, setLiveBanners] = useState<CarouselBanner[]>([]);
  const legacyHomeBanners = useLiveDocument<HomeBannersFallback>("settings", "home_banners");
  const appBannerSettings = useLiveDocument<AppBannerSettings>("settings", "app");
  const [expandedItem, setExpandedItem] = useState<CollectionItem | null>(null);
  const [moodOpen, setMoodOpen] = useState(false);
  const [bannerIndex, setBannerIndex] = useState(0);
  const [selectedEventInfo, setSelectedEventInfo] = useState<string | null>(null);
  const [selectedService, setSelectedService] = useState<CollectionItem | null>(null);
  const [selectedDevotional, setSelectedDevotional] = useState<CollectionItem | null>(null);
  useEffect(() => listenToCollection<CarouselBanner>("carousel_items", setLiveBanners, () => undefined), []);
  const bannerDate = new Date().toISOString().slice(0, 10);
  const banners = useMemo<CarouselBanner[]>(() => {
    const active = liveBanners.filter((banner) => !banner.eventDate || banner.eventDate >= bannerDate).filter((banner) => Boolean(banner.imageUrl));
    if (active.length) return active;
    return (legacyHomeBanners?.urls || []).filter(Boolean).map<CarouselBanner>((imageUrl, index) => ({ id: `legacy-${index}`, imageUrl }));
  }, [bannerDate, legacyHomeBanners?.urls, liveBanners]);
  const bannerIds = banners.map((banner) => banner.id).join("|");
  const bannerRotationMillis = Math.min(12, Math.max(3, Number(appBannerSettings?.bannerRotationSeconds) || 6)) * 1000;
  useEffect(() => {
    setBannerIndex((current) => banners.length ? current % banners.length : 0);
  }, [bannerIds, banners.length]);
  useEffect(() => {
    if (banners.length < 2) return;
    const timer = window.setInterval(() => setBannerIndex((current) => (current + 1) % banners.length), bannerRotationMillis);
    return () => window.clearInterval(timer);
  }, [bannerIds, bannerRotationMillis, banners.length]);
  const banner = banners[bannerIndex];
  const latestNews = useMemo(() => {
    const featured = news.filter((item) => item.featured === true);
    return (featured.length ? featured : news).slice().sort((left, right) => (right.publishedAt || 0) - (left.publishedAt || 0)).slice(0, 5);
  }, [news]);
  const upcomingServices = useMemo(() => nextServices(services), [services]);
  const media = useMemo(() => latestMedia(videos, audios, books), [videos, audios, books]);
  const devotional = useMemo(() => dailyDevotional(devotionals), [devotionals]);
  return <section className="android-home">
    <header className="android-home-greeting"><h1>Seja bem-vindo à Rhema</h1><p>Que a paz do Senhor esteja com você</p></header>
    <section className="android-banner-wrap" aria-label="Destaques da igreja">
      {banner ? <button className="android-banner-card" onClick={() => banner.eventInfo?.trim() && setSelectedEventInfo(banner.eventInfo)} disabled={!banner.eventInfo?.trim()} aria-label={banner.eventInfo?.trim() ? "Abrir informações do destaque" : "Destaque da igreja"}>
        <img src={banner.imageUrl} alt={banner.title || "Destaque MIC Rhema"} /><span className="android-banner-shade" />
        {(banner.tag || banner.title || banner.description) && <span className="android-banner-copy">{banner.tag && <small>{banner.tag}</small>}{banner.title && <strong>{banner.title}</strong>}{banner.description && <em>{banner.description}</em>}</span>}
      </button> : <div className="android-banner-card android-banner-loading" aria-live="polite"><span>Carregando destaques da igreja…</span></div>}
      {banners.length > 1 && <div className="android-banner-indicators">{banners.map((item, index) => <button aria-label={`Ir para destaque ${index + 1}`} className={index === bannerIndex ? "is-selected" : ""} onClick={() => setBannerIndex(index)} key={item.id} />)}</div>}
    </section>
    <button className="android-mood-card" onClick={() => setMoodOpen(true)}><span className="android-mood-icon">♡</span><span><strong>Como você está se sentindo hoje?</strong><small>Escolha uma palavra e encontre um plano para este momento.</small></span><ChevronRight size={20} /></button>
    <section className="android-quick-actions" aria-label="Atalhos rápidos">
      <button onClick={() => onNavigate("bible")}><BookOpen size={23}/><span>Bíblia</span></button>
      <button onClick={() => onNavigate("prayer")}><Heart size={23}/><span>Pedidos</span></button>
      <button onClick={() => onNavigate("plans")}><CalendarDays size={23}/><span>Planos</span></button>
      <button onClick={() => onNavigate("members")}><Users size={23}/><span>Membros</span></button>
    </section>
    {devotional ? <button className="android-devotional-card" onClick={() => setSelectedDevotional(devotional)}><span><small>DEVOCIONAL DIÁRIO</small><strong>{devotional.title || "Palavra para hoje"}</strong><em>{devotional.date || devotional.verseReference || devotional.content || "Leia, reflita e continue firme na Palavra."}</em></span><span className="android-read-link">Ler <ChevronRight size={17}/></span></button> : <button className="android-devotional-card" onClick={() => onNavigate("devotionals")}><span><small>DEVOCIONAL DIÁRIO</small><strong>Carregando a Palavra de hoje</strong><em>Assim que a leitura estiver disponível, ela aparecerá aqui.</em></span><span className="android-read-link">Ler <ChevronRight size={17}/></span></button>}
    <section className="android-home-section"><SectionHeading title="Notícias Bíblicas" action="Ver todas" onAction={() => onNavigate("news")} /><div className="android-horizontal-list">{latestNews.map((item) => <button className="android-news-card" key={item.id} onClick={() => setExpandedItem(item)}><img src={contentImage(item)} alt=""/><span><strong>{item.title || item.name}</strong><small>{item.category || item.tag || "Notícia bíblica"}</small><em>{newsReference(item)}</em></span></button>)}</div></section>
    <section className="android-home-section"><SectionHeading title="Próximos Cultos" action="Ver" onAction={() => onNavigate("cultos")} /><div className="android-horizontal-list">{upcomingServices.map((item) => { const date = formatServiceDate(item); return <button className="android-service-card" key={item.id} onClick={() => setSelectedService(item)}><span><small>{date.day}</small><b>{date.number}</b></span><em><strong>{item.title || "Culto"}</strong><small>{item.time || "Horário a confirmar"}</small></em></button>; })}</div></section>
    <section className="android-home-section"><SectionHeading title="Mídia" action="Ver todas" onAction={() => onNavigate("media")} /><div className="android-horizontal-list">{media.map((item) => <button className="android-media-card" key={`${item.mediaType}-${item.id}`} onClick={() => openMedia(item)}><img src={contentImage(item)} alt=""/>{item.mediaType === "Vídeo" ? <Video size={18}/> : item.mediaType === "Áudio" ? <Headphones size={18}/> : <BookOpen size={18}/>}<strong>{item.title || item.name}</strong><small>{item.mediaType}</small></button>)}</div></section>
    <InstallCard />
    {moodOpen && <div className="android-sheet-backdrop" onMouseDown={() => setMoodOpen(false)}><section className="android-mood-sheet" onMouseDown={(event) => event.stopPropagation()}><button className="dialog-close" onClick={() => setMoodOpen(false)}><X size={19}/></button><h2>Como está seu coração hoje?</h2><p>Escolha uma opção para encontrar uma leitura adequada para este momento.</p><div>{[["😊", "Feliz"], ["😟", "Ansioso"], ["😔", "Triste"], ["🙏", "Preciso de esperança"], ["😌", "Em paz"], ["😤", "Irritado"]].map(([emoji, label]) => <button key={label} onClick={() => { setMoodOpen(false); onNavigate("plans"); toast.message(`Plano de ${label.toLowerCase()} selecionado.`); }}><span>{emoji}</span>{label}</button>)}</div></section></div>}
    {selectedEventInfo && <div className="android-sheet-backdrop" onMouseDown={() => setSelectedEventInfo(null)}><section className="android-event-sheet" onMouseDown={(event) => event.stopPropagation()}><button className="dialog-close" onClick={() => setSelectedEventInfo(null)}><X size={19}/></button><p>DESTAQUE DA IGREJA</p><h2>Informações do evento</h2><div>{selectedEventInfo}</div></section></div>}
    {selectedService && <ServiceDialog item={selectedService} onClose={() => setSelectedService(null)} />}
    {selectedDevotional && <DevotionalDialog item={selectedDevotional} onClose={() => setSelectedDevotional(null)} />}
    {expandedItem && <ContentDialog item={expandedItem} onClose={() => setExpandedItem(null)} />}
  </section>;
}

function BibleView() {
  const [book, setBook] = useState("Gênesis");
  const [chapter, setChapter] = useState(1);
  const [saved, setSaved] = useState(false);
  return <section className="bible-view">
    <div className="bible-top"><p className="eyebrow">CÓDICE CONTEMPORÂNEO</p><button onClick={() => toast.message("Use a busca para encontrar uma passagem.")}><Search size={17}/> Buscar</button></div>
    <div className="bible-heading"><h1>{book} <span>{chapter}</span></h1><p>Nova Almeida Atualizada</p></div>
    <div className="book-rail">{bibleBooks.map((name) => <button key={name} onClick={() => { setBook(name); setChapter(1); }} className={book === name ? "chosen" : ""}>{name}</button>)}</div>
    <div className="chapter-strip" aria-label="Capítulos">{Array.from({ length: book === "Gênesis" ? 12 : 8 }, (_, index) => index + 1).map((number) => <button className={chapter === number ? "current" : ""} onClick={() => setChapter(number)} key={number}>{number}</button>)}</div>
    <div className="verse-paper">
      <p className="bible-kicker">{book.toUpperCase()} · CAPÍTULO {chapter}</p>
      {genesisVerses.map((verse, index) => <p className="verse" key={verse}><sup>{index + 1}</sup>{verse}</p>)}
      <button className={`save-passage ${saved ? "saved" : ""}`} onClick={() => { setSaved(!saved); toast.success(saved ? "Passagem removida dos salvos." : "Passagem salva para revisitar."); }}><Heart size={17} fill={saved ? "currentColor" : "none"}/>{saved ? "Salvo" : "Salvar passagem"}</button>
    </div>
    <div className="chapter-nav"><button onClick={() => setChapter(Math.max(1, chapter - 1))}><ChevronLeft size={18}/> Anterior</button><span>{book} {chapter}</span><button onClick={() => setChapter(chapter + 1)}>Próximo <ChevronRight size={18}/></button></div>
  </section>;
}

function NewsView() {
  const items = useLiveCollection("bible_news", sampleNews);
  const [selectedItem, setSelectedItem] = useState<CollectionItem | null>(null);
  const editorialItems = useMemo(() => {
    const featured = items.filter((item) => item.featured === true);
    return (featured.length ? featured : items).slice().sort((left, right) => (right.publishedAt || 0) - (left.publishedAt || 0));
  }, [items]);
  return <section className="page-pad android-module"><PageIntro eyebrow="NOTÍCIAS BÍBLICAS" title="Palavra que alcança a semana" text="As notícias cadastradas pela igreja aparecem aqui em tempo real." /><div className="android-list-cards android-news-list">{editorialItems.map((item) => <button key={item.id} onClick={() => setSelectedItem(item)}><img src={contentImage(item)} alt=""/><div><strong>{item.title || item.name}</strong><small>{item.category || item.tag || "Notícia bíblica"} · {newsReference(item)}</small></div><ChevronRight size={19}/></button>)}</div>{selectedItem && <ContentDialog item={selectedItem} onClose={() => setSelectedItem(null)} />}</section>;
}

function MediaView() {
  const videos = useLiveCollection("conteudos_videos", sampleMedia);
  const audios = useLiveCollection("conteudos_audios", []);
  const books = useLiveCollection("conteudos_books", []);
  const [filter, setFilter] = useState("Tudo");
  const groups = ["Tudo", "Vídeo", "Áudio", "Livro"];
  const items = useMemo(() => latestMedia(videos, audios, books).sort((left, right) => Number(right.id) - Number(left.id)), [videos, audios, books]);
  const filtered = filter === "Tudo" ? items : items.filter((item) => item.mediaType === filter);
  return <section className="page-pad"><PageIntro eyebrow="MÍDIA" title="Conteúdo para acompanhar sua semana" text="Vídeos, áudios e livros cadastrados pela igreja aparecem aqui em tempo real." />
    <div className="filter-pills">{groups.map((group) => <button className={filter === group ? "selected" : ""} onClick={() => setFilter(group)} key={group}>{group === "Tudo" ? group : `${group}s`}</button>)}</div>
    <div className="media-grid">{filtered.map((item) => <article key={`${item.mediaType}-${item.id}`} className="media-tile"><img src={contentImage(item)} alt=""/><span>{item.mediaType === "Vídeo" ? <Video size={15}/> : item.mediaType === "Áudio" ? <Headphones size={15}/> : <FileText size={15}/>} {item.mediaType}</span><div><h3>{item.title || item.name}</h3><p>{item.description || item.artist || item.author || "Disponível para você"}</p></div><button onClick={() => openMedia(item)} aria-label={`Abrir ${item.title || item.name}`}><Play size={16} fill="currentColor"/></button></article>)}</div>
  </section>;
}

function IbrView({ session, onLogin }: { session: PwaSession | null; onLogin: () => void }) {
  if (!session?.isIbr) return <section className="ibr-gate"><div className="ibr-seal"><School size={36}/></div><p className="eyebrow">INSTITUTO BÍBLICO RHEMA</p><h1>Formação para quem quer aprofundar.</h1><p>Os cursos, módulos e progresso aparecem para alunos aprovados no IBR.</p><button className="solid-button" onClick={onLogin}>{session ? "Solicitar acesso ao IBR" : "Entrar para acessar"}<ArrowRight size={17}/></button><span>Já é aluno? Entre com o mesmo nome e telefone aprovados no aplicativo.</span></section>;
  return <section className="page-pad"><PageIntro eyebrow="INSTITUTO BÍBLICO RHEMA" title={`Bem-vindo de volta, ${session.name.split(" ")[0]}.`} text="Continue sua formação no ponto em que parou." />
    <div className="course-progress"><p>SEU PROGRESSO</p><h2>Fundamentos da fé</h2><span>3 de 8 módulos concluídos</span><div className="progress-line"><i /></div><button className="solid-button" onClick={() => toast.message("A primeira aula será aberta aqui.")}>Continuar o módulo <ArrowRight size={17}/></button></div>
    <SectionHeading eyebrow="CURSOS DISPONÍVEIS" title="Sua formação" />
    <div className="course-list">{["Fundamentos da fé", "Panorama bíblico", "Vida no Espírito"].map((course, index) => <button key={course} onClick={() => toast.message(`Abrindo ${course}.`)}><span>0{index + 1}</span><div><strong>{course}</strong><small>{index === 0 ? "Em andamento" : "Novo curso"}</small></div><ChevronRight size={19}/></button>)}</div>
  </section>;
}

function MenuView({ session, onNavigate, onLogin }: { session: PwaSession | null; onNavigate: (view: AppView) => void; onLogin: () => void }) {
  return <section className="page-pad menu-view"><div className="menu-profile"><img src={ASSETS.logo} alt=""/><div><p className="eyebrow">{session ? "SUA CONTA" : "COMUNIDADE MIC RHEMA"}</p><h1>{session ? session.name : "Você já faz parte?"}</h1><button onClick={() => session ? onNavigate("profile") : onLogin()}>{session ? "Ver meu perfil" : "Entrar ou solicitar acesso"}<ArrowRight size={16}/></button></div></div>
    <div className="menu-list">{menuItems.map(({ id, label, note, icon: Icon }) => <button key={id} onClick={() => onNavigate(id)}><Icon size={21}/><div><strong>{label}</strong><small>{note}</small></div><ChevronRight size={18}/></button>)}
      <button className="admin-entry" onClick={() => onNavigate("admin")}><LockKeyhole size={21}/><div><strong>Área ADM</strong><small>Conteúdos, membros e painel</small></div><ChevronRight size={18}/></button>
    </div>
  </section>;
}

function ProfileView({ session, onLogin, onLogout }: { session: PwaSession | null; onLogin: () => void; onLogout: () => void }) {
  if (!session) return <section className="ibr-gate"><div className="ibr-seal"><CircleUserRound size={36}/></div><p className="eyebrow">MEU PERFIL</p><h1>Seu caminho também tem história.</h1><p>Entre para escolher avatar, acompanhar emblemas, salvos e preferências.</p><button className="solid-button" onClick={onLogin}>Entrar na minha conta <ArrowRight size={17}/></button></section>;
  const chooseAvatar = async () => { try { await savePwaProfile(session.uid, { avatarId: "priscila", avatarUpdatedFrom: "pwa" }); toast.success("Avatar salvo no seu perfil."); } catch (error) { toast.error(error instanceof Error ? error.message : "Não foi possível salvar o avatar."); } };
  return <section className="page-pad profile-view"><div className="profile-hero"><button className="badge-frame" onClick={chooseAvatar} aria-label="Escolher avatar bíblico"><img src={ASSETS.logo} alt="Avatar bíblico escolhido"/></button><p className="eyebrow">CAMINHANTE</p><h1>{session.name}</h1><span><BadgeCheck size={16}/> Membro MIC Rhema</span></div>
    <div className="badge-feature"><Crown size={22}/><div><p>PRÓXIMO EMBLEMA</p><strong>Guardião da Palavra</strong><span>Conclua mais 2 leituras bíblicas nesta semana.</span></div><ChevronRight size={18}/></div>
    <SectionHeading eyebrow="SEU ESPAÇO" title="Continue cuidando" />
    <div className="profile-actions"><button onClick={() => toast.message("Seus salvos serão sincronizados com sua conta.")}><Heart size={19}/><span>Salvos</span><small>Passagens e conteúdos</small></button><button onClick={() => toast.message("Histórico de leitura disponível.")}><BookOpen size={19}/><span>Leituras</span><small>Retome de onde parou</small></button></div><button className="profile-logout" onClick={onLogout}>Sair da minha conta</button>
  </section>;
}

function SettingsView({ session }: { session: PwaSession | null }) {
  const [open, setOpen] = useState<string | null>(null);
  const [allOpen, setAllOpen] = useState(false);
  const selectAll = () => { setAllOpen(!allOpen); setOpen(null); };
  const savePreference = async (key: string, value: boolean) => { if (!session) return; try { await savePwaProfile(session.uid, { pwaSettings: { [key]: value } }); } catch (_) { toast.error("Não foi possível sincronizar esta preferência agora."); } };
  return <section className="page-pad settings-view"><PageIntro eyebrow="CONFIGURAÇÕES" title="Do seu jeito, sem ruído" text="Suas escolhas ficam no aparelho e, com conta conectada, acompanham seu perfil." />
    <div className="settings-actions"><button onClick={selectAll}>{allOpen ? "Minimizar tudo" : "Expandir tudo"}</button><span>{firebaseEnabled ? "Sincronização disponível" : "Modo local de demonstração"}</span></div>
    <div className="settings-accordions">{settingsSections.map(([title, summary], index) => { const isOpen = allOpen || open === title; return <section className="setting-section" key={title}><button onClick={() => setOpen(isOpen ? null : title)}><div><span>0{index + 1}</span><strong>{title}</strong><small>{summary}</small></div><ChevronDown className={isOpen ? "rotated" : ""} size={19}/></button>{isOpen && <div className="setting-body"><p>{summary}. Esta preferência será aplicada pela PWA assim que o respectivo módulo estiver conectado à conta.</p><label><input type="checkbox" defaultChecked={index === 0 || index === 4} onChange={(event) => savePreference(title, event.target.checked)}/><span>Ativar esta preferência</span></label></div>}</section>; })}</div>
  </section>;
}

function AdminView({ session, onLogin }: { session: PwaSession | null; onLogin: () => void }) {
  if (!session?.isAdmin) return <section className="ibr-gate admin-gate"><div className="ibr-seal"><ShieldCheck size={36}/></div><p className="eyebrow">ÁREA ADMINISTRATIVA</p><h1>Gestão da igreja, com proteção.</h1><p>Entre com o acesso administrativo para ver membros, solicitações e conteúdo publicado.</p><button className="solid-button" onClick={onLogin}>Entrar como administrador <LockKeyhole size={17}/></button></section>;
  const pending = useLiveCollection("acessos_pendentes", []);
  const content = useLiveCollection("conteudos_videos", []);
  const [showCreate, setShowCreate] = useState(false);
  const approveFirst = async () => { const member = pending.find((item) => item.isApproved !== true); if (!member) return toast.message("Nenhuma solicitação pendente agora."); try { await approveMemberRequest(member.id); toast.success(`${member.name || "Membro"} aprovado.`); } catch (error) { toast.error(error instanceof Error ? error.message : "Não foi possível aprovar agora."); } };
  const cards = [["Solicitações", String(pending.filter((item) => item.isApproved !== true).length), Users], ["Conteúdos ativos", String(content.length), ImageIcon], ["Alunos IBR", String(pending.filter((item) => item.isIbr === true).length), School], ["Painel", "Abrir", LayoutDashboard]] as const;
  return <section className="page-pad admin-view"><PageIntro eyebrow="ÁREA ADM" title="Tudo em ordem para servir melhor" text="Os dados são sincronizados em tempo real com o aplicativo Android." />
    <div className="admin-stats">{cards.map(([label, value, Icon]) => <button key={label} onClick={() => toast.message(`${label}: conectando aos dados reais.`)}><Icon size={19}/><strong>{value}</strong><span>{label}</span></button>)}</div>
    <section className="admin-work"><p className="eyebrow">GESTÃO RÁPIDA</p><h2>O que você precisa fazer agora?</h2><button onClick={approveFirst}><Users size={20}/><span>Aprovar membros e acessos</span><ChevronRight size={18}/></button><button onClick={() => setShowCreate(true)}><Plus size={20}/><span>Adicionar conteúdo</span><ChevronRight size={18}/></button><button onClick={() => toast.message("As seções do painel serão abertas de forma recolhível.")}><Settings2 size={20}/><span>Configurar aparência e abas</span><ChevronRight size={18}/></button></section>{showCreate && <AdminContentDialog session={session} onClose={() => setShowCreate(false)} />}
  </section>;
}

function AdminContentDialog({ session, onClose }: { session: PwaSession; onClose: () => void }) {
  const [title, setTitle] = useState(""); const [description, setDescription] = useState(""); const [mediaUrl, setMediaUrl] = useState(""); const [busy, setBusy] = useState(false);
  const publish = async (event: React.FormEvent) => { event.preventDefault(); setBusy(true); try { await createAdminContent({ collectionName: "conteudos_videos", title, description, mediaUrl }); try { const result = await sendPwaPush({ title: `Foi adicionado o vídeo: ${title.trim()}`, body: description.trim() || "Abra o MIC Rhema para assistir.", link: "https://bichocutela.github.io/Mic-RHEMA-Ia-Studio-Google/" }); toast.success(`Conteúdo publicado e aviso enviado para ${result.sent} PWA(s).`); } catch (notificationError) { toast.message(notificationError instanceof Error ? notificationError.message : "Conteúdo publicado; o aviso poderá ser enviado depois."); } onClose(); } catch (error) { toast.error(error instanceof Error ? error.message : "Não foi possível publicar."); } finally { setBusy(false); } };
  return <div className="content-dialog-backdrop"><form className="auth-dialog" onSubmit={publish}><button className="dialog-close" type="button" onClick={onClose}><X size={19}/></button><p className="eyebrow">NOVO CONTEÚDO</p><h2>Publicar em mídia</h2><p>O conteúdo aparece na PWA e no app Android pelo Firestore.</p><label>Título<input value={title} onChange={(event) => setTitle(event.target.value)} required placeholder="Nome do conteúdo"/></label><label>Descrição<input value={description} onChange={(event) => setDescription(event.target.value)} required placeholder="Breve descrição"/></label><label>URL da capa (opcional)<input value={mediaUrl} onChange={(event) => setMediaUrl(event.target.value)} placeholder="https://..."/></label><button className="solid-button" disabled={busy}>{busy ? "Publicando…" : "Publicar conteúdo"}<ArrowRight size={17}/></button></form></div>;
}

function DiscipuladoView() { return <section className="page-pad"><PageIntro eyebrow="DISCIPULADO" title="Estude com calma, leve com você" text="Uma biblioteca pública para aprofundar a caminhada em qualquer lugar." /><div className="pdf-feature"><img src={ASSETS.devotional} alt="Estudo bíblico"/><div><p>ESTUDO BÍBLICO</p><h2>Começando uma nova caminhada</h2><span>PDF · 24 páginas</span><button className="solid-button" onClick={() => toast.message("O leitor de PDF será aberto dentro da PWA.")}>Ler estudo <FileText size={17}/></button></div></div></section>; }
function CultosView() { const services = useLiveCollection("cultos_agenda", []); const [selectedService, setSelectedService] = useState<CollectionItem | null>(null); const upcoming = useMemo(() => nextServices(services), [services]); return <section className="page-pad"><PageIntro eyebrow="CULTOS" title="A igreja se encontra aqui" text="A agenda cadastrada pela igreja aparece em tempo real."/><div className="schedule-list">{upcoming.map((item) => { const date = formatServiceDate(item); return <button key={item.id} onClick={() => setSelectedService(item)}><div><small>{date.day}</small><strong>{date.number}</strong></div><span><b>{item.title || "Culto"}</b><small>{item.time || "Horário a confirmar"} · {date.complete}</small></span><ChevronRight size={18}/></button>; })}</div>{selectedService && <ServiceDialog item={selectedService} onClose={() => setSelectedService(null)} />}</section>; }
function PlansView() {
  const [selectedCategory, setSelectedCategory] = useState<(typeof androidPlans)[number] | null>(null);
  const [selectedTheme, setSelectedTheme] = useState<(typeof androidPlans)[number]["themes"][number] | null>(null);
  if (selectedTheme) return <section className="page-pad"><button className="text-button" onClick={() => setSelectedTheme(null)}>← Voltar aos temas</button><PlanThemeReader theme={selectedTheme} onClose={() => setSelectedTheme(null)} /></section>;
  if (selectedCategory) return <section className="page-pad android-module"><button className="text-button" onClick={() => setSelectedCategory(null)}>← Explorar emoções</button><PageIntro eyebrow="PLANOS DE LEITURA" title={selectedCategory.name} text={`${selectedCategory.themes.length} temas oficiais disponíveis neste plano.`} /><div className="android-list-cards">{selectedCategory.themes.map((theme, index) => <button key={theme.title} onClick={() => setSelectedTheme(theme)}><span>{String(index + 1).padStart(2, "0")}</span><div><strong>{theme.title}</strong><small>{theme.verses.join(" · ")}</small></div><ChevronRight size={19}/></button>)}</div></section>;
  return <section className="page-pad android-module"><PageIntro eyebrow="PLANOS DE LEITURA" title="Explorar por emoções" text="Os mesmos 16 caminhos e 592 temas que o Android oferece." /><div className="plan-grid android-plan-categories">{androidPlans.map((category) => <button key={category.name} style={{ backgroundColor: category.color }} onClick={() => setSelectedCategory(category)}><span>{String(category.themes.length).padStart(2, "0")}</span><h2>{category.name}</h2><p>{category.themes.length} temas</p><ArrowRight size={18}/></button>)}</div></section>;
}

function DevotionalsView() { const devotionals = useLiveCollection("devocionais", []); const [selected, setSelected] = useState<CollectionItem | null>(null); const items = useMemo(() => devotionals.filter((item) => item.approved !== false && item.isApproved !== false).slice().sort((left, right) => (right.timestamp || 0) - (left.timestamp || 0)), [devotionals]); return <section className="page-pad android-module"><PageIntro eyebrow="DEVOCIONAIS" title="Palavra para todos os dias" text="As leituras cadastradas pela igreja aparecem em tempo real." /><div className="android-list-cards">{items.map((item, index) => <button key={item.id} onClick={() => setSelected(item)}><span>{String(index + 1).padStart(2, "0")}</span><div><strong>{item.title || "Devocional diário"}</strong><small>{item.date || item.verseReference || "Ler agora"}</small></div><ChevronRight size={19}/></button>)}</div>{selected && <DevotionalDialog item={selected} onClose={() => setSelected(null)} />}</section>; }
function PlanThemeReader({ theme, onClose }: { theme: (typeof androidPlans)[number]["themes"][number]; onClose: () => void }) { return <article className="android-plan-reader"><img src={theme.imageUrl} alt=""/><p className="eyebrow">PLANO DE LEITURA</p><h1>{theme.title}</h1><p className="plan-theme-verses">{theme.verses.join(" · ")}</p><p>{theme.content}</p><button className="solid-button" onClick={() => { toast.success("Tema de plano aberto."); onClose(); }}>Concluir leitura <ArrowRight size={17}/></button></article>; }

function CommunityView({ view, session, onLogin }: { view: "prayer" | "members" | "team" | "donations" | "about"; session: PwaSession | null; onLogin: () => void }) {
  const content = {
    prayer: { eyebrow: "PEDIDOS DE ORAÇÃO", title: "Conte com a igreja em oração", text: "Envie seu pedido. Ele será tratado com cuidado pela comunidade.", action: "Enviar pedido", icon: Heart },
    members: { eyebrow: "MEMBROS", title: session ? "Sua comunidade" : "Área de Membros", text: session ? "Acompanhe seus conteúdos, favoritos e acesso aprovado." : "Entre ou solicite acesso para acompanhar sua jornada.", action: session ? "Ver meu perfil" : "Entrar ou solicitar acesso", icon: Users },
    team: { eyebrow: "EQUIPE", title: "Pessoas que servem com você", text: "Conheça os ministérios e a equipe da igreja.", action: "Ver equipe", icon: Users },
    donations: { eyebrow: "DÍZIMOS E OFERTAS", title: "Generosidade que alcança pessoas", text: "Use esta área para consultar os dados de contribuição cadastrados pela igreja.", action: "Ver orientações", icon: Heart },
    about: { eyebrow: "SOBRE", title: "MIC Rhema", text: "Igreja em movimento, reunindo palavra, comunhão e formação em um só lugar.", action: "Conhecer a igreja", icon: Church },
  }[view];
  const Icon = content.icon;
  return <section className="page-pad android-module"><PageIntro eyebrow={content.eyebrow} title={content.title} text={content.text} /><article className="android-module-card"><Icon size={29}/><div><strong>{view === "members" && session ? session.name : content.title}</strong><small>{view === "members" && !session ? "Seu acesso será aprovado pela administração." : "Informações atualizadas pela igreja."}</small></div><ChevronRight size={20}/></article><button className="android-primary-action" onClick={() => view === "members" && !session ? onLogin() : toast.message(`${content.action}: este fluxo será aberto aqui.`)}>{content.action}<ArrowRight size={18}/></button></section>;
}

function PageIntro({ eyebrow, title, text }: { eyebrow: string; title: string; text: string }) { return <header className="page-intro"><p className="eyebrow">{eyebrow}</p><h1>{title}</h1><p>{text}</p></header>; }
function ServiceDialog({ item, onClose }: { item: CollectionItem; onClose: () => void }) { const date = formatServiceDate(item); return <div className="content-dialog-backdrop" role="presentation" onMouseDown={onClose}><article className="content-dialog service-dialog" role="dialog" aria-modal="true" onMouseDown={(event) => event.stopPropagation()}><button className="dialog-close" onClick={onClose}><X size={19}/></button><p className="eyebrow">{date.day} · {date.complete} · {item.time || "Horário a confirmar"}</p><h2>{item.title || "Culto"}</h2><p>{item.description || item.content || "A igreja espera você neste encontro."}</p><button className="solid-button" onClick={onClose}>Entendi <ArrowRight size={17}/></button></article></div>; }
function DevotionalDialog({ item, onClose }: { item: CollectionItem; onClose: () => void }) { return <div className="content-dialog-backdrop" role="presentation" onMouseDown={onClose}><article className="content-dialog devotional-dialog" role="dialog" aria-modal="true" onMouseDown={(event) => event.stopPropagation()}><button className="dialog-close" onClick={onClose}><X size={19}/></button><p className="eyebrow">DEVOCIONAL DIÁRIO{item.date ? ` · ${item.date}` : ""}</p><h2>{item.title || "Palavra para hoje"}</h2>{(item.verse || item.verseReference) && <blockquote>{item.verse}{item.verseReference && <footer>{item.verseReference}</footer>}</blockquote>}<p>{item.content || "A leitura estará disponível em breve."}</p><button className="solid-button" onClick={onClose}>Concluir leitura <ArrowRight size={17}/></button></article></div>; }
function ContentDialog({ item, onClose }: { item: CollectionItem; onClose: () => void }) { const isNews = Boolean(item.book || item.content); return <div className="content-dialog-backdrop" role="presentation" onMouseDown={onClose}><article className="content-dialog" role="dialog" aria-modal="true" onMouseDown={(event) => event.stopPropagation()}><button className="dialog-close" onClick={onClose}><X size={19}/></button><img src={contentImage(item)} alt=""/><p className="eyebrow">{isNews ? `${item.category || "NOTÍCIA BÍBLICA"} · ${newsReference(item)}` : item.tag || "MIC RHEMA"}</p><h2>{item.title || item.name}</h2><p>{item.content || item.summary || item.subtitle || item.description || "Conteúdo disponível no MIC Rhema."}</p><button className="solid-button" onClick={() => { toast.success(isNews ? "Notícia aberta." : "Conteúdo aberto."); onClose(); }}>{isNews ? "Ler novamente" : "Continuar"} <ArrowRight size={17}/></button></article></div>; }

function SignInDialog({ onClose, onSuccess }: { onClose: () => void; onSuccess: (session: PwaSession) => void }) {
  const [name, setName] = useState(""); const [phone, setPhone] = useState(""); const [password, setPassword] = useState(""); const [admin, setAdmin] = useState(false); const [requesting, setRequesting] = useState(false); const [busy, setBusy] = useState(false);
  const submit = async (event: React.FormEvent) => { event.preventDefault(); setBusy(true); try { const session = await signInPwa({ name: admin ? "admin" : name, phone, password: admin ? password : undefined }); onSuccess(session); toast.success(`Bem-vindo, ${session.name}.`); onClose(); } catch (error) { toast.error(error instanceof Error ? error.message : "Não foi possível entrar."); } finally { setBusy(false); } };
  const request = async () => { if (!name.trim() || phone.replace(/\D/g, "").length < 8) return toast.error("Informe nome e telefone para solicitar acesso."); setBusy(true); try { await submitPendingAccessRequest({ name, phone }); toast.success("Solicitação enviada. A administração receberá seu pedido."); setRequesting(false); } catch (error) { toast.error(error instanceof Error ? error.message : "Não foi possível enviar a solicitação."); } finally { setBusy(false); } };
  return <div className="content-dialog-backdrop" role="presentation"><form className="auth-dialog" onSubmit={submit}><button className="dialog-close" type="button" onClick={onClose}><X size={19}/></button><img src={ASSETS.logo} alt=""/><p className="eyebrow">{requesting ? "SOLICITAR ACESSO" : "ENTRAR NO MIC RHEMA"}</p><h2>{requesting ? "Vamos conhecer você." : admin ? "Acesso administrativo" : "Seu espaço na comunidade"}</h2><p>{requesting ? "Envie seu nome e telefone. A administração aprovará seu acesso no painel." : admin ? "Use o login administrativo registrado para a igreja." : "Informe o mesmo nome e telefone aprovados no aplicativo."}</p>{!admin && <label>Nome<input value={name} onChange={(e) => setName(e.target.value)} required placeholder="Seu nome"/></label>}<label>{admin ? "Telefone ou usuário" : "Telefone"}<input inputMode="tel" value={phone} onChange={(e) => setPhone(e.target.value)} required placeholder={admin ? "admin" : "(00) 00000-0000"}/></label>{admin && <label>Senha<input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required placeholder="Sua senha"/></label>}{requesting ? <button className="solid-button" type="button" disabled={busy} onClick={request}>{busy ? "Enviando…" : "Enviar solicitação"}<ArrowRight size={17}/></button> : <button className="solid-button" disabled={busy}>{busy ? "Entrando…" : "Continuar"}<ArrowRight size={17}/></button>}{!admin && <button className="text-button" type="button" onClick={() => setRequesting(!requesting)}>{requesting ? "Já fui aprovado" : "Ainda não tenho acesso"}</button>}<button className="text-button" type="button" onClick={() => { setAdmin(!admin); setRequesting(false); }}>{admin ? "Entrar como membro" : "Sou administrador"}</button></form></div>;
}

export default function Home() {
  const [view, setView] = useState<AppView>("home"); const [session, setSession] = useState<PwaSession | null>(null); const [showLogin, setShowLogin] = useState(false); const [drawerOpen, setDrawerOpen] = useState(false);
  useEffect(() => {
    const stored = localStorage.getItem("mic-rhema-pwa-session");
    if (stored) setSession(JSON.parse(stored) as PwaSession);
    if (!firebaseAuth) return;
    return onAuthStateChanged(firebaseAuth, async (user) => {
      if (!user) return;
      const claims = (await user.getIdTokenResult()).claims;
      setSession((current) => current || { uid: user.uid, name: "Membro MIC Rhema", isAdmin: claims.isAdmin === true, isIbr: claims.isIbr === true });
    });
  }, []);
  useEffect(() => {
    let unsubscribe: () => void = () => undefined;
    listenToForegroundPush((payload) => toast.message(payload.notification?.title || "MIC Rhema", { description: payload.notification?.body || "Você recebeu uma novidade." }))
      .then((cleanup) => { unsubscribe = cleanup; })
      .catch(() => undefined);
    return () => unsubscribe();
  }, []);
  const persistSession = (next: PwaSession) => { localStorage.setItem("mic-rhema-pwa-session", JSON.stringify(next)); setSession(next); };
  const enableNotifications = async () => {
    try {
      await subscribeToPwaPush();
      toast.success("Avisos ativados. Você receberá novidades mesmo com a PWA fechada.");
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Não foi possível ativar os avisos agora.");
    }
  };
  const logout = async () => { if (firebaseAuth) await signOut(firebaseAuth); localStorage.removeItem("mic-rhema-pwa-session"); setSession(null); setView("home"); toast.message("Você saiu da sua conta."); };
  const viewComponent = useMemo(() => {
    if (view === "bible") return <BibleView />; if (view === "news") return <NewsView />; if (view === "media") return <MediaView />; if (view === "devotionals") return <DevotionalsView />; if (view === "ibr") return <IbrView session={session} onLogin={() => setShowLogin(true)} />; if (view === "menu") return <MenuView session={session} onNavigate={setView} onLogin={() => setShowLogin(true)} />; if (view === "profile") return <ProfileView session={session} onLogin={() => setShowLogin(true)} onLogout={logout} />; if (view === "settings") return <SettingsView session={session} />; if (view === "admin") return <AdminView session={session} onLogin={() => setShowLogin(true)} />; if (view === "discipulado") return <DiscipuladoView />; if (view === "cultos") return <CultosView />; if (view === "plans") return <PlansView />; if (["prayer", "members", "team", "donations", "about"].includes(view)) return <CommunityView view={view as "prayer" | "members" | "team" | "donations" | "about"} session={session} onLogin={() => setShowLogin(true)} />; return <HomeView onNavigate={setView} />;
  }, [view, session]);
  return <PwaShell
    active={view}
    onNavigate={setView}
    drawerOpen={drawerOpen}
    onOpenDrawer={() => setDrawerOpen(true)}
    onCloseDrawer={() => setDrawerOpen(false)}
    onProfile={() => session ? setView("profile") : setShowLogin(true)}
    session={session}
    onNotifications={enableNotifications}
  >{viewComponent}{showLogin && <SignInDialog onClose={() => setShowLogin(false)} onSuccess={persistSession} />}</PwaShell>;
}
