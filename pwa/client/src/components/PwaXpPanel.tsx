import { useEffect, useMemo, useState } from "react";
import { Gift, History, RefreshCcw, ShoppingBag, Sparkles } from "lucide-react";
import { toast } from "sonner";
import { loadPwaXpDashboard, redeemPwaXp, type PwaXpAccount, type PwaXpDashboard } from "@/lib/xp";
import { PwaQuizPanel } from "./PwaQuizPanel";
import "./PwaXpPanel.css";

type XpSection = "summary" | "quiz" | "shop" | "history";

export function PwaXpPanel({ onAccount }: { onAccount?: (account: PwaXpAccount) => void }) {
  const [dashboard, setDashboard] = useState<PwaXpDashboard | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [redeeming, setRedeeming] = useState<string | null>(null);
  const [section, setSection] = useState<XpSection>("summary");

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
      {!dashboard.unlocked ? <p className="parity-status">A Loja XP é liberada no Nível 8 — Semente da Fé.</p> : !dashboard.items.length ? <p className="parity-status">Nenhuma recompensa disponível agora.</p> : (
        <div className="xp-shop-list">
          {dashboard.items.map(item => {
            const count = redeemedCounts.get(item.id) || 0;
            const limit = Math.max(1, item.limit_per_member || 1);
            const limitReached = count >= limit;
            const soldOut = item.stock !== null && item.stock <= 0;
            const insufficient = account.balance < item.cost;
            const label = limitReached ? "Já resgatado" : soldOut ? "Esgotado" : insufficient ? "Saldo insuficiente" : redeeming === item.id ? "Resgatando…" : `Resgatar por ${item.cost} XP`;
            return <article key={item.id} className="xp-shop-item">
              <div className="xp-shop-item-main">
                {item.image_url && <img src={item.image_url} alt=""/>}
                <div><strong>{item.name}</strong><small>{item.description}</small><b>{item.cost} XP</b>{count > 0 && <em>Resgates: {Math.min(count,limit)}/{limit}</em>}</div>
              </div>
              <button className="parity-primary" disabled={limitReached || soldOut || insufficient || redeeming === item.id} onClick={() => void redeem(item.id,item.cost)}><Gift size={17}/>{label}</button>
            </article>;
          })}
        </div>
      )}
    </div>}

    {section === "history" && <div className="xp-section-body">
      <div className="parity-title xp-subtitle"><div><p>MOVIMENTAÇÕES</p><h3>Histórico XP</h3></div><History size={22}/></div>
      {!dashboard.transactions.length ? <p className="parity-status">Ainda não há movimentações de XP.</p> : (
        <div className="xp-history-list">{dashboard.transactions.slice(0,20).map(item => <div key={item.id}><div><strong>{item.description || item.activity}</strong><small>{item.date_key}</small></div><b>{item.type === "spend" ? "-" : "+"}{item.amount} XP</b></div>)}</div>
      )}
    </div>}

    {error && <p className="parity-warning xp-panel-error">{error}</p>}
    <button className="back-link xp-refresh" onClick={() => void reload()}><RefreshCcw size={16}/> Atualizar XP</button>
  </section>;
}
