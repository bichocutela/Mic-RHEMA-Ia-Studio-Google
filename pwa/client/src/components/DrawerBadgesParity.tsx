import { useEffect, useMemo, useState } from "react";
import { Award, X } from "lucide-react";
import { loadPwaMemberProfile, type PwaMemberProfile } from "@/lib/firebase";
import type { PwaSessionLike } from "./AndroidParityViews";
import { BiblicalBadgeAvatar } from "./BiblicalBadgeAvatar";

const badges=[
 {id:"caminhante",name:"Caminhante",description:"O início de uma jornada de fé e conhecimento.",requirement:"Criar o perfil e escolher um avatar",level:1},
 {id:"semeador",name:"Semeador",description:"Quem planta a Palavra no coração todos os dias.",requirement:"Ler 3 devocionais e concluir 1 tema de plano",level:2},
 {id:"discipulo",name:"Discípulo",description:"Um passo firme no aprendizado da Palavra.",requirement:"Concluir 1 plano, 3 temas e ler 3 capítulos da Bíblia",level:3},
 {id:"perseverante",name:"Perseverante",description:"Constância para continuar mesmo nos dias difíceis.",requirement:"Acumular 60 minutos ativos e realizar 10 atividades",level:4},
 {id:"estudante_rhema",name:"Estudante Rhema",description:"Dedicação reconhecida ao estudo no Instituto Bíblico Rhema.",requirement:"Ler 3 livros, assistir 3 vídeos e ouvir 2 áudios",level:5},
 {id:"mestre_da_palavra",name:"Mestre da Palavra",description:"Conhecimento construído com disciplina e compromisso.",requirement:"Concluir 1 curso IBR, ler 3 notícias e 10 capítulos da Bíblia",level:6},
 {id:"guardiao_da_fe",name:"Guardião da Fé",description:"Um testemunho de perseverança, serviço e maturidade.",requirement:"Realizar todas as atividades e acumular 180 minutos ativos",level:7},
 {id:"primeira_oracao",name:"Primeira Oração",description:"Um primeiro momento separado para falar com Deus.",requirement:"Registrar o primeiro momento de oração"},
 {id:"leitor_da_palavra",name:"Leitor da Palavra",description:"A Bíblia aberta e o coração disposto a aprender.",requirement:"Ler 10 capítulos da Bíblia"},
 {id:"coracao_grato",name:"Coração Grato",description:"Reconhecimento pelas bênçãos recebidas.",requirement:"Registrar uma mensagem de gratidão"},
 {id:"constante",name:"Constante",description:"Pequenos passos repetidos com fidelidade.",requirement:"Estudar por 7 dias consecutivos"},
 {id:"certificado_ibr",name:"Certificado IBR",description:"Uma conquista acadêmica no Instituto Bíblico Rhema.",requirement:"Receber um certificado IBR"},
];

export function DrawerBadgesParity({session}:{session:PwaSessionLike}){
 const[profile,setProfile]=useState<PwaMemberProfile|null>(null);const[selected,setSelected]=useState<(typeof badges)[number]|null>(null);
 useEffect(()=>{if(!session){setProfile(null);return}let active=true;loadPwaMemberProfile().then(value=>{if(active)setProfile(value)}).catch(()=>undefined);return()=>{active=false}},[session?.uid]);
 const unlocked=useMemo(()=>new Set(profile?.unlockedBadgeIds?.length?profile.unlockedBadgeIds:["caminhante"]),[profile?.unlockedBadgeIds]);
 const avatar=profile?.avatarId||"davi";
 if(!session)return <div className="drawer-badges"><span>SEU CAMINHO</span><b>Entre para acompanhar emblemas</b></div>;
 return <>
  <section style={{padding:"10px 14px"}}>
   <div style={{display:"flex",alignItems:"center",gap:8,marginBottom:8}}><Award size={18}/><div><strong style={{display:"block"}}>Emblemas</strong><small>Veja seus níveis e conquistas</small></div></div>
   <div style={{display:"flex",gap:10,overflowX:"auto",padding:"4px 1px 8px",scrollbarWidth:"none"}}>
    {badges.map(badge=>{const ok=unlocked.has(badge.id);return <button key={badge.id} onClick={()=>setSelected(badge)} style={{flex:"0 0 76px",border:0,background:"transparent",color:"inherit",padding:0,textAlign:"center"}}>
      <BiblicalBadgeAvatar avatarId={avatar} badgeId={badge.id} size={66} locked={!ok} title={badge.name}/>
      <small style={{display:"block",marginTop:5,whiteSpace:"nowrap",overflow:"hidden",textOverflow:"ellipsis",fontWeight:ok?700:500,opacity:ok?1:.62}}>{badge.name}</small>
    </button>})}
   </div>
  </section>
  {selected&&<div onClick={()=>setSelected(null)} style={{position:"fixed",inset:0,zIndex:1400,background:"rgba(0,0,0,.62)",display:"grid",placeItems:"center",padding:20}}>
   <article onClick={e=>e.stopPropagation()} style={{width:"min(92vw,380px)",background:"var(--card,#fffdf7)",color:"var(--foreground,#1d1b20)",borderRadius:22,padding:20,textAlign:"center",boxShadow:"0 22px 60px rgba(0,0,0,.28)"}}>
    <button aria-label="Fechar" onClick={()=>setSelected(null)} style={{float:"right",border:0,background:"transparent",color:"inherit"}}><X/></button>
    <div style={{display:"grid",placeItems:"center",margin:"18px auto 10px"}}><BiblicalBadgeAvatar avatarId={avatar} badgeId={selected.id} size={176} locked={!unlocked.has(selected.id)} title={selected.name}/></div>
    <h2 style={{marginBottom:6}}>{selected.level?`Nível ${selected.level}: ${selected.name}`:selected.name}</h2>
    <strong style={{color:unlocked.has(selected.id)?"#2e7d32":"var(--muted-foreground,#6b7280)"}}>{unlocked.has(selected.id)?"Emblema conquistado":"Ainda bloqueado"}</strong>
    <p style={{lineHeight:1.5,color:"var(--muted-foreground,#6b7280)"}}>{unlocked.has(selected.id)?selected.description:`Para atingir: ${selected.requirement}`}</p>
   </article>
  </div>}
 </>
}
