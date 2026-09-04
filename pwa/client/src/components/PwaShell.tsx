import { useEffect, useMemo, useState } from "react";
import {
  BookHeart, BookOpen, ChevronDown, ChevronRight, CircleUserRound, Copy, FileText, Grid2X2,
  HandHeart, Heart, Home, Info, Landmark, LockKeyhole, Menu as MenuIcon, PlayCircle, School,
  Settings, Users, type LucideIcon,
} from "lucide-react";
import { listenToCollection, listenToDocument } from "@/lib/firebase";
import { MembersParityView, type PwaSessionLike } from "./AndroidParityViews";
import { AdminParityView } from "./AdminParityView";
import { PrayerParityView } from "./PrayerParityView";
import { AboutParityView } from "./AboutParityView";
import { SettingsParityViewV2 } from "./SettingsParityViewV2";
import { ProfileParityViewV2 } from "./ProfileParityViewV2";
import { HomeParityView } from "./HomeParityView";
import { BibleParityViewV2 } from "./BibleParityViewV2";
import { MediaParityViewV2 } from "./MediaParityViewV2";
import { DevotionalsParityView } from "./DevotionalsParityView";
import { IbrParityView } from "./IbrParityView";
import { NewsParityView } from "./NewsParityView";
import { PlansParityView } from "./PlansParityView";
import { CultosParityView } from "./CultosParityView";
import { DiscipuladoParityViewV2 } from "./DiscipuladoParityViewV2";
import { DrawerBadgesParity } from "./DrawerBadgesParity";
import "./AndroidParityViews.css";

export type AppView =
  | "home" | "bible" | "news" | "devotionals" | "media" | "ibr" | "menu" | "profile"
  | "settings" | "admin" | "discipulado" | "cultos" | "plans" | "prayer"
  | "members" | "team" | "donations" | "about";

type TeamMember = { id:string; name?:string; role?:string; category?:string; imageUrl?:string; order?:number };
type DonationSettings = { pixKey?:string; qrCodeUrl?:string };

const primaryItems:Array<{id:AppView;label:string;icon:LucideIcon}>=[
  {id:"home",label:"Início",icon:Home},
  {id:"bible",label:"Bíblia",icon:BookOpen},
  {id:"devotionals",label:"Devocionais",icon:FileText},
  {id:"ibr",label:"IBR",icon:School},
];

const drawerGroups:Array<{title:string;icon:LucideIcon;items:Array<{id:AppView;label:string;icon:LucideIcon}>}>=[
  {title:"CONTEÚDO",icon:Grid2X2,items:[
    {id:"home",label:"Início",icon:Home},{id:"bible",label:"Bíblia",icon:BookOpen},
    {id:"devotionals",label:"Devocionais",icon:FileText},{id:"ibr",label:"Cursos IBR",icon:School},
    {id:"discipulado",label:"Discipulado",icon:BookHeart},{id:"media",label:"Mídia",icon:PlayCircle},
    {id:"plans",label:"Planos",icon:BookOpen},
  ]},
  {title:"COMUNIDADE",icon:Users,items:[
    {id:"prayer",label:"Pedidos de Oração",icon:HandHeart},{id:"members",label:"Membros",icon:Users},
    {id:"team",label:"Equipe",icon:CircleUserRound},
  ]},
  {title:"IGREJA",icon:Landmark,items:[
    {id:"cultos",label:"Cultos",icon:Landmark},{id:"donations",label:"Dízimos e Ofertas",icon:Heart},
  ]},
  {title:"SISTEMA",icon:Settings,items:[
    {id:"settings",label:"Configurações",icon:Settings},{id:"about",label:"Sobre",icon:Info},
  ]},
];

function TeamParityView(){
  const[members,setMembers]=useState<TeamMember[]>([]);
  const[category,setCategory]=useState("Todos");
  useEffect(()=>listenToCollection<TeamMember>("equipe",setMembers,()=>setMembers([])),[]);
  const categories=useMemo(()=>["Todos",...Array.from(new Set(members.map(member=>member.category?.trim()).filter(Boolean) as string[]))],[members]);
  const visible=useMemo(()=>members
    .filter(member=>category==="Todos"||member.category?.toLowerCase()===category.toLowerCase())
    .slice().sort((a,b)=>Number(a.order||0)-Number(b.order||0)),[members,category]);
  return <section className="page-pad android-module">
    <div className="android-section-heading"><div><p>EQUIPE</p><h2>Nossa Equipe</h2></div></div>
    <div className="filter-pills">{categories.map(item=><button key={item} className={category===item?"selected":""} onClick={()=>setCategory(item)}>{item}</button>)}</div>
    {!visible.length?<p className="empty-module">Nenhum membro da equipe cadastrado nesta categoria.</p>:<div className="android-list-cards">{visible.map(member=><article key={member.id} className="android-module-card">
      {member.imageUrl?<img src={member.imageUrl} alt={`Foto de ${member.name||"membro"}`} style={{width:58,height:58,borderRadius:"50%",objectFit:"cover"}}/>:<CircleUserRound size={38}/>}<div><strong>{member.name||"Membro da equipe"}</strong><small>{[member.role,member.category].filter(Boolean).join(" · ")||"Equipe MIC Rhema"}</small></div>
    </article>)}</div>}
  </section>;
}

function DonationsParityView(){
  const[settings,setDonationSettings]=useState<(DonationSettings&{id:string})|null>(null);
  useEffect(()=>listenToDocument<DonationSettings>("settings","donations",setDonationSettings,()=>setDonationSettings(null)),[]);
  const pixKey=settings?.pixKey?.trim()||"";
  const qr=settings?.qrCodeUrl?.trim()||"";
  const copy=async()=>{if(!pixKey)return;try{await navigator.clipboard.writeText(pixKey)}catch{ return; }};
  return <section className="page-pad android-module">
    <div className="android-section-heading"><div><p>IGREJA</p><h2>Dízimos e Ofertas</h2></div></div><p>Contribua com a obra de Deus.</p>
    {!pixKey&&!qr?<p className="empty-module">As informações de doação ainda não foram configuradas.</p>:<article className="android-module-card" style={{display:"flex",flexDirection:"column",alignItems:"center",gap:14}}>
      {qr&&<img src={qr} alt="QR Code Pix" style={{width:210,maxWidth:"100%",aspectRatio:"1 / 1",objectFit:"contain",borderRadius:16}}/>}
      {pixKey&&<><div style={{textAlign:"center"}}><strong>Chave PIX</strong><small style={{display:"block",marginTop:6,wordBreak:"break-all"}}>{pixKey}</small></div><button className="android-primary-action" onClick={()=>void copy()}><Copy size={18}/><span>Copiar Chave</span></button></>}
    </article>}
  </section>;
}

function AndroidDrawer({active,onNavigate,onProfile,onClose,session,onNotifications}:{active:AppView;onNavigate:(view:AppView)=>void;onProfile:()=>void;onClose:()=>void;session:PwaSessionLike;onNotifications:()=>void}){
  const[expanded,setExpanded]=useState<Set<string>>(()=>new Set(["CONTEÚDO"]));
  const toggle=(title:string)=>setExpanded(current=>{const next=new Set(current);next.has(title)?next.delete(title):next.add(title);return next});
  const go=(view:AppView)=>{onNavigate(view);onClose()};
  const userName=session?.name||"Entrar";
  return <aside className="android-drawer" role="dialog" aria-modal="true" aria-label="Menu do MIC Rhema">
    <button className="drawer-dismiss" aria-label="Fechar menu" onClick={onClose}/>
    <section className="drawer-sheet">
      <button className="drawer-profile" onClick={()=>{onProfile();onClose()}}><span className="drawer-avatar">{session?userName.slice(0,1).toUpperCase():<CircleUserRound size={25}/>}</span><span><strong>{userName}</strong><small>{session?"Meu Perfil":"Solicite acesso para membros"}</small></span><ChevronRight size={19}/></button>
      <DrawerBadgesParity session={session}/>
      {drawerGroups.map(group=>{const GroupIcon=group.icon;const open=expanded.has(group.title);return <section className="drawer-group" key={group.title}>
        <button className="drawer-group-title" onClick={()=>toggle(group.title)}><span><GroupIcon size={18}/>{group.title}</span><ChevronDown className={open?"is-open":""} size={18}/></button>
        {open&&<div className="drawer-items">{group.items.map(({id,label,icon:Icon})=><button className={active===id?"is-current":""} key={id} onClick={()=>go(id)}><Icon size={19}/><span>{label}</span></button>)}</div>}
      </section>})}
      <section className="drawer-group drawer-admin-group"><button className="drawer-group-title" onClick={()=>toggle("ADMINISTRAÇÃO")}><span><LockKeyhole size={18}/>ADMINISTRAÇÃO</span><ChevronDown className={expanded.has("ADMINISTRAÇÃO")?"is-open":""} size={18}/></button>{expanded.has("ADMINISTRAÇÃO")&&<div className="drawer-items"><button className={active==="admin"?"is-current":""} onClick={()=>go("admin")}><LockKeyhole size={19}/><span>Área ADM</span></button></div>}</section>
      <button className="drawer-notifications" onClick={onNotifications}><span>Ativar notificações</span><small>Escolha receber avisos desta PWA</small></button>
    </section>
  </aside>;
}

export function PwaShell({children,active,onNavigate,drawerOpen,onCloseDrawer,onOpenDrawer,onProfile,session,onNotifications}:{children:React.ReactNode;active:AppView;onNavigate:(view:AppView)=>void;drawerOpen:boolean;onCloseDrawer:()=>void;onOpenDrawer:()=>void;onProfile:()=>void;session:PwaSessionLike;onNotifications:()=>void}){
  const content=active==="home"?<HomeParityView session={session} onNavigate={onNavigate}/>
    :active==="bible"?<BibleParityViewV2/>
    :active==="news"?<NewsParityView onNavigate={onNavigate}/>
    :active==="devotionals"?<DevotionalsParityView/>
    :active==="media"?<MediaParityViewV2/>
    :active==="ibr"?<IbrParityView session={session} onLogin={onProfile}/>
    :active==="plans"?<PlansParityView/>
    :active==="cultos"?<CultosParityView/>
    :active==="discipulado"?<DiscipuladoParityViewV2/>
    :active==="admin"?<AdminParityView session={session}/>
    :active==="profile"&&session&&!session.isAdmin?<ProfileParityViewV2 session={session} onNavigateHome={()=>onNavigate("home")}/>
    :active==="settings"?<SettingsParityViewV2 session={session} onProfile={onProfile} onNotifications={onNotifications}/>
    :active==="members"&&!session?.isAdmin?<MembersParityView session={session} onProfile={onProfile}/>
    :active==="prayer"?<PrayerParityView/>
    :active==="team"?<TeamParityView/>
    :active==="donations"?<DonationsParityView/>
    :active==="about"?<AboutParityView/>
    :children;
  return <div className="android-app-shell">
    <main className="android-app-content">{content}</main>
    <nav className="android-bottom-dock" aria-label="Navegação principal">{primaryItems.map(({id,label,icon:Icon})=>{const selected=active===id;return <button className={selected?"is-active":""} key={id} onClick={()=>onNavigate(id)} aria-current={selected?"page":undefined}><Icon size={20} strokeWidth={selected?2.4:1.9}/>{selected&&<span>{label}</span>}</button>})}<button onClick={onOpenDrawer} aria-label="Abrir menu"><MenuIcon size={22}/></button></nav>
    {drawerOpen&&<AndroidDrawer active={active} onNavigate={onNavigate} onProfile={onProfile} onClose={onCloseDrawer} session={session} onNotifications={onNotifications}/>} 
  </div>;
}

export function SectionHeading({eyebrow,title,action,onAction}:{eyebrow?:string;title:string;action?:string;onAction?:()=>void}){
  return <div className="android-section-heading"><div>{eyebrow&&<p>{eyebrow}</p>}<h2>{title}</h2></div>{action&&<button onClick={onAction}>{action}<ChevronRight size={16}/></button>}</div>;
}
