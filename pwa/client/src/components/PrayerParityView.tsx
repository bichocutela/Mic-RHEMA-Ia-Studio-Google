import { useState } from "react";
import { HandHeart, Send } from "lucide-react";
import { toast } from "sonner";
import { submitPrayerRequest } from "@/lib/firebase";
import "./PrayerParityView.css";

export function PrayerParityView() {
  const [name, setName] = useState("");
  const [request, setRequest] = useState("");
  const [busy, setBusy] = useState(false);

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!name.trim() || !request.trim()) {
      toast.error("Preencha todos os campos.");
      return;
    }
    setBusy(true);
    try {
      await submitPrayerRequest({ name: name.trim(), request: request.trim() });
      setName("");
      setRequest("");
      toast.success("Pedido enviado com sucesso!");
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Não foi possível enviar o pedido. Tente novamente.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className="page-pad prayer-parity-page">
      <header className="prayer-parity-header">
        <div><p>PEDIDOS DE ORAÇÃO</p><h1>Pedidos de Oração</h1><span>Compartilhe seu pedido conosco</span></div>
        <HandHeart size={30} />
      </header>

      <article className="prayer-verse-card">
        <div className="prayer-emoji" aria-hidden="true">🙏</div>
        <blockquote>“Orai uns pelos outros, para que sejais curados. A oração do justo tem grande poder.”</blockquote>
        <strong>— Tiago 5:16</strong>
      </article>

      <form className="prayer-parity-form" onSubmit={submit}>
        <h2>Enviar Pedido</h2>
        <label>
          <span>Seu nome</span>
          <input value={name} onChange={(event) => setName(event.target.value)} placeholder="Como podemos te chamar?" required />
        </label>
        <label>
          <span>Pedido de oração</span>
          <textarea value={request} onChange={(event) => setRequest(event.target.value)} placeholder="Descreva seu pedido de oração..." rows={6} required />
        </label>
        <button disabled={busy}>
          <Send size={18} />
          {busy ? "Enviando…" : "🙏 Enviar Pedido"}
        </button>
      </form>

      <aside className="prayer-parity-footer">
        Seus pedidos são recebidos pela equipe pastoral da MIC Rhema e levados em oração com amor e cuidado.
      </aside>
    </section>
  );
}
