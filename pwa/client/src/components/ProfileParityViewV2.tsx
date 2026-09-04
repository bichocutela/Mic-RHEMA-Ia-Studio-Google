import { useEffect, useMemo, useState } from "react";
import { signOut } from "firebase/auth";
import { BadgeCheck, BookOpen, LogOut, RefreshCcw, Save, Trophy, UserRound } from "lucide-react";
import { toast } from "sonner";
import {
  firebaseAuth, listenToCollection, listenToIbrProgress, loadPwaMemberProfile, savePwaMemberProfile,
  type PwaMemberProfile,
} from "@/lib/firebase";
import type { PwaSessionLike } from "./AndroidParityViews";
import { BiblicalBadgeAvatar } from "./BiblicalBadgeAvatar";
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
  const totalActivities=Object.values(counts).reduce((a,b)=>a+b,0);
  const activeMinutes = count(profile,"active_minutes");
  const completedLessons=progress.filter((item)=>item.isCompleted).length;
  const completedCourses = courses.filter((course) => (course.chapters?.length || 0) > 0 && course.chapters!.every((chapter) => progress.some((item) => item.courseId===course.id && item.chapterId===chapter.id && item.isCompleted))).length;
  const calculated = new Set(profile.unlockedBadgeIds?.length ? profile.unlockedBadgeIds : ["caminhante"]);
  calculated.add("caminhante");
  if(counts.devotionals>=3 && counts.plan_themes>=1) calculated.add("semeador");
  if(counts.plans>=1 && counts.plan_themes>=3 && counts.bible_chapters>=3) calculated.add("discipulo");
  if(activeMinutes>=60 && totalActivities>=10) calculated.add("perseverante");
  if(counts.books>=3 && counts.videos>=3 && counts.audios>=2) calculated.add("estudante_rhema");
  if(completedCourses>=1 && counts.bible_news>=3 && counts.bible_chapters>=10) calculated.add("mestre_da_palavra");
  if(calculated.has("mestre_da_palavra") && Object.values(counts).every((value)=>value>=1) && activeMinutes>=180) calculated.add("guardiao_da_fe");
  const levels = badges.filter((badge)=>badge.level).sort((a,b)=>(a.level||0)-(b.level||0));
  const highest = Math.max(1,...levels.filter((badge)=>calculated.has(badge.id)).map((badge)=>badge.level||1));
  const next = levels.find((badge)=>(badge.level||0)>highest);
  const missionFraction=(id:string)=>{
    if(id==="caminhante")return 1;
    if(id==="semeador")return Math.min(counts.devotionals/3,counts.plan_themes/1);
    if(id==="discipulo")return Math.min(counts.plans/1,counts.plan_themes/3,counts.bible_chapters/3);
    if(id==="perseverante")return Math.min(activeMinutes/60,totalActivities/10);
    if(id==="estudante_rhema")return Math.min(counts.books/3,counts.videos/3,counts.audios/2);
    if(id==="mestre_da_palavra")return Math.min(completedCourses/1,counts.bible_news/3,counts.bible_chapters/10);
    if(id==="guardiao_da_fe")return calculated.has("mestre_da_palavra")?Math.min(Math.min(...Object.values(counts)),activeMinutes/180):0;
    return calculated.has(id)?1:0;
  };
  const fraction=next?missionFraction(next.id):1;
  return { counts, totalActivities, activeMinutes, completedLessons, completedCourses, calculated, next, missionFraction, fraction: Math.max(0,Math.min(1,fraction)) };
}

export function ProfileParityViewV2({ session, onNavigateHome }: { session: PwaSessionLike; onNavigateHome: () => void }) {
  const [profile,setProfile]=useState<PwaMemberProfile|null>(null); const [draft,setDraft]=useState<PwaMemberProfile|null>(null);
  const [courses,setCourses]=useState<IbrCourse[]>([]); const [ibrProgress,setIbrProgress]=useState<IbrProgress[]>([]);
  const [loading,setLoading]=useState(true); const [loadError,setLoadError]=useState(""); const [saving,setSaving]=useState(false); const [avatarsOpen,setAvatarsOpen]=useState(false); const [badgesOpen,setBadgesOpen]=useState(false); const [missionsOpen,setMissionsOpen]=useState(true); const [focusedBadgeId,setFocusedBadgeId]=useState<string|null>(null);
  const reload=async()=>{setLoading(true);setLoadError("");try{const value=await loadPwaMemberProfile();setProfile(value);setDraft(value);}catch(error){const message=error instanceof Error?error.message:"Não foi possível carregar o perfil.";setLoadError(message);toast.error(message);}finally{setLoading(false)}};
  useEffect(()=>{if(session)void reload()},[session?.uid]);
  useEffect(()=>listenToCollection<IbrCourse>("ibr_courses",setCourses,()=>setCourses([])),[]);
  useEffect(()=>profile?.id?listenToIbrProgress<IbrProgress>(profile.id,setIbrProgress,()=>setIbrProgress([])):()=>undefined,[profile?.id]);
  useEffect(()=>{
    if(!profile?.id)return;
    const pending=localStorage.getItem("micrhema:pwa:open-badge");
    if(!pending)return;
    setFocusedBadgeId(pending);setBadgesOpen(true);setMissionsOpen(false);
    localStorage.removeItem("micrhema:pwa:open-badge");
    window.setTimeout(()=>document.getElementById(`profile-badge-${pending}`)?.scrollIntoView({behavior:"smooth",block:"center"}),180);
  },[profile?.id]);
  const summary=useMemo(()=>profile?computeProgress(profile,courses,ibrProgress):null,[profile,courses,ibrProgress]);
  const logout=async()=>{if(firebaseAuth)await signOut(firebaseAuth).catch(()=>undefined);localStorage.removeItem("mic-rhema-pwa-session");onNavigateHome();window.location.reload();};
  if(!session)return <section className="parity-page"><div className="parity-empty"><UserRound size={46}/><h1>Meu Perfil</h1><p>Entre para acessar seus dados e conquistas.</p></div></section>;
  if(loading)return <section className="parity-page"><p className="parity-status">Sincronizando seu perfil com o Android…</p></section>;
  if(loadError||!profile||!draft||!summary)return <section className="parity-page"><div className="parity-empty"><UserRound size={46}/><h1>Não foi possível sincronizar o perfil</h1><p>{session.isAdmin?"Sua sessão de administrador pode ser anterior à unificação com o perfil Android. Atualize ou saia e entre novamente uma vez para vincular o mesmo cadastro.":loadError}</p><button className="parity-primary" onClick={()=>void reload()}><RefreshCcw size={17}/> Tentar novamente</button>{session.isAdmin&&<button className="parity-danger" onClick={()=>void logout()}><LogOut size={18}/> Sair e renovar sessão</button>}</div></section>;
  const avatar=avatars.find((item)=>item.id===draft.avatarId)||avatars[0];
  const equipped=badges.find((item)=>item.id===draft.equippedBadgeId)||badges[0];
  const persist=async(next:PwaMemberProfile,success:string)=>{setSaving(true);try{const updated=await savePwaMemberProfile({name:next.name,phone:next.phone,address:next.address,birthDate:next.birthDate,email:next.email,avatarId:next.avatarId,equippedBadgeId:next.equippedBadgeId});setProfile(updated);setDraft(updated);const stored=localStorage.getItem("mic-rhema-pwa-session");if(stored){const parsed=JSON.parse(stored);parsed.name=updated.name;localStorage.setItem("mic-rhema-pwa-session",JSON.stringify(parsed));}toast.success(success);}catch(error){toast.error(error instanceof Error?error.message:"Não foi possível salvar.");setDraft(profile);}finally{setSaving(false)}};
  const save=()=>persist(draft,"Perfil atualizado também para o Android.");
  const chooseAvatar=(id:string)=>{const next={...draft,avatarId:id};setDraft(next);setAvatarsOpen(false);void persist(next,"Avatar sincronizado com o Android.")};
  const chooseBadge=(id:string)=>{if(!summary.calculated.has(id))return;const next={...draft,equippedBadgeId:id};setDraft(next);setFocusedBadgeId(null);setBadgesOpen(false);void persist(next,"Emblema equipado na sua conta.")};
  const levels=badges.filter((badge)=>badge.level);

  return <section className="parity-page profile-v2-root">
    <header className="profile-v2-hero"><BiblicalBadgeAvatar avatarId={avatar.id} badgeId={equipped.id} size={96} title={`${avatar.name} · ${equipped.name}`}/><div><p>{session.isAdmin?"ADMINISTRADOR · ":""}SEU AVATAR BÍBLICO</p><h1>{draft.name}</h1><span>{avatar.name} · Nível {equipped.level||1}: {equipped.name}</span></div></header>
    <article className="profile-v2-level"><Trophy size={25}/><div><strong>Progresso das conquistas</strong><span>{summary.completedLessons} aulas IBR concluídas · {summary.completedCourses} cursos concluídos</span>{summary.next?<><small>Próximo: {summary.next.name} — {summary.next.requirement}</small><div className="profile-v2-progress"><i style={{width:`${Math.round(summary.fraction*100)}%`}}/></div><b>{Math.round(summary.fraction*100)}%</b></>:<small>Todos os níveis principais foram alcançados.</small>}</div></article>
    <div className="profile-v2-stats"><div><strong>{summary.calculated.size}</strong><span>Conquistas</span></div><div><strong>{summary.activeMinutes}</strong><span>Minutos ativos</span></div><div><strong>{summary.completedCourses}</strong><span>Cursos IBR</span></div><div><strong>{summary.counts.bible_chapters}</strong><span>Capítulos</span></div></div>

    <div className="profile-fields"><label>Nome completo<input value={draft.name} onChange={(e)=>setDraft({...draft,name:e.target.value})}/></label><label>Telefone<input inputMode="tel" value={draft.phone} onChange={(e)=>setDraft({...draft,phone:e.target.value.replace(/\D/g,"").slice(0,15)})}/></label><label>Endereço<input value={draft.address} onChange={(e)=>setDraft({...draft,address:e.target.value})}/></label><label>Data de nascimento<input inputMode="numeric" placeholder="dd/mm/aaaa" value={draft.birthDate} onChange={(e)=>setDraft({...draft,birthDate:e.target.value.replace(/[^0-9/]/g,"").slice(0,10)})}/></label><label>E-mail para certificado IBR<input type="email" value={draft.email} onChange={(e)=>setDraft({...draft,email:e.target.value})}/></label></div>
    <button className="parity-primary" disabled={saving} onClick={()=>void save()}><Save size={18}/>{saving?"Sincronizando…":"Salvar dados pessoais"}</button>

    <div className="profile-choice-actions"><button onClick={()=>setAvatarsOpen(!avatarsOpen)}><UserRound size={18}/> Escolher avatar</button><button onClick={()=>setBadgesOpen(!badgesOpen)}><BadgeCheck size={18}/> Emblemas e níveis</button><button onClick={()=>setMissionsOpen(!missionsOpen)}><Trophy size={18}/> Missões</button></div>
    {avatarsOpen&&<div className="profile-v2-avatar-grid">{avatars.map((item)=><button disabled={saving} key={item.id} className={draft.avatarId===item.id?"selected":""} onClick={()=>chooseAvatar(item.id)}><img src={avatarUrl(item.id)} alt={item.name} loading="lazy"/><small>{item.name}</small></button>)}</div>}
    {badgesOpen&&<div className="profile-v2-badges">{badges.map((badge)=>{const unlocked=summary.calculated.has(badge.id);const selected=draft.equippedBadgeId===badge.id;const focused=focusedBadgeId===badge.id;return <button id={`profile-badge-${badge.id}`} key={badge.id} disabled={!unlocked||saving} className={`${selected?"selected":""}${focused?" celebration-focus":""}`} onClick={()=>chooseBadge(badge.id)}><BiblicalBadgeAvatar avatarId={avatar.id} badgeId={badge.id} size={64} locked={!unlocked} title={badge.name}/><span><strong>{badge.level?`Nível ${badge.level} · `:""}{badge.name}</strong><small>{unlocked?badge.description:`Bloqueado — ${badge.requirement}`}</small><em>{selected?"Emblema equipado":focused&&unlocked?"Novo emblema desbloqueado · toque para usar":unlocked?"Conquistado · toque para equipar":"Ainda não conquistado"}</em></span></button>})}</div>}
    {missionsOpen&&<section className="profile-v2-missions"><header><strong>Missões dos emblemas</strong><small>As mesmas regras do Android. O progresso feito em qualquer versão conta para a mesma conta.</small></header>{levels.map((badge)=>{const fraction=Math.max(0,Math.min(1,summary.missionFraction(badge.id)));const done=summary.calculated.has(badge.id);return <article key={badge.id} className={done?"done":""}><BiblicalBadgeAvatar avatarId={avatar.id} badgeId={badge.id} size={72} locked={!done} title={`Nível ${badge.level} · ${badge.name}`}/><div><strong>Nível {badge.level} · {badge.name}</strong><small>{badge.requirement}</small><div className="profile-v2-progress"><i style={{width:`${Math.round(fraction*100)}%`}}/></div><em>{done?"Concluída":`${Math.round(fraction*100)}% concluído`}</em></div></article>})}</section>}
    <article className="profile-v2-activity"><BookOpen size={21}/><div><strong>Atividade sincronizada</strong><small>{summary.counts.devotionals} devocionais · {summary.counts.plan_themes} temas · {summary.counts.plans} planos · {summary.counts.books} livros · {summary.counts.videos} vídeos · {summary.counts.audios} áudios · {summary.counts.bible_news} notícias · {summary.counts.bible_chapters} capítulos</small></div></article>
    <button className="parity-danger" onClick={()=>void logout()}><LogOut size={18}/> Sair da conta</button><button className="profile-v2-refresh" onClick={()=>void reload()}><RefreshCcw size={17}/> Atualizar progresso</button>
  </section>;
}
