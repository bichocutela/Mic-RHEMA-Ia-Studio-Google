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
          const data=payload.data||{};
          const category=String(data.category||"");
          const title=payload.notification?.title||String(data.title||"MIC Rhema");
          const body=payload.notification?.body||String(data.body||"Você recebeu uma novidade.");
          if(category==="pwa_self_test"){
            void navigator.serviceWorker?.ready
              .then(registration=>registration.showNotification(title,{body,tag:"micrhema-pwa-self-test"}))
              .catch(()=>undefined);
            return;
          }
          const collection=String(data.collection||"");
          const documentId=String(data.documentId||"");
          const isAdminPrayer=collection==="prayer_requests";
          const isPrayerResponse=collection==="prayer_response"||category==="prayer_response";
          if(isAdminPrayer||isPrayerResponse)window.dispatchEvent(new CustomEvent("micrhema:prayer-updated"));
          const target=isAdminPrayer?"admin":isPrayerResponse?"prayer":"";
          toast.message(title,{
            description:body,
            action:target?{label:"Abrir",onClick:()=>{
              const params=new URLSearchParams();params.set("view",target);
              if(isAdminPrayer){params.set("section","prayers");if(documentId)params.set("request",documentId);window.dispatchEvent(new CustomEvent("micrhema:open-admin-prayer"));}
              else if(documentId)params.set("request",documentId);
              window.history.replaceState({},"",`${window.location.pathname}?${params.toString()}`);
              setView(target as AppView);
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
      const {subscribeToPwaPush,sendPwaSelfTest}=await import("@/lib/push");
      const registration=await subscribeToPwaPush();
      await sendPwaSelfTest(registration.token);
      toast.success("Avisos ativados",{description:"Enviei agora uma notificação de teste para este aparelho."});
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
