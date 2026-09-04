import { useEffect, useMemo, useState } from "react";
import { Sparkles, Trophy, X } from "lucide-react";
import { loadPwaMemberProfile } from "@/lib/firebase";
import { PWA_BADGE_UNLOCK_EVENT } from "@/lib/badge-activity";
import { BiblicalBadgeAvatar } from "./BiblicalBadgeAvatar";
import { badgeForId, biblicalBadges } from "./BiblicalBadgeCatalog";
import "./BadgeUnlockCelebration.css";

const badgeIds=new Set(biblicalBadges.map((badge)=>badge.id));
const confettiColors=["#ffd54f","#ffb300","#66bb6a","#42a5f5","#ef5350","#ab47bc","#ec407a"];

export function BadgeUnlockCelebration({onOpenBadges}:{onOpenBadges:()=>void}){
  const[queue,setQueue]=useState<string[]>([]);
  const[avatarId,setAvatarId]=useState("davi");
  const currentId=queue[0]||"";
  const badge=currentId?badgeForId(currentId):null;
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
      const ids=(detail?.badgeIds||[]).filter((id)=>badgeIds.has(id));
      if(!ids.length)return;
      setQueue(current=>[...current,...ids.filter(id=>!current.includes(id))]);
      void loadPwaMemberProfile().then(profile=>setAvatarId(profile.avatarId||"davi")).catch(()=>undefined);
      if("vibrate" in navigator) navigator.vibrate?.([90,45,130]);
    };
    window.addEventListener(PWA_BADGE_UNLOCK_EVENT,handler as EventListener);
    return()=>window.removeEventListener(PWA_BADGE_UNLOCK_EVENT,handler as EventListener);
  },[]);

  if(!badge||!currentId)return null;
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
      {badge.rarity&&<strong style={{color:"var(--pwa-primary,#8a6500)"}}>{badge.rarity}</strong>}
      <p>{badge.description}</p>
      {queue.length>1&&<small>Você ainda conquistou mais {queue.length-1} emblema(s) agora.</small>}
      <button className="badge-unlock-primary" onClick={open}><Trophy size={19}/>Ver e usar meu emblema</button>
      <button className="badge-unlock-later" onClick={close}>Continuar depois</button>
    </section>
  </div>;
}
