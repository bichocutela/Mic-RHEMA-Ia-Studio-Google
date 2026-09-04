import { useState } from "react";
import { ArrowRight, BookOpen, CheckCircle, ChevronLeft, ChevronRight } from "lucide-react";
import { toast } from "sonner";
import androidPlans from "@/data/android-plans.json";
import { recordPwaActivity } from "@/lib/badge-activity";
import "./AndroidParityViews.css";

type Category=(typeof androidPlans)[number];
type Theme=Category["themes"][number];

export function PlansParityView(){
  const[category,setCategory]=useState<Category|null>(null);const[theme,setTheme]=useState<Theme|null>(null);
  if(theme&&category)return <ThemeReader category={category} theme={theme} onBack={()=>setTheme(null)} onDone={async()=>{await recordPwaActivity("plan_themes",`${category.name}:${theme.title}`).catch(()=>undefined);await recordPwaActivity("plans",category.name).catch(()=>undefined);toast.success("Tema concluído e progresso sincronizado.");setTheme(null)}}/>;
  if(category)return <section className="parity-page"><button className="back-link" onClick={()=>setCategory(null)}><ChevronLeft size={18}/> Voltar às categorias</button><div className="parity-title"><div><p>PLANOS DE LEITURA</p><h1>{category.name}</h1><span>{category.themes.length} temas oficiais disponíveis.</span></div><BookOpen size={30}/></div><div className="android-list-cards">{category.themes.map((item,index)=><button key={item.title} onClick={()=>setTheme(item)}><span>{String(index+1).padStart(2,"0")}</span><div><strong>{item.title}</strong><small>{item.verses.join(" · ")}</small></div><ChevronRight size={19}/></button>)}</div></section>;
  return <section className="parity-page"><div className="parity-title"><div><p>PLANOS DE LEITURA</p><h1>Explorar por emoções</h1><span>Os mesmos caminhos e temas oficiais do Android.</span></div><BookOpen size={30}/></div><div className="plan-grid android-plan-categories">{androidPlans.map((item)=><button key={item.name} style={{backgroundColor:item.color}} onClick={()=>setCategory(item)}><span>{String(item.themes.length).padStart(2,"0")}</span><h2>{item.name}</h2><p>{item.themes.length} temas</p><ArrowRight size={18}/></button>)}</div></section>;
}

function ThemeReader({category,theme,onBack,onDone}:{category:Category;theme:Theme;onBack:()=>void;onDone:()=>void|Promise<void>}){
  return <section className="parity-page"><button className="back-link" onClick={onBack}><ChevronLeft size={18}/> Voltar aos temas</button><article className="android-plan-reader">{theme.imageUrl&&<img src={theme.imageUrl} alt=""/>}<p>{category.name.toUpperCase()}</p><h1>{theme.title}</h1><div className="filter-pills">{theme.verses.map(verse=><span key={verse}>{verse}</span>)}</div><article className="parity-reader">{String(theme.content||"").split(/\n{2,}/).map((part,index)=><p key={index}>{part}</p>)}</article><button className="parity-primary" onClick={()=>void onDone()}><CheckCircle size={18}/> Concluir leitura</button></article></section>;
}
