import { useEffect, useMemo, useState } from "react";
import { BookOpen, CalendarDays, ChevronDown, ChevronLeft, ChevronRight, X } from "lucide-react";
import { listenToCollection } from "@/lib/firebase";
import { appDateKey, formatAppDate, parseAppDate, todayKey } from "@/lib/parity-utils";
import auto2027 from "@/data/android-devotionals-2027.json";
import "./AndroidParityViews.css";

type Devotional = {
  id: string; title?: string; date?: string; verse?: string; verseReference?: string; content?: string;
  timestamp?: number; approved?: boolean; isApproved?: boolean; type?: string;
};
type SortMode = "recent" | "oldest";

function approved(item: Devotional) { return item.approved !== false && item.isApproved !== false; }
function mergeAutomatic(remote: Devotional[]) {
  const base = remote.filter(approved);
  const dates = new Set(base.map((item) => appDateKey(item.date)).filter(Boolean));
  const ids = new Set(base.map((item) => item.id));
  const automatic = (auto2027 as Devotional[]).filter((item) => !ids.has(item.id) && !dates.has(appDateKey(item.date)));
  return [...base, ...automatic];
}
function dateValue(item: Devotional) { return parseAppDate(item.date)?.getTime() ?? Number(item.timestamp || 0); }

export function todayOrLatestDevotional(items: Devotional[], now = new Date()) {
  const key = todayKey(now);
  const available = mergeAutomatic(items).filter((item) => {
    const parsed = parseAppDate(item.date);
    return !parsed || parsed.getTime() <= new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
  });
  return available.find((item) => appDateKey(item.date) === key)
    || available.slice().sort((a, b) => dateValue(b) - dateValue(a))[0]
    || null;
}

export function DevotionalsParityView() {
  const [remote, setRemote] = useState<Devotional[]>([]);
  const [sort, setSort] = useState<SortMode>("recent");
  const [sortOpen, setSortOpen] = useState(false);
  const [selected, setSelected] = useState<Devotional | null>(null);
  useEffect(() => listenToCollection<Devotional>("devocionais", setRemote, () => setRemote([])), []);
  const today = useMemo(() => new Date(), []);
  const items = useMemo(() => mergeAutomatic(remote)
    .filter((item) => { const parsed = parseAppDate(item.date); return !parsed || parsed.getTime() <= new Date(today.getFullYear(), today.getMonth(), today.getDate()).getTime(); })
    .sort((a, b) => sort === "recent" ? dateValue(b) - dateValue(a) : dateValue(a) - dateValue(b)), [remote, sort, today]);

  if (selected) return <section className="parity-page devotional-reader">
    <button className="back-link" onClick={() => setSelected(null)}><ChevronLeft size={18}/> Voltar aos devocionais</button>
    <div className="parity-title"><div><p>DEVOCIONAL</p><h1>{selected.title || "Palavra para hoje"}</h1><span>{formatAppDate(selected.date)}</span></div><BookOpen size={30}/></div>
    {(selected.verseReference || selected.verse) && <article className="parity-reader"><strong>{selected.verseReference || "Referência bíblica"}</strong>{selected.verse && <p>{selected.verse}</p>}</article>}
    <article className="parity-reader" style={{ marginTop: 14 }}>{String(selected.content || "Conteúdo ainda não cadastrado.").split(/\n{2,}/).map((part, index) => <p key={index}>{part}</p>)}</article>
  </section>;

  return <section className="parity-page android-module">
    <div className="parity-title"><div><p>DEVOCIONAIS</p><h1>Devocionais</h1><span>Do devocional diário mais recente aos anteriores.</span></div><BookOpen size={30}/></div>
    <div style={{ display: "flex", justifyContent: "flex-end", marginBottom: 14, position: "relative" }}>
      <button className="back-link" onClick={() => setSortOpen((value) => !value)}><CalendarDays size={17}/>{sort === "recent" ? "Mais recente" : "Mais antigo"}<ChevronDown size={17}/></button>
      {sortOpen && <div style={{ position: "absolute", top: 42, right: 0, zIndex: 20, minWidth: 190, borderRadius: 14, padding: 8, background: "var(--card, #fffdf7)", boxShadow: "0 12px 30px rgba(0,0,0,.18)" }}>
        <button className="back-link" style={{ width: "100%", padding: 10 }} onClick={() => { setSort("recent"); setSortOpen(false); }}>Mais recente</button>
        <button className="back-link" style={{ width: "100%", padding: 10 }} onClick={() => { setSort("oldest"); setSortOpen(false); }}>Mais antigo</button>
      </div>}
    </div>
    {!items.length ? <p className="parity-status">Nenhum devocional disponível.</p> : <div className="android-list-cards">{items.map((item) => <button key={item.id} onClick={() => setSelected(item)}><span><CalendarDays size={18}/></span><div><strong>{item.title || "Devocional"}</strong><small>{formatAppDate(item.date)}{item.verseReference ? ` · ${item.verseReference}` : ""}</small></div><ChevronRight size={19}/></button>)}</div>}
  </section>;
}
