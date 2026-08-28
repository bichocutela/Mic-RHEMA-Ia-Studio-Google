import { useEffect, useState } from "react";
import { CircleUserRound, Info, Mail, MapPin, Phone } from "lucide-react";
import { listenToDocument } from "@/lib/firebase";

type AboutSettings = {
  pastorName?: string;
  pastorTitle?: string;
  missionTagline?: string;
  rhemaMeaning?: string;
};

export function AboutParityView() {
  const [about, setAbout] = useState<(AboutSettings & { id: string }) | null>(null);
  useEffect(() => listenToDocument<AboutSettings>("settings", "about", setAbout, () => setAbout(null)), []);

  const pastorName = about?.pastorName?.trim() || "Pastor Evaldo Leôncio";
  const pastorTitle = about?.pastorTitle?.trim() || "Liderança";
  const mission = about?.missionTagline?.trim() || "Conectando Pessoas e Transformando Vidas.";
  const rhemaMeaning = about?.rhemaMeaning?.trim() || "Rhema é a palavra revelada de Deus para um momento específico.";

  return <section className="page-pad android-module">
    <div className="android-section-heading"><div><p>SOBRE</p><h2>MIC Rhema</h2></div></div>
    <div className="android-list-cards">
      <article className="android-module-card"><CircleUserRound size={29}/><div><strong>{pastorTitle}</strong><small>{pastorName}</small></div></article>
      <article className="android-module-card"><Info size={29}/><div><strong>Nossa Missão</strong><small>{mission}</small></div></article>
      <article className="android-module-card"><Info size={29}/><div><strong>O que significa Rhema</strong><small>{rhemaMeaning}</small></div></article>
      <article className="android-module-card"><MapPin size={29}/><div><strong>Localização</strong><small>Rua Todos os Santos – Natal/RN</small></div></article>
      <a className="android-module-card" href="tel:+5584988041804"><Phone size={29}/><div><strong>Telefone</strong><small>84 98804 1804</small></div></a>
      <a className="android-module-card" href="mailto:micrhema2@gmail.com"><Mail size={29}/><div><strong>E-mail</strong><small>micrhema2@gmail.com</small></div></a>
    </div>
  </section>;
}
