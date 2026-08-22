/**
 * SANTUÁRIO EM MOVIMENTO — Navegação móvel azul-petróleo, editorial e comunitária.
 * A camada inferior aproxima a PWA da estrutura do app Android sem esconder o conteúdo.
 */
import { BookOpen, Grid2X2, Home, Menu, Radio, School, type LucideIcon } from "lucide-react";

export type AppView = "home" | "bible" | "media" | "ibr" | "menu" | "profile" | "settings" | "admin" | "discipulado" | "cultos" | "plans";

const items: Array<{ id: AppView; label: string; icon: LucideIcon }> = [
  { id: "home", label: "Início", icon: Home },
  { id: "bible", label: "Bíblia", icon: BookOpen },
  { id: "media", label: "Mídia", icon: Radio },
  { id: "ibr", label: "IBR", icon: School },
  { id: "menu", label: "Mais", icon: Menu },
];

export function PwaShell({
  children,
  active,
  onNavigate,
}: {
  children: React.ReactNode;
  active: AppView;
  onNavigate: (view: AppView) => void;
}) {
  return (
    <div className="app-shell min-h-dvh bg-[#f7f2ea] text-[#1c292c]">
      <main className="mx-auto min-h-dvh w-full max-w-[760px] pb-28">{children}</main>
      <nav className="bottom-dock" aria-label="Navegação principal">
        {items.map(({ id, label, icon: Icon }) => (
          <button
            className={`bottom-nav-item ${active === id ? "is-active" : ""}`}
            key={id}
            onClick={() => onNavigate(id)}
            aria-current={active === id ? "page" : undefined}
          >
            <Icon size={20} strokeWidth={active === id ? 2.4 : 1.8} />
            <span>{label}</span>
          </button>
        ))}
      </nav>
    </div>
  );
}

export function SectionHeading({ eyebrow, title, action, onAction }: { eyebrow?: string; title: string; action?: string; onAction?: () => void }) {
  return (
    <div className="section-heading">
      <div>
        {eyebrow && <p className="eyebrow">{eyebrow}</p>}
        <h2>{title}</h2>
      </div>
      {action && <button className="text-action" onClick={onAction}>{action}<Grid2X2 size={14} /></button>}
    </div>
  );
}
