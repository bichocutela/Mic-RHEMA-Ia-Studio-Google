import { useEffect, useMemo, useState } from "react";
import { signOut } from "firebase/auth";
import { BadgeCheck, BookOpen, LogOut, RefreshCcw, Save, Trophy, UserRound, X } from "lucide-react";
import { toast } from "sonner";
import {
  firebaseAuth, listenToCollection, listenToIbrProgress, loadPwaMemberProfile, savePwaMemberProfile,
  type PwaMemberProfile,
} from "@/lib/firebase";
import type { PwaSessionLike } from "./AndroidParityViews";
import { BiblicalBadgeAvatar } from "./BiblicalBadgeAvatar";
import { badgeForId, biblicalBadges, levelBadges, type PwaBiblicalBadge } from "./BiblicalBadgeCatalog";
import "./ProfileParityViewV2.css";

type Avatar = { id: string; name: string };
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

const rawBase = "https://raw.githubusercontent.com/bichocutela/Mic-RHEMA-Ia-Studio-Google/main/app/src/main/res/drawable-nodpi";
const avatarUrl = (id: string) => `${rawBase}/avatar_${id}.png`;
const count = (profile: PwaMemberProfile, key: string) => new Set(profile.badgeActivityIds?.[key] || []).size;
const coreActivityKeys = ["plans","plan_themes","books","videos","bible_chapters","bible_news","devotionals","audios"] as const;
const allMissionKeys = [...coreActivityKeys,"quiz_correct","quiz_correct_no_easy_hint","quiz_correct_no_hint","quiz_hard_correct"] as const;
const ratio=(current:number,target:number)=>target<=0?1:current/target;

function parseXp(profile:PwaMemberProfile){
  return [...new Set(profile.badgeActivityIds?.xp_awards || [])].reduce((total,entry)=>{
    const value=Number(entry.slice(entry.lastIndexOf("=")+1));
    return total+(Number.isFinite(value)&&value>0?value:0);
  },0);
}

function readLevelBaseline(profile:PwaMemberProfile,badgeId:string){
  const entries=profile.badgeActivityIds?.[`__level_mission_baseline__:${badgeId}`] || [];
  const result:Record<string,number>={};
  entries.forEach((entry)=>{const index=entry.indexOf("=");if(index<=0)return;const value=Number(entry.slice(index+1));if(Number.isFinite(value))result[entry.slice(0,index)]=value;});
  return result;
}

function computeProgress(profile: PwaMemberProfile, courses: IbrCourse[], progress: IbrProgress[]) {
  const counts = {
    plans: count(profile,"plans"), plan_themes: count(profile,"plan_themes"), books: count(profile,"books"),
    videos: count(profile,"videos"), bible_chapters: count(profile,"bible_chapters"), bible_news: count(profile,"bible_news"),
    devotionals: count(profile,"devotionals"), audios: count(profile,"audios"),
    quiz_correct: count(profile,"quiz_correct"), quiz_correct_no_easy_hint: count(profile,"quiz_correct_no_easy_hint"),
    quiz_correct_no_hint: count(profile,"quiz_correct_no_hint"), quiz_hard_correct: count(profile,"quiz_hard_correct"),
  };
  const activeMinutes=count(profile,"active_minutes");
  const totalXp=parseXp(profile);
  const completedLessons=progress.filter((item)=>item.isCompleted).length;
  const completedCourses=courses.filter((course)=>(course.chapters?.length||0)>0&&course.chapters!.every((chapter)=>progress.some((item)=>item.courseId===course.id&&item.chapterId===chapter.id&&item.isCompleted))).length;
  const calculated=new Set(profile.unlockedBadgeIds?.length?profile.unlockedBadgeIds:["caminhante"]);
  calculated.add("caminhante");

  const highest=Math.max(1,...levelBadges.filter((badge)=>calculated.has(badge.id)).map((badge)=>badge.level||1));
  const next=levelBadges.find((badge)=>(badge.level||0)>highest);

  const levelCounters=(badgeId:string)=>{
    const baseline=readLevelBaseline(profile,badgeId);
    const baselineExists=Object.keys(baseline).length>0;
    const delta=(key:string,current:number)=>Math.max(0,current-(baselineExists?(baseline[key]??current):current));
    const levelCounts:Record<string,number>={};
    allMissionKeys.forEach((key)=>{levelCounts[key]=delta(key,counts[key as keyof typeof counts]||0)});
    const levelActiveMinutes=delta("active_minutes",activeMinutes);
    const levelCompletedCourses=delta("completed_ibr_courses",completedCourses);
    const totalActivities=coreActivityKeys.reduce((sum,key)=>sum+(levelCounts[key]||0),0);
    const usedCoreAreas=coreActivityKeys.filter((key)=>(levelCounts[key]||0)>=1).length;
    return {levelCounts,levelActiveMinutes,levelCompletedCourses,totalActivities,usedCoreAreas};
  };

  const missionFraction=(id:string)=>{
    if(calculated.has(id))return 1;
    const c=levelCounters(id);const v=(key:string)=>c.levelCounts[key]||0;
    switch(id){
      case "caminhante": return 1;
      case "semeador": return Math.min(ratio(v("devotionals"),3),ratio(v("plan_themes"),1));
      case "discipulo": return Math.min(ratio(v("plans"),1),ratio(v("plan_themes"),3),ratio(v("bible_chapters"),3));
      case "perseverante": return Math.min(ratio(c.levelActiveMinutes,60),ratio(c.totalActivities,10));
      case "estudante_rhema": return Math.min(ratio(v("books"),3),ratio(v("videos"),3),ratio(v("audios"),2));
      case "mestre_da_palavra": return Math.min(ratio(c.levelCompletedCourses,1),ratio(v("bible_news"),3),ratio(v("bible_chapters"),10));
      case "guardiao_da_fe": return Math.min(ratio(c.usedCoreAreas,coreActivityKeys.length),ratio(c.levelActiveMinutes,180));
      case "semente_da_fe": return Math.min(ratio(totalXp,100),ratio(v("bible_chapters"),2),ratio(v("quiz_correct"),2));
      case "caminho_da_promessa": return Math.min(ratio(totalXp,200),ratio(v("bible_chapters"),3),ratio(v("quiz_correct"),3));
      case "escudo_da_fe": return Math.min(ratio(totalXp,350),ratio(v("quiz_correct_no_easy_hint"),5),ratio(c.levelActiveMinutes,10));
      case "aguas_vivas": return Math.min(ratio(totalXp,500),ratio(v("bible_chapters"),5),ratio(v("quiz_correct"),5));
      case "videira_verdadeira": return Math.min(ratio(totalXp,650),ratio(v("devotionals"),2),ratio(v("quiz_correct"),6));
      case "luz_do_mundo": return Math.min(ratio(totalXp,850),ratio(v("quiz_correct_no_easy_hint"),8),ratio(v("bible_chapters"),5));
      case "armadura_de_deus": return Math.min(ratio(totalXp,1050),ratio(v("quiz_correct"),10),ratio(v("quiz_hard_correct"),3),ratio(c.levelActiveMinutes,15));
      case "leao_de_juda": return Math.min(ratio(totalXp,1250),ratio(v("bible_chapters"),10),ratio(v("quiz_correct"),10),ratio(c.levelActiveMinutes,20));
      case "chama_do_espirito": return Math.min(ratio(totalXp,1450),ratio(v("quiz_hard_correct"),5),ratio(v("quiz_correct_no_hint"),10));
      case "coroa_da_vida": return Math.min(ratio(totalXp,1650),ratio(v("quiz_correct_no_easy_hint"),12),ratio(c.levelActiveMinutes,30));
      case "asas_da_promessa": return Math.min(ratio(totalXp,1850),ratio(v("bible_chapters"),15),ratio(v("quiz_hard_correct"),8),ratio(c.levelActiveMinutes,30));
      case "tabernaculo": return Math.min(ratio(totalXp,2050),ratio(c.usedCoreAreas,coreActivityKeys.length),ratio(v("quiz_hard_correct"),10));
      case "arca_da_alianca": return Math.min(ratio(totalXp,2300),ratio(v("bible_chapters"),20),ratio(v("quiz_hard_correct"),12),ratio(c.levelActiveMinutes,45));
      case "nova_jerusalem": return Math.min(ratio(totalXp,2600),ratio(v("quiz_correct"),20),ratio(v("quiz_hard_correct"),15),ratio(c.levelActiveMinutes,60));
      case "gloria_eterna": return Math.min(ratio(totalXp,3000),ratio(v("quiz_correct"),30),ratio(v("quiz_hard_correct"),20),ratio(c.levelActiveMinutes,120));
      default:return 0;
    }
  };
  const fraction=next?missionFraction(next.id):1;
  return { counts, activeMinutes, totalXp, completedLessons, completedCourses, calculated, next, missionFraction, fraction:Math.max(0,Math.min(1,fraction)) };
}

export function ProfileParityViewV2({ session, onNavigateHome }: { session: PwaSessionLike; onNavigateHome: () => void }) {
  const [profile,setProfile]=useState<PwaMemberProfile|null>(null); const [draft,setDraft]=useState<PwaMemberProfile|null>(null);
  const [courses,setCourses]=useState<IbrCourse[]>([]); const [ibrProgress,setIbrProgress]=useState<IbrProgress[]>([]);
  const [loading,setLoading]=useState(true); const [loadError,setLoadError]=useState(""); const [saving,setSaving]=useState(false); const [avatarsOpen,setAvatarsOpen]=useState(false); const [badgesOpen,setBadgesOpen]=useState(false); const [missionsOpen,setMissionsOpen]=useState(true); const [focusedBadgeId,setFocusedBadgeId]=useState<string|null>(null); const [previewBadgeId,setPreviewBadgeId]=useState<string|null>(null);
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
  const equipped=badgeForId(draft.equippedBadgeId);
  const persist=async(next:PwaMemberProfile,success:string)=>{setSaving(true);try{const updated=await savePwaMemberProfile({name:next.name,phone:next.phone,address:next.address,birthDate:next.birthDate,email:next.email,avatarId:next.avatarId,equippedBadgeId:next.equippedBadgeId});setProfile(updated);setDraft(updated);const stored=localStorage.getItem("mic-rhema-pwa-session");if(stored){const parsed=JSON.parse(stored);parsed.name=updated.name;localStorage.setItem("mic-rhema-pwa-session",JSON.stringify(parsed));}toast.success(success);}catch(error){toast.error(error instanceof Error?error.message:"Não foi possível salvar.");setDraft(profile);}finally{setSaving(false)}};
  const save=()=>persist(draft,"Perfil atualizado também para o Android.");
  const chooseAvatar=(id:string)=>{const next={...draft,avatarId:id};setDraft(next);setAvatarsOpen(false);void persist(next,"Avatar sincronizado com o Android.")};
  const chooseBadge=(badge:PwaBiblicalBadge)=>{if(!summary.calculated.has(badge.id)||saving)return;const next={...draft,equippedBadgeId:badge.id};setDraft(next);setFocusedBadgeId(null);setPreviewBadgeId(null);void persist(next,"Emblema equipado na sua conta.")};
  const previewBadge=previewBadgeId?badgeForId(previewBadgeId):null;

  return <section className="parity-page profile-v2-root">
    <header className="profile-v2-hero"><BiblicalBadgeAvatar avatarId={avatar.id} badgeId={equipped.id} size={96} title={`${avatar.name} · ${equipped.name}`}/><div><p>{session.isAdmin?"ADMINISTRADOR · ":""}SEU AVATAR BÍBLICO</p><h1>{draft.name}</h1><span>{avatar.name} · Nível {equipped.level||1}: {equipped.name}</span></div></header>
    <article className="profile-v2-level"><Trophy size={25}/><div><strong>Progresso das conquistas</strong><span>{summary.completedLessons} aulas IBR concluídas · {summary.completedCourses} cursos concluídos · {summary.totalXp} XP</span>{summary.next?<><small>Próximo: {summary.next.name} — {summary.next.requirement}</small><div className="profile-v2-progress"><i style={{width:`${Math.round(summary.fraction*100)}%`}}/></div><b>{Math.round(summary.fraction*100)}%</b></>:<small>Todos os níveis principais foram alcançados.</small>}</div></article>
    <div className="profile-v2-stats"><div><strong>{summary.calculated.size}</strong><span>Conquistas</span></div><div><strong>{summary.totalXp}</strong><span>XP acumulado</span></div><div><strong>{summary.activeMinutes}</strong><span>Minutos ativos</span></div><div><strong>{summary.counts.bible_chapters}</strong><span>Capítulos</span></div></div>

    <div className="profile-fields"><label>Nome completo<input value={draft.name} onChange={(e)=>setDraft({...draft,name:e.target.value})}/></label><label>Telefone<input inputMode="tel" value={draft.phone} onChange={(e)=>setDraft({...draft,phone:e.target.value.replace(/\D/g,"").slice(0,15)})}/></label><label>Endereço<input value={draft.address} onChange={(e)=>setDraft({...draft,address:e.target.value})}/></label><label>Data de nascimento<input inputMode="numeric" placeholder="dd/mm/aaaa" value={draft.birthDate} onChange={(e)=>setDraft({...draft,birthDate:e.target.value.replace(/[^0-9/]/g,"").slice(0,10)})}/></label><label>E-mail para certificado IBR<input type="email" value={draft.email} onChange={(e)=>setDraft({...draft,email:e.target.value})}/></label></div>
    <button className="parity-primary" disabled={saving} onClick={()=>void save()}><Save size={18}/>{saving?"Sincronizando…":"Salvar dados pessoais"}</button>

    <div className="profile-choice-actions"><button onClick={()=>setAvatarsOpen(!avatarsOpen)}><UserRound size={18}/> Escolher avatar</button><button onClick={()=>setBadgesOpen(!badgesOpen)}><BadgeCheck size={18}/> Emblemas e níveis</button><button onClick={()=>setMissionsOpen(!missionsOpen)}><Trophy size={18}/> Missões</button></div>
    {avatarsOpen&&<div className="profile-v2-avatar-grid">{avatars.map((item)=><button disabled={saving} key={item.id} className={draft.avatarId===item.id?"selected":""} onClick={()=>chooseAvatar(item.id)}><img src={avatarUrl(item.id)} alt={item.name} loading="lazy"/><small>{item.name}</small></button>)}</div>}
    {badgesOpen&&<div className="profile-v2-badges">{biblicalBadges.map((badge)=>{const unlocked=summary.calculated.has(badge.id);const selected=draft.equippedBadgeId===badge.id;const focused=focusedBadgeId===badge.id;return <button id={`profile-badge-${badge.id}`} key={badge.id} disabled={saving} className={`${selected?"selected":""}${focused?" celebration-focus":""}`} onClick={()=>setPreviewBadgeId(badge.id)}><BiblicalBadgeAvatar avatarId={avatar.id} badgeId={badge.id} size={64} locked={!unlocked} title={badge.name}/><span><strong>{badge.level?`Nível ${badge.level} · `:""}{badge.name}</strong><small>{unlocked?badge.description:`Bloqueado — ${badge.requirement}`}</small><em>{selected?"Emblema equipado":focused&&unlocked?"Novo emblema desbloqueado · toque para ver":unlocked?"Conquistado · toque para ver":"Toque para visualizar"}</em></span></button>})}</div>}
    {missionsOpen&&<section className="profile-v2-missions"><header><strong>Missões dos emblemas</strong><small>As mesmas regras do Android. Cada novo nível começa suas missões do zero; o XP permanece acumulado.</small></header>{levelBadges.map((badge)=>{const fraction=Math.max(0,Math.min(1,summary.missionFraction(badge.id)));const done=summary.calculated.has(badge.id);return <article key={badge.id} className={done?"done":""} onClick={()=>setPreviewBadgeId(badge.id)} role="button" tabIndex={0} onKeyDown={(event)=>{if(event.key==="Enter"||event.key===" "){event.preventDefault();setPreviewBadgeId(badge.id)}}}><BiblicalBadgeAvatar avatarId={avatar.id} badgeId={badge.id} size={72} locked={!done} title={`Nível ${badge.level} · ${badge.name}`}/><div><strong>Nível {badge.level} · {badge.name}{badge.rarity?` · ${badge.rarity}`:""}</strong><small>{badge.requirement}</small><div className="profile-v2-progress"><i style={{width:`${Math.round(fraction*100)}%`}}/></div><em>{done?"Concluída · toque para ver":`${Math.round(fraction*100)}% concluído · toque para ver`}</em></div></article>})}</section>}
    <article className="profile-v2-activity"><BookOpen size={21}/><div><strong>Atividade sincronizada</strong><small>{summary.counts.devotionals} devocionais · {summary.counts.plan_themes} temas · {summary.counts.plans} planos · {summary.counts.books} livros · {summary.counts.videos} vídeos · {summary.counts.audios} áudios · {summary.counts.bible_news} notícias · {summary.counts.bible_chapters} capítulos</small></div></article>
    <button className="parity-danger" onClick={()=>void logout()}><LogOut size={18}/> Sair da conta</button><button className="profile-v2-refresh" onClick={()=>void reload()}><RefreshCcw size={17}/> Atualizar progresso</button>

    {previewBadge&&<div className="profile-v2-badge-modal" role="presentation" onClick={()=>setPreviewBadgeId(null)}>
      <article role="dialog" aria-modal="true" aria-label={`Emblema ${previewBadge.name}`} onClick={(event)=>event.stopPropagation()}>
        <button className="profile-v2-badge-close" aria-label="Fechar" onClick={()=>setPreviewBadgeId(null)}><X size={22}/></button>
        <h2>{previewBadge.level?`Nível ${previewBadge.level}: ${previewBadge.name}`:previewBadge.name}</h2>
        <div className="profile-v2-badge-preview"><BiblicalBadgeAvatar avatarId={avatar.id} badgeId={previewBadge.id} size={230} locked={!summary.calculated.has(previewBadge.id)} dimWhenLocked={false} title={previewBadge.name}/></div>
        {previewBadge.rarity&&<strong className="profile-v2-rarity">{previewBadge.rarity}</strong>}
        <b>{summary.calculated.has(previewBadge.id)?"Emblema conquistado":"Ainda bloqueado"}</b>
        <p>{previewBadge.description}</p>
        {!summary.calculated.has(previewBadge.id)&&<small>Para desbloquear: {previewBadge.requirement}</small>}
        <div className="profile-v2-badge-actions">
          {summary.calculated.has(previewBadge.id)&&<button className="parity-primary" disabled={saving||draft.equippedBadgeId===previewBadge.id} onClick={()=>chooseBadge(previewBadge)}>{draft.equippedBadgeId===previewBadge.id?"Em uso":"Usar emblema"}</button>}
          <button className="profile-v2-modal-back" onClick={()=>setPreviewBadgeId(null)}>Voltar</button>
        </div>
      </article>
    </div>}
  </section>;
}
