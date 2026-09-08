import { useEffect, useState } from "react";
import { onAuthStateChanged, signOut } from "firebase/auth";
import { toast } from "sonner";
import { AndroidLoginParity } from "@/components/AndroidLoginParity";
import { PwaShell, type AppView } from "@/components/PwaShell";
import { firebaseAuth } from "@/lib/firebase";
import type { PwaSession } from "@/lib/pwa-auth";

function initialViewFromUrl(): AppView {
  const requested = new URLSearchParams(window.location.search).get("view") || "";
  const allowed = new Set<AppView>(["home","bible","news","devotionals","media","ibr","menu","profile","settings","admin","discipulado","cultos","plans","prayer","members","team","donations","about"]);
  return allowed.has(requested as AppView) ? requested as AppView : "home";
}

function readStoredSession(): PwaSession | null {
  try {
    const stored=localStorage.getItem("mic-rhema-pwa-session");
    return stored?JSON.parse(stored) as PwaSession:null;
  } catch {
    localStorage.removeItem("mic-rhema-pwa-session");
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
  const[session,setSession]=useState<PwaSession|null>(()=>readStoredSession());
  const[showLogin,setShowLogin]=useState(false);
  const[showAdminLogin,setShowAdminLogin]=useState(false);
  const[drawerOpen,setDrawerOpen]=useState(false);

  useEffect(()=>{
    if(!firebaseAuth)return;
    return onAuthStateChanged(firebaseAuth,async user=>{
      if(!user){
        localStorage.removeItem("mic-rhema-pwa-session");
        setSession(null);
        return;
      }
      const cached=readStoredSession();
      if(cached?.uid===user.uid){
        setSession(cached);
        return;
      }
      const claims=(await user.getIdTokenResult().catch(()=>null))?.claims||{};
      const next:PwaSession={uid:user.uid,name:"Membro MIC Rhema",isAdmin:claims.isAdmin===true,isIbr:claims.isIbr===true};
      localStorage.setItem("mic-rhema-pwa-session",JSON.stringify(next));
      setSession(next);
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

  const persistSession=(next:PwaSession)=>{
    localStorage.setItem("mic-rhema-pwa-session",JSON.stringify(next));
    setSession(next);
    window.setTimeout(()=>void import("@/lib/push").then(module=>module.syncPwaPushPreferences()).catch(()=>undefined),0);
  };

  const clearSession=async()=>{
    if(firebaseAuth)await signOut(firebaseAuth).catch(()=>undefined);
    localStorage.removeItem("mic-rhema-pwa-session");
    setSession(null);
    setDrawerOpen(false);
  };

  const logout=async()=>{
    await clearSession();
    setShowLogin(false);
    setShowAdminLogin(false);
    setView("home");
    window.history.replaceState({},"",window.location.pathname);
    toast.success("Sessão encerrada.");
  };

  const renewLegacyAdminSession=async()=>{
    await clearSession();
    setView("profile");
    setShowAdminLogin(true);
    toast.message("Entre novamente como administrador para renovar sua sessão.");
  };

  const navigate=(next:AppView)=>{
    setView(next);
    if(next==="admin"&&!session?.isAdmin)setShowAdminLogin(true);
  };

  const openProfile=()=>{
    if(!session){setShowLogin(true);return;}
    if(session.isAdmin&&session.uid==="admin"){
      void renewLegacyAdminSession();
      return;
    }
    setView("profile");
  };

  const enableNotifications=async()=>{
    try{
      const {subscribeToPwaPush}=await import("@/lib/push");
      await subscribeToPwaPush();
      toast.success("Notificações ativadas",{description:"Este aparelho receberá os avisos permitidos nas suas configurações."});
    }catch(error){toast.error("Não foi possível ativar os avisos",{description:error instanceof Error?error.message:"Tente novamente em instantes."})}
  };

  return <>
    <PwaShell
      active={view}
      onNavigate={navigate}
      drawerOpen={drawerOpen}
      onOpenDrawer={()=>setDrawerOpen(true)}
      onCloseDrawer={()=>setDrawerOpen(false)}
      onProfile={openProfile}
      onAdminLogin={()=>setShowAdminLogin(true)}
      session={session}
      onNotifications={enableNotifications}
    />
    {session?.isAdmin&&view==="admin"&&<button
      type="button"
      onClick={()=>void logout()}
      aria-label="Sair da administração"
      style={{position:"fixed",top:"calc(env(safe-area-inset-top, 0px) + 12px)",right:16,zIndex:80,border:"1px solid #cf4a42",borderRadius:14,background:"var(--card, #fffdf7)",color:"#a23831",padding:"10px 14px",fontWeight:900,boxShadow:"0 4px 16px rgba(45,32,24,.12)"}}
    >Sair</button>}
    {showLogin&&<AndroidLoginParity onClose={()=>setShowLogin(false)} onSuccess={persistSession}/>} 
    {showAdminLogin&&<AndroidLoginParity initialAdmin onClose={()=>setShowAdminLogin(false)} onSuccess={persistSession}/>} 
  </>;
}