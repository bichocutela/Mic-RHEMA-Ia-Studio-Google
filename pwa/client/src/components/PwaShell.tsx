/**
 * PARIDADE ANDROID — Casca Material 3: fundo creme, barra flutuante bege,
 * item ativo azul-marinho e drawer agrupado igual ao MainActivity Android.
 */
import { useState } from "react";
import {
  BookOpen, ChevronDown, ChevronRight, CircleUserRound, FileText, Grid2X2, HandHeart,
  Heart, Home, Info, Landmark, LockKeyhole, Menu as MenuIcon, PlayCircle, School,
  Settings, Users, type LucideIcon,
} from "lucide-react";

export type AppView =
  | "home" | "bible" | "news" | "devotionals" | "media" | "ibr" | "menu" | "profile"
  | "settings" | "admin" | "discipulado" | "cultos" | "plans" | "prayer"
  | "members" | "team" | "donations" | "about";

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

function AndroidDrawer({
  active, onNavigate, onProfile, onClose, session, onNotifications,
}: {
  active: AppView;
  onNavigate: (view: AppView) => void;
  onProfile: () => void;
  onClose: () => void;
  session: { name: string; isAdmin: boolean } | null;
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
  session: { name: string; isAdmin: boolean } | null;
  onNotifications: () => void;
}) {
  return (
    <div className="android-app-shell">
      <main className="android-app-content">{children}</main>
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
