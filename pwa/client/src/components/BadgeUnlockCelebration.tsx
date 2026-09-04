import { useEffect, useMemo, useState } from "react";
import { Sparkles, Trophy, X } from "lucide-react";
import { loadPwaMemberProfile } from "@/lib/firebase";
import { PWA_BADGE_UNLOCK_EVENT } from "@/lib/badge-activity";
import { BiblicalBadgeAvatar } from "./BiblicalBadgeAvatar";
import "./BadgeUnlockCelebration.css";

type BadgeInfo = { id:string; name:string; description:string; level?:number };

const badges:Record<string,BadgeInfo> = {
  caminhante:{id:"caminhante",name:"Caminhante",description:"O início de uma jornada de fé e conhecimento.",level:1},
  semeador:{id:"semeador",name:"Semeador",description:"Quem planta a Palavra no coração todos os dias.",level:2},
  discipulo:{id:"discipulo",name:"Discípulo",description:"Um passo firme no aprendizado da Palavra.",level:3},
  perseverante:{id:"perseverante",name:"Perseverante",description:"Constância para continuar mesmo nos dias difíceis.",level:4},
  estudante_rhema:{id:"estudante_rhema",name:"Estudante Rhema",description:"Dedicação reconhecida ao estudo no Instituto Bíblico Rhema.",level:5},
  mestre_da_palavra:{id:"mestre_da_palavra",name:"Mestre da Palavra",description:"Conhecimento construído com disciplina e compromisso.",level:6},
  guardiao_da_fe:{id:"guardiao_da_fe",name:"Guardião da Fé",description:"Um testemunho de perseverança, serviço e maturidade.",level:7},
  primeira_oracao:{id:"primeira_oracao",name:"Primeira Oração",description:"Um primeiro momento separado para falar com Deus."},
  leitor_da_palavra:{id:"leitor_da_palavra",name:"Leitor da Palavra",description:"A Bíblia aberta e o coração disposto a aprender."},
  coracao_grato:{id:"coracao_grato",name:"Coração Grato",description:"Reconhecimento pelas bênçãos recebidas."},
  constante:{id:"constante",name:"Constante",description:"Pequenos passos repetidos com fidelidade."},
  certificado_ibr:{id:"certificado_ibr",name:"Certificado IBR",description:"Uma conquista acadêmica no Instituto Bíblico Rhema."},
};

const confettiColors=["#ffd54f","#ffb300","#66bb6a","#42a5f5","#ef5350","#ab47bc","#ec407a"];

export function BadgeUnlockCelebration({onOpenBadges}:{onOpenBadges:()=>void}){
  const[queue,setQueue]=useState<string[]>([]);
  const[avatarId,setAvatarId]=useState("davi");
  const currentId=queue[0]||"";
  const badge=badges[currentId];
  const pieces=useMemo(()=>Array.from({length:54},(_,index)=>({
    left:(index*37)%100,
    delay:(index%9)*65,
    duration:1700+(index%7)*120,
    color:confettiColors[index%confettiColors.length],
    rotate:(index*41)%180,
  })),[]);

  useEffect(()=>{
    const handler=(event:Event)=>{
      const detail=(event as CustomEvent<{badgeIds?:string[]}>).detail;
      const ids=(detail?.badgeIds||[]).filter(id=>badges[id]);
      if(!ids.length)return;
      setQueue(current=>[...current,...ids.filter(id=>!current.includes(id))]);
      void loadPwaMemberProfile().then(profile=>setAvatarId(profile.avatarId||"davi")).catch(()=>undefined);
      if("vibrate" in navigator) navigator.vibrate?.([90,45,130]);
    };
    window.addEventListener(PWA_BADGE_UNLOCK_EVENT,handler as EventListener);
    return()=>window.removeEventListener(PWA_BADGE_UNLOCK_EVENT,handler as EventListener);
  },[]);

  if(!badge)return null;
  const close=()=>setQueue(current=>current.slice(1));
  const open=()=>{
    localStorage.setItem("micrhema:pwa:open-badge",badge.id);
    setQueue([]);
    onOpenBadges();
  };

  return <div className="badge-unlock-overlay" role="dialog" aria-modal="true" aria-label={`Emblema ${badge.name} desbloqueado`}>
    <div className="badge-unlock-confetti" aria-hidden="true">{pieces.map((piece,index)=><i key={index} style={{left:`${piece.left}%`,animationDelay:`${piece.delay}ms`,animationDuration:`${piece.duration}ms`,backgroundColor:piece.color,transform:`rotate(${piece.rotate}deg)`}}/>)}</div>
    <button className="badge-unlock-close" aria-label="Continuar depois" onClick={close}><X size={22}/></button>
    <section className="badge-unlock-card">
      <div className="badge-unlock-kicker"><Sparkles size={18}/>{badge.level?"NOVO NÍVEL DESBLOQUEADO!":"NOVO EMBLEMA DESBLOQUEADO!"}</div>
      <h2>Parabéns!</h2>
      <p className="badge-unlock-lead">Sua constância fez você avançar na jornada MIC Rhema.</p>
      <div className="badge-unlock-medal"><BiblicalBadgeAvatar avatarId={avatarId} badgeId={badge.id} size={218} title={badge.name}/></div>
      <div className="badge-unlock-title"><Trophy size={22}/><strong>{badge.level?`Nível ${badge.level} · ${badge.name}`:badge.name}</strong></div>
      <p>{badge.description}</p>
      {queue.length>1&&<small>Você ainda conquistou mais {queue.length-1} emblema(s) agora.</small>}
      <button className="badge-unlock-primary" onClick={open}><Trophy size={19}/>Ver e usar meu emblema</button>
      <button className="badge-unlock-later" onClick={close}>Continuar depois</button>
    </section>
  </div>;
}
