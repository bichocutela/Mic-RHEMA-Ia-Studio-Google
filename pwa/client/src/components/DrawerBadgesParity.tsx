import { useEffect, useMemo, useState } from "react";
import { Award, X } from "lucide-react";
import { loadPwaMemberProfile, type PwaMemberProfile } from "@/lib/firebase";
import type { PwaSessionLike } from "./AndroidParityViews";
import { BiblicalBadgeAvatar } from "./BiblicalBadgeAvatar";
import { biblicalBadges, type PwaBiblicalBadge } from "./BiblicalBadgeCatalog";

export function DrawerBadgesParity({session}:{session:PwaSessionLike}){
 const[profile,setProfile]=useState<PwaMemberProfile|null>(null);const[selected,setSelected]=useState<PwaBiblicalBadge|null>(null);
 useEffect(()=>{if(!session){setProfile(null);return}let active=true;loadPwaMemberProfile().then(value=>{if(active)setProfile(value)}).catch(()=>undefined);return()=>{active=false}},[session?.uid]);
 const unlocked=useMemo(()=>new Set(profile?.unlockedBadgeIds?.length?profile.unlockedBadgeIds:["caminhante"]),[profile?.unlockedBadgeIds]);
 const avatar=profile?.avatarId||"davi";
 if(!session)return <div className="drawer-badges"><span>SEU CAMINHO</span><b>Entre para acompanhar emblemas</b></div>;
 return <>
  <section style={{padding:"10px 14px"}}>
   <div style={{display:"flex",alignItems:"center",gap:8,marginBottom:8}}><Award size={18}/><div><strong style={{display:"block"}}>Emblemas</strong><small>Veja seus níveis e conquistas</small></div></div>
   <div style={{display:"flex",gap:10,overflowX:"auto",padding:"4px 1px 8px",scrollbarWidth:"none"}}>
    {biblicalBadges.map(badge=>{const ok=unlocked.has(badge.id);return <button key={badge.id} onClick={()=>setSelected(badge)} style={{flex:"0 0 76px",border:0,background:"transparent",color:"inherit",padding:0,textAlign:"center",cursor:"pointer"}}>
      <BiblicalBadgeAvatar avatarId={avatar} badgeId={badge.id} size={66} locked={!ok} title={badge.name}/>
      <small style={{display:"block",marginTop:5,whiteSpace:"nowrap",overflow:"hidden",textOverflow:"ellipsis",fontWeight:ok?700:500,opacity:ok?1:.62}}>{badge.name}</small>
    </button>})}
   </div>
  </section>
  {selected&&<div onClick={()=>setSelected(null)} style={{position:"fixed",inset:0,zIndex:1400,background:"rgba(0,0,0,.68)",display:"grid",placeItems:"center",padding:20,backdropFilter:"blur(6px)"}}>
   <article onClick={e=>e.stopPropagation()} style={{width:"min(92vw,400px)",maxHeight:"86vh",overflow:"auto",background:"var(--card,#fffdf7)",color:"var(--foreground,#1d1b20)",borderRadius:24,padding:20,textAlign:"center",boxShadow:"0 22px 60px rgba(0,0,0,.32)"}}>
    <button aria-label="Fechar" onClick={()=>setSelected(null)} style={{float:"right",width:42,height:42,display:"grid",placeItems:"center",border:0,borderRadius:"50%",background:"color-mix(in srgb,currentColor 7%,transparent)",color:"inherit"}}><X/></button>
    <div style={{display:"grid",placeItems:"center",clear:"both",margin:"6px auto 10px"}}><BiblicalBadgeAvatar avatarId={avatar} badgeId={selected.id} size={210} locked={!unlocked.has(selected.id)} dimWhenLocked={false} title={selected.name}/></div>
    <h2 style={{marginBottom:6}}>{selected.level?`Nível ${selected.level}: ${selected.name}`:selected.name}</h2>
    {selected.rarity&&<div style={{display:"inline-block",padding:"5px 10px",borderRadius:999,background:"color-mix(in srgb,var(--pwa-primary,#8a6500) 13%,transparent)",color:"var(--pwa-primary,#8a6500)",fontSize:12,fontWeight:800,marginBottom:8}}>{selected.rarity}</div>}
    <div><strong style={{color:unlocked.has(selected.id)?"var(--pwa-primary,#8a6500)":"var(--muted-foreground,#6b7280)"}}>{unlocked.has(selected.id)?"Emblema conquistado":"Ainda bloqueado"}</strong></div>
    <p style={{lineHeight:1.5,color:"var(--muted-foreground,#6b7280)"}}>{selected.description}</p>
    {!unlocked.has(selected.id)&&<p style={{lineHeight:1.45,color:"var(--muted-foreground,#6b7280)",fontSize:13}}>Para desbloquear: {selected.requirement}</p>}
   </article>
  </div>}
 </>
}
