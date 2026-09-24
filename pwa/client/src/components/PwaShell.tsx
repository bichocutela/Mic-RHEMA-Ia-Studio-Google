import { lazy, Suspense, useEffect, useMemo, useState } from "react";
import {
  BookHeart, BookOpen, ChevronDown, ChevronRight, CircleUserRound, Copy, FileText, Grid2X2,
  HandHeart, Heart, Home, Info, Landmark, LockKeyhole, Menu as MenuIcon, PlayCircle, School,
  Settings, ShoppingBag, Users, type LucideIcon,
} from "lucide-react";
import { listenToCollection, listenToDocument, loadPwaMemberProfile, type PwaMemberProfile } from "@/lib/firebase";
import { startPwaActiveMinuteTracker } from "@/lib/badge-activity";
import type { PwaSessionLike } from "./AndroidParityViews";
import { HomeParityView } from "./HomeParityView";
import { DrawerBadgesParity } from "./DrawerBadgesParity";
import { BadgeUnlockCelebration } from "./BadgeUnlockCelebration";
import { BiblicalBadgeAvatar } from "./BiblicalBadgeAvatar";
import { LiveStreamSurface } from "./LiveStreamSurface";
import "./AndroidParityViews.css";

const AdminParityView=lazy(()=>import("./AdminParityView").then(module=>({default:module.AdminParityView})));
const PwaXpAdminPanel=lazy(()=>import("./PwaXpAdminPanel").then(module=>({default:module.PwaXpAdminPanel})));
const PrayerParityView=lazy(()=>import("./PrayerParityView").then(module=>({default:module.PrayerParityView})));
const AboutParityView=lazy(()=>import("./AboutParityView").then(module=>({default:module.AboutParityView})));
const SettingsParityViewV2=lazy(()=>import("./SettingsParityViewV2").then(module=>({default:module.SettingsParityViewV2})));
const ProfileParityViewV2=lazy(()=>import("./ProfileParityViewV2").then(module=>({default:module.ProfileParityViewV2})));
const BibleParityViewV2=lazy(()=>import("./BibleParityViewV2").then(module=>({default:module.BibleParityViewV2})));
const MediaParityViewV2=lazy(()=>import("./MediaParityViewV2").then(module=>({default:module.MediaParityViewV2})));
const DevotionalsParityView=lazy(()=>import("./DevotionalsParityView").then(module=>({default:module.DevotionalsParityView})));
const IbrParityView=lazy(()=>import("./IbrParityView").then(module=>({default:module.IbrParityView})));
const NewsParityView=lazy(()=>import("./NewsParityView").then(module=>({default:module.NewsParityView})));
const PlansParityView=lazy(()=>import("./PlansParityView").then(module=>({default:module.PlansParityView})));
const CultosParityView=lazy(()=>import("./CultosParityView").then(module=>({default:module.CultosParityView})));
const DiscipuladoParityViewV2=lazy(()=>import("./DiscipuladoParityViewV2").then(module=>({default:module.DiscipuladoParityViewV2})));
const MembersParityView=lazy(()=>import("./AndroidParityViews").then(module=>({default:module.MembersParityView})));

export type AppView =
  | "home" | "bible" | "news" | "devotionals" | "media" | "ibr" | "menu" | "profile"
  | "settings" | "admin" | "xp-admin" | "discipulado" | "cultos" | "plans" | "prayer"
  | "members" | "team" | "donations" | "about";

type TeamMember = { id:string; name?:string; role?:string; category?:string; imageUrl?:string; order?:number };
type DonationSettings = { pixKey?:string; qrCodeUrl?:string };

type AppTabConfig = {
  id:string; title?:string; iconName?:string; isPrivate?:boolean; isVisible?:boolean;
  showInBottomBar?:boolean; order?:number; type?:string; systemRoute?:string|null;
};
type GlobalAppSettings = { showDonationsTab?:boolean; notificationsEnabled?:boolean };

const fallbackTabs:AppTabConfig[]=[
  {id:"1",title:"Início",iconName:"Home",isVisible:true,showInBottomBar:true,order:0,systemRoute:"home"},
  {id:"bible_tab",title:"Bíblia",iconName:"MenuBook",isVisible:true,showInBottomBar:false,order:1,systemRoute:"bible"},
  {id:"2",title:"Cultos",iconName:"DateRange",isVisible:true,showInBottomBar:true,order:2,systemRoute:"services"},
  {id:"3",title:"Devocionais",iconName:"Book",isVisible:true,showInBottomBar:false,order:3,systemRoute:"devocionais"},
  {id:"4",title:"Cursos IBR",iconName:"School",isVisible:true,showInBottomBar:false,order:4,systemRoute:"ibr"},
  {id:"discipulado_tab",title:"Discipulado",iconName:"MenuBook",isVisible:true,showInBottomBar:false,order:5,systemRoute:"discipulado"},
  {id:"5",title:"Mídia",iconName:"PlayArrow",isVisible:true,showInBottomBar:false,order:6,systemRoute:"content"},
  {id:"6",title:"Pedidos de Oração",iconName:"Favorite",isVisible:true,showInBottomBar:true,order:7,systemRoute:"prayer"},
  {id:"plans_tab",title:"Planos",iconName:"List",isVisible:true,showInBottomBar:true,order:8,systemRoute:"plans"},
  {id:"team_tab",title:"Equipe",iconName:"Groups",isVisible:true,showInBottomBar:false,order:9,systemRoute:"equipe"},
  {id:"7",title:"Membros",iconName:"Person",isVisible:true,showInBottomBar:false,order:10,systemRoute:"members"},
  {id:"8",title:"Sobre",iconName:"Info",isVisible:true,showInBottomBar:false,order:11,systemRoute:"about"},
  {id:"settings_tab",title:"Configurações",iconName:"Settings",isVisible:true,showInBottomBar:false,order:12,systemRoute:"settings"},
  {id:"10",title:"Dízimos e Ofertas",iconName:"VolunteerActivism",isVisible:true,showInBottomBar:true,order:13,systemRoute:"donations"},
  {id:"admin_tab",title:"Área ADM",iconName:"Lock",isVisible:true,showInBottomBar:false,order:14,systemRoute:"admin"},
];

const tabRouteMap:Record<string,AppView>={
  home:"home", bible:"bible", devocionais:"devotionals", services:"cultos", ibr:"ibr",
  discipulado:"discipulado", content:"media", prayer:"prayer", plans:"plans", equipe:"team",
  members:"members", about:"about", settings:"settings", donations:"donations", admin:"admin",
};
const tabIdRouteMap:Record<string,AppView>={
  "1":"home",bible_tab:"bible","2":"cultos","3":"devotionals","4":"ibr",discipulado_tab:"discipulado",
  "5":"media","6":"prayer",plans_tab:"plans",team_tab:"team","7":"members","8":"about",
  settings_tab:"settings","10":"donations",admin_tab:"admin",
};
function tabView(tab:AppTabConfig):AppView|null{
  const route=String(tab.systemRoute||"").trim();
  return tabRouteMap[route]||tabIdRouteMap[tab.id]||null;
}
function tabIcon(tab:AppTabConfig):LucideIcon{
  const title=String(tab.title||"");
  if(title==="Bíblia")return BookOpen;if(title==="Devocionais")return FileText;if(title==="Cursos IBR")return School;
  if(title==="Mídia")return PlayCircle;if(title==="Pedidos de Oração")return HandHeart;if(title==="Membros")return Users;
  if(title==="Equipe")return CircleUserRound;if(title==="Dízimos e Ofertas")return Heart;if(title==="Configurações")return Settings;
  if(title==="Sobre")return Info;if(title==="Área ADM")return LockKeyhole;if(title==="Cultos")return Landmark;
  if(title==="Discipulado")return BookHeart;if(title==="Planos")return BookOpen;if(title==="Início")return Home;
  const name=String(tab.iconName||"");
  if(name==="Home")return Home;if(name==="Book"||name==="MenuBook"||name==="List")return BookOpen;
  if(name==="School")return School;if(name==="PlayArrow"||name==="Video")return PlayCircle;if(name==="Favorite")return Heart;
  if(name==="People"||name==="Groups"||name==="Person")return Users;if(name==="Settings")return Settings;
  if(name==="Info")return Info;if(name==="Lock")return LockKeyhole;if(name==="DateRange")return Landmark;
  return Grid2X2;
}
function groupForTitle(title:string){
  if(["Pedidos de Oração","Membros","Equipe"].includes(title))return "COMUNIDADE";
  if(["Cultos","Dízimos e Ofertas"].includes(title))return "IGREJA";
  if(["Configurações","Sobre"].includes(title))return "SISTEMA";
  if(["Área ADM"].includes(title))return "ADMINISTRAÇÃO";
  return "CONTEÚDO";
}
const groupIcons:Record<string,LucideIcon>={
  "CONTEÚDO":Grid2X2,"COMUNIDADE":Users,"IGREJA":Landmark,"SISTEMA":Settings,"ADMINISTRAÇÃO":LockKeyhole,
};

function RouteFallback(){return <section className="page-pad android-module"><div className="parity-empty"><span className="pwa-route-spinner" aria-hidden="true"/><p>Carregando…</p></div></section>}
function AccessPrompt({admin,onAction}:{admin?:boolean;onAction:()=>void}){return <section className="parity-page"><div className="parity-empty"><LockKeyhole size={48}/><h1>{admin?"Área Administrativa":"Área de Membros"}</h1><p>{admin?"Entre com o acesso administrativo para continuar.":"Entre na sua conta para abrir este conteúdo."}</p><button className="android-primary-action" onClick={onAction}>{admin?"Entrar como administrador":"Entrar"}</button></div></section>}

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
      {member.imageUrl?<img loading="lazy" src={member.imageUrl} alt={`Foto de ${member.name||"membro"}`} style={{width:58,height:58,borderRadius:"50%",objectFit:"cover"}}/>:<CircleUserRound size={38}/>}<div><strong>{member.name||"Membro da equipe"}</strong><small>{[member.role,member.category].filter(Boolean).join(" · ")||"Equipe MIC Rhema"}</small></div>
    </article>)}</div>}
  </section>;
}

function DonationsParityView(){
  const[settings,setDonationSettings]=useState<(DonationSettings&{id:string})|null>(null);
  useEffect(()=>listenToDocument<DonationSettings>("settings","donations",setDonationSettings,()=>setDonationSettings(null)),[]);
  const pixKey=settings?.pixKey?.trim()||"";
  const qr=settings?.qrCodeUrl?.trim()||"";
  const copy=async()=>{if(!pixKey)return;try{await navigator.clipboard.writeText(pixKey)}catch{return}};
  return <section className="page-pad android-module">
    <div className="android-section-heading"><div><p>IGREJA</p><h2>Dízimos e Ofertas</h2></div></div><p>Contribua com a obra de Deus.</p>
    {!pixKey&&!qr?<p className="empty-module">As informações de doação ainda não foram configuradas.</p>:<article className="android-module-card" style={{display:"flex",flexDirection:"column",alignItems:"center",gap:14}}>
      {qr&&<img loading="lazy" src={qr} alt="QR Code Pix" style={{width:210,maxWidth:"100%",aspectRatio:"1 / 1",objectFit:"contain",borderRadius:16}}/>}
      {pixKey&&<><div style={{textAlign:"center"}}><strong>Chave PIX</strong><small style={{display:"block",marginTop:6,wordBreak:"break-all"}}>{pixKey}</small></div><button className="android-primary-action" onClick={()=>void copy()}><Copy size={18}/><span>Copiar Chave</span></button></>}
    </article>}
  </section>;
}

function AndroidDrawer({active,onNavigate,onProfile,onClose,session,onNotifications,drawerTabs,notificationsEnabled}:{active:AppView;onNavigate:(view:AppView)=>void;onProfile:()=>void;onClose:()=>void;session:PwaSessionLike;onNotifications:()=>void;drawerTabs:AppTabConfig[];notificationsEnabled:boolean}){
  const[expanded,setExpanded]=useState<Set<string>>(()=>new Set(["CONTEÚDO"]));
  const[drawerProfile,setDrawerProfile]=useState<PwaMemberProfile|null>(null);
  useEffect(()=>{
    if(!session){setDrawerProfile(null);return;}
    let activeRequest=true;
    void loadPwaMemberProfile().then(profile=>{if(activeRequest)setDrawerProfile(profile)}).catch(()=>{if(activeRequest)setDrawerProfile(null)});
    return()=>{activeRequest=false};
  },[session?.uid]);
  const toggle=(title:string)=>setExpanded(current=>{const next=new Set(current);next.has(title)?next.delete(title):next.add(title);return next});
  const go=(view:AppView)=>{onNavigate(view);onClose()};
  const userName=session?.name||"Entrar";
  const drawerAvatarId=drawerProfile?.avatarId||"davi";
  const drawerBadgeId=drawerProfile?.equippedBadgeId||"caminhante";
  return <aside className="android-drawer" role="dialog" aria-modal="true" aria-label="Menu do MIC Rhema">
    <button className="drawer-dismiss" aria-label="Fechar menu" onClick={onClose}/>
    <section className="drawer-sheet">
      <button className="drawer-profile" onClick={()=>{onProfile();onClose()}}>
        {session?<span className="drawer-avatar" style={{width:58,height:58,display:"grid",placeItems:"center",background:"transparent",overflow:"visible"}}><BiblicalBadgeAvatar avatarId={drawerAvatarId} profilePhotoUrl={drawerProfile?.profilePhotoUrl||""} badgeId={drawerBadgeId} size={58} title={`Avatar de ${userName}`}/></span>:<span className="drawer-avatar"><CircleUserRound size={25}/></span>}
        <span><strong>{userName}</strong><small>{session?"Meu Perfil":"Solicite acesso para membros"}</small></span><ChevronRight size={19}/>
      </button>
      <DrawerBadgesParity session={session}/>
      {(["CONTEÚDO","COMUNIDADE","IGREJA","SISTEMA","ADMINISTRAÇÃO"] as const).map(groupTitle=>{const items=drawerTabs.filter(tab=>groupForTitle(String(tab.title||""))===groupTitle).map(tab=>({tab,view:tabView(tab)})).filter((item):item is {tab:AppTabConfig;view:AppView}=>Boolean(item.view));if(!items.length)return null;const GroupIcon=groupIcons[groupTitle];const open=expanded.has(groupTitle);return <section className="drawer-group" key={groupTitle}>
        <button className="drawer-group-title" onClick={()=>toggle(groupTitle)}><span><GroupIcon size={18}/>{groupTitle}</span><ChevronDown className={open?"is-open":""} size={18}/></button>
        {open&&<div className="drawer-items">{items.map(({tab,view})=>{const Icon=tabIcon(tab);return <button className={active===view?"is-current":""} key={tab.id} onClick={()=>go(view)}><Icon size={19}/><span>{tab.title||"Aba"}</span></button>})}</div>}
      </section>})}
      {notificationsEnabled&&<button className="drawer-notifications" onClick={onNotifications}><span>Ativar notificações</span><small>Escolha receber avisos desta PWA</small></button>}
    </section>
  </aside>;
}

export function PwaShell({active,onNavigate,drawerOpen,onCloseDrawer,onOpenDrawer,onProfile,onAdminLogin,session,onNotifications}:{active:AppView;onNavigate:(view:AppView)=>void;drawerOpen:boolean;onCloseDrawer:()=>void;onOpenDrawer:()=>void;onProfile:()=>void;onAdminLogin:()=>void;session:PwaSessionLike;onNotifications:()=>void}){
  useEffect(()=>startPwaActiveMinuteTracker(),[]);
  const[remoteTabs,setRemoteTabs]=useState<AppTabConfig[]|null>(null);
  const[globalSettings,setGlobalSettings]=useState<GlobalAppSettings>({});
  useEffect(()=>listenToCollection<AppTabConfig>("app_tabs",items=>setRemoteTabs(items),()=>setRemoteTabs(null)),[]);
  useEffect(()=>listenToDocument<GlobalAppSettings>("settings","app",value=>setGlobalSettings(value||{}),()=>setGlobalSettings({})),[]);
  const visibleTabs=useMemo(()=>{
    const source=(remoteTabs??fallbackTabs).slice().sort((a,b)=>Number(a.order||0)-Number(b.order||0));
    return source.filter(tab=>tab.isVisible!==false&&(tab.id!=="10"||globalSettings.showDonationsTab!==false)&&Boolean(tabView(tab)));
  },[remoteTabs,globalSettings.showDonationsTab]);
  const bottomTabs=useMemo(()=>visibleTabs.filter(tab=>tab.showInBottomBar===true),[visibleTabs]);
  const drawerTabs=useMemo(()=>visibleTabs.filter(tab=>tab.showInBottomBar!==true),[visibleTabs]);
  const content=active==="home"?<HomeParityView session={session} onNavigate={onNavigate}/>
    :active==="bible"?<BibleParityViewV2/>
    :active==="news"?<NewsParityView onNavigate={onNavigate}/>
    :active==="devotionals"?<DevotionalsParityView/>
    :active==="media"?<MediaParityViewV2/>
    :active==="ibr"?<IbrParityView session={session} onLogin={onProfile}/>
    :active==="plans"?<PlansParityView/>
    :active==="cultos"?<CultosParityView/>
    :active==="discipulado"?<DiscipuladoParityViewV2/>
    :active==="admin"&&!session?.isAdmin?<AccessPrompt admin onAction={onAdminLogin}/>
    :active==="admin"?<AdminParityView session={session}/>
    :active==="xp-admin"&&!session?.isAdmin?<AccessPrompt admin onAction={onAdminLogin}/>
    :active==="xp-admin"?<PwaXpAdminPanel/>
    :active==="profile"&&!session?<AccessPrompt onAction={onProfile}/>
    :active==="profile"?<ProfileParityViewV2 session={session} onNavigateHome={()=>onNavigate("home")}/>
    :active==="settings"?<SettingsParityViewV2 session={session} onProfile={onProfile} onNotifications={onNotifications}/>
    :active==="members"?<MembersParityView session={session} onProfile={onProfile}/>
    :active==="prayer"?<PrayerParityView session={session}/>
    :active==="team"?<TeamParityView/>
    :active==="donations"?<DonationsParityView/>
    :active==="about"?<AboutParityView/>
    :<HomeParityView session={session} onNavigate={onNavigate}/>;
  return <div className="android-app-shell">
    <main className="android-app-content"><LiveStreamSurface visible={active==="home"}/><Suspense fallback={<RouteFallback/>}>{content}</Suspense></main>
    {active!=="admin"&&active!=="xp-admin"&&<nav className="android-bottom-dock" aria-label="Navegação principal">{bottomTabs.map(tab=>{const id=tabView(tab)!;const Icon=tabIcon(tab);const selected=active===id;return <button className={selected?"is-active":""} key={tab.id} onClick={()=>onNavigate(id)} aria-current={selected?"page":undefined}><Icon size={20} strokeWidth={selected?2.4:1.9}/>{selected&&<span>{tab.title||"Aba"}</span>}</button>})}<button onClick={onOpenDrawer} aria-label="Abrir menu"><MenuIcon size={22}/></button></nav>}
    {drawerOpen&&<AndroidDrawer active={active} onNavigate={onNavigate} onProfile={onProfile} onClose={onCloseDrawer} session={session} onNotifications={onNotifications} drawerTabs={drawerTabs} notificationsEnabled={globalSettings.notificationsEnabled!==false}/>} 
    <BadgeUnlockCelebration onOpenBadges={()=>{onCloseDrawer();onNavigate("profile")}}/>
  </div>;
}

export function SectionHeading({eyebrow,title,action,onAction}:{eyebrow?:string;title:string;action?:string;onAction?:()=>void}){
  return <div className="android-section-heading"><div>{eyebrow&&<p>{eyebrow}</p>}<h2>{title}</h2></div>{action&&<button onClick={onAction}>{action}<ChevronRight size={16}/></button>}</div>;
}