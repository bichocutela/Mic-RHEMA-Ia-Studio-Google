import { useState } from "react";
import { BadgeCheck, CircleDot, Gift, ReceiptText, Shield, ShoppingBag, Sparkles } from "lucide-react";
import { PwaXpAdminRewards, PwaXpAdminRedemptions } from "./PwaXpAdminRewards";
import { PwaXpAdminBadges, PwaXpAdminCosmetics } from "./PwaXpAdminCatalog";
import { PwaXpAdminEffects } from "./PwaXpAdminEffects";
import "./PwaXpAdminPanel.css";

type Section = "rewards" | "redemptions" | "badges" | "distinctives" | "frames" | "effects";
const tabs: Array<{id:Section;label:string;icon:typeof Gift}> = [
  {id:"rewards",label:"Recompensas",icon:Gift},
  {id:"redemptions",label:"Resgates",icon:ReceiptText},
  {id:"badges",label:"Emblemas",icon:BadgeCheck},
  {id:"distinctives",label:"Distintivos",icon:CircleDot},
  {id:"frames",label:"Molduras",icon:Shield},
  {id:"effects",label:"Efeitos de Luz",icon:Sparkles},
];

export function PwaXpAdminPanel(){
  const [section,setSection]=useState<Section>("rewards");
  return <section className="parity-page xp-admin-root">
    <header className="xp-admin-hero"><div><small>ADMINISTRAÇÃO · JORNADA XP</small><h1>Loja XP</h1><p>Mesmo catálogo, resgates e personalizações usados pelo Android.</p></div><ShoppingBag size={34}/></header>
    <nav className="xp-admin-tabs" aria-label="Categorias da administração da Loja XP">{tabs.map(({id,label,icon:Icon})=><button key={id} className={section===id?"selected":""} onClick={()=>setSection(id)}><Icon size={18}/><span>{label}</span></button>)}</nav>
    <div className="xp-admin-safety-note"><strong>Sincronização central</strong><span>As alterações desta tela usam o mesmo backend do Android. Nada é salvo somente no navegador.</span></div>
    {section==="rewards"&&<PwaXpAdminRewards/>}
    {section==="redemptions"&&<PwaXpAdminRedemptions/>}
    {section==="badges"&&<PwaXpAdminBadges/>}
    {section==="distinctives"&&<PwaXpAdminCosmetics kind="distintivo"/>}
    {section==="frames"&&<PwaXpAdminCosmetics kind="moldura"/>}
    {section==="effects"&&<PwaXpAdminEffects/>}
  </section>;
}
