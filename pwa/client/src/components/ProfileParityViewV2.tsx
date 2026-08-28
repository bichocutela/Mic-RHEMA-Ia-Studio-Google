import { useEffect, useMemo, useState } from "react";
import { signOut } from "firebase/auth";
import { BadgeCheck, BookOpen, LogOut, RefreshCcw, Save, Trophy, UserRound } from "lucide-react";
import { toast } from "sonner";
import {
  firebaseAuth, listenToCollection, listenToIbrProgress, loadPwaMemberProfile, savePwaMemberProfile,
  type PwaMemberProfile,
} from "@/lib/firebase";
import type { PwaSessionLike } from "./AndroidParityViews";
import "./ProfileParityViewV2.css";

type Avatar = { id: string; name: string };
type Badge = { id: string; name: string; description: string; requirement: string; level?: number; achievement?: boolean };
type IbrCourse = { id: string; chapters?: Array<{ id?: string }> };
type IbrProgress = { id: string; courseId?: string; chapterId?: string; isCompleted?: boolean };

const avatars: Avatar[] = [
  { id:"davi",name:"Davi"},{id:"ester",name:"Ester"},{id:"daniel",name:"Daniel"},{id:"rute",name:"Rute"},
  { id:"moises",name:"Moisés"},{id:"noe",name:"Noé"},{id:"maria",name:"Maria"},{id:"paulo",name:"Paulo"},
  { id:"josue",name:"Josué"},{id:"abraao",name:"Abraão"},{id:"sara",name:"Sara"},{id:"rebeca",name:"Rebeca"},
  { id:"jaco",name:"Jacó"},{id:"jose",name:"José"},{id:"samuel",name:"Samuel"},{id:"elias",name:"Elias"},
  { id:"isaias",name:"Isaías"},{id:"jeremias",name:"Jeremias"},{id:"joao_batista",name:"João Batista"},
  { id:"timoteo",name:"Timóteo"},{id:"priscila",name:"Priscila"},{id:"lidia",name:"Lídia"},
];

const badges: Badge[] = [
  {id:"caminhante",name:"Caminhante",description:"O início de uma jornada de fé e conhecimento.",requirement:"Criar o perfil e escolher um avatar",level:1},
  {id:"semeador",name:"Semeador",description:"Quem planta a Palavra no coração todos os dias.",requirement:"Ler 3 devocionais e concluir 1 tema de plano",level:2},
  {id:"discipulo",name:"Discípulo",description:"Um passo firme no aprendizado da Palavra.",requirement:"Concluir 1 plano, 3 temas e ler 3 capítulos da Bíblia",level:3},
  {id:"perseverante",name:"Perseverante",description:"Constância para continuar mesmo nos dias difíceis.",requirement:"Acumular 60 minutos ativos e realizar 10 atividades",level:4},
  {id:"estudante_rhema",name:"Estudante Rhema",description:"Dedicação reconhecida ao estudo no Instituto Bíblico Rhema.",requirement:"Ler 3 livros, assistir 3 vídeos e ouvir 2 áudios",level:5},
  {id:"mestre_da_palavra",name:"Mestre da Palavra",description:"Conhecimento construído com disciplina e compromisso.",requirement:"Concluir 1 curso IBR, ler 3 notícias e 10 capítulos da Bíblia",level:6},
  {id:"guardiao_da_fe",name:"Guardião da Fé",description:"Um testemunho de perseverança, serviço e maturidade.",requirement:"Realizar todas as atividades e acumular 180 minutos ativos",level:7},
  {id:"primeira_oracao",name:"Primeira Oração",description:"Um primeiro momento separado para falar com Deus.",requirement:"Registrar o primeiro momento de oração",achievement:true},
  {id:"leitor_da_palavra",name:"Leitor da Palavra",description:"A Bíblia aberta e o coração disposto a aprender.",requirement:"Ler 10 capítulos da Bíblia",achievement:true},
  {id:"coracao_grato",name:"Coração Grato",description:"Reconhecimento pelas bênçãos recebidas.",requirement:"Registrar uma mensagem de gratidão",achievement:true},
  {id:"constante",name:"Constante",description:"Pequenos passos repetidos com fidelidade.",requirement:"Estudar por 7 dias consecutivos",achievement:true},
  {id:"certificado_ibr",name:"Certificado IBR",description:"Uma conquista acadêmica no Instituto Bíblico Rhema.",requirement:"Receber um certificado IBR",achievement:true},
];

const rawBase = "https://raw.githubusercontent.com/bichocutela/Mic-RHEMA-Ia-Studio-Google/main/app/src/main/res/drawable-nodpi";
const avatarUrl = (id: string) => `${rawBase}/avatar_${id}.png`;
const count = (profile: PwaMemberProfile, key: string) => new Set(profile.badgeActivityIds?.[key] || []).size;

function computeProgress(profile: PwaMemberProfile, courses: IbrCourse[], progress: IbrProgress[]) {
  const counts = {
    plans: count(profile,"plans"), plan_themes: count(profile,"plan_themes"), books: count(profile,"books"),
    videos: count(profile,"videos"), bible_chapters: count(profile,"bible_chapters"), bible_news: count(profile,"bible_news"),
    devotionals: count(profile,"devotionals"), audios: count(profile,"audios"),
  };
  const activeMinutes = count(profile,"active_minutes");
  const completedCourses = courses.filter((course) => (course.chapters?.length || 0) > 0 && course.chapters!.every((chapter) => progress.some((item) => item.courseId===course.id && item.chapterId===chapter.id && item.isCompleted))).length;
  const calculated = new Set(profile.unlockedBadgeIds?.length ? profile.unlockedBadgeIds : ["caminhante"]);
  if(counts.devotionals>=3 && counts.plan_themes>=1) calculated.add("semeador");
  if(counts.plans>=1 && counts.plan_themes>=3 && counts.bible_chapters>=3) calculated.add("discipulo");
  if(activeMinutes>=60 && Object.values(counts).reduce((a,b)=>a+b,0)>=10) calculated.add("perseverante");
  if(counts.books>=3 && counts.videos>=3 && counts.audios>=2) calculated.add("estudante_rhema");
  if(completedCourses>=1 && counts.bible_news>=3 && counts.bible_chapters>=10) calculated.add("mestre_da_palavra");
  if(calculated.has("mestre_da_palavra") && Object.values(counts).every((value)=>value>=1) && activeMinutes>=180) calculated.add("guardiao_da_fe");
  const levels = badges.filter((badge)=>badge.level).sort((a,b)=>(a.level||0)-(b.level||0));
  const highest = Math.max(1,...levels.filter((badge)=>calculated.has(badge.id)).map((badge)=>badge.level||1));
  const next = levels.find((badge)=>(badge.level||0)>highest);
  let fraction=1;
  if(next?.id==="semeador") fraction=Math.min(counts.devotionals/3,counts.plan_themes/1);
  if(next?.id==="discipulo") fraction=Math.min(counts.plans/1,counts.plan_themes/3,counts.bible_chapters/3);
  if(next?.id==="perseverante") fraction=Math.min(activeMinutes/60,Object.values(counts).reduce((a,b)=>a+b,0)/10);
  if(next?.id==="estudante_rhema") fraction=Math.min(counts.books/3,counts.videos/3,counts.audios/2);
  if(next?.id==="mestre_da_palavra") fraction=Math.min(completedCourses/1,counts.bible_news/3,counts.bible_chapters/10);
  if(next?.id==="guardiao_da_fe") fraction=Math.min(1,Math.min(...Object.values(counts)),activeMinutes/180);
  return { counts, activeMinutes, completedCourses, calculated, next, fraction: Math.max(0,Math.min(1,fraction)) };
}

export function ProfileParityViewV2({ session, onNavigateHome }: { session: PwaSessionLike; onNavigateHome: () => void }) {
  const [profile,setProfile]=useState<PwaMemberProfile|null>(null); const [draft,setDraft]=useState<PwaMemberProfile|null>(null);
  const [courses,setCourses]=useState<IbrCourse[]>([]); const [ibrProgress,setIbrProgress]=useState<IbrProgress[]>([]);
  const [loading,setLoading]=useState(true); const [saving,setSaving]=useState(false); const [avatarsOpen,setAvatarsOpen]=useState(false); const [badgesOpen,setBadgesOpen]=useState(false);
  const reload=async()=>{setLoading(true);try{const value=await loadPwaMemberProfile();setProfile(value);setDraft(value);}catch(error){toast.error(error instanceof Error?error.message:"Não foi possível carregar o perfil.");}finally{setLoading(false)}};
  useEffect(()=>{if(session)void reload()},[session?.uid]);
  useEffect(()=>listenToCollection<IbrCourse>("ibr_courses",setCourses,()=>setCourses([])),[]);
  useEffect(()=>profile?.id?listenToIbrProgress<IbrProgress>(profile.id,setIbrProgress,()=>setIbrProgress([])):()=>undefined,[profile?.id]);
  const summary=useMemo(()=>profile?computeProgress(profile,courses,ibrProgress):null,[profile,courses,ibrProgress]);
  if(!session)return <section className="parity-page"><div className="parity-empty"><UserRound size={46}/><h1>Meu Perfil</h1><p>Entre para acessar seus dados e conquistas.</p></div></section>;
  if(loading||!profile||!draft||!summary)return <section className="parity-page"><p className="parity-status">Sincronizando seu perfil…</p></section>;
  const avatar=avatars.find((item)=>item.id===draft.avatarId)||avatars[0];
  const equipped=badges.find((item)=>item.id===draft.equippedBadgeId)||badges[0];
  const save=async()=>{setSaving(true);try{const updated=await savePwaMemberProfile({name:draft.name,phone:draft.phone,address:draft.address,birthDate:draft.birthDate,email:draft.email,avatarId:draft.avatarId,equippedBadgeId:draft.equippedBadgeId});setProfile(updated);setDraft(updated);const stored=localStorage.getItem("mic-rhema-pwa-session");if(stored){const parsed=JSON.parse(stored);parsed.name=updated.name;localStorage.setItem("mic-rhema-pwa-session",JSON.stringify(parsed));}toast.success("Perfil atualizado também para o Android.");}catch(error){toast.error(error instanceof Error?error.message:"Não foi possível salvar.");}finally{setSaving(false)}};
  const logout=async()=>{if(firebaseAuth)await signOut(firebaseAuth).catch(()=>undefined);localStorage.removeItem("mic-rhema-pwa-session");onNavigateHome();window.location.reload();};

  return <section className="parity-page profile-v2-root">
    <header className="profile-v2-hero"><img src={avatarUrl(avatar.id)} alt={`Avatar bíblico ${avatar.name}`}/><div><p>SEU AVATAR BÍBLICO</p><h1>{draft.name}</h1><span>{avatar.name} · {equipped.name}</span></div></header>
    <article className="profile-v2-level"><Trophy size={25}/><div><strong>{equipped.name}</strong><span>{equipped.description}</span>{summary.next?<><small>Próximo nível: {summary.next.name} — {summary.next.requirement}</small><div className="profile-v2-progress"><i style={{width:`${Math.round(summary.fraction*100)}%`}}/></div><b>{Math.round(summary.fraction*100)}%</b></>:<small>Todos os níveis principais alcançados.</small>}</div></article>
    <div className="profile-v2-stats"><div><strong>{summary.calculated.size}</strong><span>Conquistas</span></div><div><strong>{summary.activeMinutes}</strong><span>Minutos ativos</span></div><div><strong>{summary.completedCourses}</strong><span>Cursos IBR</span></div><div><strong>{summary.counts.bible_chapters}</strong><span>Capítulos</span></div></div>
    <div className="profile-fields"><label>Nome completo<input value={draft.name} onChange={(e)=>setDraft({...draft,name:e.target.value})}/></label><label>Telefone<input inputMode="tel" value={draft.phone} onChange={(e)=>setDraft({...draft,phone:e.target.value.replace(/\D/g,"").slice(0,15)})}/></label><label>Endereço<input value={draft.address} onChange={(e)=>setDraft({...draft,address:e.target.value})}/></label><label>Data de nascimento<input inputMode="numeric" placeholder="dd/mm/aaaa" value={draft.birthDate} onChange={(e)=>setDraft({...draft,birthDate:e.target.value.replace(/[^0-9/]/g,"").slice(0,10)})}/></label><label>E-mail para certificado IBR<input type="email" value={draft.email} onChange={(e)=>setDraft({...draft,email:e.target.value})}/></label></div>
    <div className="profile-choice-actions"><button onClick={()=>setAvatarsOpen(!avatarsOpen)}><UserRound size={18}/> Trocar avatar</button><button onClick={()=>setBadgesOpen(!badgesOpen)}><BadgeCheck size={18}/> Emblemas e níveis</button></div>
    {avatarsOpen&&<div className="profile-v2-avatar-grid">{avatars.map((item)=><button key={item.id} className={draft.avatarId===item.id?"selected":""} onClick={()=>{setDraft({...draft,avatarId:item.id});setAvatarsOpen(false)}}><img src={avatarUrl(item.id)} alt={item.name} loading="lazy"/><small>{item.name}</small></button>)}</div>}
    {badgesOpen&&<div className="profile-v2-badges">{badges.map((badge)=>{const unlocked=summary.calculated.has(badge.id);const equipable=(profile.unlockedBadgeIds||[]).includes(badge.id)||badge.id==="caminhante";return <button key={badge.id} disabled={!equipable} className={draft.equippedBadgeId===badge.id?"selected":""} onClick={()=>equipable&&setDraft({...draft,equippedBadgeId:badge.id})}><BadgeCheck size={20}/><span><strong>{badge.level?`Nível ${badge.level} · `:""}{badge.name}</strong><small>{unlocked?badge.description:`Bloqueado — ${badge.requirement}`}</small>{unlocked&&!equipable&&<em>Conquista calculada; abra o Android para sincronizar o desbloqueio.</em>}</span></button>})}</div>}
    <article className="profile-v2-activity"><BookOpen size={21}/><div><strong>Atividade registrada</strong><small>{summary.counts.devotionals} devocionais · {summary.counts.plan_themes} temas · {summary.counts.books} livros · {summary.counts.videos} vídeos · {summary.counts.audios} áudios · {summary.counts.bible_news} notícias</small></div></article>
    <button className="parity-primary" disabled={saving} onClick={()=>void save()}><Save size={18}/>{saving?"Salvando…":"Salvar alterações"}</button><button className="parity-danger" onClick={()=>void logout()}><LogOut size={18}/> Sair da conta</button><button className="profile-v2-refresh" onClick={()=>void reload()}><RefreshCcw size={17}/> Atualizar progresso</button>
  </section>;
}
