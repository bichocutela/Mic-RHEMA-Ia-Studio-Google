import { importPKCS8, SignJWT } from "npm:jose@5.10.0";
import { studies } from "./studies.ts";

const KEY_HASH = "ba749b2eba2b9a00951b19d4efcf09970cbff65f5c93816ff8d5bb393dfe3a23";
const BASE = "https://firestore.googleapis.com/v1/projects/mic-rhema/databases/(default)/documents";
const FIREBASE_API_KEY = "AIzaSyD-GPqTLRFmOiNATJwzKUHGqJeTPQcf0E8";
type Value = { stringValue?: string; booleanValue?: boolean; integerValue?: string };
type Doc = { name: string; fields?: Record<string, Value>; updateTime?: string };
const headers = { "content-type": "application/json", "cache-control": "no-store", "access-control-allow-origin": "*", "access-control-allow-headers": "authorization,content-type,x-scheduler-key" };
const json = (data: unknown, status = 200) => new Response(JSON.stringify(data), { status, headers });
const str = (d: Doc, k: string) => d.fields?.[k]?.stringValue || "";
const num = (d: Doc, k: string) => Number(d.fields?.[k]?.integerValue || 0);
function encode(values: Record<string, string | number | boolean>): Record<string, Value> {
  return Object.fromEntries(Object.entries(values).map(([k,v]) => [k,typeof v === "boolean" ? {booleanValue:v} : typeof v === "number" ? {integerValue:String(v)} : {stringValue:v}]));
}
function driveId(url: string) { return url.match(/\/d\/([^/?]+)/)?.[1] || url.match(/[?&]id=([^&]+)/)?.[1] || ""; }
async function googleToken(scope = "https://www.googleapis.com/auth/datastore https://www.googleapis.com/auth/firebase.messaging") {
  const a = JSON.parse(Deno.env.get("FIREBASE_SERVICE_ACCOUNT_JSON") || "{}");
  if (a.project_id !== "mic-rhema" || !a.client_email || !a.private_key) throw new Error("Credencial do MICRHEMA indisponível");
  const now = Math.floor(Date.now()/1000);
  const jwt = await new SignJWT({iss:a.client_email,scope,aud:"https://oauth2.googleapis.com/token"}).setProtectedHeader({alg:"RS256",typ:"JWT"}).setIssuedAt(now).setExpirationTime(now+3600).sign(await importPKCS8(a.private_key.replace(/\\n/g,"\n"),"RS256"));
  const r = await fetch("https://oauth2.googleapis.com/token", {method:"POST",body:new URLSearchParams({grant_type:"urn:ietf:params:oauth:grant-type:jwt-bearer",assertion:jwt})});
  const b = await r.json(); if (!r.ok || !b.access_token) throw new Error("Autenticação do publicador indisponível"); return b.access_token as string;
}
async function scheduler(request: Request) {
  const bytes = new TextEncoder().encode(request.headers.get("x-scheduler-key") || "");
  const hash = Array.from(new Uint8Array(await crypto.subtle.digest("SHA-256",bytes))).map(v=>v.toString(16).padStart(2,"0")).join("");
  return hash === KEY_HASH;
}
async function admin(request: Request) {
  const idToken = request.headers.get("authorization")?.replace(/^Bearer\s+/i,""); if (!idToken) return false;
  const r = await fetch(`https://identitytoolkit.googleapis.com/v1/accounts:lookup?key=${FIREBASE_API_KEY}`, {method:"POST",headers:{"content-type":"application/json"},body:JSON.stringify({idToken})});
  const b = await r.json(); const u = b.users?.[0]; if (!r.ok || !u?.localId) return false;
  const claims = JSON.parse(u.customAttributes || "{}"); return claims.isAdmin === true || u.email === "admin@micrhema.app";
}
async function get(token: string, collection: string, id: string): Promise<Doc | null> {
  const r = await fetch(`${BASE}/${collection}/${encodeURIComponent(id)}`, {headers:{authorization:`Bearer ${token}`}});
  if (r.status === 404) return null; if (!r.ok) throw new Error(`Leitura falhou: ${r.status}`); return r.json();
}
async function list(token: string, collection: string) {
  const result: Doc[] = []; let page = "";
  do { const u = new URL(`${BASE}/${collection}`); u.searchParams.set("pageSize","100"); if (page) u.searchParams.set("pageToken",page);
    const r = await fetch(u,{headers:{authorization:`Bearer ${token}`}}); if (!r.ok) throw new Error(`Consulta falhou: ${r.status}`);
    const b = await r.json(); result.push(...(b.documents||[])); page=b.nextPageToken||"";
  } while(page); return result;
}
async function query(token: string, collection: string, where: unknown): Promise<Doc[]> {
  const r = await fetch(`${BASE}:runQuery`, {method:"POST",headers:{authorization:`Bearer ${token}`,"content-type":"application/json"},body:JSON.stringify({structuredQuery:{from:[{collectionId:collection}],where,limit:100}})});
  if (!r.ok) throw new Error(`Fila indisponível: ${r.status}`); const b = await r.json(); return b.filter((v:{document?:Doc})=>v.document).map((v:{document:Doc})=>v.document);
}
async function patch(token: string, d: Doc, values: Record<string, string | number | boolean>) {
  const u = new URL(`https://firestore.googleapis.com/v1/${d.name}`); for (const k of Object.keys(values)) u.searchParams.append("updateMask.fieldPaths",k);
  if (d.updateTime) u.searchParams.set("currentDocument.updateTime",d.updateTime);
  const r = await fetch(u,{method:"PATCH",headers:{authorization:`Bearer ${token}`,"content-type":"application/json"},body:JSON.stringify({fields:encode(values)})});
  if (r.status===409||r.status===412) return null; if (!r.ok) throw new Error(`Atualização falhou: ${r.status}`); return r.json() as Promise<Doc>;
}
function pushData(d: Doc) {
  const id=d.name.split("/").pop()!; const title=str(d,"title");
  const body="Um novo estudo está disponível no Discipulado. Abra o aplicativo para estudar.";
  return {topic:"all_users",title,body,data:{title,body,collection:"discipulado_pdfs",documentId:id,category:"content_updates",destination:"discipulado"}};
}
async function notify(token: string, d: Doc) {
  if (d.fields?.isPublished?.booleanValue!==true || !d.fields?.releaseNotificationPending?.booleanValue) return false;
  if (str(d,"releaseNotificationState")==="sending" && Date.parse(str(d,"releaseNotificationLeaseUntil"))>Date.now()) return false;
  const lock=await patch(token,d,{releaseNotificationState:"sending",releaseNotificationLeaseUntil:new Date(Date.now()+180000).toISOString()}); if(!lock)return false;
  try {
    // Do not announce hidden/deleted studies. The canonical PUBLIC collection
    // is the only source of truth. This also respects a concurrent "Ocultar".
    const current=await get(token,"discipulado_pdfs",d.name.split("/").pop()!);
    if (!current || current.fields?.isPublished?.booleanValue!==true ||
        current.fields?.releaseNotificationPending?.booleanValue!==true) return false;
    const r=await fetch(`${Deno.env.get("SUPABASE_URL")}/functions/v1/notify-fcm`,{method:"POST",headers:{"content-type":"application/json",apikey:Deno.env.get("SUPABASE_ANON_KEY")||""},body:JSON.stringify(pushData(d)),signal:AbortSignal.timeout(30000)});
    const b=await r.json(); if(!r.ok||b.ok!==true)throw new Error(`Aviso não aceito: ${r.status}`);
    if(!await patch(token,lock,{releaseNotificationPending:false,releaseNotificationState:"sent",releaseNotificationSentAt:new Date().toISOString(),releaseNotificationLeaseUntil:""}))throw new Error("Confirmação de envio não registrada");
    return true;
  } catch(e) { await patch(token,lock,{releaseNotificationState:"pending",releaseNotificationLeaseUntil:""}); throw e; }
}
async function release(token: string) {
  const now=Date.now();
  const due=await query(token,"discipulado_schedules",{compositeFilter:{op:"AND",filters:[{fieldFilter:{field:{fieldPath:"scheduledPublishAt"},op:"GREATER_THAN",value:{integerValue:"0"}}},{fieldFilter:{field:{fieldPath:"scheduledPublishAt"},op:"LESS_THAN_OR_EQUAL",value:{integerValue:String(now)}}}]}});
  const published:string[]=[]; const failed:string[]=[];
  for(const d of due){
    const id=d.name.split("/").pop()!; const existing=await get(token,"discipulado_pdfs",id);
    const name=`projects/mic-rhema/databases/(default)/documents/discipulado_pdfs/${id}`;
    const fields={...d.fields,...encode({isPublished:true,scheduledPublishAt:0,releaseNotificationPending:true,publishedAt:now,releaseNotificationState:"pending"})};
    const r=await fetch(`${BASE}:commit`,{method:"POST",headers:{authorization:`Bearer ${token}`,"content-type":"application/json"},body:JSON.stringify({writes:[{update:{name,fields},currentDocument:existing?{updateTime:existing.updateTime}:{exists:false}},{delete:d.name,currentDocument:{updateTime:d.updateTime}}]})});
    if(r.status===409||r.status===412)continue; if(!r.ok){failed.push(id);continue;}published.push(str(d,"title"));
  }
  const pending=await query(token,"discipulado_pdfs",{fieldFilter:{field:{fieldPath:"releaseNotificationPending"},op:"EQUAL",value:{booleanValue:true}}});
  const notified:string[]=[];
  for(const d of pending){try{if(await notify(token,d))notified.push(str(d,"title"));}catch(e){console.error("notification pending",d.name,e instanceof Error?e.message:"unknown");failed.push(d.name.split("/").pop()!);}}
  return {ok:failed.length===0,published,notified,failed,checkedAt:new Date(now).toISOString()};
}
async function importStudies(token:string){
  const existing=await list(token,"discipulado_pdfs"); const schedules=await list(token,"discipulado_schedules"); const imported:string[]=[];
  for(const s of studies){if([...existing,...schedules].some(d=>driveId(str(d,"fileUrl"))===s.driveId))continue;
    const id=`drive-${s.driveId}`; const fields=encode({id,title:s.title,subtitle:s.subtitle,description:s.description,category:s.category,fileUrl:s.fileUrl,fileType:"word",storagePath:"",coverUrl:"",pageCount:0,order:s.order,isPublished:false,scheduledPublishAt:Date.parse(s.releaseAt),createdAt:Date.now(),sourceDriveId:s.driveId,releaseNotificationPending:false});
    const u=new URL(`${BASE}/discipulado_schedules/${id}`);u.searchParams.set("currentDocument.exists","false");
    const r=await fetch(u,{method:"PATCH",headers:{authorization:`Bearer ${token}`,"content-type":"application/json"},body:JSON.stringify({fields})});
    if(r.status===409||r.status===412)continue;if(!r.ok)throw new Error(`Cadastro falhou: ${r.status}`);imported.push(s.title);
  }return{ok:true,imported};
}
Deno.serve(async(request)=>{
  if(request.method==="OPTIONS")return new Response("ok",{headers});
  if(request.method!=="POST")return json({error:"Método não permitido"},405);
  try{
    const isScheduler=await scheduler(request);const input=await request.json().catch(()=>({}));
    if(!isScheduler&&(input.action!=="release"||!await admin(request)))return json({error:"Não autorizado"},401);
    const token=await googleToken();
    if(input.action==="release")return json(await release(token));
    if(input.action==="import")return json(await importStudies(token));
    if(input.action==="inspect"||input.action==="preview"){
      const [published,scheduled]=await Promise.all([list(token,"discipulado_pdfs"),list(token,"discipulado_schedules")]);
      const describe=(d:Doc)=>({id:d.name.split("/").pop(),title:str(d,"title"),subtitle:str(d,"subtitle"),description:str(d,"description"),category:str(d,"category"),scheduledPublishAt:num(d,"scheduledPublishAt"),isPublished:d.fields?.isPublished?.booleanValue,pendingNotification:d.fields?.releaseNotificationPending?.booleanValue});
      return json({ok:true,published:published.map(describe),scheduled:scheduled.map(describe).sort((a,b)=>a.scheduledPublishAt-b.scheduledPublishAt)});
    }
    if(input.action==="validateNotification"){
      const p=pushData({name:"test",fields:encode({title:studies[0].title})});const r=await fetch("https://fcm.googleapis.com/v1/projects/mic-rhema/messages:send",{method:"POST",headers:{authorization:`Bearer ${token}`,"content-type":"application/json"},body:JSON.stringify({validate_only:true,message:{topic:p.topic,data:p.data,android:{priority:"high",ttl:"86400s"}}})});return json({ok:r.ok,status:r.status,result:await r.json()});
    }
    return json({error:"Ação inválida"},400);
  }catch(e){console.error("discipulado-release",e instanceof Error?e.message:"unknown");return json({error:e instanceof Error?e.message:"Falha no publicador"},500);}
});
