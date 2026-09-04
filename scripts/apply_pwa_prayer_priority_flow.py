from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def read(path):
    return (ROOT / path).read_text(encoding='utf-8')

def write(path, content):
    target = ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(content, encoding='utf-8')

def replace(path, old, new, count=1):
    text = read(path)
    if old not in text:
        raise SystemExit(f'Anchor not found in {path}: {old[:120]!r}')
    text = text.replace(old, new, count)
    write(path, text)

write('pwa/client/src/lib/prayer-device.ts', r'''const DEVICE_ID_KEY = "micrhema:prayer-device-id";
const DEVICE_SECRET_KEY = "micrhema:prayer-device-secret";

export type PrayerDeviceIdentity = { deviceId: string; deviceSecret: string };

function randomId(prefix: string) {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) return `${prefix}_${crypto.randomUUID()}`;
  const bytes = new Uint8Array(24);
  crypto.getRandomValues(bytes);
  return `${prefix}_${Array.from(bytes, (value) => value.toString(16).padStart(2, "0")).join("")}`;
}

export function getPrayerDeviceIdentity(): PrayerDeviceIdentity {
  let deviceId = localStorage.getItem(DEVICE_ID_KEY) || "";
  let deviceSecret = localStorage.getItem(DEVICE_SECRET_KEY) || "";
  if (!deviceId) {
    deviceId = randomId("device");
    localStorage.setItem(DEVICE_ID_KEY, deviceId);
  }
  if (!deviceSecret) {
    deviceSecret = randomId("secret");
    localStorage.setItem(DEVICE_SECRET_KEY, deviceSecret);
  }
  return { deviceId, deviceSecret };
}
''')

write('pwa/client/src/lib/push.ts', r'''/** Web Push da PWA com preferências equivalentes às Configurações do Android. */
import { getMessaging, getToken, isSupported, onMessage, type MessagePayload } from "firebase/messaging";
import { firebaseApp, firebaseAuth } from "./firebase";
import { getPrayerDeviceIdentity } from "./prayer-device";

const VAPID_PUBLIC_KEY = "BHkibd35bzMzP9t3If0K32xxrgMlulTQXvevAe370icbBosqINSM1WDL_TEi3k6Ja7LhHHqn6ec7NiCyArEjSkM";
const supabaseUrl = import.meta.env.VITE_SUPABASE_URL || "https://cwphbkdtorfpgmnlafqb.supabase.co";
const settingsKey = "micrhema:pwa:settings";

export type PwaPushPreferences = {
  enabled: boolean; courses: boolean; devotional: boolean; events: boolean; service: boolean;
  news: boolean; media: boolean; ibr: boolean; sermons: boolean;
};

function isStandalonePwa() {
  return window.matchMedia?.("(display-mode: standalone)").matches || Boolean((navigator as Navigator & { standalone?: boolean }).standalone);
}
function isAppleMobile() { return /iPad|iPhone|iPod/.test(navigator.userAgent); }

export function pwaPushPreferences(): PwaPushPreferences {
  let settings: Record<string, unknown> = {};
  try { settings = JSON.parse(localStorage.getItem(settingsKey) || "{}"); } catch { settings = {}; }
  const bool = (key: string, fallback = true) => typeof settings[key] === "boolean" ? settings[key] as boolean : fallback;
  return {
    enabled: bool("notifications"), courses: bool("notifCourses"), devotional: bool("notifDevotional"),
    events: bool("notifEvents"), service: bool("notifService"), news: bool("notifNews"), media: bool("notifMedia"),
    ibr: bool("notifIbr"), sermons: bool("notifSermons"),
  };
}

async function registerToken(requestPermission: boolean) {
  if (!firebaseApp || !("serviceWorker" in navigator) || !("Notification" in window)) throw new Error("Este navegador não oferece suporte a notificações.");
  if (isAppleMobile() && !isStandalonePwa()) throw new Error("No iPhone, instale o MIC Rhema na Tela de Início antes de ativar os avisos.");
  if (!(await isSupported())) throw new Error("Este navegador ainda não oferece suporte a avisos do MIC Rhema.");
  const permission = Notification.permission === "default" && requestPermission ? await Notification.requestPermission() : Notification.permission;
  if (permission !== "granted") throw new Error("As notificações não foram autorizadas. Você pode ativá-las nas configurações do dispositivo.");
  const registration = await navigator.serviceWorker.ready;
  const token = await getToken(getMessaging(firebaseApp), { vapidKey: VAPID_PUBLIC_KEY, serviceWorkerRegistration: registration });
  if (!token) throw new Error("Não foi possível registrar este dispositivo para receber avisos.");
  const { deviceId } = getPrayerDeviceIdentity();
  const idToken = await firebaseAuth?.currentUser?.getIdToken().catch(() => "") || "";
  const response = await fetch(`${supabaseUrl}/functions/v1/pwa-push-subscribe`, {
    method: "POST",
    headers: { "content-type": "application/json", ...(idToken ? { authorization: `Bearer ${idToken}` } : {}) },
    body: JSON.stringify({ token, deviceId, platform: isAppleMobile() ? "ios-web" : "web", preferences: pwaPushPreferences() }),
  });
  const payload = await response.json().catch(() => ({}));
  if (!response.ok || payload.ok !== true) throw new Error(payload.error || "Não foi possível concluir a inscrição de avisos.");
  return { token, deviceId };
}

export async function subscribeToPwaPush() { return registerToken(true); }
export async function ensurePrayerPushRegistration() { return registerToken(true); }
export async function syncPwaPushPreferences() {
  if (!("Notification" in window) || Notification.permission !== "granted") return false;
  try { await registerToken(false); return true; } catch { return false; }
}

export async function listenToForegroundPush(onPayload: (payload: MessagePayload) => void) {
  if (!firebaseApp || !(await isSupported())) return () => undefined;
  return onMessage(getMessaging(firebaseApp), onPayload);
}

export async function sendPwaPush(input: { title: string; body: string; link?: string; category?: string }) {
  const idToken = await firebaseAuth?.currentUser?.getIdToken();
  if (!idToken) throw new Error("Entre novamente como administrador para enviar o aviso.");
  const response = await fetch(`${supabaseUrl}/functions/v1/pwa-push-send`, {
    method: "POST", headers: { "content-type": "application/json", authorization: `Bearer ${idToken}` }, body: JSON.stringify(input),
  });
  const payload = await response.json().catch(() => ({}));
  if (!response.ok || payload.ok !== true) throw new Error(payload.error || "Não foi possível enviar o aviso da PWA.");
  return payload as { recipients: number; sent: number };
}
''')

replace('pwa/client/src/lib/firebase.ts',
'''import { addDoc, collection, doc, getFirestore, onSnapshot, serverTimestamp, setDoc, updateDoc, type DocumentData } from "firebase/firestore";''',
'''import { addDoc, collection, doc, getFirestore, onSnapshot, serverTimestamp, setDoc, updateDoc, type DocumentData } from "firebase/firestore";\nimport { getPrayerDeviceIdentity } from "./prayer-device";''')

old_prayer = '''/** PARIDADE ANDROID — grava PrayerRequest com os mesmos campos usados pela PrayerScreen do APK. */
export async function submitPrayerRequest(input: { name: string; request: string }) {
  const response = await fetch(`${supabaseUrl}/functions/v1/pwa-prayer-request`, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ name: input.name.trim(), request: input.request.trim() }),
  });
  const payload = await response.json().catch(() => ({}));
  if (!response.ok || !payload.ok) throw new Error(payload.error || "Não foi possível enviar o pedido agora.");
}
'''
new_prayer = '''export type PrayerHistoryItem = {
  id: string; name: string; request: string; date: string; createdAt: number; status: string;
  answeredAt: number; answeredDate: string; responseMessage: string; answeredBy: string;
};

async function prayerApi(body: Record<string, unknown>, admin = false) {
  const idToken = await firebaseAuth?.currentUser?.getIdToken().catch(() => "") || "";
  const functionName = admin ? "pwa-prayer-admin" : "pwa-prayer-request";
  const response = await fetch(`${supabaseUrl}/functions/v1/${functionName}`, {
    method: "POST",
    headers: { "content-type": "application/json", ...(idToken ? { authorization: `Bearer ${idToken}` } : {}) },
    body: JSON.stringify(body),
  });
  const payload = await response.json().catch(() => ({}));
  if (!response.ok || payload.ok !== true) throw new Error(payload.error || "Não foi possível atualizar os pedidos de oração agora.");
  return payload;
}

/** PARIDADE ANDROID — grava o pedido com identidade do membro e também do aparelho PWA. */
export async function submitPrayerRequest(input: { name: string; request: string; notificationToken?: string }) {
  const identity = getPrayerDeviceIdentity();
  return prayerApi({ action: "submit", name: input.name.trim(), request: input.request.trim(), notificationToken: input.notificationToken || "", ...identity });
}

/** Histórico privado: membro autenticado vê sua conta; visitante vê somente este aparelho. */
export async function loadPrayerHistory(): Promise<PrayerHistoryItem[]> {
  const identity = getPrayerDeviceIdentity();
  const payload = await prayerApi({ action: "history", ...identity });
  return Array.isArray(payload.items) ? payload.items as PrayerHistoryItem[] : [];
}

/** Ação pastoral protegida, compartilhada com o mesmo documento que o Android usa. */
export async function markPrayerAsAnswered(requestId: string) {
  return prayerApi({ action: "mark_answered", requestId }, true);
}
'''
replace('pwa/client/src/lib/firebase.ts', old_prayer, new_prayer)

write('pwa/client/src/components/PrayerParityView.tsx', r'''import { useEffect, useMemo, useState } from "react";
import { BellRing, CheckCircle2, Clock3, HandHeart, RefreshCcw, Send } from "lucide-react";
import { toast } from "sonner";
import { loadPrayerHistory, submitPrayerRequest, type PrayerHistoryItem } from "@/lib/firebase";
import { ensurePrayerPushRegistration } from "@/lib/push";
import type { PwaSession } from "@/lib/pwa-auth";
import "./PrayerParityView.css";

export function PrayerParityView({ session }: { session?: PwaSession | null }) {
  const [name, setName] = useState(session?.name || "");
  const [request, setRequest] = useState("");
  const [busy, setBusy] = useState(false);
  const [historyBusy, setHistoryBusy] = useState(true);
  const [history, setHistory] = useState<PrayerHistoryItem[]>([]);

  useEffect(() => { if (session?.name && !name) setName(session.name); }, [session?.name]);
  const refresh = async () => {
    setHistoryBusy(true);
    try { setHistory(await loadPrayerHistory()); }
    catch (error) { toast.error(error instanceof Error ? error.message : "Não foi possível carregar seu histórico."); }
    finally { setHistoryBusy(false); }
  };
  useEffect(() => { void refresh(); }, [session?.uid]);
  useEffect(() => {
    const update = () => void refresh();
    window.addEventListener("micrhema:prayer-updated", update);
    const visible = () => { if (document.visibilityState === "visible") void refresh(); };
    document.addEventListener("visibilitychange", visible);
    return () => { window.removeEventListener("micrhema:prayer-updated", update); document.removeEventListener("visibilitychange", visible); };
  }, [session?.uid]);

  const pending = useMemo(() => history.filter((item) => item.status !== "respondida" && !item.answeredAt).length, [history]);
  const answered = useMemo(() => history.filter((item) => item.status === "respondida" || item.answeredAt > 0).length, [history]);

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!name.trim() || !request.trim()) return toast.error("Preencha todos os campos.");
    setBusy(true);
    let notificationToken = "";
    try {
      try { notificationToken = (await ensurePrayerPushRegistration()).token; }
      catch (notificationError) {
        toast.message("Seu pedido será enviado normalmente.", { description: notificationError instanceof Error ? `${notificationError.message} Ative os avisos para receber a confirmação da oração.` : "Ative os avisos para receber a confirmação da oração." });
      }
      await submitPrayerRequest({ name: name.trim(), request: request.trim(), notificationToken });
      setRequest("");
      toast.success("Pedido enviado à equipe pastoral.", { description: "Quando a oração for realizada, o histórico será atualizado e este aparelho será avisado se as notificações estiverem ativas." });
      await refresh();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Não foi possível enviar o pedido. Tente novamente.");
    } finally { setBusy(false); }
  };

  return <section className="page-pad prayer-parity-page">
    <header className="prayer-parity-header"><div><p>PEDIDOS DE ORAÇÃO</p><h1>Seu pedido está sendo cuidado</h1><span>Um espaço privado entre você e a equipe pastoral.</span></div><HandHeart size={30}/></header>
    <article className="prayer-verse-card"><div className="prayer-emoji" aria-hidden="true">🙏</div><blockquote>“Orai uns pelos outros, para que sejais curados. A oração do justo tem grande poder.”</blockquote><strong>— Tiago 5:16</strong></article>

    <div className="prayer-status-strip"><div><Clock3/><strong>{pending}</strong><span>Aguardando oração</span></div><div><CheckCircle2/><strong>{answered}</strong><span>Orações respondidas</span></div></div>

    <form className="prayer-parity-form" onSubmit={submit}><h2>Enviar novo pedido</h2><label><span>Seu nome</span><input value={name} onChange={(event) => setName(event.target.value)} placeholder="Como podemos te chamar?" required /></label><label><span>Pedido de oração</span><textarea value={request} onChange={(event) => setRequest(event.target.value)} placeholder="Descreva seu pedido de oração..." rows={6} required /></label><button disabled={busy}><Send size={18}/>{busy ? "Enviando…" : "Enviar Pedido"}</button><small><BellRing size={14}/> Se os avisos estiverem ativos, somente este aparelho recebe a confirmação quando a oração for realizada.</small></form>

    <section className="prayer-history-section"><header><div><p>SEU HISTÓRICO</p><h2>Pedidos e respostas</h2></div><button onClick={() => void refresh()} disabled={historyBusy} aria-label="Atualizar histórico"><RefreshCcw className={historyBusy ? "spin" : ""} size={18}/></button></header>
      {historyBusy && !history.length ? <div className="prayer-history-empty">Carregando seus pedidos…</div> : !history.length ? <div className="prayer-history-empty">Seu primeiro pedido aparecerá aqui depois do envio.</div> : <div className="prayer-history-list">{history.map((item) => {
        const done = item.status === "respondida" || item.answeredAt > 0;
        return <article key={item.id} className={done ? "is-answered" : "is-pending"}><div className="prayer-history-icon">{done ? <CheckCircle2/> : <Clock3/>}</div><div className="prayer-history-copy"><div><strong>{done ? "Oração respondida" : "Oração pendente"}</strong><time>{done ? item.answeredDate || item.date : item.date}</time></div><p>{item.request}</p>{done && <aside><b>🙏 A equipe pastoral orou por você.</b><span>{item.responseMessage || `Oração respondida em ${item.answeredDate || item.date}.`}</span></aside>}</div></article>;
      })}</div>}
    </section>
    <aside className="prayer-parity-footer">Seus pedidos são visíveis à equipe pastoral responsável. O seu histórico não é exibido para outros membros.</aside>
  </section>;
}
''')

write('pwa/client/src/components/PrayerParityView.css', r'''.prayer-parity-page{display:grid;gap:18px;padding-bottom:120px}.prayer-parity-header{display:flex;justify-content:space-between;align-items:center;gap:16px}.prayer-parity-header p,.prayer-history-section header p{margin:0 0 4px;font-size:.72rem;font-weight:850;letter-spacing:.12em;color:var(--pwa-primary,#8a6500)}.prayer-parity-header h1{margin:0;font-size:1.55rem;line-height:1.15}.prayer-parity-header span{display:block;margin-top:5px;color:var(--muted-foreground,#6b7280);font-size:.91rem}.prayer-verse-card{border-radius:20px;background:#143454;padding:24px;color:white;text-align:center;box-shadow:0 8px 26px rgba(20,52,84,.16)}.prayer-emoji{font-size:2rem;margin-bottom:12px}.prayer-verse-card blockquote{margin:0;color:#fde68a;font-style:italic;line-height:1.55}.prayer-verse-card strong{display:block;margin-top:12px;color:#fbbf24}.prayer-status-strip{display:grid;grid-template-columns:1fr 1fr;gap:10px}.prayer-status-strip>div{display:grid;grid-template-columns:auto 1fr;column-gap:9px;align-items:center;padding:15px;border-radius:16px;background:color-mix(in srgb,var(--card,#fff) 88%,transparent);box-shadow:0 2px 12px rgba(0,0,0,.05)}.prayer-status-strip svg{grid-row:1/3;color:#a87809}.prayer-status-strip strong{font-size:1.15rem}.prayer-status-strip span{font-size:.76rem;color:var(--muted-foreground,#6b7280)}.prayer-parity-form{display:grid;gap:15px;padding:20px;border-radius:20px;background:color-mix(in srgb,var(--card,#fff) 88%,transparent);box-shadow:0 2px 14px rgba(0,0,0,.06)}.prayer-parity-form h2{margin:0 0 2px;font-size:1.1rem}.prayer-parity-form label{display:grid;gap:7px}.prayer-parity-form label span{font-size:.82rem;font-weight:750;color:var(--muted-foreground,#6b7280)}.prayer-parity-form input,.prayer-parity-form textarea{width:100%;box-sizing:border-box;border:1px solid transparent;border-radius:13px;padding:13px 14px;background:color-mix(in srgb,var(--muted,#f4f1ea) 75%,transparent);color:inherit;font:inherit;outline:none}.prayer-parity-form input:focus,.prayer-parity-form textarea:focus{border-color:#dca628}.prayer-parity-form textarea{resize:vertical;min-height:132px}.prayer-parity-form button{display:flex;justify-content:center;align-items:center;gap:8px;border:0;border-radius:13px;padding:14px 16px;background:#f3c344;color:#111;font-weight:850;font:inherit}.prayer-parity-form button:disabled{opacity:.65}.prayer-parity-form>small{display:flex;align-items:flex-start;gap:7px;line-height:1.4;color:var(--muted-foreground,#6b7280)}.prayer-parity-form>small svg{flex:0 0 auto;margin-top:2px}.prayer-history-section{display:grid;gap:12px}.prayer-history-section>header{display:flex;align-items:center;justify-content:space-between}.prayer-history-section h2{margin:0;font-size:1.2rem}.prayer-history-section header button{display:grid;place-items:center;width:38px;height:38px;border:0;border-radius:12px;background:color-mix(in srgb,var(--muted,#f4f1ea) 80%,transparent);color:inherit}.prayer-history-list{display:grid;gap:11px}.prayer-history-list>article{display:flex;gap:12px;padding:16px;border-radius:18px;background:color-mix(in srgb,var(--card,#fff) 90%,transparent);border:1px solid color-mix(in srgb,var(--border,#ddd) 70%,transparent)}.prayer-history-list>article.is-answered{border-color:color-mix(in srgb,#2f8a57 34%,transparent);background:color-mix(in srgb,#e9f7ef 55%,var(--card,#fff))}.prayer-history-icon{display:grid;place-items:center;flex:0 0 auto;width:38px;height:38px;border-radius:50%;background:color-mix(in srgb,#e8c15d 24%,transparent);color:#8a6500}.is-answered .prayer-history-icon{background:color-mix(in srgb,#2f8a57 18%,transparent);color:#28794c}.prayer-history-copy{min-width:0;flex:1}.prayer-history-copy>div{display:flex;justify-content:space-between;gap:10px;align-items:flex-start}.prayer-history-copy time{font-size:.72rem;color:var(--muted-foreground,#6b7280);white-space:nowrap}.prayer-history-copy p{margin:7px 0 0;line-height:1.48}.prayer-history-copy aside{display:grid;gap:3px;margin-top:12px;padding:11px 12px;border-radius:12px;background:color-mix(in srgb,#2f8a57 11%,transparent)}.prayer-history-copy aside b{color:#28794c;font-size:.83rem}.prayer-history-copy aside span{font-size:.8rem;line-height:1.4;color:var(--muted-foreground,#56615b)}.prayer-history-empty{padding:24px;border-radius:16px;text-align:center;color:var(--muted-foreground,#6b7280);background:color-mix(in srgb,var(--muted,#f4f1ea) 58%,transparent)}.prayer-parity-footer{padding:16px;border-radius:14px;background:color-mix(in srgb,var(--muted,#f4f1ea) 70%,transparent);text-align:center;color:var(--muted-foreground,#6b7280);font-size:.85rem;line-height:1.5}.spin{animation:prayer-spin 1s linear infinite}@keyframes prayer-spin{to{transform:rotate(360deg)}}@media(max-width:560px){.prayer-parity-page{gap:16px}.prayer-verse-card{padding:22px 18px}.prayer-parity-form{padding:18px}.prayer-history-copy>div{display:grid;gap:2px}.prayer-status-strip{gap:8px}.prayer-status-strip>div{padding:13px 11px}}
''')

# PwaShell: entregar sessão para histórico privado.
replace('pwa/client/src/components/PwaShell.tsx', ':active==="prayer"?<PrayerParityView/>', ':active==="prayer"?<PrayerParityView session={session}/>')

# Admin: seção pastoral, prioridade antes das ações rápidas e resposta protegida.
replace('pwa/client/src/components/AdminParityView.tsx',
'  Heart, Image, LayoutDashboard, LockKeyhole, Newspaper, Pencil, Plus, RefreshCcw, Save,',
'  BellRing, CheckCircle2, HandHeart, Heart, Image, LayoutDashboard, LockKeyhole, Newspaper, Pencil, Plus, RefreshCcw, Save,')
replace('pwa/client/src/components/AdminParityView.tsx',
'import { listenToCollection, listenToDocument } from "@/lib/firebase";',
'import { listenToCollection, listenToDocument, markPrayerAsAnswered } from "@/lib/firebase";')
replace('pwa/client/src/components/AdminParityView.tsx',
'"services" | "banners" | "donations" | "members"',
'"services" | "banners" | "donations" | "prayers" | "members"')
replace('pwa/client/src/components/AdminParityView.tsx',
'    { id: "donations", title: "Dízimos e Ofertas", subtitle: "PIX e QR Code", icon: Heart },\n    { id: "team",',
'    { id: "donations", title: "Dízimos e Ofertas", subtitle: "PIX e QR Code", icon: Heart },\n    { id: "prayers", title: "Pedidos de Oração", subtitle: "Fila pastoral e histórico", icon: HandHeart },\n    { id: "team",')
replace('pwa/client/src/components/AdminParityView.tsx',
'  const [section, setSection] = useState<Section>("dashboard");',
'''  const [section, setSection] = useState<Section>(() => new URLSearchParams(window.location.search).get("section") === "prayers" ? "prayers" : "dashboard");
  useEffect(() => { const open = () => setSection("prayers"); window.addEventListener("micrhema:open-admin-prayer", open); return () => window.removeEventListener("micrhema:open-admin-prayer", open); }, []);''')
replace('pwa/client/src/components/AdminParityView.tsx',
'  const books = useAdminCollection("conteudos_books");\n  const pending = members.filter',
'  const books = useAdminCollection("conteudos_books");\n  const prayers = useAdminCollection("prayer_requests");\n  const pendingPrayerCount = prayers.filter((item) => item.status !== "respondida" && Number(item.answeredAt || 0) <= 0).length;\n  const pending = members.filter')
replace('pwa/client/src/components/AdminParityView.tsx',
'    <h2>Ações rápidas</h2><div className="admin-quick-grid">',
'''    {pendingPrayerCount > 0 && <button className="admin-prayer-priority" onClick={() => onOpen("prayers")}><span className="admin-prayer-priority-icon"><BellRing size={24}/></span><span><small>ATENÇÃO PASTORAL</small><strong>Oração Pendente</strong><em>{pendingPrayerCount === 1 ? "1 novo pedido aguarda oração" : `${pendingPrayerCount} pedidos aguardam oração`}</em></span><b>{pendingPrayerCount}</b><ChevronRight size={20}/></button>}
    <h2>Ações rápidas</h2><div className="admin-quick-grid">''')
replace('pwa/client/src/components/AdminParityView.tsx',
'  if (section === "donations") return <DonationsAdmin/>;',
'  if (section === "donations") return <DonationsAdmin/>;\n  if (section === "prayers") return <PrayerAdmin/>;')

admin_component = r'''
function PrayerAdmin() {
  const items = useAdminCollection("prayer_requests");
  const focusedId = new URLSearchParams(window.location.search).get("request") || "";
  const pending = items.filter((item) => item.status !== "respondida" && Number(item.answeredAt || 0) <= 0).slice().sort((a,b) => Number(b.id === focusedId) - Number(a.id === focusedId) || Number(b.createdAt || 0) - Number(a.createdAt || 0));
  const answered = items.filter((item) => item.status === "respondida" || Number(item.answeredAt || 0) > 0).slice().sort((a,b) => Number(b.answeredAt || 0) - Number(a.answeredAt || 0)).slice(0,30);
  const [busyId,setBusyId]=useState("");
  const mark = async (item: AnyDoc) => {
    if (!window.confirm(`Confirmar que a oração por ${item.name || "este pedido"} foi realizada?`)) return;
    setBusyId(item.id);
    try { const result = await markPrayerAsAnswered(item.id); toast.success("Oração marcada como respondida.", { description: result.notified ? `A confirmação foi enviada para ${result.notified} aparelho(s).` : "O histórico da pessoa já foi atualizado." }); }
    catch (error) { toast.error(error instanceof Error ? error.message : "Não foi possível confirmar a oração."); }
    finally { setBusyId(""); }
  };
  return <div className="admin-section admin-prayer-section"><SectionHeader title="Oração Pendente" subtitle="Pedidos que aguardam o cuidado da equipe pastoral."/>
    <article className="admin-prayer-summary"><HandHeart size={30}/><div><strong>{pending.length}</strong><span>{pending.length === 1 ? "pedido aguardando oração" : "pedidos aguardando oração"}</span></div></article>
    {!pending.length ? <div className="admin-prayer-empty"><CheckCircle2 size={34}/><strong>Nenhuma oração pendente</strong><span>Todos os pedidos recebidos já foram cuidados.</span></div> : <div className="admin-prayer-list">{pending.map((item) => <article key={item.id} className={item.id === focusedId ? "is-focused" : ""}>{item.id === focusedId && <small className="admin-prayer-focus"><BellRing size={14}/> ABERTO PELA NOTIFICAÇÃO</small>}<header><div><strong>{item.name || "Pedido sem nome"}</strong><span>{item.date || "Data não informada"}</span></div><em>Pendente</em></header><p>{item.request}</p><button disabled={busyId === item.id} onClick={() => void mark(item)}><CheckCircle2 size={18}/>{busyId === item.id ? "Confirmando…" : "Oração Feita"}</button></article>)}</div>}
    {answered.length > 0 && <><h2 className="admin-prayer-history-title">Histórico de orações respondidas</h2><div className="admin-prayer-answered">{answered.map((item) => <article key={item.id}><CheckCircle2 size={18}/><div><strong>{item.name || "Pedido sem nome"}</strong><p>{item.request}</p><small>Oração respondida · {item.answeredDate || item.date || "data registrada"}</small></div></article>)}</div></>}
  </div>;
}

'''
replace('pwa/client/src/components/AdminParityView.tsx', 'function SectionHeader({ title, subtitle, onAdd, addLabel = "Novo" }', admin_component + 'function SectionHeader({ title, subtitle, onAdd, addLabel = "Novo" }')

with (ROOT / 'pwa/client/src/components/AdminParityView.css').open('a', encoding='utf-8') as handle:
    handle.write(r'''
.admin-prayer-priority{width:100%;display:grid;grid-template-columns:auto 1fr auto auto;gap:12px;align-items:center;text-align:left;border:1px solid color-mix(in srgb,#b42318 28%,transparent);border-radius:19px;padding:16px;margin:5px 0 22px;background:linear-gradient(135deg,color-mix(in srgb,#fff1ee 92%,var(--card,#fff)),color-mix(in srgb,#ffe1d8 55%,var(--card,#fff)));color:inherit;box-shadow:0 5px 18px rgba(180,35,24,.09)}.admin-prayer-priority-icon{display:grid;place-items:center;width:46px;height:46px;border-radius:50%;background:#b42318;color:white}.admin-prayer-priority>span:nth-child(2){display:grid;gap:2px}.admin-prayer-priority small{font-size:.66rem;font-weight:850;letter-spacing:.1em;color:#b42318}.admin-prayer-priority strong{font-size:1.03rem}.admin-prayer-priority em{font-style:normal;font-size:.78rem;color:var(--muted-foreground,#6b7280)}.admin-prayer-priority>b{display:grid;place-items:center;min-width:34px;height:34px;padding:0 8px;border-radius:99px;background:#b42318;color:white}.admin-prayer-section{padding-bottom:110px}.admin-prayer-summary{display:flex;gap:12px;align-items:center;padding:17px;border-radius:18px;background:color-mix(in srgb,#f3c344 20%,var(--card,#fff));margin-bottom:16px}.admin-prayer-summary>div{display:grid}.admin-prayer-summary strong{font-size:1.5rem}.admin-prayer-summary span{font-size:.82rem;color:var(--muted-foreground,#6b7280)}.admin-prayer-list{display:grid;gap:12px}.admin-prayer-list>article{display:grid;gap:12px;padding:17px;border-radius:18px;border:1px solid color-mix(in srgb,var(--border,#ddd) 75%,transparent);background:var(--card,#fff)}.admin-prayer-list>article.is-focused{border:2px solid #b8860b;box-shadow:0 0 0 4px color-mix(in srgb,#f3c344 15%,transparent)}.admin-prayer-focus{display:flex;align-items:center;gap:5px;color:#9a6d00;font-weight:850;letter-spacing:.07em}.admin-prayer-list header{display:flex;justify-content:space-between;align-items:flex-start;gap:12px}.admin-prayer-list header>div{display:grid;gap:2px}.admin-prayer-list header span{font-size:.75rem;color:var(--muted-foreground,#6b7280)}.admin-prayer-list header em{font-style:normal;font-size:.7rem;font-weight:800;padding:5px 9px;border-radius:99px;background:#fff2cc;color:#825d00}.admin-prayer-list p{margin:0;line-height:1.5}.admin-prayer-list>article>button{display:flex;align-items:center;justify-content:center;gap:7px;border:0;border-radius:12px;padding:12px;background:#2f7d4c;color:white;font-weight:800}.admin-prayer-empty{display:grid;justify-items:center;gap:7px;padding:28px 20px;border-radius:18px;background:color-mix(in srgb,#e9f7ef 58%,var(--card,#fff));color:#28794c;text-align:center}.admin-prayer-empty span{font-size:.82rem;color:var(--muted-foreground,#6b7280)}.admin-prayer-history-title{margin-top:25px}.admin-prayer-answered{display:grid;gap:9px}.admin-prayer-answered>article{display:flex;gap:10px;padding:14px;border-radius:15px;background:color-mix(in srgb,#e9f7ef 48%,var(--card,#fff));color:#28794c}.admin-prayer-answered>article>div{display:grid;gap:3px}.admin-prayer-answered p{margin:0;color:var(--foreground,#222);font-size:.84rem}.admin-prayer-answered small{color:var(--muted-foreground,#6b7280)}@media(max-width:560px){.admin-prayer-priority{grid-template-columns:auto 1fr auto}.admin-prayer-priority>svg{display:none}.admin-prayer-list header{display:grid}.admin-prayer-list header em{justify-self:start}}
''')

# Home: deep links, identidade de push após login e abertura da notificação.
replace('pwa/client/src/pages/Home.tsx',
'import { listenToForegroundPush, sendPwaPush, subscribeToPwaPush } from "@/lib/push";',
'import { listenToForegroundPush, sendPwaPush, subscribeToPwaPush, syncPwaPushPreferences } from "@/lib/push";')
insert_before_home = r'''
function initialViewFromUrl(): AppView {
  const requested = new URLSearchParams(window.location.search).get("view") || "";
  const allowed = new Set<AppView>(["home","bible","news","devotionals","media","ibr","menu","profile","settings","admin","discipulado","cultos","plans","prayer","members","team","donations","about"]);
  return allowed.has(requested as AppView) ? requested as AppView : "home";
}

'''
replace('pwa/client/src/pages/Home.tsx', 'export default function Home() {', insert_before_home + 'export default function Home() {')
replace('pwa/client/src/pages/Home.tsx',
'  const [view, setView] = useState<AppView>("home"); const [session, setSession] = useState<PwaSession | null>(null);',
'  const [view, setView] = useState<AppView>(() => initialViewFromUrl()); const [session, setSession] = useState<PwaSession | null>(null);')
old_foreground = '''  useEffect(() => {
    let unsubscribe: () => void = () => undefined;
    listenToForegroundPush((payload) => toast.message(payload.notification?.title || "MIC Rhema", { description: payload.notification?.body || "Você recebeu uma novidade." }))
      .then((cleanup) => { unsubscribe = cleanup; })
      .catch(() => undefined);
    return () => unsubscribe();
  }, []);'''
new_foreground = '''  useEffect(() => {
    let unsubscribe: () => void = () => undefined;
    listenToForegroundPush((payload) => {
      const data = payload.data || {};
      const collection = String(data.collection || "");
      const documentId = String(data.documentId || "");
      const isAdminPrayer = collection === "prayer_requests";
      const isPrayerResponse = collection === "prayer_response" || String(data.category || "") === "prayer_response";
      if (isAdminPrayer || isPrayerResponse) window.dispatchEvent(new CustomEvent("micrhema:prayer-updated"));
      const target = isAdminPrayer ? "admin" : isPrayerResponse ? "prayer" : "";
      toast.message(payload.notification?.title || String(data.title || "MIC Rhema"), {
        description: payload.notification?.body || String(data.body || "Você recebeu uma novidade."),
        action: target ? { label: "Abrir", onClick: () => {
          const params = new URLSearchParams(); params.set("view", target);
          if (isAdminPrayer) { params.set("section", "prayers"); if (documentId) params.set("request", documentId); window.dispatchEvent(new CustomEvent("micrhema:open-admin-prayer")); }
          else if (documentId) params.set("request", documentId);
          window.history.replaceState({}, "", `${window.location.pathname}?${params.toString()}`);
          setView(target as AppView);
        } } : undefined,
      });
    }).then((cleanup) => { unsubscribe = cleanup; }).catch(() => undefined);
    return () => unsubscribe();
  }, []);'''
replace('pwa/client/src/pages/Home.tsx', old_foreground, new_foreground)
replace('pwa/client/src/pages/Home.tsx',
'  const persistSession = (next: PwaSession) => { localStorage.setItem("mic-rhema-pwa-session", JSON.stringify(next)); setSession(next); };',
'  const persistSession = (next: PwaSession) => { localStorage.setItem("mic-rhema-pwa-session", JSON.stringify(next)); setSession(next); window.setTimeout(() => void syncPwaPushPreferences(), 0); };')
replace('pwa/client/src/pages/Home.tsx',
'  const logout = async () => { if (firebaseAuth) await signOut(firebaseAuth); localStorage.removeItem("mic-rhema-pwa-session"); setSession(null); setView("home"); toast.message("Você saiu da sua conta."); };',
'  const logout = async () => { if (firebaseAuth) await signOut(firebaseAuth); await syncPwaPushPreferences().catch(() => false); localStorage.removeItem("mic-rhema-pwa-session"); setSession(null); setView("home"); toast.message("Você saiu da sua conta."); };')

# Não gerar novo APK por alterações exclusivamente de backend/PWA.
replace('.github/workflows/main.yml',
"      - 'supabase/functions/pwa-prayer-request/**'\n",
"      - 'supabase/functions/pwa-prayer-request/**'\n      - 'supabase/functions/pwa-prayer-admin/**'\n      - 'supabase/functions/notify-fcm/**'\n")

write('supabase/functions/pwa-push-subscribe/index.ts', r'''import { importPKCS8, SignJWT } from "npm:jose@5.10.0";
const GOOGLE_TOKEN_URL="https://oauth2.googleapis.com/token";const DATASTORE_SCOPE="https://www.googleapis.com/auth/datastore";const FIREBASE_API_KEY=Deno.env.get("FIREBASE_WEB_API_KEY")||"AIzaSyD-GPqTLRFmOiNATJwzKUHGqJeTPQcf0E8";
const cors={"access-control-allow-origin":"*","access-control-allow-headers":"authorization, apikey, content-type, x-client-info","access-control-allow-methods":"POST, OPTIONS","content-type":"application/json; charset=utf-8","cache-control":"no-store"};
type ServiceAccount={project_id?:string;client_email?:string;private_key?:string};type Preferences={enabled?:unknown;courses?:unknown;devotional?:unknown;events?:unknown;service?:unknown;news?:unknown;media?:unknown;ibr?:unknown;sermons?:unknown};
function json(body:Record<string,unknown>,status=200){return new Response(JSON.stringify(body),{status,headers:cors})}function pref(value:unknown,fallback=true){return typeof value==="boolean"?value:fallback}
async function googleAccessToken(account:ServiceAccount){if(!account.client_email||!account.private_key)throw new Error("Credencial Firebase incompleta.");const now=Math.floor(Date.now()/1000);const key=await importPKCS8(account.private_key.replace(/\\n/g,"\n"),"RS256");const assertion=await new SignJWT({iss:account.client_email,scope:DATASTORE_SCOPE,aud:GOOGLE_TOKEN_URL}).setProtectedHeader({alg:"RS256",typ:"JWT"}).setIssuedAt(now).setExpirationTime(now+3600).sign(key);const response=await fetch(GOOGLE_TOKEN_URL,{method:"POST",headers:{"content-type":"application/x-www-form-urlencoded"},body:new URLSearchParams({grant_type:"urn:ietf:params:oauth:grant-type:jwt-bearer",assertion})});const payload=await response.json();if(!response.ok||!payload.access_token)throw new Error("Não foi possível registrar notificações agora.");return payload.access_token as string}
async function tokenId(token:string){const digest=await crypto.subtle.digest("SHA-256",new TextEncoder().encode(token));return[...new Uint8Array(digest)].map(byte=>byte.toString(16).padStart(2,"0")).join("")}
async function session(request:Request){const idToken=request.headers.get("authorization")?.replace(/^Bearer\s+/i,"");if(!idToken)return{uid:"",isAdmin:false};const response=await fetch(`https://identitytoolkit.googleapis.com/v1/accounts:lookup?key=${FIREBASE_API_KEY}`,{method:"POST",headers:{"content-type":"application/json"},body:JSON.stringify({idToken})});const payload=await response.json() as {users?:Array<{localId?:string;customAttributes?:string}>};if(!response.ok||!payload.users?.[0]?.localId)return{uid:"",isAdmin:false};let claims:Record<string,unknown>={};try{claims=JSON.parse(payload.users[0].customAttributes||"{}")}catch{claims={}}return{uid:payload.users[0].localId||"",isAdmin:claims.isAdmin===true}}
Deno.serve(async(request)=>{if(request.method==="OPTIONS")return new Response("ok",{headers:cors});if(request.method!=="POST")return json({error:"Método não permitido."},405);try{const input=await request.json() as {token?:unknown;deviceId?:unknown;platform?:unknown;preferences?:Preferences};const token=String(input.token||"").trim();if(token.length<80||token.length>4096)return json({error:"Token de notificação inválido."},400);const deviceId=String(input.deviceId||"").trim().slice(0,160);const p=input.preferences||{};const account=JSON.parse(Deno.env.get("FIREBASE_SERVICE_ACCOUNT_JSON")||"{}") as ServiceAccount;const projectId=account.project_id||Deno.env.get("FIREBASE_PROJECT_ID")||"mic-rhema";const documentId=await tokenId(token);const identity=await session(request);const response=await fetch(`https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents/pwa_push_tokens/${documentId}`,{method:"PATCH",headers:{authorization:`Bearer ${await googleAccessToken(account)}`,"content-type":"application/json"},body:JSON.stringify({fields:{token:{stringValue:token},deviceId:{stringValue:deviceId},uid:{stringValue:identity.uid},isAdmin:{booleanValue:identity.isAdmin},platform:{stringValue:String(input.platform||"web").slice(0,32)},source:{stringValue:"pwa"},updatedAt:{timestampValue:new Date().toISOString()},enabled:{booleanValue:pref(p.enabled)},prefCourses:{booleanValue:pref(p.courses)},prefDevotional:{booleanValue:pref(p.devotional)},prefEvents:{booleanValue:pref(p.events)},prefService:{booleanValue:pref(p.service)},prefNews:{booleanValue:pref(p.news)},prefMedia:{booleanValue:pref(p.media)},prefIbr:{booleanValue:pref(p.ibr)},prefSermons:{booleanValue:pref(p.sermons)}}})});if(!response.ok)throw new Error("Não foi possível salvar a inscrição de avisos.");return json({ok:true,isAdmin:identity.isAdmin})}catch(error){console.error("pwa-push-subscribe failed",error instanceof Error?error.message:"unknown");return json({error:"Não foi possível ativar notificações agora."},500)}});
''')

write('supabase/functions/pwa-prayer-request/index.ts', r'''import { importPKCS8, SignJWT } from "npm:jose@5.10.0";
const GOOGLE_TOKEN_URL="https://oauth2.googleapis.com/token";const GOOGLE_SCOPE="https://www.googleapis.com/auth/datastore https://www.googleapis.com/auth/firebase.messaging";const FIREBASE_API_KEY=Deno.env.get("FIREBASE_WEB_API_KEY")||"AIzaSyD-GPqTLRFmOiNATJwzKUHGqJeTPQcf0E8";const DEFAULT_LINK="https://bichocutela.github.io/Mic-RHEMA-Ia-Studio-Google/";
const cors={"access-control-allow-origin":"*","access-control-allow-headers":"authorization, apikey, content-type, x-client-info","access-control-allow-methods":"POST, OPTIONS","content-type":"application/json; charset=utf-8","cache-control":"no-store"};type ServiceAccount={project_id?:string;client_email?:string;private_key?:string};type Field={stringValue?:string;integerValue?:string;booleanValue?:boolean;timestampValue?:string};type Document={name?:string;fields?:Record<string,Field>};
function json(body:Record<string,unknown>,status=200){return new Response(JSON.stringify(body),{status,headers:cors})}function clean(v:unknown){return String(v??"").trim()}function base(projectId:string){return`https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents`}
async function sha256(value:string){const digest=await crypto.subtle.digest("SHA-256",new TextEncoder().encode(value));return[...new Uint8Array(digest)].map(byte=>byte.toString(16).padStart(2,"0")).join("")}
async function accessToken(account:ServiceAccount){if(!account.client_email||!account.private_key)throw new Error("Credencial Firebase incompleta.");const now=Math.floor(Date.now()/1000);const key=await importPKCS8(account.private_key.replace(/\\n/g,"\n"),"RS256");const assertion=await new SignJWT({iss:account.client_email,scope:GOOGLE_SCOPE,aud:GOOGLE_TOKEN_URL}).setProtectedHeader({alg:"RS256",typ:"JWT"}).setIssuedAt(now).setExpirationTime(now+3600).sign(key);const response=await fetch(GOOGLE_TOKEN_URL,{method:"POST",headers:{"content-type":"application/x-www-form-urlencoded"},body:new URLSearchParams({grant_type:"urn:ietf:params:oauth:grant-type:jwt-bearer",assertion})});const payload=await response.json();if(!response.ok||!payload.access_token)throw new Error("Não foi possível autenticar o pedido.");return payload.access_token as string}
async function session(request:Request){const idToken=request.headers.get("authorization")?.replace(/^Bearer\s+/i,"");if(!idToken)return null;const response=await fetch(`https://identitytoolkit.googleapis.com/v1/accounts:lookup?key=${FIREBASE_API_KEY}`,{method:"POST",headers:{"content-type":"application/json"},body:JSON.stringify({idToken})});const payload=await response.json() as {users?:Array<{localId?:string}>};return response.ok&&payload.users?.[0]?.localId?{uid:payload.users[0].localId}:null}
function fieldValue(field?:Field):unknown{return field?.stringValue??field?.integerValue??field?.booleanValue??field?.timestampValue}function item(document:Document){const f=document.fields||{};return{id:clean(fieldValue(f.id))||clean(document.name).split("/").pop()||"",name:clean(fieldValue(f.name)),request:clean(fieldValue(f.request)),date:clean(fieldValue(f.date)),createdAt:Number(fieldValue(f.createdAt)||0),status:clean(fieldValue(f.status))||"pendente",answeredAt:Number(fieldValue(f.answeredAt)||0),answeredDate:clean(fieldValue(f.answeredDate)),responseMessage:clean(fieldValue(f.responseMessage)),answeredBy:clean(fieldValue(f.answeredBy)),requesterAccessHash:clean(fieldValue(f.requesterAccessHash))}}
async function query(projectId:string,token:string,fieldPath:string,value:string){const response=await fetch(`${base(projectId)}:runQuery`,{method:"POST",headers:{authorization:`Bearer ${token}`,"content-type":"application/json"},body:JSON.stringify({structuredQuery:{from:[{collectionId:"prayer_requests"}],where:{fieldFilter:{field:{fieldPath},op:"EQUAL",value:{stringValue:value}}},limit:100}})});if(!response.ok)throw new Error("Não foi possível carregar seu histórico.");const rows=await response.json() as Array<{document?:Document}>;return rows.map(row=>row.document).filter((doc):doc is Document=>Boolean(doc))}
async function pwaAdminTokens(projectId:string,token:string){const response=await fetch(`${base(projectId)}/pwa_push_tokens?pageSize=500`,{headers:{authorization:`Bearer ${token}`}});if(!response.ok)return[];const payload=await response.json() as {documents?:Document[]};return(payload.documents||[]).filter(doc=>doc.fields?.isAdmin?.booleanValue===true&&doc.fields?.enabled?.booleanValue!==false).map(doc=>clean(doc.fields?.token?.stringValue)).filter(Boolean)}
async function fcm(projectId:string,token:string,target:Record<string,string>,title:string,body:string,data:Record<string,string>,webLink?:string){const message:Record<string,unknown>={...target,data,android:{priority:"high",ttl:"86400s"}};if(webLink)message.webpush={headers:{Urgency:"high"},notification:{title,body,icon:`${DEFAULT_LINK}icons/icon-192.png`},fcm_options:{link:webLink}};const response=await fetch(`https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`,{method:"POST",headers:{authorization:`Bearer ${token}`,"content-type":"application/json; UTF-8"},body:JSON.stringify({message})});return response.ok}
Deno.serve(async(request)=>{if(request.method==="OPTIONS")return new Response("ok",{headers:cors});if(request.method!=="POST")return json({error:"Método não permitido."},405);try{const input=await request.json() as Record<string,unknown>;const action=clean(input.action)||"submit";const account=JSON.parse(Deno.env.get("FIREBASE_SERVICE_ACCOUNT_JSON")||"{}") as ServiceAccount;const projectId=account.project_id||Deno.env.get("FIREBASE_PROJECT_ID")||"mic-rhema";const token=await accessToken(account);const identity=await session(request);const deviceId=clean(input.deviceId).slice(0,160);const deviceSecret=clean(input.deviceSecret).slice(0,220);
if(action==="history"){if(identity?.uid){const docs=await query(projectId,token,"requesterUid",identity.uid);return json({ok:true,items:docs.map(item).sort((a,b)=>b.createdAt-a.createdAt).map(({requesterAccessHash,...safe})=>safe)})}if(!deviceId||!deviceSecret)return json({ok:true,items:[]});const hash=await sha256(deviceSecret);const docs=await query(projectId,token,"requesterDeviceId",deviceId);return json({ok:true,items:docs.map(item).filter(entry=>entry.requesterAccessHash===hash).sort((a,b)=>b.createdAt-a.createdAt).map(({requesterAccessHash,...safe})=>safe)})}
const name=clean(input.name);const prayer=clean(input.request);if(!name||!prayer)return json({error:"Preencha seu nome e o pedido de oração."},400);if(name.length>120||prayer.length>4000)return json({error:"Seu pedido ultrapassa o tamanho permitido."},400);if(!deviceId||!deviceSecret)return json({error:"Não foi possível identificar este aparelho com segurança."},400);const id=crypto.randomUUID();const now=Date.now();const date=new Intl.DateTimeFormat("pt-BR",{timeZone:"America/Fortaleza"}).format(new Date(now));const notificationToken=clean(input.notificationToken).slice(0,4096);const response=await fetch(`${base(projectId)}/prayer_requests/${id}`,{method:"PATCH",headers:{authorization:`Bearer ${token}`,"content-type":"application/json"},body:JSON.stringify({fields:{id:{stringValue:id},name:{stringValue:name},request:{stringValue:prayer},date:{stringValue:date},createdAt:{integerValue:String(now)},requesterUid:{stringValue:identity?.uid||""},requesterMemberId:{stringValue:identity?.uid||""},requesterFcmToken:{stringValue:notificationToken},requesterDeviceId:{stringValue:deviceId},requesterAccessHash:{stringValue:await sha256(deviceSecret)},status:{stringValue:"pendente"},answeredAt:{integerValue:"0"},answeredDate:{stringValue:""},responseMessage:{stringValue:""},answeredBy:{stringValue:""},source:{stringValue:"pwa"},createdAtServer:{timestampValue:new Date(now).toISOString()}}})});if(!response.ok)throw new Error("Não foi possível salvar o pedido.");const title="Novo pedido de oração";const body="Há um novo pedido aguardando a equipe pastoral.";const data={collection:"prayer_requests",documentId:id,category:"prayer",destination:`admin_prayer/${id}`,title,body};await fcm(projectId,token,{topic:"prayer_admins"},title,body,data);const adminTokens=await pwaAdminTokens(projectId,token);await Promise.all(adminTokens.map(registrationToken=>fcm(projectId,token,{token:registrationToken},title,body,data,`${DEFAULT_LINK}?view=admin&section=prayers&request=${encodeURIComponent(id)}`)));return json({ok:true,id,adminWebRecipients:adminTokens.length})}catch(error){console.error("pwa-prayer-request failed",error instanceof Error?error.message:"unknown");return json({error:error instanceof Error?error.message:"Não foi possível enviar o pedido agora. Tente novamente."},500)}});
''')

write('supabase/functions/pwa-prayer-admin/index.ts', r'''import { importPKCS8, SignJWT } from "npm:jose@5.10.0";
const GOOGLE_TOKEN_URL="https://oauth2.googleapis.com/token";const GOOGLE_SCOPE="https://www.googleapis.com/auth/datastore https://www.googleapis.com/auth/firebase.messaging";const FIREBASE_API_KEY=Deno.env.get("FIREBASE_WEB_API_KEY")||"AIzaSyD-GPqTLRFmOiNATJwzKUHGqJeTPQcf0E8";const DEFAULT_LINK="https://bichocutela.github.io/Mic-RHEMA-Ia-Studio-Google/";const cors={"access-control-allow-origin":"*","access-control-allow-headers":"authorization, apikey, content-type, x-client-info","access-control-allow-methods":"POST, OPTIONS","content-type":"application/json; charset=utf-8","cache-control":"no-store"};type ServiceAccount={project_id?:string;client_email?:string;private_key?:string};type Field={stringValue?:string;integerValue?:string;booleanValue?:boolean};type Document={fields?:Record<string,Field>};function json(body:Record<string,unknown>,status=200){return new Response(JSON.stringify(body),{status,headers:cors})}function clean(v:unknown){return String(v??"").trim()}function base(projectId:string){return`https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents`}
async function session(request:Request){const idToken=request.headers.get("authorization")?.replace(/^Bearer\s+/i,"");if(!idToken)throw new Error("Acesso pastoral obrigatório.");const response=await fetch(`https://identitytoolkit.googleapis.com/v1/accounts:lookup?key=${FIREBASE_API_KEY}`,{method:"POST",headers:{"content-type":"application/json"},body:JSON.stringify({idToken})});const payload=await response.json() as {users?:Array<{localId?:string;customAttributes?:string}>};if(!response.ok||!payload.users?.[0]?.localId)throw new Error("Sessão administrativa inválida.");let claims:Record<string,unknown>={};try{claims=JSON.parse(payload.users[0].customAttributes||"{}")}catch{claims={}}if(claims.isAdmin!==true)throw new Error("Acesso pastoral obrigatório.");return{uid:payload.users[0].localId||""}}
async function accessToken(account:ServiceAccount){if(!account.client_email||!account.private_key)throw new Error("Credencial Firebase incompleta.");const now=Math.floor(Date.now()/1000);const key=await importPKCS8(account.private_key.replace(/\\n/g,"\n"),"RS256");const assertion=await new SignJWT({iss:account.client_email,scope:GOOGLE_SCOPE,aud:GOOGLE_TOKEN_URL}).setProtectedHeader({alg:"RS256",typ:"JWT"}).setIssuedAt(now).setExpirationTime(now+3600).sign(key);const response=await fetch(GOOGLE_TOKEN_URL,{method:"POST",headers:{"content-type":"application/x-www-form-urlencoded"},body:new URLSearchParams({grant_type:"urn:ietf:params:oauth:grant-type:jwt-bearer",assertion})});const payload=await response.json();if(!response.ok||!payload.access_token)throw new Error("Não foi possível autenticar a ação pastoral.");return payload.access_token as string}
async function fcm(projectId:string,token:string,registrationToken:string,title:string,body:string,data:Record<string,string>){const response=await fetch(`https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`,{method:"POST",headers:{authorization:`Bearer ${token}`,"content-type":"application/json; UTF-8"},body:JSON.stringify({message:{token:registrationToken,data,android:{priority:"high",ttl:"86400s"},webpush:{headers:{Urgency:"high"},notification:{title,body,icon:`${DEFAULT_LINK}icons/icon-192.png`},fcm_options:{link:`${DEFAULT_LINK}?view=prayer&request=${encodeURIComponent(data.documentId)}`}}}})});return response.ok}
Deno.serve(async(request)=>{if(request.method==="OPTIONS")return new Response("ok",{headers:cors});if(request.method!=="POST")return json({error:"Método não permitido."},405);try{const pastor=await session(request);const input=await request.json() as {action?:unknown;requestId?:unknown};if(clean(input.action)!=="mark_answered")return json({error:"Ação inválida."},400);const requestId=clean(input.requestId);if(!requestId)return json({error:"Pedido inválido."},400);const account=JSON.parse(Deno.env.get("FIREBASE_SERVICE_ACCOUNT_JSON")||"{}") as ServiceAccount;const projectId=account.project_id||Deno.env.get("FIREBASE_PROJECT_ID")||"mic-rhema";const token=await accessToken(account);const source=await fetch(`${base(projectId)}/prayer_requests/${encodeURIComponent(requestId)}`,{headers:{authorization:`Bearer ${token}`}});if(source.status===404)return json({error:"Pedido de oração não encontrado."},404);if(!source.ok)throw new Error("Não foi possível carregar o pedido.");const document=await source.json() as Document;const f=document.fields||{};const now=Date.now();const date=new Intl.DateTimeFormat("pt-BR",{timeZone:"America/Fortaleza"}).format(new Date(now));const responseMessage=`Oração respondida — a equipe pastoral orou por este pedido em ${date}.`;const params=["status","answeredAt","answeredDate","responseMessage","answeredBy"].map(field=>`updateMask.fieldPaths=${encodeURIComponent(field)}`).join("&");const updated=await fetch(`${base(projectId)}/prayer_requests/${encodeURIComponent(requestId)}?${params}`,{method:"PATCH",headers:{authorization:`Bearer ${token}`,"content-type":"application/json"},body:JSON.stringify({fields:{status:{stringValue:"respondida"},answeredAt:{integerValue:String(now)},answeredDate:{stringValue:date},responseMessage:{stringValue:responseMessage},answeredBy:{stringValue:"Equipe Pastoral"}}})});if(!updated.ok)throw new Error("Não foi possível confirmar a oração.");const targets=new Set<string>();const savedToken=clean(f.requesterFcmToken?.stringValue);if(savedToken)targets.add(savedToken);const requesterDeviceId=clean(f.requesterDeviceId?.stringValue);const requesterUid=clean(f.requesterUid?.stringValue);const pwaTokens=await fetch(`${base(projectId)}/pwa_push_tokens?pageSize=500`,{headers:{authorization:`Bearer ${token}`}});if(pwaTokens.ok){const payload=await pwaTokens.json() as {documents?:Document[]};for(const doc of payload.documents||[]){const fields=doc.fields||{};if(fields.enabled?.booleanValue===false)continue;const sameDevice=requesterDeviceId&&clean(fields.deviceId?.stringValue)===requesterDeviceId;const sameUid=requesterUid&&clean(fields.uid?.stringValue)===requesterUid;if(sameDevice||sameUid){const candidate=clean(fields.token?.stringValue);if(candidate)targets.add(candidate)}}}const title="🙏 Oração respondida";const body=`Seu pedido de oração foi atendido em ${date}. A equipe pastoral orou por você.`;const data={collection:"prayer_response",documentId:requestId,category:"prayer_response",destination:"prayer",title,body};const results=await Promise.all([...targets].map(registrationToken=>fcm(projectId,token,registrationToken,title,body,data)));return json({ok:true,requestId,answeredDate:date,notified:results.filter(Boolean).length,answeredBy:pastor.uid})}catch(error){console.error("pwa-prayer-admin failed",error instanceof Error?error.message:"unknown");return json({error:error instanceof Error?error.message:"Não foi possível concluir a ação pastoral."},403)}});
''')

write('supabase/functions/notify-fcm/index.ts', r'''import { importPKCS8, SignJWT } from "npm:jose@5.10.0";
const GOOGLE_SCOPE="https://www.googleapis.com/auth/firebase.messaging https://www.googleapis.com/auth/datastore";const FCM_TOKEN_URL="https://oauth2.googleapis.com/token";const DEFAULT_PROJECT_ID="mic-rhema";const DEFAULT_LINK="https://bichocutela.github.io/Mic-RHEMA-Ia-Studio-Google/";const MAX_TEXT_LENGTH=180;type NotificationRequest={topic?:string;token?:string;title?:string;body?:string;data?:Record<string,string|number|boolean>};type ServiceAccount={project_id?:string;client_email?:string;private_key?:string};type FireDoc={fields?:Record<string,{stringValue?:string;booleanValue?:boolean}>};const corsHeaders={"Access-Control-Allow-Origin":"*","Access-Control-Allow-Headers":"authorization, apikey, content-type, x-client-info","Access-Control-Allow-Methods":"POST, OPTIONS","Content-Type":"application/json"};function json(body:Record<string,unknown>,status=200){return new Response(JSON.stringify(body),{status,headers:corsHeaders})}function cleanText(value:unknown,fallback:string){const text=String(value??fallback).trim();return text.slice(0,MAX_TEXT_LENGTH)||fallback}
async function accessToken(account:ServiceAccount){if(!account.client_email||!account.private_key)throw new Error("FIREBASE_SERVICE_ACCOUNT_JSON sem client_email ou private_key.");const now=Math.floor(Date.now()/1000);const assertion=await new SignJWT({iss:account.client_email,scope:GOOGLE_SCOPE,aud:FCM_TOKEN_URL}).setProtectedHeader({alg:"RS256",typ:"JWT"}).setIssuedAt(now).setExpirationTime(now+3600).sign(await importPKCS8(account.private_key.replace(/\\n/g,"\n"),"RS256"));const response=await fetch(FCM_TOKEN_URL,{method:"POST",headers:{"Content-Type":"application/x-www-form-urlencoded"},body:new URLSearchParams({grant_type:"urn:ietf:params:oauth:grant-type:jwt-bearer",assertion})});if(!response.ok)throw new Error(`Falha ao autenticar no FCM: ${response.status}`);const payload=await response.json();if(!payload.access_token)throw new Error("O Google não retornou um access_token.");return payload.access_token as string}
function prayerLink(data:Record<string,string>){const collection=data.collection||"";const id=data.documentId||"";if(collection==="prayer_requests")return`${DEFAULT_LINK}?view=admin&section=prayers&request=${encodeURIComponent(id)}`;if(collection==="prayer_response")return`${DEFAULT_LINK}?view=prayer&request=${encodeURIComponent(id)}`;return DEFAULT_LINK}
async function send(projectId:string,token:string,target:Record<string,string>,title:string,body:string,data:Record<string,string>,includeWeb=true){const message:Record<string,unknown>={...target,data,android:{priority:"high",ttl:"86400s"}};if(includeWeb)message.webpush={headers:{Urgency:"high"},notification:{title,body,icon:`${DEFAULT_LINK}icons/icon-192.png`},fcm_options:{link:prayerLink(data)}};const response=await fetch(`https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`,{method:"POST",headers:{Authorization:`Bearer ${token}`,"Content-Type":"application/json; UTF-8"},body:JSON.stringify({message})});return{ok:response.ok,status:response.status,body:await response.text()}}
async function adminWebTokens(projectId:string,token:string){const response=await fetch(`https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents/pwa_push_tokens?pageSize=500`,{headers:{authorization:`Bearer ${token}`}});if(!response.ok)return[];const payload=await response.json() as {documents?:FireDoc[]};return(payload.documents||[]).filter(doc=>doc.fields?.isAdmin?.booleanValue===true&&doc.fields?.enabled?.booleanValue!==false).map(doc=>String(doc.fields?.token?.stringValue||"")).filter(Boolean)}
Deno.serve(async(request)=>{if(request.method==="OPTIONS")return new Response("ok",{headers:corsHeaders});if(request.method!=="POST")return json({error:"Método não permitido."},405);try{const configuredKey=Deno.env.get("SUPABASE_ANON_KEY");const providedKey=request.headers.get("apikey");if(configuredKey&&providedKey&&providedKey!==configuredKey)return json({error:"Chave pública do Supabase inválida."},401);const input=await request.json() as NotificationRequest;const directToken=String(input.token??"").trim().slice(0,4096);const topic=cleanText(input.topic,"all_users").replace(/[^a-zA-Z0-9_.-]/g,"");const title=cleanText(input.title,"Nova atualização disponível");const body=cleanText(input.body,"Confira as novidades no MIC Rhema.");const account=JSON.parse(Deno.env.get("FIREBASE_SERVICE_ACCOUNT_JSON")??"{}") as ServiceAccount;const projectId=account.project_id||Deno.env.get("FIREBASE_PROJECT_ID")||DEFAULT_PROJECT_ID;const token=await accessToken(account);const data=Object.fromEntries(Object.entries(input.data??{}).map(([key,value])=>[key,String(value)]));data.title=data.title||title;data.body=data.body||body;data.category=data.category||"content_updates";const primary=await send(projectId,token,directToken?{token:directToken}:{topic},title,body,data,true);if(!primary.ok){console.error("FCM rejected notification",primary.status,primary.body);return json({error:"FCM rejeitou a notificação.",details:primary.body.slice(0,500)},502)}let webAdmins=0;if(!directToken&&topic==="prayer_admins"){const admins=await adminWebTokens(projectId,token);const results=await Promise.all(admins.map(registrationToken=>send(projectId,token,{token:registrationToken},title,body,data,true)));webAdmins=results.filter(result=>result.ok).length}return json({ok:true,target:directToken?"token":`topic:${topic}`,webAdmins,message:primary.body})}catch(error){console.error("notify-fcm failed",error);return json({error:error instanceof Error?error.message:"Erro ao enviar notificação."},500)}});
''')

print('PWA prayer priority flow patch applied.')
