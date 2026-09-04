import fs from "node:fs";
import path from "node:path";

const root=process.cwd();
const read=(file)=>fs.readFileSync(path.resolve(root,file),"utf8");
const write=(file,text)=>fs.writeFileSync(path.resolve(root,file),text);
function replaceRequired(text,from,to,label){
  if(text.includes(to))return text;
  if(!text.includes(from))throw new Error(`${label}: trecho original não encontrado`);
  return text.replace(from,to);
}
function replaceRegex(text,regex,to,label,already){
  if(already&&text.includes(already))return text;
  if(!regex.test(text))throw new Error(`${label}: padrão não encontrado`);
  return text.replace(regex,to);
}

// O banner precisa continuar recebendo gesto de swipe mesmo quando não possui evento clicável.
{
  const file="client/src/components/HomeParityView.tsx";let text=read(file);
  text=text.replace(' disabled={!banner.eventInfo?.trim()} onTouchStart=', ' aria-disabled={!banner.eventInfo?.trim()} onTouchStart=');
  write(file,text);
}
{
  const file="client/src/components/HomeParityView.css";let text=read(file);
  if(!text.includes("touch-action:pan-y"))text=text.replace(".home-v2-banner{position:relative;", ".home-v2-banner{position:relative;touch-action:pan-y;user-select:none;-webkit-user-select:none;");
  write(file,text);
}

// Alterações nas preferências de push passam a ser sincronizadas com o token já autorizado.
{
  const file="client/src/components/SettingsParityViewV2.tsx";let text=read(file);
  text=replaceRequired(text,
    'import { firebaseAuth } from "@/lib/firebase";',
    'import { firebaseAuth } from "@/lib/firebase";\nimport { syncPwaPushPreferences } from "@/lib/push";',
    "import push settings");
  text=replaceRequired(text,
    '  useEffect(() => { applySettings(settings); }, [settings]);',
    '  useEffect(() => {\n    applySettings(settings);\n    const timer = window.setTimeout(() => { void syncPwaPushPreferences(); }, 350);\n    return () => window.clearTimeout(timer);\n  }, [settings]);',
    "sincronização settings");
  write(file,text);
}

// ADM: uploads reais, exclusão física segura e suporte completo a Fotos/Álbuns.
{
  const file="client/src/components/AdminParityView.tsx";let text=read(file);
  text=replaceRequired(text,
    '} from "@/lib/admin-firestore";\nimport "./AdminParityView.css";',
    '} from "@/lib/admin-firestore";\nimport { deleteAdminStoredAssetsFromDocument, uploadAdminMedia } from "@/lib/admin-storage";\nimport "./AdminParityView.css";',
    "import storage ADM");
  text=replaceRequired(text,
    'type Chapter = { id: string; title: string; description: string; durationMinutes: number; type: "VIDEO" | "AUDIO" | "TEXT"; videoUrl: string; audioUrl: string; textContent: string; isYoutube?: boolean; youtubeId?: string };',
    'type Chapter = { id: string; title: string; description: string; durationMinutes: number; type: "VIDEO" | "AUDIO" | "TEXT"; videoUrl: string; audioUrl: string; textContent: string; studyPdfUrl?: string; isYoutube?: boolean; youtubeId?: string };',
    "tipo capítulo IBR");

  if(!text.includes("function UploadField(")){
    const marker='function Metric({ label, value, icon: Icon, onClick }';
    const index=text.indexOf(marker);if(index<0)throw new Error("UploadField: marcador Metric não encontrado");
    const component=`function UploadField({ label, accept, onUploaded }: { label: string; accept: string; onUploaded: (url: string) => void }) {\n  const [busy,setBusy]=useState(false);\n  return <label className="admin-field"><span>{label}</span><input type="file" accept={accept} disabled={busy} onChange={async(event)=>{const file=event.target.files?.[0];if(!file)return;setBusy(true);try{const result=await uploadAdminMedia(file);onUploaded(result.url);toast.success("Arquivo enviado ao Supabase.");}catch(error){toast.error(error instanceof Error?error.message:"Falha no envio.");}finally{setBusy(false);event.target.value="";}}}/>{busy&&<small>Enviando arquivo…</small>}</label>;\n}\n\n`;
    text=text.slice(0,index)+component+text.slice(index);
  }

  text=replaceRequired(text,
    'async function remove(collectionName:string,item:AnyDoc){if(!window.confirm(`Excluir “${item.title||item.name||item.id}”?`))return;try{await deleteAdminDocument(collectionName,item.id);toast.success("Registro excluído.")}catch(error){toast.error(error instanceof Error?error.message:"Não foi possível excluir.")}}',
    'async function remove(collectionName:string,item:AnyDoc){if(!window.confirm(`Excluir “${item.title||item.name||item.id}”?`))return;try{await deleteAdminStoredAssetsFromDocument(item);await deleteAdminDocument(collectionName,item.id);toast.success("Registro e arquivos próprios excluídos.")}catch(error){toast.error(error instanceof Error?error.message:"Não foi possível excluir.")}}',
    "exclusão física ADM");

  const mediaBlock=`function MediaAdmin() {
  const videos=useAdminCollection("conteudos_videos").map(item=>({...item,_type:"video"}));
  const audios=useAdminCollection("conteudos_audios").map(item=>({...item,_type:"audio"}));
  const books=useAdminCollection("conteudos_books").map(item=>({...item,_type:"book"}));
  const albums=useAdminCollection("conteudos_albums").map(item=>({...item,_type:"album"}));
  const items=[...videos,...audios,...books,...albums];const[editing,setEditing]=useState<AnyDoc|null>(null);
  return <div className="admin-section"><SectionHeader title="Mídia" subtitle="Livros, áudios, vídeos e fotos compartilhados com Android e PWA." onAdd={()=>setEditing({id:createAdminDocumentId("conteudos_videos"),_type:"video"})} addLabel="Adicionar mídia"/><AdminRows items={items} title={item=>item.title||"Mídia"} subtitle={item=>\`${'${'}item._type==="video"?"Vídeo":item._type==="audio"?"Áudio":item._type==="book"?"Livro / PDF":"Fotos / Álbum"} · ${'${'}item.artist||item.author||item.description||""}\`} onEdit={setEditing} onDelete={item=>remove(mediaCollection(item._type),item)}/>{editing&&<MediaForm item={editing} onClose={()=>setEditing(null)}/>}</div>;
}
function mediaCollection(type:string){return type==="audio"?"conteudos_audios":type==="book"?"conteudos_books":type==="album"?"conteudos_albums":"conteudos_videos";}
function MediaForm({item,onClose}:{item:AnyDoc;onClose:()=>void}){
  if(item._type==="album")return <AlbumForm item={item} onClose={onClose}/>;
  const[d,setD]=useState({type:item._type||"video",title:item.title||"",description:item.description||"",url:item.videoUrl||item.audioUrl||item.bookUrl||item.mediaUrl||"",cover:item.thumbnailUrl||item.coverUrl||item.imageUrl||"",credit:item.artist||item.author||""});
  const accept=d.type==="video"?"video/mp4,video/webm,video/quicktime,video/3gpp":d.type==="audio"?"audio/*":"application/pdf";
  const save=async()=>{if(!d.title.trim()||!d.url.trim())return toast.error("Informe título e conteúdo.");const collectionName=mediaCollection(d.type);const payload:any={title:d.title.trim(),description:d.description.trim(),imageUrl:d.cover.trim(),isApproved:true,approved:true,publishedAt:Number(item.publishedAt||Date.now())};if(d.type==="video")Object.assign(payload,{videoUrl:d.url.trim(),thumbnailUrl:d.cover.trim()});else if(d.type==="audio")Object.assign(payload,{audioUrl:d.url.trim(),coverUrl:d.cover.trim(),artist:d.credit.trim()});else Object.assign(payload,{bookUrl:d.url.trim(),coverUrl:d.cover.trim(),author:d.credit.trim()});if(item._type&&item._type!==d.type){await deleteAdminStoredAssetsFromDocument(item);await deleteAdminDocument(mediaCollection(item._type),item.id);}await saveAdminDocument(collectionName,item.id,payload);toast.success("Mídia salva e sincronizada.");onClose();};
  return <AdminModal title="Mídia" onClose={onClose}><div className="admin-form"><label className="admin-field"><span>Tipo</span><select value={d.type} onChange={event=>setD({...d,type:event.target.value,url:""})}><option value="video">Vídeo</option><option value="audio">Áudio</option><option value="book">Livro / PDF</option></select></label><Field label="Título" value={d.title} onChange={value=>setD({...d,title:value})}/><Field label="Descrição" value={d.description} onChange={value=>setD({...d,description:value})} multiline/><UploadField label="Enviar arquivo do conteúdo" accept={accept} onUploaded={url=>setD({...d,url})}/><Field label="Ou usar URL externa" value={d.url} onChange={value=>setD({...d,url:value})}/><UploadField label="Enviar capa" accept="image/jpeg,image/png,image/webp" onUploaded={url=>setD({...d,cover:url})}/><Field label="Ou usar URL da capa" value={d.cover} onChange={value=>setD({...d,cover:value})}/><Field label="Autor / artista" value={d.credit} onChange={value=>setD({...d,credit:value})}/><SaveButton onClick={save}/></div></AdminModal>;
}
function AlbumForm({item,onClose}:{item:AnyDoc;onClose:()=>void}){
  const[d,setD]=useState({title:item.title||"",description:item.description||"",coverUrl:item.coverUrl||"",driveFolderUrl:item.driveFolderUrl||""});
  const[photos,setPhotos]=useState<Array<{url:string;caption:string}>>(Array.isArray(item.photos)?item.photos.map((photo:any)=>({url:String(photo.url||""),caption:String(photo.caption||"")})):[]);
  const addPhoto=(url:string)=>setPhotos(current=>[...current,{url,caption:""}]);
  const save=async()=>{if(!d.title.trim())return toast.error("Informe o título do álbum.");const cleanPhotos=photos.filter(photo=>photo.url.trim()).map(photo=>({url:photo.url.trim(),caption:photo.caption.trim()}));const cover=d.coverUrl.trim()||cleanPhotos[0]?.url||"";await saveAdminDocument("conteudos_albums",item.id,{title:d.title.trim(),description:d.description.trim(),coverUrl:cover,driveFolderUrl:d.driveFolderUrl.trim(),photos:cleanPhotos,isApproved:true,approved:true,publishedAt:Number(item.publishedAt||Date.now())});toast.success("Álbum salvo e sincronizado.");onClose();};
  return <AdminModal title="Álbum de Fotos" onClose={onClose}><div className="admin-form"><Field label="Título" value={d.title} onChange={value=>setD({...d,title:value})}/><Field label="Descrição" value={d.description} onChange={value=>setD({...d,description:value})} multiline/><UploadField label="Enviar capa" accept="image/jpeg,image/png,image/webp" onUploaded={url=>setD({...d,coverUrl:url})}/><Field label="URL da capa" value={d.coverUrl} onChange={value=>setD({...d,coverUrl:value})}/><Field label="Pasta externa do álbum (opcional)" value={d.driveFolderUrl} onChange={value=>setD({...d,driveFolderUrl:value})}/><div className="admin-nested-header"><strong>Fotos</strong><UploadField label="Adicionar foto" accept="image/jpeg,image/png,image/webp" onUploaded={addPhoto}/></div>{photos.map((photo,index)=><div className="admin-nested" key={index}><button className="nested-delete" onClick={()=>setPhotos(current=>current.filter((_,position)=>position!==index))}><Trash2 size={16}/></button><Field label="URL" value={photo.url} onChange={value=>setPhotos(current=>current.map((entry,position)=>position===index?{...entry,url:value}:entry))}/><Field label="Legenda" value={photo.caption} onChange={value=>setPhotos(current=>current.map((entry,position)=>position===index?{...entry,caption:value}:entry))}/></div>)}<SaveButton onClick={save}/></div></AdminModal>;
}

function PlansAdmin`;
  text=replaceRegex(text,/function MediaAdmin\(\)[\s\S]*?\nfunction PlansAdmin/,mediaBlock,"bloco mídia ADM","function AlbumForm(");

  const ibrBlock=`function IbrAdmin(){const items=useAdminCollection("ibr_courses");const[editing,setEditing]=useState<AnyDoc|null>(null);return <div className="admin-section"><SectionHeader title="Instituto Bíblico Rhema" subtitle="Cursos, módulos, aulas, vídeos, áudios, textos e PDFs." onAdd={()=>setEditing({id:createAdminDocumentId("ibr_courses"),chapters:[]})} addLabel="Novo curso"/><AdminRows items={items} title={item=>item.title||"Curso IBR"} subtitle={item=>\`${'${'}item.theme||"IBR"} · ${'${'}Array.isArray(item.chapters)?item.chapters.length:0} aula(s)\`} onEdit={setEditing} onDelete={item=>remove("ibr_courses",item)}/>{editing&&<IbrForm item={editing} onClose={()=>setEditing(null)}/>}</div>}
function IbrForm({item,onClose}:{item:AnyDoc;onClose:()=>void}){const[d,setD]=useState({title:item.title||"",theme:item.theme||"",description:item.description||"",imageUrl:item.imageUrl||""});const[chapters,setChapters]=useState<Chapter[]>(Array.isArray(item.chapters)?item.chapters:[]);const add=()=>setChapters([...chapters,{id:crypto.randomUUID(),title:"",description:"",durationMinutes:0,type:"VIDEO",videoUrl:"",audioUrl:"",textContent:"",studyPdfUrl:""}]);const patchChapter=(index:number,patch:Partial<Chapter>)=>setChapters(current=>current.map((chapter,position)=>position===index?{...chapter,...patch}:chapter));const save=async()=>{if(!d.title.trim())return toast.error("Informe o título do curso.");await replaceAdminDocument("ibr_courses",item.id,{...d,chapters});toast.success("Curso IBR sincronizado.");onClose();};return <AdminModal title="Curso IBR" onClose={onClose}><div className="admin-form"><Field label="Título" value={d.title} onChange={value=>setD({...d,title:value})}/><Field label="Tema" value={d.theme} onChange={value=>setD({...d,theme:value})}/><Field label="Descrição" value={d.description} onChange={value=>setD({...d,description:value})} multiline/><UploadField label="Enviar imagem do curso" accept="image/jpeg,image/png,image/webp" onUploaded={url=>setD({...d,imageUrl:url})}/><Field label="URL da imagem" value={d.imageUrl} onChange={value=>setD({...d,imageUrl:value})}/><div className="admin-nested-header"><strong>Aulas</strong><button onClick={add}><Plus size={16}/>Adicionar aula</button></div>{chapters.map((chapter,index)=><div className="admin-nested" key={chapter.id||index}><button className="nested-delete" onClick={()=>setChapters(current=>current.filter((_,position)=>position!==index))}><Trash2 size={16}/></button><Field label="Título da aula" value={chapter.title||""} onChange={value=>patchChapter(index,{title:value})}/><Field label="Descrição" value={chapter.description||""} onChange={value=>patchChapter(index,{description:value})}/><div className="admin-form-grid"><label className="admin-field"><span>Tipo</span><select value={chapter.type||"VIDEO"} onChange={event=>patchChapter(index,{type:event.target.value as Chapter["type"]})}><option value="VIDEO">Vídeo</option><option value="AUDIO">Áudio</option><option value="TEXT">Texto</option></select></label><Field label="Duração (min)" value={chapter.durationMinutes||0} onChange={value=>patchChapter(index,{durationMinutes:Number(value)||0})} type="number"/></div>{chapter.type==="VIDEO"&&<><UploadField label="Enviar vídeo da aula" accept="video/mp4,video/webm,video/quicktime,video/3gpp" onUploaded={url=>patchChapter(index,{videoUrl:url})}/><Field label="URL do vídeo / YouTube" value={chapter.videoUrl||""} onChange={value=>patchChapter(index,{videoUrl:value})}/></>}{chapter.type==="AUDIO"&&<><UploadField label="Enviar áudio da aula" accept="audio/*" onUploaded={url=>patchChapter(index,{audioUrl:url})}/><Field label="URL do áudio" value={chapter.audioUrl||""} onChange={value=>patchChapter(index,{audioUrl:value})}/></>}{chapter.type==="TEXT"&&<Field label="Conteúdo da aula" value={chapter.textContent||""} onChange={value=>patchChapter(index,{textContent:value})} multiline/>}<UploadField label="Enviar PDF de estudo (opcional)" accept="application/pdf" onUploaded={url=>patchChapter(index,{studyPdfUrl:url})}/><Field label="URL do PDF de estudo" value={chapter.studyPdfUrl||""} onChange={value=>patchChapter(index,{studyPdfUrl:value})}/></div>)}<SaveButton onClick={save}/></div></AdminModal>}

function TabsAdmin`;
  text=replaceRegex(text,/function IbrAdmin\(\)[\s\S]*?\nfunction TabsAdmin/,ibrBlock,"bloco IBR ADM","Enviar PDF de estudo (opcional)");

  const bannerBlock=`function BannerForm({item,onClose}:{item:AnyDoc;onClose:()=>void}){const[d,setD]=useState({imageUrl:item.imageUrl||"",title:item.title||"",description:item.description||"",tag:item.tag||"",eventDate:item.eventDate||"",eventInfo:item.eventInfo||""});const save=async()=>{if(!d.imageUrl.trim())return toast.error("Informe a imagem.");await saveAdminDocument("carousel_items",item.id,d);toast.success("Destaque salvo.");onClose();};return <AdminModal title="Destaque da Home" onClose={onClose}><div className="admin-form"><UploadField label="Enviar banner 16:9" accept="image/jpeg,image/png,image/webp" onUploaded={url=>setD({...d,imageUrl:url})}/><Field label="URL da imagem 16:9" value={d.imageUrl} onChange={value=>setD({...d,imageUrl:value})}/>{d.imageUrl&&<img src={d.imageUrl} alt="Prévia do banner" style={{width:"100%",aspectRatio:"16/9",objectFit:"contain",borderRadius:16,background:"#ece8df"}}/>}<Field label="Título" value={d.title} onChange={value=>setD({...d,title:value})}/><Field label="Descrição" value={d.description} onChange={value=>setD({...d,description:value})}/><Field label="Tag" value={d.tag} onChange={value=>setD({...d,tag:value})}/><Field label="Data do evento (opcional)" value={d.eventDate} onChange={value=>setD({...d,eventDate:value})} type="date"/><Field label="Informações do evento" value={d.eventInfo} onChange={value=>setD({...d,eventInfo:value})} multiline/><SaveButton onClick={save}/></div></AdminModal>}`;
  text=replaceRegex(text,/function BannerForm\([\s\S]*?\n\nfunction DonationsAdmin/,bannerBlock+"\n\nfunction DonationsAdmin","banner ADM","Enviar banner 16:9");

  const discipuladoBlock=`function DiscipuladoAdmin(){const items=useAdminCollection("discipulado_pdfs").sort((a,b)=>Number(a.order||0)-Number(b.order||0));const[editing,setEditing]=useState<AnyDoc|null>(null);return <div className="admin-section"><SectionHeader title="Estudos de Discipulado" subtitle="PDFs públicos disponíveis no Android e PWA." onAdd={()=>setEditing({id:createAdminDocumentId("discipulado_pdfs")})} addLabel="Novo PDF"/><AdminRows items={items} title={item=>item.title||"Estudo"} subtitle={item=>\`${'${'}item.category||"Estudos bíblicos"}${'${'}item.isPublished===false?" · Oculto":""}\`} onEdit={setEditing} onDelete={item=>remove("discipulado_pdfs",item)}/>{editing&&<DiscipuladoForm item={editing} onClose={()=>setEditing(null)}/>}</div>}
function DiscipuladoForm({item,onClose}:{item:AnyDoc;onClose:()=>void}){const[d,setD]=useState({title:item.title||"",subtitle:item.subtitle||"",description:item.description||"",category:item.category||"Estudos bíblicos",fileUrl:item.fileUrl||"",order:String(item.order||0),isPublished:item.isPublished!==false});const save=async()=>{if(!d.title.trim()||!d.fileUrl.trim())return toast.error("Informe título e PDF.");await saveAdminDocument("discipulado_pdfs",item.id,{...d,order:Number(d.order)||0});toast.success("Estudo sincronizado.");onClose();};return <AdminModal title="Estudo em PDF" onClose={onClose}><div className="admin-form"><Field label="Título" value={d.title} onChange={value=>setD({...d,title:value})}/><Field label="Subtítulo" value={d.subtitle} onChange={value=>setD({...d,subtitle:value})}/><Field label="Categoria" value={d.category} onChange={value=>setD({...d,category:value})}/><Field label="Descrição" value={d.description} onChange={value=>setD({...d,description:value})} multiline/><UploadField label="Enviar PDF" accept="application/pdf" onUploaded={url=>setD({...d,fileUrl:url})}/><Field label="Ou usar URL do PDF" value={d.fileUrl} onChange={value=>setD({...d,fileUrl:value})}/><Field label="Ordem" value={d.order} onChange={value=>setD({...d,order:value})}/><Check label="Publicado" checked={d.isPublished} onChange={value=>setD({...d,isPublished:value})}/><SaveButton onClick={save}/></div></AdminModal>}

function MembersAdmin`;
  text=replaceRegex(text,/function DiscipuladoAdmin\(\)[\s\S]*?\nfunction MembersAdmin/,discipuladoBlock,"discipulado ADM","Enviar PDF\" accept=\"application/pdf");
  write(file,text);
}

console.log("Ajustes finais de paridade aplicados.");
