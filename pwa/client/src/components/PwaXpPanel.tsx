import { useEffect, useMemo, useState } from "react";
import { Gift, History, RefreshCcw, ShoppingBag, Sparkles } from "lucide-react";
import { toast } from "sonner";
import { loadPwaXpDashboard, redeemPwaXp, type PwaXpAccount, type PwaXpDashboard } from "@/lib/xp";

export function PwaXpPanel({ onAccount }: { onAccount?: (account: PwaXpAccount) => void }) {
  const [dashboard, setDashboard] = useState<PwaXpDashboard | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [redeeming, setRedeeming] = useState<string | null>(null);

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

  if (loading && !dashboard) return <section className="parity-card"><p className="parity-status">Sincronizando Jornada XP…</p></section>;
  if (error && !dashboard) return <section className="parity-card"><p className="parity-warning">{error}</p><button className="parity-primary" onClick={() => void reload()}><RefreshCcw size={17}/> Tentar novamente</button></section>;
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

  return <section className="parity-card" style={{marginTop:18}}>
    <div className="parity-title" style={{marginBottom:12}}><div><p>JORNADA CENTRAL</p><h2>XP e Loja</h2><span>Mesmo saldo, histórico e resgates do Android.</span></div><Sparkles size={28}/></div>
    <div style={{display:"grid",gridTemplateColumns:"repeat(3,minmax(0,1fr))",gap:10}}>
      <div className="profile-v2-stat"><strong>{account.total_earned} XP</strong><small>XP Total</small></div>
      <div className="profile-v2-stat"><strong>{account.balance} XP</strong><small>Saldo XP</small></div>
      <div className="profile-v2-stat"><strong>{dashboard.streak}</strong><small>dias seguidos</small></div>
    </div>

    {!dashboard.unlocked ? <p className="parity-status" style={{marginTop:12}}>A Loja XP é liberada no Nível 8 — Semente da Fé.</p> : <>
      <div className="parity-title" style={{marginTop:20,marginBottom:10}}><div><p>RECOMPENSAS</p><h3>Loja XP</h3></div><ShoppingBag size={24}/></div>
      {!dashboard.items.length ? <p className="parity-status">Nenhuma recompensa disponível agora.</p> : <div className="android-list-cards">
        {dashboard.items.map(item => {
          const count = redeemedCounts.get(item.id) || 0;
          const limit = Math.max(1, item.limit_per_member || 1);
          const limitReached = count >= limit;
          const soldOut = item.stock !== null && item.stock <= 0;
          const insufficient = account.balance < item.cost;
          return <article key={item.id} style={{padding:14,border:"1px solid rgba(127,127,127,.25)",borderRadius:16}}>
            <div style={{display:"flex",gap:12,alignItems:"center"}}>{item.image_url && <img src={item.image_url} alt="" style={{width:66,height:66,objectFit:"cover",borderRadius:12}}/>}<div style={{flex:1}}><strong>{item.name}</strong><small style={{display:"block",opacity:.75}}>{item.description}</small><b>{item.cost} XP</b>{count>0&&<small style={{display:"block"}}>Resgates: {Math.min(count,limit)}/{limit}</small>}</div></div>
            <button className="parity-primary" style={{width:"100%",marginTop:10}} disabled={limitReached||soldOut||insufficient||redeeming===item.id} onClick={()=>void redeem(item.id,item.cost)}><Gift size={17}/>{limitReached?"Já resgatado":soldOut?"Esgotado":insufficient?"Saldo insuficiente":redeeming===item.id?"Resgatando…":`Resgatar por ${item.cost} XP`}</button>
          </article>;
        })}
      </div>
    </>}

    {!!dashboard.entitlements.length && <><div className="parity-title" style={{marginTop:20,marginBottom:8}}><div><p>COLEÇÃO</p><h3>Minhas recompensas</h3></div><Gift size={22}/></div><div className="filter-pills">{dashboard.entitlements.map(item=><span key={item.id}>{item.item_name}</span>)}</div></>}

    <div className="parity-title" style={{marginTop:20,marginBottom:8}}><div><p>MOVIMENTAÇÕES</p><h3>Histórico XP</h3></div><History size={22}/></div>
    {!dashboard.transactions.length ? <p className="parity-status">Ainda não há movimentações de XP.</p> : <div className="android-list-cards">{dashboard.transactions.slice(0,12).map(item=><div key={item.id} style={{display:"flex",justifyContent:"space-between",gap:12,padding:"10px 0"}}><div><strong>{item.description||item.activity}</strong><small style={{display:"block",opacity:.7}}>{item.date_key}</small></div><b>{item.type==="spend"?"-":"+"}{item.amount} XP</b></div>)}</div>}
    {error&&<p className="parity-warning">{error}</p>}
    <button className="back-link" onClick={()=>void reload()}><RefreshCcw size={16}/> Atualizar XP</button>
  </section>;
}
