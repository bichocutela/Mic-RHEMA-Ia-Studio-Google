import { useEffect, useMemo, useRef, useState } from "react";
import { BadgeCheck, BookOpen, ChevronLeft, ChevronRight, Download, ExternalLink, FileText, Headphones, School, Video } from "lucide-react";
import { toast } from "sonner";
import { listenToCollection, listenToIbrProgress, saveIbrProgress } from "@/lib/firebase";
import { safeFilename, youtubeVideoId } from "@/lib/parity-utils";
import type { PwaSessionLike } from "./AndroidParityViews";
import "./AndroidParityViews.css";

type Chapter = { id:string; title?:string; description?:string; durationMinutes?:number; type?:string; videoUrl?:string; audioUrl?:string; textContent?:string; isYoutube?:boolean; youtube?:boolean; youtubeId?:string; studyPdfUrl?:string };
type Course = { id:string; title?:string; theme?:string; description?:string; imageUrl?:string; chapters?:Chapter[] };
type Progress = { id:string; courseId:string; chapterId:string; lastPositionSeconds?:number; totalDurationSeconds?:number; isCompleted?:boolean };

function googleDriveDownload(url: string) {
  try {
    const parsed = new URL(url);
    if (!parsed.hostname.includes("drive.google.com")) return url;
    const id = parsed.pathname.match(/\/file\/d\/([^/]+)/)?.[1] || parsed.searchParams.get("id");
    return id ? `https://drive.google.com/uc?export=download&id=${encodeURIComponent(id)}` : url;
  } catch { return url; }
}

export function IbrParityView({ session, onLogin }: { session: PwaSessionLike; onLogin: () => void }) {
  const [courses,setCourses]=useState<Course[]>([]); const [progress,setProgress]=useState<Progress[]>([]);
  const [courseId,setCourseId]=useState<string|null>(null); const [lessonKey,setLessonKey]=useState<{courseId:string;chapterId:string}|null>(null);
  useEffect(()=>session?.isIbr?listenToCollection<Course>("ibr_courses",setCourses,()=>setCourses([])):()=>undefined,[session?.isIbr]);
  useEffect(()=>session?.uid&&session.isIbr?listenToIbrProgress<Progress>(session.uid,setProgress,()=>setProgress([])):()=>undefined,[session?.uid,session?.isIbr]);
  if(!session?.isIbr) return <section className="parity-page"><div className="parity-empty"><School size={46}/><h1>Instituto Bíblico Rhema</h1><p>Entre com um cadastro aprovado no IBR para acessar cursos, aulas, materiais e progresso.</p><button className="parity-primary" onClick={onLogin}>{session?"Solicitar acesso ao IBR":"Entrar para acessar"}</button></div></section>;
  const ordered=courses.slice().sort((a,b)=>String(a.id).localeCompare(String(b.id),undefined,{numeric:true}));
  const progressFor=(c:string,l:string)=>progress.find((item)=>item.courseId===c&&item.chapterId===l);
  const selectedCourse=ordered.find((item)=>item.id===courseId)||null;
  const lessonCourse=lessonKey?ordered.find((item)=>item.id===lessonKey.courseId)||null:null;
  const lesson=lessonCourse?.chapters?.find((item)=>item.id===lessonKey?.chapterId)||null;
  const openLesson=async(course:Course,chapter:Chapter)=>{if(!progressFor(course.id,chapter.id)){await saveIbrProgress(session.uid,{courseId:course.id,chapterId:chapter.id,lastPositionSeconds:1,totalDurationSeconds:Number(chapter.durationMinutes||0)*60,isCompleted:false}).catch(()=>undefined);}setLessonKey({courseId:course.id,chapterId:chapter.id});};
  const complete=async()=>{if(!lessonCourse||!lesson)return;await saveIbrProgress(session.uid,{courseId:lessonCourse.id,chapterId:lesson.id,lastPositionSeconds:Number(lesson.durationMinutes||0)*60,totalDurationSeconds:Number(lesson.durationMinutes||0)*60,isCompleted:true});toast.success("Aula concluída e progresso sincronizado.");};
  if(lessonCourse&&lesson) return <IbrLesson course={lessonCourse} lesson={lesson} done={Boolean(progressFor(lessonCourse.id,lesson.id)?.isCompleted)} onBack={()=>setLessonKey(null)} onComplete={()=>void complete()}/>;
  if(selectedCourse) return <section className="parity-page"><button className="back-link" onClick={()=>setCourseId(null)}><ChevronLeft size={18}/> Voltar ao IBR</button><div className="parity-title"><div><p>{selectedCourse.theme||"MÓDULO IBR"}</p><h1>{selectedCourse.title||"Curso IBR"}</h1><span>{selectedCourse.description||"Formação bíblica"}</span></div><School size={30}/></div><div className="android-list-cards">{(selectedCourse.chapters||[]).map((chapter,index)=>{const done=Boolean(progressFor(selectedCourse.id,chapter.id)?.isCompleted);return <button key={chapter.id} onClick={()=>void openLesson(selectedCourse,chapter)}><span>{String(index+1).padStart(2,"0")}</span><div><strong>{chapter.title||"Aula IBR"}</strong><small>{chapter.description||`${Number(chapter.durationMinutes||0)} min`}</small></div>{done?<BadgeCheck size={20}/>:<ChevronRight size={19}/>}</button>})}</div></section>;
  const completeCourse=(course:Course)=>Boolean(course.chapters?.length)&&course.chapters!.every((chapter)=>progressFor(course.id,chapter.id)?.isCompleted);
  const completed=ordered.filter(completeCourse).length; const totalLessons=ordered.reduce((n,c)=>n+(c.chapters?.length||0),0); const completedLessons=ordered.reduce((n,c)=>n+(c.chapters||[]).filter(ch=>progressFor(c.id,ch.id)?.isCompleted).length,0); const percent=totalLessons?Math.round(completedLessons/totalLessons*100):0;
  return <section className="parity-page"><div className="parity-title"><div><p>INSTITUTO BÍBLICO RHEMA</p><h1>Seus cursos</h1><span>{completed} curso(s) concluído(s) · {percent}% das aulas</span></div><School size={30}/></div><div className="profile-v2-progress"><i style={{width:`${percent}%`}}/></div><div className="android-list-cards" style={{marginTop:16}}>{ordered.map(course=><button key={course.id} onClick={()=>setCourseId(course.id)}><span><School size={18}/></span><div><strong>{course.title||"Curso IBR"}</strong><small>{course.description||`${course.chapters?.length||0} aula(s)`}</small></div>{completeCourse(course)?<BadgeCheck size={20}/>:<ChevronRight size={19}/>}</button>)}</div></section>;
}

function IbrLesson({course,lesson,done,onBack,onComplete}:{course:Course;lesson:Chapter;done:boolean;onBack:()=>void;onComplete:()=>void}){
  const audioRef=useRef<HTMLAudioElement|null>(null); const type=(lesson.type||(lesson.textContent?"TEXT":lesson.videoUrl?"VIDEO":"AUDIO")).toUpperCase(); const videoId=lesson.youtubeId||youtubeVideoId(lesson.videoUrl); const pdf=String(lesson.studyPdfUrl||"").trim();
  const downloadPdf=()=>{if(!pdf)return;const a=document.createElement("a");a.href=googleDriveDownload(pdf);a.target="_blank";a.rel="noopener noreferrer";a.download=`${safeFilename(lesson.title||"material-ibr")}.pdf`;document.body.appendChild(a);a.click();a.remove();};
  return <section className="parity-page ibr-lesson-view"><button className="back-link" onClick={onBack}><ChevronLeft size={18}/> Voltar às aulas</button><div className="parity-title"><div><p>{course.title||"CURSO IBR"}</p><h1>{lesson.title||"Aula IBR"}</h1><span>{lesson.description||"Acompanhe a aula e marque como concluída."}</span></div></div>
    {type==="TEXT"?<article className="parity-reader">{String(lesson.textContent||"Nenhum conteúdo adicionado.").split(/\n{2,}/).map((p,i)=><p key={i}>{p}</p>)}</article>:type==="AUDIO"?<div className="audio-reader"><Headphones size={42}/><audio ref={audioRef} controls src={lesson.audioUrl}>Seu navegador não suporta áudio.</audio></div>:videoId?<iframe className="parity-video" title={lesson.title||"Vídeo IBR"} src={`https://www.youtube-nocookie.com/embed/${videoId}?rel=0&playsinline=1`} allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowFullScreen/>:<video className="parity-video" controls playsInline src={lesson.videoUrl}>Seu navegador não suporta vídeo.</video>}
    <div className="lesson-meta" style={{display:"flex",gap:10,margin:"14px 0"}}><span>{type==="VIDEO"?<Video size={15}/>:type==="AUDIO"?<Headphones size={15}/>:<BookOpen size={15}/>} {type==="VIDEO"?"Vídeo-aula":type==="AUDIO"?"Áudio-aula":"Leitura"}</span><span>{Number(lesson.durationMinutes||0)} min</span></div>
    <button className="parity-primary" disabled={done} onClick={onComplete}><BadgeCheck size={18}/>{done?"Aula concluída":"Marcar como concluída"}</button>
    {pdf&&<section style={{marginTop:22}}><p style={{fontWeight:800,marginBottom:10}}>Conteúdos para estudo</p><article className="android-module-card" style={{display:"flex",alignItems:"center",gap:12,padding:16}}><FileText size={28}/><div style={{flex:1}}><strong style={{display:"block"}}>Material complementar em PDF</strong><small>Baixe para estudar junto com esta aula.</small></div><button className="back-link" onClick={downloadPdf}><Download size={18}/> Baixar PDF</button></article></section>}
    {type==="VIDEO"&&lesson.videoUrl&&<a className="back-link" style={{marginTop:14}} href={lesson.videoUrl} target="_blank" rel="noreferrer"><ExternalLink size={17}/> Abrir no YouTube</a>}
  </section>;
}
