/**
 * PARIDADE ANDROID — Casca Material 3: fundo creme, barra flutuante bege,
 * item ativo azul-marinho e drawer agrupado igual ao MainActivity Android.
 */
import { useEffect, useMemo, useState } from "react";
import {
  BookHeart, BookOpen, ChevronDown, ChevronRight, CircleUserRound, Copy, FileText, Grid2X2,
  HandHeart, Heart, Home, Info, Landmark, LockKeyhole, Mail, MapPin, Menu as MenuIcon,
  Phone, PlayCircle, School, Settings, Users, type LucideIcon,
} from "lucide-react";
import { listenToCollection, listenToDocument } from "@/lib/firebase";
import {
  BibleParityView, MediaParityView, MembersParityView,
  type PwaSessionLike,
} from "./AndroidParityViews";
import { AdminParityView } from "./AdminParityView";
import { PrayerParityView } from "./PrayerParityView";
import { AboutParityView as AboutParityViewSync } from "./AboutParityView";
import { SettingsParityViewV2 } from "./SettingsParityViewV2";
import { ProfileParityViewV2 } from "./ProfileParityViewV2";
import "./AndroidParityViews.css";

export type AppView =
  | "home" | "bible" | "news" | "devotionals" | "media" | "ibr" | "menu" | "profile"
  | "settings" | "admin" | "discipulado" | "cultos" | "plans" | "prayer"
  | "members" | "team" | "donations" | "about";

type TeamMember = {
  id: string;
  name?: string;
  role?: string;
  category?: string;
  imageUrl?: string;
  order?: number;
};

type DonationSettings = {
  pixKey?: string;
  qrCodeUrl?: string;
};

type DiscipuladoPdf = {
  id: string;
  title?: string;
  subtitle?: string;
  description?: string;
  category?: string;
  fileUrl?: string;
  storagePath?: string;
  order?: number;
  isPublished?: boolean;
  createdAt?: number;
};

const primaryItems: Array<{ id: AppView; label: string; icon: LucideIcon }> = [
  { id: "home", label: "Início", icon: Home },
  { id: "bible", label: "Bíblia", icon: BookOpen },
  { id: "devotionals", label: "Devocionais", icon: FileText },
  { id: "ibr", label: "IBR", icon: School },
];

const drawerGroups: Array<{ title: string; icon: LucideIcon; items: Array<{ id: AppView; label: string; icon: LucideIcon }> }> = [
  {
    title: "CONTEÚDO", icon: Grid2X2,
    items: [
      { id: "home", label: "Início", icon: Home },
      { id: "bible", label: "Bíblia", icon: BookOpen },
      { id: "devotionals", label: "Devocionais", icon: FileText },
      { id: "ibr", label: "Cursos IBR", icon: School },
      { id: "discipulado", label: "Discipulado", icon: BookHeart },
      { id: "media", label: "Mídia", icon: PlayCircle },
      { id: "plans", label: "Planos", icon: BookOpen },
    ],
  },
  {
    title: "COMUNIDADE", icon: Users,
    items: [
      { id: "prayer", label: "Pedidos de Oração", icon: HandHeart },
      { id: "members", label: "Membros", icon: Users },
      { id: "team", label: "Equipe", icon: CircleUserRound },
    ],
  },
  {
    title: "IGREJA", icon: Landmark,
    items: [
      { id: "cultos", label: "Cultos", icon: Landmark },
      { id: "donations", label: "Dízimos e Ofertas", icon: Heart },
    ],
  },
  {
    title: "SISTEMA", icon: Settings,
    items: [
      { id: "settings", label: "Configurações", icon: Settings },
      { id: "about", label: "Sobre", icon: Info },
    ],
  },
];

function TeamParityView() {
  const [members, setMembers] = useState<TeamMember[]>([]);
  const [category, setCategory] = useState("Todos");
  useEffect(() => listenToCollection<TeamMember>("equipe", setMembers, () => setMembers([])), []);
  const categories = useMemo(() => ["Todos", ...Array.from(new Set(members.map((member) => member.category?.trim()).filter(Boolean) as string[]))], [members]);
  const visible = useMemo(() => members
    .filter((member) => category === "Todos" || member.category?.toLowerCase() === category.toLowerCase())
    .slice()
    .sort((a, b) => Number(a.order || 0) - Number(b.order || 0)), [members, category]);

  return <section className="page-pad android-module">
    <div className="android-section-heading"><div><p>EQUIPE</p><h2>Nossa Equipe</h2></div></div>
    <div className="filter-pills">{categories.map((item) => <button key={item} className={category === item ? "selected" : ""} onClick={() => setCategory(item)}>{item}</button>)}</div>
    {!visible.length ? <p className="empty-module">Nenhum membro da equipe cadastrado nesta categoria.</p> : <div className="android-list-cards">{visible.map((member) => <article key={member.id} className="android-module-card">
      {member.imageUrl ? <img src={member.imageUrl} alt={`Foto de ${member.name || "membro da equipe"}`} style={{ width: 58, height: 58, borderRadius: "50%", objectFit: "cover" }} /> : <CircleUserRound size={38} />}
      <div><strong>{member.name || "Membro da equipe"}</strong><small>{[member.role, member.category].filter(Boolean).join(" · ") || "Equipe MIC Rhema"}</small></div>
    </article>)}</div>}
  </section>;
}

function DiscipuladoParityView() {
  const [items, setItems] = useState<DiscipuladoPdf[]>([]);
  useEffect(() => listenToCollection<DiscipuladoPdf>("discipulado_pdfs", setItems, () => setItems([])), []);
  const published = useMemo(() => items.filter((item) => item.isPublished !== false).slice().sort((a, b) => {
    const order = Number(a.order || 0) - Number(b.order || 0);
    return order !== 0 ? order : Number(b.createdAt || 0) - Number(a.createdAt || 0);
  }), [items]);
  const openPdf = (item: DiscipuladoPdf) => {
    if (!item.fileUrl) return;
    window.open(item.fileUrl, "_blank", "noopener,noreferrer");
  };
  return <section className="page-pad android-module">
    <div className="android-section-heading"><div><p>DISCIPULADO</p><h2>Estudos de Discipulado</h2></div></div>
    <p>PDFs publicados pela igreja para todos os usuários.</p>
    {!published.length ? <p className="empty-module">Nenhum estudo publicado ainda.</p> : <div className="android-list-cards">{published.map((item, index) => <button key={item.id} onClick={() => openPdf(item)} disabled={!item.fileUrl}>
      <span>{String(index + 1).padStart(2, "0")}</span><div><strong>{item.title || "Estudo bíblico"}</strong><small>{[item.category, item.subtitle, item.description].filter(Boolean).join(" · ") || "PDF de discipulado"}</small></div><ChevronRight size={19}/>
    </button>)}</div>}
  </section>;
}

function DonationsParityView() {
  const [settings, setSettings] = useState<(DonationSettings & { id: string }) | null>(null);
  useEffect(() => listenToDocument<DonationSettings>("settings", "donations", setSettings, () => setSettings(null)), []);
  const pixKey = settings?.pixKey?.trim() || "";
  const qrCodeUrl = settings?.qrCodeUrl?.trim() || "";
  const copyPix = async () => {
    if (!pixKey) return;
    try { await navigator.clipboard.writeText(pixKey); } catch { /* Safari antigo pode bloquear clipboard fora de HTTPS. */ }
  };
  return <section className="page-pad android-module">
    <div className="android-section-heading"><div><p>IGREJA</p><h2>Dízimos e Ofertas</h2></div></div>
    <p>Contribua com a obra de Deus.</p>
    {!pixKey && !qrCodeUrl ? <p className="empty-module">As informações de doação ainda não foram configuradas.</p> : <article className="android-module-card" style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 14 }}>
      {qrCodeUrl && <img src={qrCodeUrl} alt="QR Code Pix" style={{ width: 210, maxWidth: "100%", aspectRatio: "1 / 1", objectFit: "contain", borderRadius: 16 }} />}
      {pixKey && <><div style={{ textAlign: "center" }}><strong>Chave PIX</strong><small style={{ display: "block", marginTop: 6, wordBreak: "break-all" }}>{pixKey}</small></div><button className="android-primary-action" onClick={() => void copyPix()}><Copy size={18}/><span>Copiar Chave</span></button></>}
    </article>}
  </section>;
}

function AboutParityViewLegacy() {
  return <section className="page-pad android-module">
    <div className="android-section-heading"><div><p>SOBRE</p><h2>MIC Rhema</h2></div></div>
    <div className="android-list-cards">
      <article className="android-module-card"><CircleUserRound size={29}/><div><strong>Liderança</strong><small>Pastor Evaldo Leôncio</small></div></article>
      <article className="android-module-card"><Info size={29}/><div><strong>Nossa Missão</strong><small>Conectando Pessoas e Transformando Vidas. Rhema é a palavra revelada de Deus para um momento específico.</small></div></article>
      <article className="android-module-card"><MapPin size={29}/><div><strong>Localização</strong><small>Rua Todos os Santos – Natal/RN</small></div></article>
      <a className="android-module-card" href="tel:+5584988041804"><Phone size={29}/><div><strong>Telefone</strong><small>84 98804 1804</small></div><ChevronRight size={20}/></a>
      <a className="android-module-card" href="mailto:micrhema2@gmail.com"><Mail size={29}/><div><strong>E-mail</strong><small>micrhema2@gmail.com</small></div><ChevronRight size={20}/></a>
    </div>
  </section>;
}

function AndroidDrawer({
  active, onNavigate, onProfile, onClose, session, onNotifications,
}: {
  active: AppView;
  onNavigate: (view: AppView) => void;
  onProfile: () => void;
  onClose: () => void;
  session: PwaSessionLike;
  onNotifications: () => void;
}) {
  const [expanded, setExpanded] = useState<Set<string>>(() => new Set(["CONTEÚDO"]));
  const toggle = (title: string) => setExpanded((current) => {
    const next = new Set(current);
    if (next.has(title)) next.delete(title); else next.add(title);
    return next;
  });
  const go = (view: AppView) => { onNavigate(view); onClose(); };
  const userName = session?.name || "Entrar";
  return (
    <aside className="android-drawer" role="dialog" aria-modal="true" aria-label="Menu do MIC Rhema">
      <button className="drawer-dismiss" aria-label="Fechar menu" onClick={onClose} />
      <section className="drawer-sheet">
        <button className="drawer-profile" onClick={() => { onProfile(); onClose(); }}>
          <span className="drawer-avatar">{session ? userName.slice(0, 1).toUpperCase() : <CircleUserRound size={25} />}</span>
          <span><strong>{userName}</strong><small>{session ? "Meu Perfil" : "Solicite acesso para membros"}</small></span>
          <ChevronRight size={19} />
        </button>
        <div className="drawer-badges"><span>SEU CAMINHO</span><b>{session ? "Continue sua jornada" : "Entre para acompanhar"}</b></div>
        {drawerGroups.map((group) => {
          const GroupIcon = group.icon;
          const open = expanded.has(group.title);
          return <section className="drawer-group" key={group.title}>
            <button className="drawer-group-title" onClick={() => toggle(group.title)}>
              <span><GroupIcon size={18} />{group.title}</span><ChevronDown className={open ? "is-open" : ""} size={18} />
            </button>
            {open && <div className="drawer-items">{group.items.map(({ id, label, icon: Icon }) => <button className={active === id ? "is-current" : ""} key={id} onClick={() => go(id)}><Icon size={19} /><span>{label}</span></button>)}</div>}
          </section>;
        })}
        <section className="drawer-group drawer-admin-group">
          <button className="drawer-group-title" onClick={() => toggle("ADMINISTRAÇÃO")}>
            <span><LockKeyhole size={18} />ADMINISTRAÇÃO</span><ChevronDown className={expanded.has("ADMINISTRAÇÃO") ? "is-open" : ""} size={18} />
          </button>
          {expanded.has("ADMINISTRAÇÃO") && <div className="drawer-items"><button className={active === "admin" ? "is-current" : ""} onClick={() => go("admin")}><LockKeyhole size={19} /><span>Área ADM</span></button></div>}
        </section>
        <button className="drawer-notifications" onClick={onNotifications}><span>Ativar notificações</span><small>Escolha receber avisos desta PWA</small></button>
      </section>
    </aside>
  );
}

export function PwaShell({
  children, active, onNavigate, drawerOpen, onCloseDrawer, onOpenDrawer, onProfile, session, onNotifications,
}: {
  children: React.ReactNode;
  active: AppView;
  onNavigate: (view: AppView) => void;
  drawerOpen: boolean;
  onCloseDrawer: () => void;
  onOpenDrawer: () => void;
  onProfile: () => void;
  session: PwaSessionLike;
  onNotifications: () => void;
}) {
  const synchronizedContent = active === "bible" ? <BibleParityView />
    : active === "media" ? <MediaParityView />
    : active === "admin" && session?.isAdmin ? <AdminParityView session={session} />
    : active === "profile" && session && !session.isAdmin ? <ProfileParityViewV2 session={session} onNavigateHome={() => onNavigate("home")} />
    : active === "settings" ? <SettingsParityViewV2 session={session} onProfile={onProfile} onNotifications={onNotifications} />
    : active === "members" && !session?.isAdmin ? <MembersParityView session={session} onProfile={onProfile} />
    : active === "prayer" ? <PrayerParityView />
    : active === "team" ? <TeamParityView />
    : active === "discipulado" ? <DiscipuladoParityView />
    : active === "donations" ? <DonationsParityView />
    : active === "about" ? <AboutParityViewSync />
    : children;
  return (
    <div className="android-app-shell">
      <main className="android-app-content">{synchronizedContent}</main>
      <nav className="android-bottom-dock" aria-label="Navegação principal">
        {primaryItems.map(({ id, label, icon: Icon }) => {
          const selected = active === id;
          return <button className={selected ? "is-active" : ""} key={id} onClick={() => onNavigate(id)} aria-current={selected ? "page" : undefined}>
            <Icon size={20} strokeWidth={selected ? 2.4 : 1.9} />{selected && <span>{label}</span>}
          </button>;
        })}
        <button onClick={onOpenDrawer} aria-label="Abrir menu"><MenuIcon size={22} /></button>
      </nav>
      {drawerOpen && <AndroidDrawer active={active} onNavigate={onNavigate} onProfile={onProfile} onClose={onCloseDrawer} session={session} onNotifications={onNotifications} />}
    </div>
  );
}

export function SectionHeading({ eyebrow, title, action, onAction }: { eyebrow?: string; title: string; action?: string; onAction?: () => void }) {
  return <div className="android-section-heading"><div>{eyebrow && <p>{eyebrow}</p>}<h2>{title}</h2></div>{action && <button onClick={onAction}>{action}<ChevronRight size={16} /></button>}</div>;
}
