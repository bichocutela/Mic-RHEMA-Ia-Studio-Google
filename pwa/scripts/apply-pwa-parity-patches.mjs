import fs from "node:fs";
import path from "node:path";

function read(file){return fs.readFileSync(path.resolve(process.cwd(),file),"utf8")}
function write(file,value){fs.writeFileSync(path.resolve(process.cwd(),file),value)}
function replaceOnce(text,from,to,label){const count=text.split(from).length-1;if(count!==1)throw new Error(`${label}: esperado 1 trecho, encontrado ${count}`);return text.replace(from,to)}

// PwaShell: deixa de depender das telas legadas do Home.tsx para os módulos centrais já auditados.
{
  const file="client/src/components/PwaShell.tsx";let text=read(file);
  text=replaceOnce(text,
    'import { BibleParityView, MediaParityView, MembersParityView, type PwaSessionLike } from "./AndroidParityViews";',
    'import { MembersParityView, type PwaSessionLike } from "./AndroidParityViews";\nimport { BibleParityViewV2 } from "./BibleParityViewV2";\nimport { MediaParityViewV2 } from "./MediaParityViewV2";\nimport { DevotionalsParityView } from "./DevotionalsParityView";\nimport { IbrParityView } from "./IbrParityView";\nimport { NewsParityView } from "./NewsParityView";',
    "imports PwaShell");
  text=replaceOnce(text,
    ':active==="bible"?<BibleParityView/>:active==="media"?<MediaParityView/>',
    ':active==="bible"?<BibleParityViewV2/>:active==="media"?<MediaParityViewV2/>\n    :active==="devotionals"?<DevotionalsParityView/>:active==="ibr"?<IbrParityView session={session} onLogin={onProfile}/>:active==="news"?<NewsParityView onNavigate={onNavigate}/>',
    "roteamento PwaShell");
  write(file,text);
}

// Home: mesma lógica segura do Android para data do devocional, notícias por sessão e miniaturas do YouTube.
{
  const file="client/src/components/HomeParityView.tsx";let text=read(file);
  text=replaceOnce(text,
    'import type { AppView } from "./PwaShell";',
    'import type { AppView } from "./PwaShell";\nimport { appDateKey, contentTimestamp, parseAppDate, stableSessionShuffle, todayKey as localTodayKey, youtubeThumbnail } from "@/lib/parity-utils";',
    "import utilitários Home");
  text=replaceOnce(text,
    'function mediaImage(item:Media){const id=youtubeId(item.videoUrl||item.mediaUrl);return item.imageUrl||item.thumbnailUrl||item.coverUrl||(id?`https://i.ytimg.com/vi/${id}/hqdefault.jpg`:"")}',
    'function mediaImage(item:Media){const official=youtubeThumbnail(item.videoUrl||item.mediaUrl);return official||item.imageUrl||item.thumbnailUrl||item.coverUrl||""}',
    "thumbnail Home");
  text=replaceOnce(text,
    'const devotional=useMemo(()=>{const items=devotionals.filter(approved).sort((a,b)=>(b.timestamp||0)-(a.timestamp||0));const d=new Date();const pt=`${String(d.getDate()).padStart(2,"0")}/${String(d.getMonth()+1).padStart(2,"0")}/${d.getFullYear()}`;return items.find(i=>i.date===pt)||items[0]},[devotionals]);',
    'const devotional=useMemo(()=>{const d=new Date();const today=localTodayKey(d);const dayStart=new Date(d.getFullYear(),d.getMonth(),d.getDate()).getTime();const items=devotionals.filter(approved).filter(item=>{const parsed=parseAppDate(item.date);return !parsed||parsed.getTime()<=dayStart}).sort((a,b)=>{const bd=parseAppDate(b.date)?.getTime()||Number(b.timestamp||0);const ad=parseAppDate(a.date)?.getTime()||Number(a.timestamp||0);return bd-ad});return items.find(i=>appDateKey(i.date)===today)||items[0]},[devotionals]);',
    "devocional seguro Home");
  text=replaceOnce(text,
    'const latestNews=useMemo(()=>{const featured=news.filter(n=>n.featured);return(featured.length?featured:news).slice().sort((a,b)=>(b.publishedAt||0)-(a.publishedAt||0)).slice(0,5)},[news]);',
    'const latestNews=useMemo(()=>{const approvedNews=news.filter(item=>item.approved!==false&&item.isApproved!==false);const featured=approvedNews.filter(n=>n.featured);const source=(featured.length?featured:approvedNews).map(item=>({...item,id:String(item.id)}));return stableSessionShuffle(source,"micrhema:pwa:home-news-session",5)},[news]);',
    "notícias aleatórias Home");
  text=replaceOnce(text,
    'const media=useMemo(()=>[...videos.filter(approved).slice().sort((a,b)=>Number(b.id)-Number(a.id)).slice(0,3).map(i=>({...i,kind:"Vídeo" as const})),...audios.filter(approved).slice().sort((a,b)=>Number(b.id)-Number(a.id)).slice(0,3).map(i=>({...i,kind:"Áudio" as const})),...books.filter(approved).slice().sort((a,b)=>Number(b.id)-Number(a.id)).slice(0,3).map(i=>({...i,kind:"Livro" as const}))],[videos,audios,books]);',
    'const media=useMemo(()=>[...videos.filter(approved).slice().sort((a,b)=>contentTimestamp(b as any)-contentTimestamp(a as any)).slice(0,3).map(i=>({...i,kind:"Vídeo" as const})),...audios.filter(approved).slice().sort((a,b)=>contentTimestamp(b as any)-contentTimestamp(a as any)).slice(0,3).map(i=>({...i,kind:"Áudio" as const})),...books.filter(approved).slice().sort((a,b)=>contentTimestamp(b as any)-contentTimestamp(a as any)).slice(0,3).map(i=>({...i,kind:"Livro" as const}))],[videos,audios,books]);',
    "ordenação mídia Home");
  write(file,text);
}

// O build sempre deriva o calendário 2027 do fonte Android, evitando duas fontes de verdade.
{
  const file="package.json";let text=read(file);
  text=replaceOnce(text,
    '"sync:android-plans": "node scripts/sync-android-plans.mjs",\n    "build": "pnpm sync:android-plans && vite build',
    '"sync:android-plans": "node scripts/sync-android-plans.mjs",\n    "sync:android-devotionals": "node scripts/sync-android-devotionals-2027.mjs",\n    "build": "pnpm sync:android-plans && pnpm sync:android-devotionals && vite build',
    "scripts package.json");
  write(file,text);
}

console.log("Patches de paridade PWA aplicados com sucesso.");
