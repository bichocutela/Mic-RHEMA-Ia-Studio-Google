import fs from "node:fs";
import path from "node:path";

function read(file){return fs.readFileSync(path.resolve(process.cwd(),file),"utf8")}
function write(file,value){fs.writeFileSync(path.resolve(process.cwd(),file),value)}
function replaceOnce(text,from,to,label){const count=text.split(from).length-1;if(count!==1)throw new Error(`${label}: esperado 1 trecho, encontrado ${count}`);return text.replace(from,to)}

// Shell: todas as áreas centrais passam a usar as telas auditadas de paridade.
{
  const file="client/src/components/PwaShell.tsx";let text=read(file);
  text=replaceOnce(text,
    'import { BibleParityView, MediaParityView, MembersParityView, type PwaSessionLike } from "./AndroidParityViews";',
    'import { MembersParityView, type PwaSessionLike } from "./AndroidParityViews";\nimport { BibleParityViewV2 } from "./BibleParityViewV2";\nimport { MediaParityViewV2 } from "./MediaParityViewV2";\nimport { DevotionalsParityView } from "./DevotionalsParityView";\nimport { IbrParityView } from "./IbrParityView";\nimport { NewsParityView } from "./NewsParityView";\nimport { PlansParityView } from "./PlansParityView";\nimport { CultosParityView } from "./CultosParityView";\nimport { DiscipuladoParityViewV2 } from "./DiscipuladoParityViewV2";\nimport { DrawerBadgesParity } from "./DrawerBadgesParity";\nimport { startPwaActiveMinuteTracker } from "@/lib/badge-activity";',
    "imports PwaShell");
  text=replaceOnce(text,
    '<div className="drawer-badges"><span>SEU CAMINHO</span><b>{session?"Continue sua jornada":"Entre para acompanhar"}</b></div>',
    '<DrawerBadgesParity session={session}/>',
    "emblemas no drawer");
  text=replaceOnce(text,
    'export function PwaShell({children,active,onNavigate,drawerOpen,onCloseDrawer,onOpenDrawer,onProfile,session,onNotifications}:{children:React.ReactNode;active:AppView;onNavigate:(view:AppView)=>void;drawerOpen:boolean;onCloseDrawer:()=>void;onOpenDrawer:()=>void;onProfile:()=>void;session:PwaSessionLike;onNotifications:()=>void}){\n  const content=',
    'export function PwaShell({children,active,onNavigate,drawerOpen,onCloseDrawer,onOpenDrawer,onProfile,session,onNotifications}:{children:React.ReactNode;active:AppView;onNavigate:(view:AppView)=>void;drawerOpen:boolean;onCloseDrawer:()=>void;onOpenDrawer:()=>void;onProfile:()=>void;session:PwaSessionLike;onNotifications:()=>void}){\n  useEffect(()=>startPwaActiveMinuteTracker(),[session?.uid]);\n  const content=',
    "tracker de minutos ativos");
  text=replaceOnce(text,
    ':active==="bible"?<BibleParityView/>:active==="media"?<MediaParityView/>',
    ':active==="bible"?<BibleParityViewV2/>:active==="media"?<MediaParityViewV2/>\n    :active==="devotionals"?<DevotionalsParityView/>:active==="ibr"?<IbrParityView session={session} onLogin={onProfile}/>:active==="news"?<NewsParityView onNavigate={onNavigate}/>\n    :active==="plans"?<PlansParityView/>:active==="cultos"?<CultosParityView/>:active==="discipulado"?<DiscipuladoParityViewV2/>',
    "roteamento PwaShell");
  write(file,text);
}

// Home: mesmas regras Android para data, notícias por sessão, ordem recente e thumbnail.
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

// Progresso real de emblemas: abrir conteúdos na PWA registra a mesma atividade do Android.
{
  const file="client/src/components/DevotionalsParityView.tsx";let text=read(file);
  text=replaceOnce(text,'import { listenToCollection } from "@/lib/firebase";','import { listenToCollection } from "@/lib/firebase";\nimport { recordPwaActivity } from "@/lib/badge-activity";',"badge devocionais import");
  text=replaceOnce(text,'onClick={() => setSelected(item)}','onClick={() => { setSelected(item); void recordPwaActivity("devotionals", item.id).catch(() => undefined); }}',"badge devocionais click");write(file,text);
}
{
  const file="client/src/components/NewsParityView.tsx";let text=read(file);
  text=replaceOnce(text,'import { listenToCollection } from "@/lib/firebase";','import { listenToCollection } from "@/lib/firebase";\nimport { recordPwaActivity } from "@/lib/badge-activity";',"badge notícias import");
  text=replaceOnce(text,'onClick={()=>setSelected(item)}','onClick={()=>{setSelected(item);void recordPwaActivity("bible_news",item.id).catch(()=>undefined)}}',"badge notícias click");write(file,text);
}
{
  const file="client/src/components/MediaParityViewV2.tsx";let text=read(file);
  text=replaceOnce(text,'import { listenToCollection } from "@/lib/firebase";','import { listenToCollection } from "@/lib/firebase";\nimport { recordPwaActivity } from "@/lib/badge-activity";',"badge mídia import");
  text=replaceOnce(text,'onClick={()=>setSelected(item)}','onClick={()=>{setSelected(item);const key=item.kind==="Vídeo"?"videos":item.kind==="Áudio"?"audios":item.kind==="Livro"?"books":null;if(key)void recordPwaActivity(key,item.id).catch(()=>undefined)}}',"badge mídia click");write(file,text);
}
{
  const file="client/src/components/BibleParityViewV2.tsx";let text=read(file);
  text=replaceOnce(text,'import { normalizeSearch } from "@/lib/parity-utils";','import { normalizeSearch } from "@/lib/parity-utils";\nimport { recordPwaActivity } from "@/lib/badge-activity";',"badge Bíblia import");
  text=replaceOnce(text,'.then(setVerses).catch(e=>{if(e?.name!=="AbortError")setError(e.message||"Falha ao carregar.")})','.then(value=>{setVerses(value);void recordPwaActivity("bible_chapters",`${translation}:${book}:${chapter}`).catch(()=>undefined)}).catch(e=>{if(e?.name!=="AbortError")setError(e.message||"Falha ao carregar.")})',"badge capítulo Bíblia");write(file,text);
}

// Preferências de push são persistidas no servidor sempre que o usuário muda Configurações.
{
  const file="client/src/components/SettingsParityViewV2.tsx";let text=read(file);
  text=replaceOnce(text,'import { firebaseAuth } from "@/lib/firebase";','import { firebaseAuth } from "@/lib/firebase";\nimport { syncPwaPushPreferences } from "@/lib/push";',"push settings import");
  text=replaceOnce(text,'  useEffect(() => { applySettings(settings); }, [settings]);','  useEffect(() => { applySettings(settings); const timer=window.setTimeout(()=>void syncPwaPushPreferences(),300); return()=>window.clearTimeout(timer); }, [settings]);',"sync preferências push");write(file,text);
}

// O perfil não manda mais o usuário abrir o Android para persistir um emblema calculado na PWA.
{
  const file="client/src/components/ProfileParityViewV2.tsx";let text=read(file);
  text=text.replace('{unlocked&&!equipable&&<em>Conquista calculada; abra o Android para sincronizar o desbloqueio.</em>}','{unlocked&&!equipable&&<em>Conquista calculada; a sincronização ocorre automaticamente ao registrar atividades.</em>}');
  write(file,text);
}

// O build deriva automaticamente calendário e planos dos fontes Android.
{
  const file="package.json";let text=read(file);
  text=replaceOnce(text,
    '"sync:android-plans": "node scripts/sync-android-plans.mjs",\n    "build": "pnpm sync:android-plans && vite build',
    '"sync:android-plans": "node scripts/sync-android-plans.mjs",\n    "sync:android-devotionals": "node scripts/sync-android-devotionals-2027.mjs",\n    "build": "pnpm sync:android-plans && pnpm sync:android-devotionals && vite build',
    "scripts package.json");
  write(file,text);
}

console.log("Patches de paridade PWA aplicados com sucesso.");
