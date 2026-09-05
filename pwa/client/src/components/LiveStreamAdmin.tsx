import { useEffect, useState } from "react";
import { Radio, RefreshCcw, Save } from "lucide-react";
import { toast } from "sonner";
import { listenToDocument } from "@/lib/firebase";
import { saveAdminSetting } from "@/lib/admin-firestore";
import { refreshLiveStream, type LiveStreamSettings } from "./LiveStreamSurface";

export function LiveStreamAdmin() {
  const [remote,setRemote]=useState<LiveStreamSettings|null>(null);
  const [autoEnabled,setAutoEnabled]=useState(true);
  const [manualEnabled,setManualEnabled]=useState(false);
  const [youtubeHandle,setYoutubeHandle]=useState("@micrhemaoficial");
  const [manualUrl,setManualUrl]=useState("");
  const [manualTitle,setManualTitle]=useState("Estamos ao vivo");
  const [saving,setSaving]=useState(false);
  const [checking,setChecking]=useState(false);

  useEffect(()=>listenToDocument<LiveStreamSettings>("settings","live_stream",value=>{
    setRemote(value);
    setAutoEnabled(value.autoEnabled!==false);
    setManualEnabled(value.manualEnabled===true);
    setYoutubeHandle(value.youtubeHandle?.trim()||"@micrhemaoficial");
    setManualUrl(value.manualUrl?.trim()||"");
    setManualTitle(value.manualTitle?.trim()||"Estamos ao vivo");
  },()=>setRemote(null)),[]);

  const save=async()=>{
    if(manualEnabled&&!manualUrl.trim()){toast.error("Informe o link da live para ativar o modo manual.");return;}
    setSaving(true);
    try{
      await saveAdminSetting("live_stream",{
        autoEnabled,
        manualEnabled,
        youtubeHandle:youtubeHandle.trim()||"@micrhemaoficial",
        manualUrl:manualUrl.trim(),
        manualTitle:manualTitle.trim()||"Estamos ao vivo",
        sourceEditor:"pwa",
      });
      await refreshLiveStream(true);
      toast.success("Configuração da transmissão salva.");
    }catch(error){toast.error(error instanceof Error?error.message:"Não foi possível salvar a transmissão.");}
    finally{setSaving(false);}
  };
  const check=async()=>{
    setChecking(true);
    try{await refreshLiveStream(true);toast.success("Canal verificado agora.");}
    catch(error){toast.error(error instanceof Error?error.message:"Não foi possível verificar o canal.");}
    finally{setChecking(false);}
  };

  return <div className="admin-section">
    <header className="admin-section-header"><div><p>IGREJA</p><h1>Transmissão ao vivo</h1><span>Modo automático pelo YouTube com ativação manual de reserva. O modo manual tem prioridade enquanto estiver ligado.</span></div><Radio size={30}/></header>
    <div className="admin-form-grid">
      <label className="admin-check"><input type="checkbox" checked={autoEnabled} onChange={event=>setAutoEnabled(event.target.checked)}/><span>Detecção automática do canal</span></label>
      <label className="admin-field"><span>Canal ou @handle do YouTube</span><input value={youtubeHandle} onChange={event=>setYoutubeHandle(event.target.value)} placeholder="@micrhemaoficial"/></label>
      <label className="admin-check"><input type="checkbox" checked={manualEnabled} onChange={event=>setManualEnabled(event.target.checked)}/><span>Forçar transmissão manual</span></label>
      <label className="admin-field"><span>Link manual da live</span><input value={manualUrl} onChange={event=>setManualUrl(event.target.value)} placeholder="https://youtube.com/watch?v=..."/></label>
      <label className="admin-field"><span>Título exibido</span><input value={manualTitle} onChange={event=>setManualTitle(event.target.value)} placeholder="Estamos ao vivo"/></label>
    </div>
    <article className="android-module-card" style={{marginTop:14,display:"grid",gap:5}}>
      <strong style={{color:remote?.isLive?"#dc2626":undefined}}>{remote?.isLive?"● AO VIVO AGORA":"OFFLINE"}</strong>
      <small>Canal: {remote?.youtubeHandle||youtubeHandle}{remote?.channelId?` · ${remote.channelId}`:""}</small>
      {remote?.isLive&&<span>{remote.title||"Transmissão MIC Rhema"} · {remote.source==="manual"?"manual":"automática"}</span>}
      {remote?.autoError&&<small style={{color:"var(--destructive,#b91c1c)"}}>Última verificação: {remote.autoError}</small>}
      {remote?.upcomingTitle&&<small>Próxima agendada: {remote.upcomingTitle}</small>}
    </article>
    <div className="admin-form-actions" style={{marginTop:14}}>
      <button disabled={saving} onClick={()=>void save()}><Save size={18}/>{saving?"Salvando…":"Salvar transmissão"}</button>
      <button disabled={checking} onClick={()=>void check()}><RefreshCcw size={18}/>{checking?"Verificando…":"Verificar agora"}</button>
    </div>
  </div>;
}
