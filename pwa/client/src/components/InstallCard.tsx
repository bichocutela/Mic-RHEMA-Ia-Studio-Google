/** PARIDADE ANDROID — orientação discreta para instalação e permissão explícita de avisos. */
import { BellRing, Download, Share } from "lucide-react";
import { toast } from "sonner";

export function InstallCard() {
  const installed = window.matchMedia?.("(display-mode: standalone)").matches;
  const askNotification = async () => {
    if (!("Notification" in window)) return toast.error("Este navegador não oferece notificações web.");
    const result = await Notification.requestPermission();
    if (result === "granted") toast.success("Permissão concedida. A PWA concluirá sua inscrição para receber avisos.");
    else toast.message("Você pode ativar essa permissão depois nas configurações do iPhone.");
  };

  if (installed) {
    return (
      <section className="install-card installed-card">
        <div className="install-icon"><BellRing size={19} /></div>
        <div><p className="eyebrow">PWA INSTALADA</p><strong>Receba os avisos da igreja</strong><span>Ative apenas se desejar receber lembretes e novidades.</span></div>
        <button className="quiet-button" onClick={askNotification}>Ativar</button>
      </section>
    );
  }

  return (
    <section className="install-card">
      <div className="install-icon"><Download size={19} /></div>
      <div>
        <p className="eyebrow">NO IPHONE</p>
        <strong>Instale o MIC Rhema na Tela de Início</strong>
        <span>No Safari, toque em <Share size={13} className="inline-icon" /> Compartilhar e escolha “Adicionar à Tela de Início”.</span>
      </div>
    </section>
  );
}
