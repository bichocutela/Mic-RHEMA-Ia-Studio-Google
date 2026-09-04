import { useEffect, useMemo, useState } from "react";
import { BellRing, CheckCircle2, Clock3, HandHeart, RefreshCcw, Send } from "lucide-react";
import { toast } from "sonner";
import { loadPrayerHistory, submitPrayerRequest, type PrayerHistoryItem } from "@/lib/firebase";
import { ensurePrayerPushRegistration } from "@/lib/push";
import "./PrayerParityView.css";

export function PrayerParityView({ session }: { session?: { uid: string; name: string; isAdmin: boolean; isIbr?: boolean } | null }) {
  const [name, setName] = useState(session?.name || "");
  const [request, setRequest] = useState("");
  const [busy, setBusy] = useState(false);
  const [historyBusy, setHistoryBusy] = useState(true);
  const [history, setHistory] = useState<PrayerHistoryItem[]>([]);

  useEffect(() => { if (session?.name && !name) setName(session.name); }, [session?.name]);
  const refresh = async () => {
    setHistoryBusy(true);
    try { setHistory(await loadPrayerHistory()); }
    catch (error) { toast.error(error instanceof Error ? error.message : "Não foi possível carregar seu histórico."); }
    finally { setHistoryBusy(false); }
  };
  useEffect(() => { void refresh(); }, [session?.uid]);
  useEffect(() => {
    const update = () => void refresh();
    window.addEventListener("micrhema:prayer-updated", update);
    const visible = () => { if (document.visibilityState === "visible") void refresh(); };
    document.addEventListener("visibilitychange", visible);
    return () => { window.removeEventListener("micrhema:prayer-updated", update); document.removeEventListener("visibilitychange", visible); };
  }, [session?.uid]);

  const pending = useMemo(() => history.filter((item) => item.status !== "respondida" && !item.answeredAt).length, [history]);
  const answered = useMemo(() => history.filter((item) => item.status === "respondida" || item.answeredAt > 0).length, [history]);

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!name.trim() || !request.trim()) return toast.error("Preencha todos os campos.");
    setBusy(true);
    let notificationToken = "";
    try {
      try { notificationToken = (await ensurePrayerPushRegistration()).token; }
      catch (notificationError) {
        toast.message("Seu pedido será enviado normalmente.", { description: notificationError instanceof Error ? `${notificationError.message} Ative os avisos para receber a confirmação da oração.` : "Ative os avisos para receber a confirmação da oração." });
      }
      await submitPrayerRequest({ name: name.trim(), request: request.trim(), notificationToken });
      setRequest("");
      toast.success("Pedido enviado à equipe pastoral.", { description: "Quando a oração for realizada, o histórico será atualizado e este aparelho será avisado se as notificações estiverem ativas." });
      await refresh();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Não foi possível enviar o pedido. Tente novamente.");
    } finally { setBusy(false); }
  };

  return <section className="page-pad prayer-parity-page">
    <header className="prayer-parity-header"><div><p>PEDIDOS DE ORAÇÃO</p><h1>Seu pedido está sendo cuidado</h1><span>Um espaço privado entre você e a equipe pastoral.</span></div><HandHeart size={30}/></header>
    <article className="prayer-verse-card"><div className="prayer-emoji" aria-hidden="true">🙏</div><blockquote>“Orai uns pelos outros, para que sejais curados. A oração do justo tem grande poder.”</blockquote><strong>— Tiago 5:16</strong></article>

    <div className="prayer-status-strip"><div><Clock3/><strong>{pending}</strong><span>Aguardando oração</span></div><div><CheckCircle2/><strong>{answered}</strong><span>Orações respondidas</span></div></div>

    <form className="prayer-parity-form" onSubmit={submit}><h2>Enviar novo pedido</h2><label><span>Seu nome</span><input value={name} onChange={(event) => setName(event.target.value)} placeholder="Como podemos te chamar?" required /></label><label><span>Pedido de oração</span><textarea value={request} onChange={(event) => setRequest(event.target.value)} placeholder="Descreva seu pedido de oração..." rows={6} required /></label><button disabled={busy}><Send size={18}/>{busy ? "Enviando…" : "Enviar Pedido"}</button><small><BellRing size={14}/> Se os avisos estiverem ativos, somente este aparelho recebe a confirmação quando a oração for realizada.</small></form>

    <section className="prayer-history-section"><header><div><p>SEU HISTÓRICO</p><h2>Pedidos e respostas</h2></div><button onClick={() => void refresh()} disabled={historyBusy} aria-label="Atualizar histórico"><RefreshCcw className={historyBusy ? "spin" : ""} size={18}/></button></header>
      {historyBusy && !history.length ? <div className="prayer-history-empty">Carregando seus pedidos…</div> : !history.length ? <div className="prayer-history-empty">Seu primeiro pedido aparecerá aqui depois do envio.</div> : <div className="prayer-history-list">{history.map((item) => {
        const done = item.status === "respondida" || item.answeredAt > 0;
        return <article key={item.id} className={done ? "is-answered" : "is-pending"}><div className="prayer-history-icon">{done ? <CheckCircle2/> : <Clock3/>}</div><div className="prayer-history-copy"><div><strong>{done ? "Oração respondida" : "Oração pendente"}</strong><time>{done ? item.answeredDate || item.date : item.date}</time></div><p>{item.request}</p>{done && <aside><b>🙏 A equipe pastoral orou por você.</b><span>{item.responseMessage || `Oração respondida em ${item.answeredDate || item.date}.`}</span></aside>}</div></article>;
      })}</div>}
    </section>
    <aside className="prayer-parity-footer">Seus pedidos são visíveis à equipe pastoral responsável. O seu histórico não é exibido para outros membros.</aside>
  </section>;
}
