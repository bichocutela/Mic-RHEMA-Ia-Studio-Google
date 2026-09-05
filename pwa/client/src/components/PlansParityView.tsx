import { useEffect, useState } from "react";
import { ArrowRight, BookOpen, CheckCircle, ChevronLeft, ChevronRight } from "lucide-react";
import { toast } from "sonner";
import androidPlans from "@/data/android-plans.json";
import { recordPwaActivity } from "@/lib/badge-activity";
import { awardPwaXp, tryAwardPwaXp } from "@/lib/xp";
import "./AndroidParityViews.css";

type Category=(typeof androidPlans)[number];
type Theme=Category["themes"][number];

export function PlansParityView(){
  const[category,setCategory]=useState<Category|null>(null);const[theme,setTheme]=useState<Theme|null>(null);
  if(theme&&category)return <ThemeReader category={category} theme={theme} onBack={()=>setTheme(null)} onDone={async()=>{
    const localThemeId=`${category.name}:${theme.title}`;
    const centralThemeId=`${category.name}::${theme.title}`;
    await awardPwaXp("plan_theme",centralThemeId);
    await awardPwaXp("plan_day",centralThemeId);
    await recordPwaActivity("plan_themes",localThemeId).catch(()=>undefined);
    const complete=await tryAwardPwaXp("plan_complete",category.name);
    if(complete)await recordPwaActivity("plans",category.name).catch(()=>undefined);
    toast.success(complete?"Plano concluído e XP sincronizado com o Android.":"Tema concluído e XP sincronizado.");
    setTheme(null);
  }}/>;
  if(category)return <section className="parity-page"><button className="back-link" onClick={()=>setCategory(null)}><ChevronLeft size={18}/> Voltar às categorias</button><div className="parity-title"><div><p>PLANOS DE LEITURA</p><h1>{category.name}</h1><span>{category.themes.length} temas oficiais disponíveis.</span></div><BookOpen size={30}/></div><div className="android-list-cards">{category.themes.map((item,index)=><button key={item.title} onClick={()=>setTheme(item)}><span>{String(index+1).padStart(2,"0")}</span><div><strong>{item.title}</strong><small>{item.verses.join(" · ")}</small></div><ChevronRight size={19}/></button>)}</div></section>;
  return <section className="parity-page"><div className="parity-title"><div><p>PLANOS DE LEITURA</p><h1>Explorar por emoções</h1><span>Os mesmos caminhos e temas oficiais do Android.</span></div><BookOpen size={30}/></div><div className="plan-grid android-plan-categories">{androidPlans.map((item)=><button key={item.name} style={{backgroundColor:item.color}} onClick={()=>setCategory(item)}><span>{String(item.themes.length).padStart(2,"0")}</span><h2>{item.name}</h2><p>{item.themes.length} temas</p><ArrowRight size={18}/></button>)}</div></section>;
}

function ThemeReader({category,theme,onBack,onDone}:{category:Category;theme:Theme;onBack:()=>void;onDone:()=>void|Promise<void>}){
  const[ready,setReady]=useState(false);const[saving,setSaving]=useState(false);
  useEffect(()=>{
    let armed=false;let finished=false;
    const check=()=>{if(!armed||finished)return;const root=document.documentElement;const short=root.scrollHeight<=window.innerHeight+80;const progress=(window.scrollY+window.innerHeight)/Math.max(1,root.scrollHeight);if(short||progress>=.8){finished=true;setReady(true);window.removeEventListener("scroll",check)}};
    const timer=window.setTimeout(()=>{armed=true;check();window.addEventListener("scroll",check,{passive:true})},20_000);
    return()=>{window.clearTimeout(timer);window.removeEventListener("scroll",check)};
  },[category.name,theme.title]);
  const finish=async()=>{if(!ready||saving)return;setSaving(true);try{await onDone()}catch(error){toast.error(error instanceof Error?error.message:"Não foi possível registrar o XP deste plano.")}finally{setSaving(false)}};
  return <section className="parity-page"><button className="back-link" onClick={onBack}><ChevronLeft size={18}/> Voltar aos temas</button><article className="android-plan-reader">{theme.imageUrl&&<img src={theme.imageUrl} alt=""/>}<p>{category.name.toUpperCase()}</p><h1>{theme.title}</h1><div className="filter-pills">{theme.verses.map(verse=><span key={verse}>{verse}</span>)}</div><article className="parity-reader">{String(theme.content||"").split(/\n{2,}/).map((part,index)=><p key={index}>{part}</p>)}</article><button className="parity-primary" disabled={!ready||saving} onClick={()=>void finish()}><CheckCircle size={18}/>{saving?"Sincronizando XP…":ready?"Concluir leitura":"Continue lendo para concluir"}</button></article></section>;
}
