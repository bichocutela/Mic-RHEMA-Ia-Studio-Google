import { useEffect, useMemo, useState } from "react";
import { ChevronLeft, ChevronRight, Eye, Gift, History, RefreshCcw, ShoppingBag, Sparkles, X } from "lucide-react";
import { toast } from "sonner";
import { loadPwaXpDashboard, redeemPwaXp, type PwaXpAccount, type PwaXpDashboard, type PwaXpItem } from "@/lib/xp";
import { resolvePwaXpAssetUrl } from "@/lib/profile-customization";
import { PwaQuizPanel } from "./PwaQuizPanel";
import "./PwaXpPanel.css";

type XpSection = "summary" | "quiz" | "shop" | "history";
const XP_PAGE_SIZE = 10;
const ALL_CATEGORIES = "Todos";
const categoryPriority = (value: string) => {
  const normalized = value.toLowerCase();
  if (normalized === "efeitos de luz") return 0;
  if (normalized === "distintivos") return 1;
  if (normalized === "molduras") return 2;
  if (normalized === "emblemas") return 3;
  if (normalized === "temas") return 4;
  if (normalized.includes("personaliza")) return 5;
  if (normalized.includes("digital")) return 6;
  if (normalized.includes("físic") || normalized.includes("fisic")) return 7;
  return 20;
};

function ShopPreview({ item, onClose }: { item: PwaXpItem; onClose: () => void }) {
  const imageUrl = resolvePwaXpAssetUrl(item.image_url);
  const profileItem = item.kind === "profile";
  return <div className="xp-preview-backdrop" role="presentation" onClick={onClose}>
    <article className="xp-preview-modal" role="dialog" aria-modal="true" aria-label={`Prévia de ${item.name}`} onClick={(event) => event.stopPropagation()}>
      <button className="xp-preview-close" aria-label="Fechar" onClick={onClose}><X size={20}/></button>
      <div className="xp-preview-visual">{imageUrl ? <img src={imageUrl} alt={item.name}/> : <Sparkles size={54}/>}</div>
      <div className="xp-preview-copy"><small>{item.category || "Recompensa"}</small><h3>{item.name}</h3><p>{item.description || "Sem descrição cadastrada."}</p><strong>{item.cost} XP</strong></div>
      <div className="xp-preview-note">{profileItem ? "Após o resgate, este item aparecerá em Meu Perfil > Personalização, seguindo as mesmas regras do Android." : item.kind === "physical" ? "Produto físico: após o resgate, acompanhe o código e o status de entrega em seus resgates." : "Esta é a prévia do arquivo ou recompensa cadastrada pelo administrador."}</div>
      <button className="profile-v2-modal-back" onClick={onClose}>Fechar prévia</button>
    </article>
  </div>;
}

export function PwaXpPanel({ onAccount }: { onAccount?: (account: PwaXpAccount) => void }) {
  const [dashboard, setDashboard] = useState<PwaXpDashboard | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [redeeming, setRedeeming] = useState<string | null>(null);
  const [section, setSection] = useState<XpSection>("summary");
  const [selectedCategory, setSelectedCategory] = useState(ALL_CATEGORIES);
  const [currentPage, setCurrentPage] = useState(0);
  const [previewItem, setPreviewItem] = useState<PwaXpItem | null>(null);

  const reload = async () => {
    setLoading(true);
    setError("");
    try {
      const value = await loadPwaXpDashboard();
      setDashboard(value);
      onAccount?.(value.account);
    } catch (failure) {
      setError(failure instanceof Error ? failure.message : "Não foi possível carregar a Jornada XP.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { void reload(); }, []);

  const redeemedCounts = useMemo(() => {
    const result = new Map<string, number>();
    for (const redemption of dashboard?.redemptions || []) {
      if (redemption.status === "cancelado") continue;
      result.set(redemption.item_id, (result.get(redemption.item_id) || 0) + 1);
    }
    return result;
  }, [dashboard?.redemptions]);

  const categories = useMemo(() => [...new Set((dashboard?.items || []).map((item) => item.category || "Recompensas"))]
    .sort((a, b) => categoryPriority(a) - categoryPriority(b) || a.localeCompare(b, "pt-BR")), [dashboard?.items]);
  const filteredItems = useMemo(() => (dashboard?.items || [])
    .filter((item) => selectedCategory === ALL_CATEGORIES || (item.category || "Recompensas") === selectedCategory)
    .slice().sort((a, b) => categoryPriority(a.category || "Recompensas") - categoryPriority(b.category || "Recompensas") || a.cost - b.cost || a.name.localeCompare(b.name, "pt-BR")), [dashboard?.items, selectedCategory]);
  const totalPages = Math.max(1, Math.ceil(filteredItems.length / XP_PAGE_SIZE));
  const safePage = Math.min(currentPage, totalPages - 1);
  const pageItems = filteredItems.slice(safePage * XP_PAGE_SIZE, (safePage + 1) * XP_PAGE_SIZE);

  useEffect(() => { setCurrentPage(0); }, [selectedCategory]);
  useEffect(() => { if (currentPage !== safePage) setCurrentPage(safePage); }, [currentPage, safePage]);

  if (loading && !dashboard) {
    return <section className="parity-card xp-panel-card"><p className="parity-status">Sincronizando Jornada XP…</p></section>;
  }
  if (error && !dashboard) {
    return <section className="parity-card xp-panel-card"><p className="parity-warning">{error}</p><button className="parity-primary" onClick={() => void reload()}><RefreshCcw size={17}/> Tentar novamente</button></section>;
  }
  if (!dashboard) return null;

  const account = dashboard.account;
  const redeem = async (itemId: string, cost: number) => {
    if (redeeming) return;
    setRedeeming(itemId);
    try {
      await redeemPwaXp(itemId, cost);
      toast.success("Resgate realizado. O mesmo saldo já está disponível no Android.");
      await reload();
    } catch (failure) {
      toast.error(failure instanceof Error ? failure.message : "Não foi possível resgatar agora.");
    } finally {
      setRedeeming(null);
    }
  };

  return <section className="parity-card xp-panel-card">
    <div className="parity-title xp-panel-title"><div><p>JORNADA CENTRAL</p><h2>XP e Loja</h2><span>Mesmo saldo, Quiz, resgates e histórico do Android.</span></div><Sparkles size={27}/></div>

    <nav className="xp-section-tabs" aria-label="Categorias da Jornada XP">
      <button className={section === "summary" ? "active" : ""} onClick={() => setSection("summary")}>Resumo</button>
      <button className={section === "quiz" ? "active" : ""} onClick={() => setSection("quiz")}>Quiz</button>
      <button className={section === "shop" ? "active" : ""} onClick={() => setSection("shop")}>Loja</button>
      <button className={section === "history" ? "active" : ""} onClick={() => setSection("history")}>Histórico</button>
    </nav>

    {section === "summary" && <div className="xp-section-body">
      <div className="xp-summary-grid">
        <div className="profile-v2-stat"><strong>{account.total_earned}</strong><small>XP total</small></div>
        <div className="profile-v2-stat"><strong>{account.balance}</strong><small>Saldo XP</small></div>
        <div className="profile-v2-stat"><strong>{dashboard.streak}</strong><small>Dias seguidos</small></div>
      </div>
      <div className="xp-summary-note"><strong>Jornada sincronizada</strong><span>Seu saldo e seus resgates são os mesmos usados no Android.</span></div>
      {dashboard.entitlements.length > 0 && <div className="xp-collection-block">
        <div className="parity-title"><div><p>COLEÇÃO</p><h3>Minhas recompensas</h3></div><Gift size={22}/></div>
        <div className="xp-entitlement-list">{dashboard.entitlements.map(item => <span key={item.id}>{item.item_name}</span>)}</div>
      </div>}
    </div>}

    {section === "quiz" && <div className="xp-section-body"><PwaQuizPanel onXpChange={reload}/></div>}

    {section === "shop" && <div className="xp-section-body">
      <div className="parity-title xp-subtitle"><div><p>RECOMPENSAS</p><h3>Loja XP</h3></div><ShoppingBag size={24}/></div>
      {!dashboard.unlocked ? <p className="parity-status">A Loja XP é liberada no Nível 8 — Semente da Fé.</p> : !dashboard.items.length ? <p className="parity-status">Nenhuma recompensa disponível agora.</p> : <>
        <div className="xp-shop-category-wrap">
          <div className="xp-shop-category-header"><strong>Categorias</strong><small>{dashboard.items.length} recompensas · 10 por página</small></div>
          <div className="xp-shop-categories"><button className={selectedCategory === ALL_CATEGORIES ? "selected" : ""} onClick={() => setSelectedCategory(ALL_CATEGORIES)}>Todos ({dashboard.items.length})</button>{categories.map((category) => <button key={category} className={selectedCategory === category ? "selected" : ""} onClick={() => setSelectedCategory(category)}>{category} ({dashboard.items.filter((item) => (item.category || "Recompensas") === category).length})</button>)}</div>
        </div>
        <div className="xp-shop-page-summary"><strong>{selectedCategory === ALL_CATEGORIES ? "Todas as recompensas" : selectedCategory}</strong><small>{filteredItems.length} item{filteredItems.length === 1 ? "" : "s"} · Página {safePage + 1} de {totalPages}</small></div>
        <div className="xp-shop-list">
          {pageItems.map(item => {
            const count = redeemedCounts.get(item.id) || 0;
            const limit = Math.max(1, item.limit_per_member || 1);
            const limitReached = count >= limit;
            const soldOut = item.stock !== null && item.stock <= 0;
            const insufficient = account.balance < item.cost;
            const imageUrl = resolvePwaXpAssetUrl(item.image_url);
            const label = limitReached ? "Já resgatado" : soldOut ? "Esgotado" : insufficient ? "Saldo insuficiente" : redeeming === item.id ? "Resgatando…" : `Resgatar por ${item.cost} XP`;
            return <article key={item.id} className="xp-shop-item">
              <div className="xp-shop-item-main">
                {imageUrl ? <img src={imageUrl} alt="" loading="lazy"/> : item.kind === "profile" ? <span className="xp-shop-profile-placeholder"><Sparkles size={26}/></span> : null}
                <div><small className="xp-shop-item-category">{item.category || "Recompensa"}</small><strong>{item.name}</strong><small>{item.description}</small><b>{item.cost} XP</b>{count > 0 && <em>Resgates: {Math.min(count,limit)}/{limit}</em>}</div>
              </div>
              <div className="xp-shop-item-actions"><button className="xp-shop-preview-button" onClick={() => setPreviewItem(item)}><Eye size={17}/> Prévia</button><button className="parity-primary" disabled={limitReached || soldOut || insufficient || redeeming === item.id} onClick={() => void redeem(item.id,item.cost)}><Gift size={17}/>{label}</button></div>
              {limitReached && item.kind === "profile" && <small className="xp-shop-owned-note">Disponível em Meu Perfil → Personalização.</small>}
            </article>;
          })}
        </div>
        {totalPages > 1 && <div className="xp-shop-pagination"><button disabled={safePage <= 0} onClick={() => setCurrentPage(Math.max(0, safePage - 1))}><ChevronLeft size={18}/> Anterior</button><strong>{safePage + 1}/{totalPages}</strong><button disabled={safePage >= totalPages - 1} onClick={() => setCurrentPage(Math.min(totalPages - 1, safePage + 1))}>Próxima <ChevronRight size={18}/></button></div>}
      </>}
    </div>}

    {section === "history" && <div className="xp-section-body">
      <div className="parity-title xp-subtitle"><div><p>MOVIMENTAÇÕES</p><h3>Histórico XP</h3></div><History size={22}/></div>
      {!dashboard.transactions.length ? <p className="parity-status">Ainda não há movimentações de XP.</p> : (
        <div className="xp-history-list">{dashboard.transactions.slice(0,20).map(item => <div key={item.id}><div><strong>{item.description || item.activity}</strong><small>{item.date_key}</small></div><b>{item.type === "spend" ? "-" : "+"}{item.amount} XP</b></div>)}</div>
      )}
    </div>}

    {error && <p className="parity-warning xp-panel-error">{error}</p>}
    <button className="back-link xp-refresh" onClick={() => void reload()}><RefreshCcw size={16}/> Atualizar XP</button>
    {previewItem && <ShopPreview item={previewItem} onClose={() => setPreviewItem(null)}/>} 
  </section>;
}
