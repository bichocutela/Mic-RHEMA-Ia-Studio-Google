import { useEffect, useMemo, useRef, useState } from "react";
import { BookOpen, Check, ChevronLeft, Download, ExternalLink, Headphones, Image as ImageIcon, Play, Search, SortAsc, Video, X } from "lucide-react";
import { listenToCollection } from "@/lib/firebase";
import { recordPwaActivity } from "@/lib/badge-activity";
import {
  contentTimestamp, normalizeSearch, resolveDisplayImageUrl, resolvePdfEmbedUrl,
  resolvePortableAssetUrl, safeFilename, youtubeThumbnail, youtubeVideoId,
} from "@/lib/parity-utils";
import "./AndroidParityViews.css";

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
  const openItem=(item:ViewItem)=>{
    setSelected(item);
    if(item.kind==="Livro")void recordPwaActivity("books",item.id).catch(()=>undefined);
    if(item.kind==="Áudio")void recordPwaActivity("audios",item.id).catch(()=>undefined);
    if(item.kind==="Vídeo")void recordPwaActivity("videos",item.id).catch(()=>undefined);
  };
  if(selected)return <MediaReaderV2 item={selected} onBack={()=>setSelected(null)}/>;
  return <section className="parity-page media-parity"><div className="parity-title"><div><p>MÍDIA</p><h1>Conteúdo da igreja</h1><span>Livros, áudios, vídeos e fotos sincronizados em tempo real com o Android.</span></div><Play size={30}/></div>
    <div style={{display:"flex",gap:8,alignItems:"center",marginBottom:12}}><label style={{flex:1,display:"flex",alignItems:"center",gap:8,border:"1px solid #9ca3af",borderRadius:22,padding:"0 14px",minHeight:48}}><Search size={19}/><input value={query} onChange={e=>setQuery(e.target.value)} placeholder="Buscar por título ou descrição..." style={{flex:1,border:0,outline:0,background:"transparent",color:"inherit"}}/>{query&&<button onClick={()=>setQuery("")} aria-label="Limpar" style={{border:0,background:"transparent",color:"inherit"}}><X size={18}/></button>}</label><div style={{position:"relative"}}><button onClick={()=>setSortOpen(v=>!v)} aria-label={`Ordenar: ${sortLabels[sort]}`} style={{width:48,height:48,borderRadius:24,border:0,display:"grid",placeItems:"center"}}><SortAsc size={20}/></button>{sortOpen&&<div style={{position:"absolute",right:0,top:54,zIndex:30,minWidth:190,background:"var(--card,#fffdf7)",borderRadius:14,padding:8,boxShadow:"0 14px 36px rgba(0,0,0,.2)"}}>{(Object.keys(sortLabels) as SortMode[]).map(option=><button key={option} onClick={()=>{setSort(option);setSortOpen(false)}} style={{width:"100%",display:"flex",gap:8,alignItems:"center",padding:10,border:0,background:"transparent",color:"inherit",fontWeight:700,textAlign:"left"}}>{sort===option?<Check size={16}/>:<span style={{width:16}}/>}{sortLabels[option]}</button>)}</div>}</div></div>
    <div className="filter-pills">{(["Livro","Áudio","Vídeo","Fotos"] as Kind[]).map(kind=><button key={kind} className={tab===kind?"selected":""} onClick={()=>setTab(kind)}>{kind==="Livro"?"Livros":kind==="Áudio"?"Áudios":kind==="Vídeo"?"Vídeos":"Fotos"}</button>)}</div>
    {!visible.length?<p className="parity-status">Nenhum conteúdo encontrado.</p>:<div className="media-parity-grid">{visible.map(item=>{const cover=image(item);return <button key={`${item.kind}-${item.id}`} onClick={()=>openItem(item)}><div className="media-cover">{cover?<img src={cover} alt="" loading="lazy"/>:item.kind==="Áudio"?<Headphones/>:item.kind==="Livro"?<BookOpen/>:item.kind==="Fotos"?<ImageIcon/>:<Video/>}</div><span>{item.kind}</span><strong>{item.title||"Conteúdo MIC Rhema"}</strong><small>{secondary(item)||"Abrir conteúdo"}</small></button>})}</div>}
  </section>
}

function MediaReaderV2({item,onBack}:{item:ViewItem;onBack:()=>void}){
  const audioRef=useRef<HTMLAudioElement|null>(null);const[mediaError,setMediaError]=useState(false);const[speed,setSpeed]=useState(1);
  const rawUrl=rawMediaUrl(item);const assetUrl=resolvePortableAssetUrl(rawUrl);const pdfUrl=resolvePdfEmbedUrl(rawUrl);const yid=item.kind==="Vídeo"?youtubeVideoId(rawUrl):"";
  const download=()=>{if(!assetUrl)return;const anchor=document.createElement("a");anchor.href=assetUrl;anchor.target="_blank";anchor.rel="noopener noreferrer";if(item.kind==="Livro")anchor.download=`${safeFilename(item.title||"livro-mic-rhema")}.pdf`;document.body.appendChild(anchor);anchor.click();anchor.remove()};
  const changeSpeed=()=>{const next=speed>=2?.75:Math.min(2,speed+.25);setSpeed(next);if(audioRef.current)audioRef.current.playbackRate=next};
  if(item.kind==="Fotos"){
    const photos=(item.photos||[]).filter(photo=>photo.url);
    return <section className="parity-page"><button className="back-link" onClick={onBack}><ChevronLeft size={18}/> Voltar à mídia</button><div className="parity-title"><div><p>FOTOS</p><h1>{item.title||"Álbum"}</h1><span>{item.description||`${photos.length} foto(s)`}</span></div></div><div style={{display:"grid",gridTemplateColumns:"repeat(2,minmax(0,1fr))",gap:10}}>{photos.map((photo,index)=>{const photoUrl=resolveDisplayImageUrl(photo.url);return <figure key={`${photo.url}-${index}`} style={{margin:0}}>{photoUrl&&<img src={photoUrl} alt={photo.caption||`Foto ${index+1}`} loading="lazy" style={{width:"100%",aspectRatio:"1",objectFit:"cover",borderRadius:16}}/>}{photo.caption&&<figcaption style={{fontSize:12,marginTop:4}}>{photo.caption}</figcaption>}</figure>})}</div>{item.driveFolderUrl&&<a className="parity-primary" href={item.driveFolderUrl} target="_blank" rel="noreferrer">Abrir álbum completo</a>}</section>
  }
  return <section className="parity-page media-reader"><button className="back-link" onClick={onBack}><ChevronLeft size={18}/> Voltar à mídia</button><div className="parity-title"><div><p>{item.kind.toUpperCase()}</p><h1>{item.title||"Conteúdo"}</h1><span>{secondary(item)||"MIC Rhema"}</span></div></div>
    {!rawUrl?<p className="parity-status">Este conteúdo ainda não possui um arquivo cadastrado.</p>:item.kind==="Vídeo"?(yid?<iframe className="parity-video" title={item.title||"Vídeo"} src={`https://www.youtube-nocookie.com/embed/${yid}?rel=0&playsinline=1`} allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowFullScreen/>:<video className="parity-video" controls playsInline preload="metadata" src={assetUrl} onError={()=>setMediaError(true)}/>):item.kind==="Áudio"?<div className="audio-reader"><Headphones size={42}/><strong>{item.title||"Áudio MIC Rhema"}</strong><audio ref={audioRef} controls preload="metadata" src={assetUrl} onError={()=>setMediaError(true)}>Seu navegador não suporta áudio.</audio><div className="audio-skip"><button onClick={()=>{if(audioRef.current)audioRef.current.currentTime=Math.max(0,audioRef.current.currentTime-15)}}>-15s</button><button onClick={()=>{if(audioRef.current)audioRef.current.currentTime+=15}}>+15s</button><button onClick={changeSpeed}>{speed.toFixed(2).replace(/\.00$/,"")}x</button></div><small>O áudio continua dentro da PWA; use os controles acima para pausar, avançar e alterar a velocidade.</small></div>:<div style={{display:"grid",gap:12}}><iframe className="parity-document" style={{width:"100%",minHeight:"68dvh",border:0,borderRadius:16,background:"#fff"}} title={item.title||"PDF"} src={pdfUrl}/><div style={{display:"flex",gap:8,flexWrap:"wrap"}}><button className="parity-primary" onClick={download}><Download size={18}/> Baixar PDF</button><a className="back-link" href={assetUrl} target="_blank" rel="noreferrer"><ExternalLink size={17}/> Abrir em tela cheia</a></div></div>}
    {mediaError&&<p className="parity-status">Não foi possível reproduzir este arquivo. Verifique se o arquivo ainda existe no armazenamento ou substitua-o pelo ADM.</p>}
    {item.description&&<article className="parity-reader" style={{marginTop:14}}><p>{item.description}</p></article>}
  </section>;
}
