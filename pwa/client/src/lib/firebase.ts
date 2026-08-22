/**
 * SANTUÁRIO EM MOVIMENTO — Conexão pública ao Firebase.
 * A PWA usa apenas configuração web pública; ações administrativas exigem sessão com claim isAdmin.
 */
import { getApp, getApps, initializeApp } from "firebase/app";
import { getAuth } from "firebase/auth";
import { addDoc, collection, doc, getFirestore, onSnapshot, serverTimestamp, setDoc, updateDoc, type DocumentData } from "firebase/firestore";

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY || "AIzaSyD-GPqTLRFmOiNATJwzKUHGqJeTPQcf0E8",
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN || "mic-rhema.firebaseapp.com",
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID || "mic-rhema",
  appId: import.meta.env.VITE_FIREBASE_APP_ID || "1:894363387794:web:f8010218d4f6c6e085234b",
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID || "894363387794",
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET || "mic-rhema.firebasestorage.app",
};

export const firebaseEnabled = Boolean(
  firebaseConfig.apiKey && firebaseConfig.authDomain && firebaseConfig.projectId && firebaseConfig.appId,
);

export const firebaseApp = firebaseEnabled
  ? getApps().length
    ? getApp()
    : initializeApp(firebaseConfig)
  : null;

export const firebaseAuth = firebaseApp ? getAuth(firebaseApp) : null;
export const firestore = firebaseApp ? getFirestore(firebaseApp) : null;

export function listenToCollection<T extends DocumentData>(
  collectionName: string,
  onData: (items: Array<T & { id: string }>) => void,
  onError?: (error: Error) => void,
) {
  if (!firestore) return () => undefined;
  return onSnapshot(
    collection(firestore, collectionName),
    (snapshot) => onData(snapshot.docs.map((doc) => ({ id: doc.id, ...doc.data() }) as T & { id: string })),
    (error) => onError?.(error),
  );
}

/** PARIDADE ANDROID — escuta documentos de configuração usados também pelo APK, sem escrever no Firebase. */
export function listenToDocument<T extends DocumentData>(
  collectionName: string,
  documentName: string,
  onData: (item: (T & { id: string }) | null) => void,
  onError?: (error: Error) => void,
) {
  if (!firestore) return () => undefined;
  return onSnapshot(
    doc(firestore, collectionName, documentName),
    (snapshot) => onData(snapshot.exists() ? ({ id: snapshot.id, ...snapshot.data() } as T & { id: string }) : null),
    (error) => onError?.(error),
  );
}

export async function submitPendingAccessRequest(input: { name: string; phone: string }) {
  if (!firestore) throw new Error("A conexão Firebase da PWA não está disponível.");
  await addDoc(collection(firestore, "acessos_pendentes"), {
    name: input.name.trim(),
    phone: input.phone.trim(),
    email: "",
    isApproved: false,
    isVip: false,
    isIbr: false,
    isAdmin: false,
    status: "pendente",
    type: "acesso",
    avatarId: "caminhante",
    createdAt: Date.now(),
    updatedAt: Date.now(),
    createdAtServer: serverTimestamp(),
  });
}

/** PARIDADE ANDROID — grava PrayerRequest com os mesmos campos usados pela PrayerScreen do APK. */
export async function submitPrayerRequest(input: { name: string; request: string }) {
  if (!firestore) throw new Error("A conexão Firebase da PWA não está disponível.");
  const requestRef = doc(collection(firestore, "prayer_requests"));
  await setDoc(requestRef, {
    id: requestRef.id,
    name: input.name.trim(),
    request: input.request.trim(),
    date: "Hoje",
    createdAt: Date.now(),
    createdAtServer: serverTimestamp(),
    source: "pwa",
  });
}

export async function savePwaProfile(uid: string, data: Record<string, unknown>) {
  if (!firestore) throw new Error("A conexão Firebase da PWA não está disponível.");
  await setDoc(doc(firestore, "users", uid), { ...data, updatedAt: Date.now(), updatedAtServer: serverTimestamp() }, { merge: true });
}

export async function approveMemberRequest(memberId: string) {
  if (!firestore) throw new Error("A conexão Firebase da PWA não está disponível.");
  await updateDoc(doc(firestore, "acessos_pendentes", memberId), { isApproved: true, status: "aprovado", updatedAt: Date.now() });
}

export async function createAdminContent(input: { collectionName: string; title: string; description: string; mediaUrl?: string }) {
  if (!firestore) throw new Error("A conexão Firebase da PWA não está disponível.");
  await addDoc(collection(firestore, input.collectionName), {
    title: input.title.trim(),
    description: input.description.trim(),
    imageUrl: input.mediaUrl?.trim() || "",
    thumbnailUrl: input.mediaUrl?.trim() || "",
    publishedAt: Date.now(),
    createdAt: Date.now(),
    updatedAt: Date.now(),
    source: "pwa",
  });
}
