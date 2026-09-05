import { useEffect, useMemo, useRef, useState } from "react";
import { BookOpen, Check, ChevronLeft, Download, ExternalLink, Headphones, Image as ImageIcon, Play, Search, SortAsc, Video, X } from "lucide-react";
import { listenToCollection } from "@/lib/firebase";
import { recordVerifiedPwaActivity } from "@/lib/badge-activity";
import { reportPwaMediaProgress, type PwaMediaType } from "@/lib/xp";
import {
  contentTimestamp, normalizeSearch, resolveAudioStreamUrl, resolveDisplayImageUrl, resolvePdfEmbedUrl,
  resolvePortableAssetUrl, safeFilename, youtubeThumbnail, youtubeVideoId,
} from "@/lib/parity-utils";
import "./AndroidParityViews.css";
import "./MediaParityViewV2.css";

type MediaItem = { id:string; title?:string; description?:string; imageUrl?:string; thumbnailUrl?:string; coverUrl?:string; videoUrl?:string; audioUrl?:string; bookUrl?:string; mediaUrl?:string; artist?:string; author?:string; presenter?:string; preacher?:string; approved?:boolean; isApproved?:boolean; publishedAt?:number; createdAt?:number; timestamp?:number; updatedAt?:number };
type AlbumPhoto={url?:string;caption?:string};
type Album={id:string;title?:string;description?:string;coverUrl?:string;photos?:AlbumPhoto[];driveFolderUrl?:string;approved?:boolean;isApproved?:boolean;createdAt?:number;publishedAt?:number};
type Kind="Livro"|"Áudio"|"Vídeo"|"Fotos";
type SortMode="recent"|"name"|"presenter"|"oldest"|"relevant";
type ViewItem=(MediaItem&{kind:Exclude<Kind,"Fotos">})|(Album&{kind:"Fotos"});

const sortLabels:Record<SortMode,string>={recent:"Mais recente",name:"Nome",presenter:"Preletor",oldest:"Mais antigo",relevant:"Relevante"};
function approved(item:{approved?:boolean;isApproved?:boolean}){return item.approved!==false&&item.isApproved!==false}
function rawMediaUrl(item:ViewItem){if(item.kind==="Fotos")return item.driveFolderUrl||"";return item.videoUrl||item.audioUrl||item.bookUrl||item.mediaUrl||""}
function secondary(item:ViewItem){if(item.kind==="Fotos")return item.description||"";return item.presenter||item.preacher||item.artist||item.author||item.description||""}
function image(item:ViewItem){
  if(item.kind==="Fotos")return resolveDisplayImageUrl(item.coverUrl||item.photos?.[0]?.url||"");
  if(item.kind==="Vídeo"){const official=youtubeThumbnail(item.videoUrl||item.mediaUrl);if(official)return official;}
  return resolveDisplayImageUrl(item.imageUrl||item.thumbnailUrl||item.coverUrl||"");
}
function relevance(query:string,item:ViewItem){const q=normalizeSearch(query);if(!q)return 0;const title=normalizeSearch(item.title||"");const sub=normalizeSearch(secondary(item));if(title===q)return 100;if(title.startsWith(q))return 85;if(title.includes(q))return 70;if(sub===q)return 60;if(sub.startsWith(q))return 50;if(sub.includes(q))return 40;return 0}
function itemTime(item:ViewItem){return contentTimestamp(item as unknown as Record<string,unknown>)}

async function markVerifiedMedia(kind:Exclude<Kind,"Fotos">,itemId:string){
  const activity=kind==="Livro"?"books":kind==="Áudio"?"audios":"videos";
  await recordVerifiedPwaActivity(activity,itemId);
}

export function MediaParityViewV2(){
  const[videos,setVideos]=useState<MediaItem[]>([]);const[audios,setAudios]=useState<MediaItem[]>([]);const[books,setBooks]=useState<MediaItem[]>([]);const[albums,setAlbums]=useState<Album[]>([]);
  const[tab,setTab]=useState<Kind>("Livro");const[query,setQuery]=useState("");const[sort,setSort]=useState<SortMode>("recent");const[sortOpen,setSortOpen]=useState(false);const[selected,setSelected]=useState<ViewItem|null>(null);
  useEffect(()=>listenToCollection<MediaItem>("conteudos_videos",setVideos,()=>setVideos([])),[]);
  useEffect(()=>listenToCollection<MediaItem>("conteudos_audios",setAudios,()=>setAudios([])),[]);
  useEffect(()=>listenToCollection<MediaItem>("conteudos_books",setBooks,()=>setBooks([])),[]);
  useEffect(()=>listenToCollection<Album>("conteudos_albums",setAlbums,()=>setAlbums([])),[]);
  const all=useMemo<ViewItem[]>(()=>[
    ...books.filter(approved).map(i=>({...i,kind:"Livro" as const})),
    ...audios.filter(approved).map(i=>({...i,kind:"Áudio" as const})),
    ...videos.filter(approved).map(i=>({...i,kind:"Vídeo" as const})),
    ...albums.filter(approved).map(i=>({...i,kind:"Fotos" as const})),
  ],[books,audios,videos,albums]);
  const visible=useMemo(()=>{const match=all.filter(item=>item.kind===tab).filter(item=>!query.trim()||normalizeSearch(item.title||"").includes(normalizeSearch(query))||normalizeSearch(secondary(item)).includes(normalizeSearch(query)));return match.sort((a,b)=>{if(sort==="name")return String(a.title||"").localeCompare(String(b.title||""),"pt-BR");if(sort==="presenter")return secondary(a).localeCompare(secondary(b),"pt-BR");if(sort==="oldest")return itemTime(a)-itemTime(b);if(sort==="relevant"&&query.trim())return relevance(query,b)-relevance(query,a)||itemTime(b)-itemTime(a);return itemTime(b)-itemTime(a)})},[all,tab,query,sort]);
  const openItem=(item:ViewItem)=>setSelected(item);
  if(selected)return <MediaReaderV2 item={selected} onBack={()=>setSelected(null)}/>;
  return <section className="parity-page media-parity"><div className="parity-title"><div><p>MÍDIA</p><h1>Conteúdo da igreja</h1><span>Livros, áudios, vídeos e fotos sincronizados em tempo real com o Android.</span></div><Play size={30}/></div>
    <div style={{display:"flex",gap:8,alignItems:"center",marginBottom:12}}><label style={{flex:1,display:"flex",alignItems:"center",gap:8,border:"1px solid #9ca3af",borderRadius:22,padding:"0 14px",minHeight:48}}><Search size={19}/><input value={query} onChange={e=>setQuery(e.target.value)} placeholder="Buscar por título ou descrição..." style={{flex:1,border:0,outline:0,background:"transparent",color:"inherit"}}/>{query&&<button onClick={()=>setQuery("")} aria-label="Limpar" style={{border:0,background:"transparent",color:"inherit"}}><X size={18}/></button>}</label><div style={{position:"relative"}}><button onClick={()=>setSortOpen(v=>!v)} aria-label={`Ordenar: ${sortLabels[sort]}`} style={{width:48,height:48,borderRadius:24,border:0,display:"grid",placeItems:"center"}}><SortAsc size={20}/></button>{sortOpen&&<div style={{position:"absolute",right:0,top:54,zIndex:30,minWidth:190,background:"var(--card,#fffdf7)",borderRadius:14,padding:8,boxShadow:"0 14px 36px rgba(0,0,0,.2)"}}>{(Object.keys(sortLabels) as SortMode[]).map(option=><button key={option} onClick={()=>{setSort(option);setSortOpen(false)}} style={{width:"100%",display:"flex",gap:8,alignItems:"center",padding:10,border:0,background:"transparent",color:"inherit",fontWeight:700,textAlign:"left"}}>{sort===option?<Check size={16}/>:<span style={{width:16}}/>}{sortLabels[option]}</button>)}</div>}</div></div>
    <div className="filter-pills">{(["Livro","Áudio","Vídeo","Fotos"] as Kind[]).map(kind=><button key={kind} className={tab===kind?"selected":""} onClick={()=>setTab(kind)}>{kind==="Livro"?"Livros":kind==="Áudio"?"Áudios":kind==="Vídeo"?"Vídeos":"Fotos"}</button>)}</div>
    {!visible.length?<p className="parity-status">Nenhum conteúdo encontrado.</p>:<div className="media-parity-grid">{visible.map(item=>{const cover=image(item);return <button key={`${item.kind}-${item.id}`} onClick={()=>openItem(item)}><div className="media-cover">{cover?<img src={cover} alt="" loading="lazy"/>:item.kind==="Áudio"?<Headphones/>:item.kind==="Livro"?<BookOpen/>:item.kind==="Fotos"?<ImageIcon/>:<Video/>}</div><span>{item.kind}</span><strong>{item.title||"Conteúdo MIC Rhema"}</strong><small>{secondary(item)||"Abrir conteúdo"}</small></button>})}</div>}
  </section>
}

function useMediaReporter(mediaType:PwaMediaType,itemId:string,snapshot:()=>{positionMs:number;durationMs:number;fraction:number;isActive:boolean},kind:Exclude<Kind,"Fotos">){
  const qualified=useRef(false);
  const snapshotRef=useRef(snapshot);
  snapshotRef.current=snapshot;
  useEffect(()=>{
    qualified.current=false;
    let sending=false;
    const report=()=>{
      if(sending||document.visibilityState!=="visible")return;
      const state=snapshotRef.current();
      sending=true;
      void reportPwaMediaProgress(mediaType,itemId,state.positionMs,state.durationMs,state.fraction,state.isActive)
        .then(result=>{if(result.qualified&&!qualified.current){qualified.current=true;void markVerifiedMedia(kind,itemId)}})
        .catch(()=>undefined)
        .finally(()=>{sending=false});
    };
    report();
    const timer=window.setInterval(report,5000);
    return()=>window.clearInterval(timer);
  },[mediaType,itemId,kind]);
}

function AudioReader({rawUrl,title,itemId}:{rawUrl:string;title:string;itemId:string}){
  const audioRef=useRef<HTMLAudioElement|null>(null);
  const primary=useMemo(()=>resolveAudioStreamUrl(rawUrl),[rawUrl]);
  const fallback=useMemo(()=>resolvePortableAssetUrl(rawUrl),[rawUrl]);
  const[src,setSrc]=useState(primary);
  const[speed,setSpeed]=useState(1);
  const[error,setError]=useState("");
  const[fallbackUsed,setFallbackUsed]=useState(false);

  useEffect(()=>{setSrc(primary);setSpeed(1);setError("");setFallbackUsed(false)},[primary]);
  useMediaReporter("audio",itemId,()=>{const el=audioRef.current;const duration=Number.isFinite(el?.duration)?Number(el?.duration):0;const position=el?.currentTime||0;return{positionMs:Math.floor(position*1000),durationMs:Math.floor(duration*1000),fraction:duration>0?position/duration:0,isActive:Boolean(el&&!el.paused&&!el.ended)}},"Áudio");
  const changeSpeed=()=>{const next=speed>=2?.75:Math.min(2,speed+.25);setSpeed(next);if(audioRef.current)audioRef.current.playbackRate=next};
  const handleError=()=>{
    if(!fallbackUsed&&fallback&&fallback!==src){
      setFallbackUsed(true);
      setError("A conexão principal falhou. Tentando carregar o arquivo diretamente…");
      setSrc(fallback);
      return;
    }
    setError("Não foi possível carregar este áudio. O arquivo pode estar indisponível ou sem permissão de reprodução.");
  };
  const retry=()=>{setFallbackUsed(false);setError("");setSrc(primary);requestAnimationFrame(()=>audioRef.current?.load())};

  return <div className="audio-reader-v2"><Headphones size={44}/><strong>{title}</strong><audio key={src} ref={audioRef} controls playsInline preload="metadata" src={src} onCanPlay={()=>setError("")} onError={handleError}>Seu navegador não suporta áudio.</audio><div className="audio-reader-controls"><button onClick={()=>{if(audioRef.current)audioRef.current.currentTime=Math.max(0,audioRef.current.currentTime-15)}}>-15s</button><button onClick={()=>{if(audioRef.current)audioRef.current.currentTime=Math.min(audioRef.current.duration||Infinity,audioRef.current.currentTime+15)}}>+15s</button><button onClick={changeSpeed}>{speed.toFixed(2).replace(/\.00$/,"")}x</button></div><p className="audio-reader-status">Reprodução dentro da PWA com suporte ao Safari/iPhone e retomada por streaming.</p>{error&&<div className="audio-error-card"><p>{error}</p><button onClick={retry}>Tentar novamente</button></div>}</div>;
}

function NativeVideoReader({src,title,itemId,onError}:{src:string;title:string;itemId:string;onError:()=>void}){
  const ref=useRef<HTMLVideoElement|null>(null);
  useMediaReporter("video",itemId,()=>{const el=ref.current;const duration=Number.isFinite(el?.duration)?Number(el?.duration):0;const position=el?.currentTime||0;return{positionMs:Math.floor(position*1000),durationMs:Math.floor(duration*1000),fraction:duration>0?position/duration:0,isActive:Boolean(el&&!el.paused&&!el.ended)}},"Vídeo");
  return <video ref={ref} className="parity-video" title={title} controls playsInline preload="metadata" src={src} onError={onError}/>;
}

function BookReader({pdfUrl,assetUrl,title,itemId,onDownload}:{pdfUrl:string;assetUrl:string;title:string;itemId:string;onDownload:()=>void}){
  useMediaReporter("book",itemId,()=>({positionMs:0,durationMs:0,fraction:0,isActive:document.visibilityState==="visible"}),"Livro");
  return <div className="pdf-reader-shell">{pdfUrl?<div className="pdf-reader-frame-wrap"><iframe className="pdf-reader-frame" title={title} src={pdfUrl} allow="fullscreen" allowFullScreen/></div>:<p className="parity-status">Não foi possível preparar o leitor deste PDF.</p>}<div className="pdf-reader-toolbar">{pdfUrl&&<a className="pdf-primary" href={pdfUrl} target="_blank" rel="noreferrer"><ExternalLink size={18}/> Abrir leitor em tela cheia</a>}<button className="pdf-secondary" onClick={onDownload}><Download size={18}/> Baixar PDF</button></div><p className="pdf-reader-help">O leitor acima é multipágina: role dentro dele para continuar o livro. O progresso de XP considera tempo real de leitura na PWA; apenas abrir o livro não conta.</p></div>;
}

function MediaReaderV2({item,onBack}:{item:ViewItem;onBack:()=>void}){
  const[mediaError,setMediaError]=useState(false);
  const rawUrl=rawMediaUrl(item);const assetUrl=resolvePortableAssetUrl(rawUrl);const pdfUrl=resolvePdfEmbedUrl(rawUrl);const yid=item.kind==="Vídeo"?youtubeVideoId(rawUrl):"";
  const download=()=>{if(!assetUrl)return;const anchor=document.createElement("a");anchor.href=assetUrl;anchor.target="_blank";anchor.rel="noopener noreferrer";if(item.kind==="Livro")anchor.download=`${safeFilename(item.title||"livro-mic-rhema")}.pdf`;document.body.appendChild(anchor);anchor.click();anchor.remove()};
  if(item.kind==="Fotos"){
    const photos=(item.photos||[]).filter(photo=>photo.url);
    return <section className="parity-page"><button className="back-link" onClick={onBack}><ChevronLeft size={18}/> Voltar à mídia</button><div className="parity-title"><div><p>FOTOS</p><h1>{item.title||"Álbum"}</h1><span>{item.description||`${photos.length} foto(s)`}</span></div></div><div style={{display:"grid",gridTemplateColumns:"repeat(2,minmax(0,1fr))",gap:10}}>{photos.map((photo,index)=>{const photoUrl=resolveDisplayImageUrl(photo.url);return <figure key={`${photo.url}-${index}`} style={{margin:0}}>{photoUrl&&<img src={photoUrl} alt={photo.caption||`Foto ${index+1}`} loading="lazy" style={{width:"100%",aspectRatio:"1",objectFit:"cover",borderRadius:16}}/>}{photo.caption&&<figcaption style={{fontSize:12,marginTop:4}}>{photo.caption}</figcaption>}</figure>})}</div>{item.driveFolderUrl&&<a className="parity-primary" href={item.driveFolderUrl} target="_blank" rel="noreferrer">Abrir álbum completo</a>}</section>
  }
  return <section className="parity-page media-reader"><button className="back-link" onClick={onBack}><ChevronLeft size={18}/> Voltar à mídia</button><div className="parity-title"><div><p>{item.kind.toUpperCase()}</p><h1>{item.title||"Conteúdo"}</h1><span>{secondary(item)||"MIC Rhema"}</span></div></div>
    {!rawUrl?<p className="parity-status">Este conteúdo ainda não possui um arquivo cadastrado.</p>:item.kind==="Vídeo"?(yid?<><iframe className="parity-video" title={item.title||"Vídeo"} src={`https://www.youtube-nocookie.com/embed/${yid}?rel=0&playsinline=1`} allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowFullScreen/><p className="parity-status">Vídeos do YouTube continuam reproduzindo normalmente; o XP só é contado quando a PWA consegue verificar o tempo real de reprodução.</p></>:<NativeVideoReader src={assetUrl} title={item.title||"Vídeo"} itemId={item.id} onError={()=>setMediaError(true)}/>):item.kind==="Áudio"?<AudioReader rawUrl={rawUrl} title={item.title||"Áudio MIC Rhema"} itemId={item.id}/>:<BookReader pdfUrl={pdfUrl} assetUrl={assetUrl} title={item.title||"Livro em PDF"} itemId={item.id} onDownload={download}/>} 
    {mediaError&&<p className="parity-status">Não foi possível reproduzir este vídeo. Verifique se o arquivo ainda existe no armazenamento.</p>}
    {item.description&&<article className="parity-reader" style={{marginTop:14}}><p>{item.description}</p></article>}
  </section>;
}
