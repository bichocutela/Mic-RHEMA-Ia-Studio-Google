/** Shared web notification icons and destinations; no credentials or platform imports. */
export function notificationPresentation(data: Record<string,string>, root = "https://bichocutela.github.io/Mic-RHEMA-Ia-Studio-Google/") {
  const field = (...keys: string[]) => keys.map(key => data[key]?.trim()).find(Boolean) || "";
  const category = field("category").toLowerCase(), collection = field("collection").toLowerCase();
  const destination = field("destination","route");
  const route = destination.includes("://") ? "" : destination;
  const [base, search] = route.split("?");
  const routeQuery = new URLSearchParams(search || "");
  const pieces = base.split("/");
  const decode = (value: string = "") => {try{return decodeURIComponent(value)}catch{return value}};
  const id = field("documentId","document_id","id") || routeQuery.get("id") || (pieces.length > 1 ? decode(pieces[1]) : "");
  const course = field("courseId","course_id") || (/^ibr_(course|lesson)$/.test(pieces[0]) ? decode(pieces[1]) : collection === "ibr_courses" ? id : "");
  const chapter = field("chapterId","chapter_id","lessonId","lesson_id") || (pieces[0] === "ibr_lesson" ? decode(pieces[2]) : "");
  const params: Record<string,string> = {};
  let icon = "general", view = "home";
  const detail = () => {if(id) params.id = id};
  if(category === "app_update" || base === "about") {icon="update";view="about"}
  else if(collection.startsWith("prayer_") || /^(prayer|prayer_response|oracao|oração)$/.test(category) || base.startsWith("prayer") || base.startsWith("admin_prayer")) {
    icon="prayer";
    const admin = collection === "prayer_requests" || base.startsWith("admin_prayer");
    view = admin ? "admin" : "prayer";
    if(admin) params.section="prayers";
    const request = id || routeQuery.get("request"); if(request) params.request=request;
  }
  else if(collection === "discipulado_pdfs" || /^(discipulado|study|estudo)$/.test(category) || base.startsWith("discipulado")) {icon="discipulado";view="discipulado";detail()}
  else if(collection === "bible_plans" || /^(plan|plans|theme|tema|plano)$/.test(category) || base === "plans") {
    icon="plan";view="plans";
    const theme=field("theme","themeName","theme_name")||routeQuery.get("theme");
    const plan=field("planId","plan_id")||routeQuery.get("planId")||(collection === "bible_plans" ? id : "");
    if(theme)params.theme=theme;if(plan)params.planId=plan;
  }
  else if(collection === "devocionais" || /devotional|devocional/.test(category) || base === "devotionals") {icon="devotional";view="devotionals";detail()}
  else if(collection === "bible_news" || /news|noticia|notícia/.test(category) || base.startsWith("news")) {icon="news";view="news";detail()}
  else if(collection === "ibr_courses" || /ibr|course|curso|lesson|aula/.test(category) || base.startsWith("ibr")) {
    icon = chapter || /^(ibr|ibr_content|lesson|aula)$/.test(category) ? "ibr" : "course";view="ibr";
    if(course)params.courseId=course;if(chapter)params.chapterId=chapter;
  }
  else if(collection.startsWith("conteudos_") || /sermon|pregacao|pregação|media|midia|mídia|audio|video|book|album|photo/.test(category) || base === "content") {
    view="media";
    const type=routeQuery.get("type") || (collection.endsWith("audios") || category === "audio" ? "audio" : collection.endsWith("books") || category === "book" ? "book" : collection.endsWith("albums") || /album|photo/.test(category) ? "album" : collection.endsWith("videos") || /video|sermon|pregacao|pregação/.test(category) ? "video" : "");
    icon=/sermon|pregacao|pregação/.test(category)?"sermon":type || "media";
    if(type)params.type=type;detail();
  }
  else if(collection === "events" || /event|evento/.test(category)) {icon="event";view="cultos";detail()}
  else if(collection === "cultos_agenda" || /service|culto/.test(category) || base === "services") {icon="service";view="cultos";detail()}
  else if(collection === "acessos_pendentes" || /^(member|members)$/.test(category)) {icon="member";view=collection === "acessos_pendentes"?"admin":"members";if(view==="admin")params.section="members"}
  else if(category === "bible" || base === "bible") {icon="book";view="bible"}
  else if(category === "live") {icon="video"}
  else if(["home","profile","settings","donations","members","team","admin"].includes(base)) view=base;
  const url=new URL(root);url.search="";url.hash="";
  url.searchParams.set("view",view);
  Object.entries(params).forEach(([key,value])=>url.searchParams.set(key,value));
  return {view,params,icon:new URL(`icons/notifications/${icon}.png`,root).href,badge:new URL(`icons/notifications/${icon}-badge.png`,root).href,link:url.href};
}
