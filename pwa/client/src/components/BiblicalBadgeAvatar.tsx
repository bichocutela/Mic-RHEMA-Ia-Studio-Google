import { Lock } from "lucide-react";

type BadgeVisual = {
  accent: string;
  level: number;
  style: "simple" | "seedling" | "star" | "olive" | "book" | "master" | "shield";
};

const badgeVisuals: Record<string, BadgeVisual> = {
  caminhante: { accent: "#90A4AE", level: 1, style: "simple" },
  semeador: { accent: "#66BB6A", level: 2, style: "seedling" },
  discipulo: { accent: "#42A5F5", level: 3, style: "star" },
  perseverante: { accent: "#9CCC65", level: 4, style: "olive" },
  estudante_rhema: { accent: "#FFC107", level: 5, style: "book" },
  mestre_da_palavra: { accent: "#FFA000", level: 6, style: "master" },
  guardiao_da_fe: { accent: "#8D6E63", level: 7, style: "shield" },
  primeira_oracao: { accent: "#7E57C2", level: 1, style: "simple" },
  leitor_da_palavra: { accent: "#5C6BC0", level: 5, style: "book" },
  coracao_grato: { accent: "#EC407A", level: 3, style: "star" },
  constante: { accent: "#26A69A", level: 4, style: "olive" },
  certificado_ibr: { accent: "#AB47BC", level: 6, style: "master" },
};

const rawBase = "https://raw.githubusercontent.com/bichocutela/Mic-RHEMA-Ia-Studio-Google/main/app/src/main/res/drawable-nodpi";
const avatarUrl = (id: string) => `${rawBase}/avatar_${id}.png`;

function TopSymbol({ visual }: { visual: BadgeVisual }) {
  const { accent, style } = visual;
  if (style === "shield") {
    return <g filter="url(#badgeShadow)"><path d="M50 3 L61 8 L59 21 Q57 29 50 34 Q43 29 41 21 L39 8 Z" fill="#191919" opacity=".72"/><path d="M50 5 L59 10 L57.5 20 Q56 26 50 31 Q44 26 42.5 20 L41 10 Z" fill="none" stroke={accent} strokeWidth="3"/><path d="M50 9v17M45 15h10" stroke="white" strokeOpacity=".72" strokeWidth="1.7" strokeLinecap="round"/></g>;
  }
  if (style === "book") {
    return <g filter="url(#badgeShadow)"><path d="M49.5 9 Q43 4 35 8v13q8-3 14.5 2z" fill={accent}/><path d="M50.5 9 Q57 4 65 8v13q-8-3-14.5 2z" fill={accent}/><path d="M50 9v14" stroke="white" strokeOpacity=".75" strokeWidth="1.5"/></g>;
  }
  if (style === "master") {
    return <g filter="url(#badgeShadow)"><path d="M50 3l3.2 6.2 6.8-1.3-3.4 6 4.8 4.8-6.8 1.1-.9 6.9-5-4.7-5 4.7-.9-6.9-6.8-1.1 4.8-4.8-3.4-6 6.8 1.3z" fill={accent} stroke="white" strokeOpacity=".55" strokeWidth="1"/><path d="M31 14l2 3.7 4.2.6-3 2.9.7 4.1-3.9-1.9-3.8 1.9.7-4.1-3-2.9 4.2-.6zm38 0l2 3.7 4.2.6-3 2.9.7 4.1-3.9-1.9-3.8 1.9.7-4.1-3-2.9 4.2-.6z" fill="white" opacity=".6"/></g>;
  }
  if (style === "star") {
    return <path d="M50 5l3.7 7.4 8.2 1.2-6 5.8 1.4 8.1-7.3-3.8-7.3 3.8 1.4-8.1-6-5.8 8.2-1.2z" fill={accent} stroke="white" strokeOpacity=".55" strokeWidth="1" filter="url(#badgeShadow)"/>;
  }
  if (style === "seedling") {
    return <g filter="url(#badgeShadow)" fill={accent}><path d="M50 23c-1-9-7-14-15-14 1 8 6 14 15 14z"/><path d="M50 23c1-9 7-14 15-14-1 8-6 14-15 14z"/><path d="M50 12v14" stroke={accent} strokeWidth="3" strokeLinecap="round"/></g>;
  }
  return <g filter="url(#badgeShadow)"><circle cx="50" cy="13" r="5.4" fill={accent}/><circle cx="48.6" cy="11.4" r="1.6" fill="white" opacity=".6"/></g>;
}

function BottomMedallion({ accent, level }: { accent: string; level: number }) {
  const points = Array.from({ length: level >= 5 ? 12 : 8 }, (_, index) => {
    const angle = -Math.PI / 2 + (index * Math.PI) / (level >= 5 ? 6 : 4);
    const radius = index % 2 === 0 ? 7.2 : 3.8;
    return `${50 + Math.cos(angle) * radius},${89 + Math.sin(angle) * radius}`;
  }).join(" ");
  return <g filter="url(#badgeShadow)"><circle cx="50" cy="89" r="9.2" fill="#1f1f1f" opacity=".72"/><polygon points={points} fill={accent} stroke="white" strokeOpacity=".5" strokeWidth="1"/></g>;
}

export function BiblicalBadgeAvatar({ avatarId, badgeId, size = 64, locked = false, className = "", title }: { avatarId: string; badgeId: string; size?: number; locked?: boolean; className?: string; title?: string }) {
  const visual = badgeVisuals[badgeId] || badgeVisuals.caminhante;
  const leaves = Math.min(8, Math.max(2, visual.level + 1));
  const leafRows = Array.from({ length: leaves }, (_, index) => {
    const y = 69 - index * (43 / Math.max(1, leaves - 1));
    const x = 15 + Math.sin((index / Math.max(1, leaves - 1)) * Math.PI) * 4;
    const rotate = -34 + index * (32 / Math.max(1, leaves - 1));
    return { y, x, rotate };
  });
  return <div className={`biblical-badge-avatar ${className}`} title={title} aria-label={title} style={{ width: size, height: size, position: "relative", flex: "0 0 auto", opacity: locked ? .34 : 1, transition: "opacity .2s ease, transform .2s ease" }}>
    <svg viewBox="0 0 100 100" aria-hidden="true" style={{ position: "absolute", inset: 0, width: "100%", height: "100%", overflow: "visible" }}>
      <defs><filter id="badgeShadow" x="-40%" y="-40%" width="180%" height="180%"><feDropShadow dx="0" dy="1.2" stdDeviation="1.4" floodOpacity=".48"/></filter></defs>
      <circle cx="50" cy="50" r="44.5" fill="none" stroke="#161616" strokeOpacity=".48" strokeWidth="4.5"/>
      <circle cx="50" cy="50" r="46" fill="none" stroke={visual.accent} strokeOpacity={.18 + visual.level * .035} strokeWidth="2.2"/>
      <path d="M19 74 C7 59 8 37 24 21" fill="none" stroke="#181818" strokeOpacity=".6" strokeWidth="5.2" strokeLinecap="round"/>
      <path d="M81 74 C93 59 92 37 76 21" fill="none" stroke="#181818" strokeOpacity=".6" strokeWidth="5.2" strokeLinecap="round"/>
      <path d="M19 74 C7 59 8 37 24 21" fill="none" stroke={visual.accent} strokeWidth="3" strokeLinecap="round"/>
      <path d="M81 74 C93 59 92 37 76 21" fill="none" stroke={visual.accent} strokeWidth="3" strokeLinecap="round"/>
      <path d="M20 72 C11 57 12 39 25 24" fill="none" stroke="white" strokeOpacity=".38" strokeWidth=".8" strokeLinecap="round"/>
      {leafRows.map((leaf, index) => <g key={`l-${index}`} transform={`translate(${leaf.x} ${leaf.y}) rotate(${leaf.rotate})`}><ellipse cx="0" cy="0" rx="3.1" ry={4.2 + visual.level * .15} fill={visual.accent}/><path d="M0-2.6V2.4" stroke="white" strokeOpacity=".46" strokeWidth=".7"/></g>)}
      {leafRows.map((leaf, index) => <g key={`r-${index}`} transform={`translate(${100 - leaf.x} ${leaf.y}) rotate(${-leaf.rotate})`}><ellipse cx="0" cy="0" rx="3.1" ry={4.2 + visual.level * .15} fill={visual.accent}/><path d="M0-2.6V2.4" stroke="white" strokeOpacity=".46" strokeWidth=".7"/></g>)}
      <TopSymbol visual={visual}/><BottomMedallion accent={visual.accent} level={visual.level}/>
    </svg>
    <img src={avatarUrl(avatarId)} alt="" draggable={false} style={{ position: "absolute", left: "14%", top: "14%", width: "72%", height: "72%", borderRadius: "50%", objectFit: "cover", background: "#f4ecd8", boxShadow: "0 0 0 1px rgba(255,255,255,.36)" }} />
    {locked && <span aria-hidden="true" style={{ position: "absolute", inset: 0, display: "grid", placeItems: "center", color: "var(--muted-foreground,#666)", opacity: 1 }}><Lock size={Math.max(16, Math.round(size * .28))} strokeWidth={2.4}/></span>}
  </div>;
}
