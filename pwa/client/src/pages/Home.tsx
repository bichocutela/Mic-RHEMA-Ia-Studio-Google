import { useEffect, useState } from "react";
import { onAuthStateChanged, signOut } from "firebase/auth";
import { toast } from "sonner";
import { AndroidLoginParity } from "@/components/AndroidLoginParity";
import { PwaShell, type AppView } from "@/components/PwaShell";
import { firebaseAdminAuth, firebaseAuth } from "@/lib/firebase";
import type { PwaSession } from "@/lib/pwa-auth";

const MEMBER_SESSION_KEY="mic-rhema-pwa-session";
const ADMIN_SESSION_KEY="mic-rhema-pwa-admin-session";

function initialViewFromUrl(): AppView {
  const requested = new URLSearchParams(window.location.search).get("view") || "";
  const allowed = new Set<AppView>(["home","bible","news","devotionals","media","ibr","menu","profile","settings","admin","discipulado","cultos","plans","prayer","members","team","donations","about"]);
  return allowed.has(requested as AppView) ? requested as AppView : "home";
}

function readStoredSession(key:string): PwaSession | null {
  try {
    const stored=localStorage.getItem(key);
    return stored?JSON.parse(stored) as PwaSession:null;
  } catch {
    localStorage.removeItem(key);
    return null;
  }
}

type PushRoute={view:AppView;adminPrayer?:boolean;requestId?:string};
function routeForPush(data:Record<string,string>):PushRoute|null{
  const collection=String(data.collection||"").toLowerCase();
  const category=String(data.category||"").toLowerCase();
  const destination=String(data.destination||"").toLowerCase();
  const documentId=String(data.documentId||"");
  if(collection==="prayer_requests"||destination.startsWith("admin_prayer"))return{view:"admin",adminPrayer:true,requestId:documentId};
  if(collection==="prayer_response"||category==="prayer_response"||destination==="prayer")return{view:"prayer",requestId:documentId};
  if(destination==="ibr"||category.includes("ibr")||category.includes("course"))return{view:"ibr"};
  if(destination==="content"||category.includes("sermon")||category.includes("media")||category.includes("audio")||category.includes("video")||category.includes("book"))return{view:"media"};
  if(destination==="services"||category.includes("event")||category.includes("service")||category.includes("culto"))return{view:"cultos"};
  if(category.includes("devotional"))return{view:"devotionals"};
  if(category.includes("news")||category.includes("noticia"))return{view:"news"};
  return null;
}

export default function Home() {
  const[view,setView]=useState<AppView>(()=>initialViewFromUrl());
  const[session,setSession]=useState<PwaSession|null>(()=>readStoredSession(MEMBER_SESSION_KEY));
  const[adminSession,setAdminSession]=useState<PwaSession|null>(()=>readStoredSession(ADMIN_SESSION_KEY));
  const[showLogin,setShowLogin]=useState(false);
  const[showAdminLogin,setShowAdminLogin]=useState(false);
  const[drawerOpen,setDrawerOpen]=useState(false);

  useEffect(()=>{
    if(!firebaseAuth)return;
    return onAuthStateChanged(firebaseAuth,async user=>{
      if(!user){
        localStorage.removeItem(MEMBER_SESSION_KEY);
        setSession(null);
        return;
      }
      const cached=readStoredSession(MEMBER_SESSION_KEY);
      if(cached?.uid===user.uid){
        const memberSession={...cached,isAdmin:false};
        localStorage.setItem(MEMBER_SESSION_KEY,JSON.stringify(memberSession));
        setSession(memberSession);
        return;
      }
      const claims=(await user.getIdTokenResult().catch(()=>null))?.claims||{};
      const next:PwaSession={uid:user.uid,name:"Membro MIC Rhema",isAdmin:false,isIbr:claims.isIbr===true};
      localStorage.setItem(MEMBER_SESSION_KEY,JSON.stringify(next));
      setSession(next);
    });
  },[]);

  useEffect(()=>{
    const adminAuth=firebaseAdminAuth;
    if(!adminAuth)return;
    return onAuthStateChanged(adminAuth,async user=>{
      if(!user){
        localStorage.removeItem(ADMIN_SESSION_KEY);
        setAdminSession(null);
        return;
      }
      const cached=readStoredSession(ADMIN_SESSION_KEY);
      if(cached?.uid===user.uid&&cached.isAdmin){
        setAdminSession(cached);
        return;
      }
      const claims=(await user.getIdTokenResult().catch(()=>null))?.claims||{};
      if(claims.isAdmin!==true){
        await signOut(adminAuth).catch(()=>undefined);
        localStorage.removeItem(ADMIN_SESSION_KEY);
        setAdminSession(null);
        return;
      }
      const next:PwaSession={uid:user.uid,name:"Administrador",isAdmin:true,isIbr:claims.isIbr===true};
      localStorage.setItem(ADMIN_SESSION_KEY,JSON.stringify(next));
      setAdminSession(next);
    });
  },[]);

  useEffect(()=>{
    let cancelled=false;
    let unsubscribe:()=>void=()=>undefined;
    const start=()=>{
      void import("@/lib/push").then(async module=>{
        if(cancelled)return;
        unsubscribe=await module.listenToForegroundPush(payload=>{
          const data=(payload.data||{}) as Record<string,string>;
          const collection=String(data.collection||"");
          const category=String(data.category||"");
          const documentId=String(data.documentId||"");
          const title=payload.notification?.title||String(data.title||"MIC Rhema");
          const body=payload.notification?.body||String(data.body||"Você recebeu uma novidade.");
          const isAdminPrayer=collection==="prayer_requests";
          const isPrayerResponse=collection==="prayer_response"||category==="prayer_response";
          if(isAdminPrayer||isPrayerResponse)window.dispatchEvent(new CustomEvent("micrhema:prayer-updated"));
          const target=routeForPush(data);
          toast.message(title,{
            description:body,
            action:target?{label:"Abrir",onClick:()=>{
              const params=new URLSearchParams();params.set("view",target.view);
              if(target.adminPrayer){params.set("section","prayers");if(documentId)params.set("request",documentId);window.dispatchEvent(new CustomEvent("micrhema:open-admin-prayer"));}
              else if(target.view==="prayer"&&target.requestId)params.set("request",target.requestId);
              window.history.replaceState({},"",`${window.location.pathname}?${params.toString()}`);
              setView(target.view);
            }}:undefined,
          });
        });
      }).catch(()=>undefined);
    };
    const idleWindow=window as Window & {requestIdleCallback?:(callback:()=>void,options?:{timeout:number})=>number;cancelIdleCallback?:(id:number)=>void};
    const idleId=idleWindow.requestIdleCallback?.(start,{timeout:2500});
    const timer=idleId==null?window.setTimeout(start,1200):null;
    return()=>{cancelled=true;unsubscribe();if(timer!=null)window.clearTimeout(timer);if(idleId!=null)idleWindow.cancelIdleCallback?.(idleId)};
  },[]);

  const persistMemberSession=(next:PwaSession)=>{
    const memberSession={...next,isAdmin:false};
    localStorage.setItem(MEMBER_SESSION_KEY,JSON.stringify(memberSession));
    setSession(memberSession);
    window.setTimeout(()=>void import("@/lib/push").then(module=>module.syncPwaPushPreferences()).catch(()=>undefined),0);
  };

  const persistAdminSession=(next:PwaSession)=>{
    if(!next.isAdmin){
      toast.error("Este acesso não possui permissão administrativa.");
      return;
    }
    localStorage.setItem(ADMIN_SESSION_KEY,JSON.stringify(next));
    setAdminSession(next);
  };

  const logoutAdmin=async()=>{
    const adminAuth=firebaseAdminAuth;
    if(adminAuth)await signOut(adminAuth).catch(()=>undefined);
    localStorage.removeItem(ADMIN_SESSION_KEY);
    setAdminSession(null);
    setShowAdminLogin(false);
    setDrawerOpen(false);
    setView("admin");
    toast.success("Sessão administrativa encerrada. Seu Meu Perfil continua conectado.");
  };

  const navigate=(next:AppView)=>{
    setView(next);
    if(next==="admin"&&!adminSession?.isAdmin)setShowAdminLogin(true);
  };

  const openProfile=()=>{
    if(!session){setShowLogin(true);return;}
    setView("profile");
  };

  const enableNotifications=async()=>{
    try{
      const {subscribeToPwaPush}=await import("@/lib/push");
      await subscribeToPwaPush();
      toast.success("Notificações ativadas",{description:"Este aparelho receberá os avisos permitidos nas suas configurações."});
    }catch(error){toast.error("Não foi possível ativar os avisos",{description:error instanceof Error?error.message:"Tente novamente em instantes."})}
  };

  const shellSession=view==="admin"?adminSession:session;

  return <>
    <PwaShell
      active={view}
      onNavigate={navigate}
      drawerOpen={drawerOpen}
      onOpenDrawer={()=>setDrawerOpen(true)}
      onCloseDrawer={()=>setDrawerOpen(false)}
      onProfile={openProfile}
      onAdminLogin={()=>setShowAdminLogin(true)}
      session={shellSession}
      onNotifications={enableNotifications}
    />
    {adminSession?.isAdmin&&view==="admin"&&<button
      type="button"
      onClick={()=>void logoutAdmin()}
      aria-label="Sair da administração"
      style={{position:"fixed",top:"calc(env(safe-area-inset-top, 0px) + 12px)",right:16,zIndex:80,border:"1px solid #cf4a42",borderRadius:14,background:"var(--card, #fffdf7)",color:"#a23831",padding:"10px 14px",fontWeight:900,boxShadow:"0 4px 16px rgba(45,32,24,.12)"}}
    >Sair</button>}
    {showLogin&&<AndroidLoginParity onClose={()=>setShowLogin(false)} onSuccess={persistMemberSession}/>} 
    {showAdminLogin&&<AndroidLoginParity initialAdmin onClose={()=>setShowAdminLogin(false)} onSuccess={persistAdminSession}/>} 
  </>;
}