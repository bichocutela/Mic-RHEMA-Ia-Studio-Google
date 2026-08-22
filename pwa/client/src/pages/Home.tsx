/**
 * SANTUÁRIO EM MOVIMENTO — Página raiz da PWA, com ritmo editorial e Bíblia como centro espiritual.
 * Superfícies quentes, azul de santuário e foco vinho-dourado; nenhuma ação administrativa é simulada.
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
import { approveMemberRequest, createAdminContent, firebaseAuth, firebaseEnabled, listenToCollection, savePwaProfile, submitPendingAccessRequest } from "@/lib/firebase";
import { onAuthStateChanged, signOut } from "firebase/auth";
import { signInPwa, type PwaSession } from "@/lib/pwa-auth";
import { listenToForegroundPush, sendPwaPush, subscribeToPwaPush } from "@/lib/push";

type CollectionItem = ContentCard & { description?: string; imageUrl?: string; thumbnailUrl?: string; coverUrl?: string; name?: string; isApproved?: boolean; isIbr?: boolean };

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
  const media = useLiveCollection("conteudos_videos", sampleMedia);
  const [expandedItem, setExpandedItem] = useState<CollectionItem | null>(null);
  return <>
    <Hero onBible={() => onNavigate("bible")} />
    <section className="quick-ribbon" aria-label="Atalhos">
      <button onClick={() => onNavigate("bible")}><BookOpen size={18}/><span>Bíblia</span></button>
      <button onClick={() => onNavigate("plans")}><Sparkles size={18}/><span>Planos</span></button>
      <button onClick={() => onNavigate("cultos")}><CalendarDays size={18}/><span>Cultos</span></button>
      <button onClick={() => onNavigate("discipulado")}><BookHeart size={18}/><span>Estudar</span></button>
    </section>
    <section className="content-section"><SectionHeading eyebrow="CONTINUE POR AQUI" title="A Palavra permanece" action="Bíblia" onAction={() => onNavigate("bible")} />
      <button className="reading-progress" onClick={() => onNavigate("bible")}>
        <div className="scripture-mark">G</div><div><strong>Gênesis 1</strong><span>“Disse Deus: Haja luz.”</span><div className="progress-line"><i /></div></div><ChevronRight size={18}/>
      </button>
    </section>
    <section className="content-section"><SectionHeading eyebrow="NOTÍCIAS BÍBLICAS" title="Para fortalecer sua caminhada" action="Ver tudo" onAction={() => toast.message("Todas as notícias aparecem quando a sincronização Firebase estiver conectada.")} />
      <HorizontalCards items={news} onOpen={setExpandedItem} />
    </section>
    <section className="wide-feature">
      <img src={ASSETS.devotional} alt="Bíblia e caderno para devocional"/><div><p className="eyebrow">DEVOCIONAL DIÁRIO</p><h2>Uma pausa que reorganiza o dia.</h2><button onClick={() => toast.message("O devocional do dia abrirá aqui.")}>Ler agora <ChevronRight size={17}/></button></div>
    </section>
    <section className="content-section"><SectionHeading eyebrow="MÍDIA" title="Ouça, assista e reveja" action="Mídia" onAction={() => onNavigate("media")} /><HorizontalCards items={media} compact onOpen={setExpandedItem} /></section>
    <section className="content-section"><SectionHeading eyebrow="CULTOS" title="Próximo encontro" />
      <button className="service-card" onClick={() => onNavigate("cultos")}><div className="date-box"><span>DOM</span><b>24</b><small>AGO</small></div><div><strong>Culto de celebração</strong><span>19:00 · Nosso santuário</span><p>Uma noite para louvar, ouvir e caminhar em família.</p></div><ChevronRight size={20}/></button>
    </section>
    <InstallCard />
    {expandedItem && <ContentDialog item={expandedItem} onClose={() => setExpandedItem(null)} />}
  </>;
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

function MediaView() {
  const items = useLiveCollection("conteudos_videos", sampleMedia);
  const [filter, setFilter] = useState("Tudo");
  const groups = ["Tudo", "Vídeos", "Áudios", "Livros"];
  return <section className="page-pad"><PageIntro eyebrow="MÍDIA" title="Conteúdo para acompanhar sua semana" text="Vídeos, áudios e leituras reunidos para continuar perto." />
    <div className="filter-pills">{groups.map((group) => <button className={filter === group ? "selected" : ""} onClick={() => setFilter(group)} key={group}>{group}</button>)}</div>
    <div className="media-grid">{items.map((item, index) => <article key={item.id} className="media-tile"><img src={contentImage(item)} alt=""/><span>{index % 3 === 0 ? <Video size={15}/> : index % 3 === 1 ? <Headphones size={15}/> : <FileText size={15}/>}{item.tag || "MÍDIA"}</span><div><h3>{item.title || item.name}</h3><p>{item.subtitle || item.description || "Disponível para você"}</p></div><button onClick={() => toast.message("O conteúdo será aberto nesta PWA.")}><Play size={16} fill="currentColor"/></button></article>)}</div>
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
function CultosView() { return <section className="page-pad"><PageIntro eyebrow="CULTOS" title="A igreja se encontra aqui" text="Escolha um encontro e veja os detalhes preparados pela administração."/><div className="schedule-list">{[["DOM", "24", "Culto de celebração", "19:00"],["QUA", "27", "Noite de oração", "19:30"],["DOM", "31", "Culto de comunhão", "19:00"]].map(([day, date, title, hour]) => <button key={date} onClick={() => toast.message(`${title}: os detalhes serão exibidos aqui.`)}><div><small>{day}</small><strong>{date}</strong></div><span><b>{title}</b><small>{hour} · Santuário MIC Rhema</small></span><ChevronRight size={18}/></button>)}</div></section>; }
function PlansView() { return <section className="page-pad"><PageIntro eyebrow="PLANOS DE LEITURA" title="Caminhos que cabem na sua semana" text="Escolha um tema e avance no seu próprio ritmo."/><div className="plan-grid">{["Começar pela Palavra", "Esperança em tempos difíceis", "Conhecendo Jesus", "Vida no Espírito"].map((plan, index) => <button key={plan} onClick={() => toast.message(`Plano “${plan}” iniciado.`)}><span>0{index + 1}</span><h2>{plan}</h2><p>{index % 2 ? "7 dias de leitura" : "14 dias de leitura"}</p><ArrowRight size={18}/></button>)}</div></section>; }

function PageIntro({ eyebrow, title, text }: { eyebrow: string; title: string; text: string }) { return <header className="page-intro"><p className="eyebrow">{eyebrow}</p><h1>{title}</h1><p>{text}</p></header>; }
function ContentDialog({ item, onClose }: { item: CollectionItem; onClose: () => void }) { return <div className="content-dialog-backdrop" role="presentation" onMouseDown={onClose}><article className="content-dialog" role="dialog" aria-modal="true" onMouseDown={(event) => event.stopPropagation()}><button className="dialog-close" onClick={onClose}><X size={19}/></button><img src={contentImage(item)} alt=""/><p className="eyebrow">{item.tag || "MIC RHEMA"}</p><h2>{item.title || item.name}</h2><p>{item.subtitle || item.description || "Conteúdo disponível no MIC Rhema."}</p><button className="solid-button" onClick={() => { toast.success("Conteúdo aberto."); onClose(); }}>Continuar <ArrowRight size={17}/></button></article></div>; }

function SignInDialog({ onClose, onSuccess }: { onClose: () => void; onSuccess: (session: PwaSession) => void }) {
  const [name, setName] = useState(""); const [phone, setPhone] = useState(""); const [password, setPassword] = useState(""); const [admin, setAdmin] = useState(false); const [requesting, setRequesting] = useState(false); const [busy, setBusy] = useState(false);
  const submit = async (event: React.FormEvent) => { event.preventDefault(); setBusy(true); try { const session = await signInPwa({ name: admin ? "admin" : name, phone, password: admin ? password : undefined }); onSuccess(session); toast.success(`Bem-vindo, ${session.name}.`); onClose(); } catch (error) { toast.error(error instanceof Error ? error.message : "Não foi possível entrar."); } finally { setBusy(false); } };
  const request = async () => { if (!name.trim() || phone.replace(/\D/g, "").length < 8) return toast.error("Informe nome e telefone para solicitar acesso."); setBusy(true); try { await submitPendingAccessRequest({ name, phone }); toast.success("Solicitação enviada. A administração receberá seu pedido."); setRequesting(false); } catch (error) { toast.error(error instanceof Error ? error.message : "Não foi possível enviar a solicitação."); } finally { setBusy(false); } };
  return <div className="content-dialog-backdrop" role="presentation"><form className="auth-dialog" onSubmit={submit}><button className="dialog-close" type="button" onClick={onClose}><X size={19}/></button><img src={ASSETS.logo} alt=""/><p className="eyebrow">{requesting ? "SOLICITAR ACESSO" : "ENTRAR NO MIC RHEMA"}</p><h2>{requesting ? "Vamos conhecer você." : admin ? "Acesso administrativo" : "Seu espaço na comunidade"}</h2><p>{requesting ? "Envie seu nome e telefone. A administração aprovará seu acesso no painel." : admin ? "Use o login administrativo registrado para a igreja." : "Informe o mesmo nome e telefone aprovados no aplicativo."}</p>{!admin && <label>Nome<input value={name} onChange={(e) => setName(e.target.value)} required placeholder="Seu nome"/></label>}<label>{admin ? "Telefone ou usuário" : "Telefone"}<input inputMode="tel" value={phone} onChange={(e) => setPhone(e.target.value)} required placeholder={admin ? "admin" : "(00) 00000-0000"}/></label>{admin && <label>Senha<input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required placeholder="Sua senha"/></label>}{requesting ? <button className="solid-button" type="button" disabled={busy} onClick={request}>{busy ? "Enviando…" : "Enviar solicitação"}<ArrowRight size={17}/></button> : <button className="solid-button" disabled={busy}>{busy ? "Entrando…" : "Continuar"}<ArrowRight size={17}/></button>}{!admin && <button className="text-button" type="button" onClick={() => setRequesting(!requesting)}>{requesting ? "Já fui aprovado" : "Ainda não tenho acesso"}</button>}<button className="text-button" type="button" onClick={() => { setAdmin(!admin); setRequesting(false); }}>{admin ? "Entrar como membro" : "Sou administrador"}</button></form></div>;
}

export default function Home() {
  const [view, setView] = useState<AppView>("home"); const [session, setSession] = useState<PwaSession | null>(null); const [showLogin, setShowLogin] = useState(false);
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
    if (view === "bible") return <BibleView />; if (view === "media") return <MediaView />; if (view === "ibr") return <IbrView session={session} onLogin={() => setShowLogin(true)} />; if (view === "menu") return <MenuView session={session} onNavigate={setView} onLogin={() => setShowLogin(true)} />; if (view === "profile") return <ProfileView session={session} onLogin={() => setShowLogin(true)} onLogout={logout} />; if (view === "settings") return <SettingsView session={session} />; if (view === "admin") return <AdminView session={session} onLogin={() => setShowLogin(true)} />; if (view === "discipulado") return <DiscipuladoView />; if (view === "cultos") return <CultosView />; if (view === "plans") return <PlansView />; return <HomeView onNavigate={setView} />;
  }, [view, session]);
  return <PwaShell active={["profile", "settings", "admin", "discipulado", "cultos", "plans"].includes(view) ? "menu" : view} onNavigate={setView}><AppHeader onMenu={() => setView("menu")} onNotifications={enableNotifications} session={session} onProfile={() => session ? setView("profile") : setShowLogin(true)} />{viewComponent}{showLogin && <SignInDialog onClose={() => setShowLogin(false)} onSuccess={persistSession} />}</PwaShell>;
}
