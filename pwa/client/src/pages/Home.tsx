import { useEffect, useState } from "react";
import { onAuthStateChanged } from "firebase/auth";
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
    const cached=readStoredSession();
    return onAuthStateChanged(firebaseAuth,async user=>{
      if(!user)return;
      if(cached?.uid===user.uid)return;
      const claims=(await user.getIdTokenResult().catch(()=>null))?.claims||{};
      setSession(current=>current||{uid:user.uid,name:"Membro MIC Rhema",isAdmin:claims.isAdmin===true,isIbr:claims.isIbr===true});
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
      onNavigate={setView}
      drawerOpen={drawerOpen}
      onOpenDrawer={()=>setDrawerOpen(true)}
      onCloseDrawer={()=>setDrawerOpen(false)}
      onProfile={()=>session?setView("profile"):setShowLogin(true)}
      onAdminLogin={()=>setShowAdminLogin(true)}
      session={session}
      onNotifications={enableNotifications}
    />
    {showLogin&&<AndroidLoginParity onClose={()=>setShowLogin(false)} onSuccess={persistSession}/>} 
    {showAdminLogin&&<AndroidLoginParity initialAdmin onClose={()=>setShowAdminLogin(false)} onSuccess={persistSession}/>} 
  </>;
}
