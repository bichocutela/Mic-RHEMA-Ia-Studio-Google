import { useState } from "react";
import { ArrowLeft, CheckCircle2, LoaderCircle, LockKeyhole } from "lucide-react";
import { toast } from "sonner";
import { ASSETS } from "@/lib/pwa-data";
import { normalizeMemberPhone, signInOrRequestPwa, signInPwa, type PwaSession } from "@/lib/pwa-auth";
import "./AndroidLoginParity.css";

type AndroidLoginParityProps = {
  onClose: () => void;
  onSuccess: (session: PwaSession) => void;
  initialAdmin?: boolean;
};

export function AndroidLoginParity({ onClose, onSuccess, initialAdmin = false }: AndroidLoginParityProps) {
  const [adminMode,setAdminMode]=useState(initialAdmin);
  const [name,setName]=useState("");
  const [phone,setPhone]=useState("");
  const [password,setPassword]=useState("");
  const [busy,setBusy]=useState(false);
  const [pendingMessage,setPendingMessage]=useState("");
  const [error,setError]=useState("");

  const submit=async(event:React.FormEvent)=>{
    event.preventDefault();
    if(busy)return;
    setError("");
    setPendingMessage("");
    setBusy(true);
    try{
      if(adminMode){
        const session=await signInPwa({name:"admin",phone:"admin",password});
        onSuccess(session);
        toast.success("Acesso administrativo iniciado.");
        onClose();
        return;
      }
      const cleanPhone=normalizeMemberPhone(phone);
      if(!name.trim()||cleanPhone.length<10||cleanPhone.length>11){
        setError("Preencha seu nome completo e um telefone válido com DDD.");
        return;
      }
      const result=await signInOrRequestPwa({name,phone:cleanPhone});
      if(result.session){
        onSuccess(result.session);
        toast.success(`Bem-vindo, ${result.session.name}.`);
        onClose();
        return;
      }
      const message=result.message||"Solicitação enviada. Aguarde a aprovação do administrador.";
      setPendingMessage(message);
      toast.success(message);
    }catch(caught){
      setError(caught instanceof Error?caught.message:"Não foi possível acessar sua conta agora.");
    }finally{
      setBusy(false);
    }
  };

  return <div className="android-login-layer" role="dialog" aria-modal="true" aria-label="Entrar no MIC Rhema">
    <div className="android-login-page">
      <button type="button" className="android-login-back" onClick={onClose} aria-label="Voltar"><ArrowLeft size={21}/><span>Voltar</span></button>
      <div className="android-login-logo"><img src={ASSETS.logo} alt="Ministério Igreja de Cristo Rhema"/></div>
      <section className="android-login-card">
        {pendingMessage?<div className="android-login-pending"><CheckCircle2 size={46}/><h1>Solicitação enviada</h1><p>{pendingMessage}</p><button type="button" onClick={onClose}>Voltar ao aplicativo</button></div>:<form onSubmit={submit}>
          <div className="android-login-heading">
            {adminMode?<><span>ADMINISTRAÇÃO</span><h1>Acesso administrativo</h1><p>Use o mesmo acesso administrativo do aplicativo Android.</p></>:<><span>ÁREA DE MEMBROS</span><h1>Entre ou peça seu acesso</h1><p>Informe seu nome e telefone. Se esse número já tiver cadastro, sua conta será recuperada em vez de criar uma nova solicitação.</p></>}
          </div>
          {!adminMode&&<label>Nome completo<input value={name} onChange={(event)=>setName(event.target.value)} autoComplete="name" placeholder="Seu nome completo"/></label>}
          {!adminMode&&<label>Número de telefone com DDD<input value={phone} onChange={(event)=>setPhone(event.target.value.replace(/\D/g,"").slice(0,13))} inputMode="numeric" autoComplete="tel" placeholder="Ex: 84999832583"/></label>}
          {adminMode&&<label>Usuário<input value="admin" readOnly autoComplete="username"/></label>}
          {adminMode&&<label>Senha<input type="password" value={password} onChange={(event)=>setPassword(event.target.value)} autoComplete="current-password" placeholder="Sua senha"/></label>}
          {error&&<p className="android-login-error">{error}</p>}
          <button className="android-login-submit" disabled={busy} type="submit">{busy?<><LoaderCircle className="android-login-spin" size={21}/> Verificando…</>:adminMode?<><LockKeyhole size={19}/> Entrar como administrador</>:"Entrar ou solicitar acesso"}</button>
          {!adminMode&&<p className="android-login-note">Seu telefone identifica a conta. Em outro aparelho, use o mesmo número para recuperar o perfil e o progresso já sincronizado.</p>}
          <button className="android-login-admin-toggle" type="button" onClick={()=>{setAdminMode(!adminMode);setError("");setPendingMessage("")}}>{adminMode?"Entrar como membro":"Acesso administrativo"}</button>
        </form>}
      </section>
    </div>
  </div>;
}
