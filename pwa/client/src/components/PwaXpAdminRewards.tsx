import { useEffect, useMemo, useState } from "react";
import { CheckCircle2, ChevronLeft, ChevronRight, Eye, Gift, Pencil, Plus, RefreshCcw, TestTube2, Upload, X } from "lucide-react";
import { toast } from "sonner";
import { uploadAdminMedia } from "@/lib/admin-storage";
import { resolvePwaXpAssetUrl } from "@/lib/profile-customization";
import {
  buildAdminXpAssetRef, loadAdminXpCatalog, loadAdminXpRedemptions, saveAdminXpItem, setAdminXpRedemptionStatus,
  type AdminXpRedemption, type AdminXpShopItem,
} from "@/lib/xp-admin";

const PAGE_SIZE = 10;
type AssetType = "image" | "video" | "audio" | "emblem" | "pdf";

const emptyItem = (): AdminXpShopItem => ({ id:"", name:"", description:"", cost:0, category:"Personalização", kind:"digital", imageUrl:"", stock:null, limitPerMember:1, active:true, availableFrom:"", availableUntil:"" });
const slug = (value:string) => value.normalize("NFD").replace(/[\u0300-\u036f]/g,"").toLowerCase().replace(/[^a-z0-9]+/g,"_").replace(/^_+|_+$/g,"").slice(0,70);
const assetTypeOf = (value:string):AssetType => {
  const match=value.match(/^micrhema-xp:\/\/([^/]+)/i)?.[1]?.toLowerCase();
  if(match==="video"||match==="audio"||match==="emblem"||match==="pdf")return match;
  return "image";
};

function Preview({item,testMode,onClose}:{item:AdminXpShopItem;testMode:boolean;onClose:()=>void}){
  const url=resolvePwaXpAssetUrl(item.imageUrl); const type=assetTypeOf(item.imageUrl);
  return <div className="xp-admin-modal-backdrop" onMouseDown={onClose}><article className="xp-admin-modal xp-admin-preview" onMouseDown={(event)=>event.stopPropagation()}>
    <header><div><small>{testMode?"MODO TESTE":"PRÉVIA REAL"}</small><h2>{item.name||"Recompensa sem nome"}</h2></div><button onClick={onClose}><X size={20}/></button></header>
    <div className="xp-admin-preview-stage">{!url?<Gift size={62}/>:type==="video"?<video controls src={url}/>:type==="audio"?<audio controls src={url}/>:type==="pdf"?<iframe title={item.name} src={url}/>:<img src={url} alt={item.name}/>}</div>
    <div className="xp-admin-preview-info"><strong>{item.cost} XP</strong><span>{item.category} · {item.kind==="physical"?"Físico":item.kind==="profile"?"Perfil":"Digital"}</span><p>{item.description||"Sem descrição cadastrada."}</p>{testMode&&<em>Nenhuma compra, entrega ou alteração será salva durante este teste.</em>}</div>
    <button className="xp-admin-secondary" onClick={onClose}>Fechar</button>
  </article></div>;
}

function RewardEditor({initial,onClose,onSaved}:{initial:AdminXpShopItem|null;onClose:()=>void;onSaved:()=>void}){
  const [draft,setDraft]=useState<AdminXpShopItem>(initial?{...initial}:emptyItem());
  const [assetType,setAssetType]=useState<AssetType>(()=>assetTypeOf(initial?.imageUrl||""));
  const [busy,setBusy]=useState(false); const [previewMode,setPreviewMode]=useState<"preview"|"test"|null>(null);
  const set=<K extends keyof AdminXpShopItem>(key:K,value:AdminXpShopItem[K])=>setDraft((current)=>({...current,[key]:value}));
  const upload=async(file:File)=>{setBusy(true);try{const result=await uploadAdminMedia(file);set("imageUrl",buildAdminXpAssetRef(assetType,result.storagePath));toast.success("Arquivo enviado ao Supabase.");}catch(error){toast.error(error instanceof Error?error.message:"Falha no upload.");}finally{setBusy(false)}};
  const save=async()=>{
    const id=slug(draft.id||draft.name); if(!id||!draft.name.trim()){toast.error("Informe o nome da recompensa.");return;} if(draft.cost<0){toast.error("O valor em XP não pode ser negativo.");return;}
    setBusy(true);try{await saveAdminXpItem({...draft,id,name:draft.name.trim(),description:draft.description.trim(),category:draft.category.trim()||"Recompensas",cost:Math.max(0,Math.floor(draft.cost)),limitPerMember:Math.max(1,Math.floor(draft.limitPerMember))});toast.success("Recompensa salva e sincronizada.");onSaved();onClose();}catch(error){toast.error(error instanceof Error?error.message:"Não foi possível salvar.");}finally{setBusy(false)}
  };
  const accept=assetType==="video"?"video/*":assetType==="audio"?"audio/*":assetType==="pdf"?"application/pdf":"image/png,image/jpeg,image/webp";
  return <><div className="xp-admin-modal-backdrop" onMouseDown={onClose}><article className="xp-admin-modal" onMouseDown={(event)=>event.stopPropagation()}><header><div><small>LOJA XP</small><h2>{initial?"Editar recompensa":"Nova recompensa"}</h2></div><button onClick={onClose}><X size={20}/></button></header>
    <div className="xp-admin-form-grid"><label>Identificador<input value={draft.id} disabled={Boolean(initial)} placeholder="gerado pelo nome" onChange={(e)=>set("id",e.target.value)}/></label><label>Nome<input value={draft.name} onChange={(e)=>set("name",e.target.value)}/></label><label className="wide">Descrição<textarea value={draft.description} onChange={(e)=>set("description",e.target.value)}/></label><label>Valor em XP<input type="number" min="0" value={draft.cost} onChange={(e)=>set("cost",Number(e.target.value)||0)}/></label><label>Categoria<input value={draft.category} onChange={(e)=>set("category",e.target.value)}/></label><label>Tipo<select value={draft.kind} onChange={(e)=>set("kind",e.target.value)}><option value="digital">Digital</option><option value="profile">Perfil</option><option value="physical">Físico</option></select></label><label>Estoque<input type="number" min="0" placeholder="vazio = ilimitado" value={draft.stock??""} onChange={(e)=>set("stock",e.target.value===""?null:Math.max(0,Number(e.target.value)||0))}/></label><label>Limite por membro<input type="number" min="1" value={draft.limitPerMember} onChange={(e)=>set("limitPerMember",Math.max(1,Number(e.target.value)||1))}/></label><label>Disponível de<input type="date" value={draft.availableFrom.slice(0,10)} onChange={(e)=>set("availableFrom",e.target.value)}/></label><label>Disponível até<input type="date" value={draft.availableUntil.slice(0,10)} onChange={(e)=>set("availableUntil",e.target.value)}/></label></div>
    <div className="xp-admin-upload"><label>Arquivo<select value={assetType} onChange={(e)=>setAssetType(e.target.value as AssetType)}><option value="image">Imagem</option><option value="video">Vídeo</option><option value="audio">Áudio</option><option value="emblem">Emblema PNG</option><option value="pdf">PDF</option></select></label><label className="xp-admin-file"><Upload size={18}/><span>{busy?"Enviando…":"Selecionar arquivo"}</span><input type="file" accept={accept} disabled={busy} onChange={(e)=>{const file=e.target.files?.[0];if(file)void upload(file);e.currentTarget.value=""}}/></label>{draft.imageUrl&&<small>Arquivo cadastrado e pronto para prévia.</small>}</div>
    <label className="xp-admin-check"><input type="checkbox" checked={draft.active} onChange={(e)=>set("active",e.target.checked)}/> Recompensa ativa</label>
    <div className="xp-admin-modal-actions"><button className="xp-admin-secondary" onClick={()=>setPreviewMode("preview")}><Eye size={17}/> Prévia</button><button className="xp-admin-secondary" onClick={()=>setPreviewMode("test")}><TestTube2 size={17}/> Testar</button><button className="parity-primary" disabled={busy} onClick={()=>void save()}>{busy?"Salvando…":"Salvar recompensa"}</button></div>
  </article></div>{previewMode&&<Preview item={draft} testMode={previewMode==="test"} onClose={()=>setPreviewMode(null)}/>}</>;
}

export function PwaXpAdminRewards(){
  const [items,setItems]=useState<AdminXpShopItem[]>([]); const [loading,setLoading]=useState(true); const [editor,setEditor]=useState<AdminXpShopItem|null|undefined>(undefined); const [preview,setPreview]=useState<{item:AdminXpShopItem;test:boolean}|null>(null); const [page,setPage]=useState(0);
  const load=async()=>{setLoading(true);try{setItems(await loadAdminXpCatalog())}catch(error){toast.error(error instanceof Error?error.message:"Falha ao carregar recompensas.");}finally{setLoading(false)}}; useEffect(()=>{void load()},[]);
  const totalPages=Math.max(1,Math.ceil(items.length/PAGE_SIZE)); const safe=Math.min(page,totalPages-1); const visible=items.slice(safe*PAGE_SIZE,(safe+1)*PAGE_SIZE);
  return <section className="xp-admin-section"><div className="xp-admin-section-head"><div><h2>Recompensas</h2><p>Cadastre produtos digitais, físicos e itens de perfil usando o mesmo catálogo do Android.</p></div><button className="parity-primary" onClick={()=>setEditor(null)}><Plus size={18}/> Nova recompensa</button></div>
    {loading?<p className="parity-status">Carregando recompensas…</p>:!items.length?<p className="parity-status">Nenhuma recompensa cadastrada.</p>:<div className="xp-admin-cards">{visible.map((item)=><article key={item.id}><div className="xp-admin-card-main"><div><small>{item.category} · {item.kind}</small><strong>{item.name}</strong><p>{item.description}</p></div><b>{item.cost} XP</b></div><div className="xp-admin-card-meta"><span>{item.stock===null?"Estoque ilimitado":`Estoque ${item.stock}`}</span><span>{item.active?"Ativa":"Inativa"}</span></div><div className="xp-admin-card-actions"><button onClick={()=>setPreview({item,test:false})}><Eye size={16}/> Prévia</button><button onClick={()=>setPreview({item,test:true})}><TestTube2 size={16}/> Testar</button><button onClick={()=>setEditor(item)}><Pencil size={16}/> Editar</button></div></article>)}</div>}
    <div className="xp-admin-pagination"><button disabled={safe===0} onClick={()=>setPage(Math.max(0,safe-1))}><ChevronLeft size={18}/> Anterior</button><strong>{safe+1}/{totalPages}</strong><button disabled={safe>=totalPages-1} onClick={()=>setPage(Math.min(totalPages-1,safe+1))}>Próxima <ChevronRight size={18}/></button></div>
    <button className="xp-admin-refresh" onClick={()=>void load()}><RefreshCcw size={16}/> Atualizar catálogo</button>
    {editor!==undefined&&<RewardEditor initial={editor} onClose={()=>setEditor(undefined)} onSaved={()=>void load()}/>} {preview&&<Preview item={preview.item} testMode={preview.test} onClose={()=>setPreview(null)}/>} 
  </section>;
}

export function PwaXpAdminRedemptions(){
  const [status,setStatus]=useState("todos"); const [items,setItems]=useState<AdminXpRedemption[]>([]); const [loading,setLoading]=useState(true); const [busyId,setBusyId]=useState(""); const [page,setPage]=useState(0);
  const load=async()=>{setLoading(true);try{setItems(await loadAdminXpRedemptions(status));setPage(0)}catch(error){toast.error(error instanceof Error?error.message:"Falha ao carregar resgates.");}finally{setLoading(false)}}; useEffect(()=>{void load()},[status]);
  const totalPages=Math.max(1,Math.ceil(items.length/PAGE_SIZE)); const safe=Math.min(page,totalPages-1); const visible=items.slice(safe*PAGE_SIZE,(safe+1)*PAGE_SIZE);
  const change=async(item:AdminXpRedemption,next:"entregue"|"cancelado")=>{const message=next==="cancelado"?`Cancelar ${item.itemName}? O XP será estornado e o estoque restaurado quando aplicável.`:`Confirmar que ${item.itemName} foi entregue?`;if(!window.confirm(message))return;setBusyId(item.id);try{await setAdminXpRedemptionStatus(item.id,next);toast.success(next==="cancelado"?"Resgate cancelado e XP estornado.":"Resgate marcado como entregue.");await load()}catch(error){toast.error(error instanceof Error?error.message:"Não foi possível atualizar o resgate.");}finally{setBusyId("")}};
  const filters=[["todos","Todos"],["pendente","Pendentes"],["entregue","Entregues"],["cancelado","Cancelados"]] as const;
  return <section className="xp-admin-section"><div className="xp-admin-section-head"><div><h2>Resgates</h2><p>Acompanhe compras, entregas e estornos feitos com XP.</p></div></div><div className="xp-admin-filter-row">{filters.map(([value,label])=><button key={value} className={status===value?"selected":""} onClick={()=>setStatus(value)}>{label}</button>)}</div>
    {loading?<p className="parity-status">Carregando resgates…</p>:!items.length?<p className="parity-status">Nenhum resgate nesta categoria.</p>:<div className="xp-admin-cards">{visible.map((item)=><article key={item.id}><div className="xp-admin-card-main"><div><small>{item.memberName||`Membro ${item.memberId.slice(0,8)}`}</small><strong>{item.itemName}</strong><p>Código: {item.code||"—"}</p></div><b>{item.cost} XP</b></div><div className="xp-admin-card-meta"><span>{item.status}</span><span>{item.createdAt?item.createdAt.replace("T"," ").slice(0,16):""}</span></div>{item.status==="pendente"&&<div className="xp-admin-card-actions"><button disabled={busyId===item.id} onClick={()=>void change(item,"entregue")}><CheckCircle2 size={16}/> Entregue</button><button className="danger" disabled={busyId===item.id} onClick={()=>void change(item,"cancelado")}><X size={16}/> Cancelar</button></div>}</article>)}</div>}
    <div className="xp-admin-pagination"><button disabled={safe===0} onClick={()=>setPage(Math.max(0,safe-1))}><ChevronLeft size={18}/> Anterior</button><strong>{safe+1}/{totalPages}</strong><button disabled={safe>=totalPages-1} onClick={()=>setPage(Math.min(totalPages-1,safe+1))}>Próxima <ChevronRight size={18}/></button></div>
  </section>;
}
