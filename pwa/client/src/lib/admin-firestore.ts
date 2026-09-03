import { collection, deleteDoc, doc, serverTimestamp, setDoc } from "firebase/firestore";
import { firestore } from "./firebase";

function db() {
  if (!firestore) throw new Error("Firebase indisponível nesta PWA.");
  return firestore;
}

function normalizedDocumentId(id: string | number | null | undefined) {
  return String(id ?? "").trim() || crypto.randomUUID();
}

function storedId(collectionName: string, documentId: string) {
  if (collectionName === "bible_news" && /^\d+$/.test(documentId)) {
    const numeric = Number(documentId);
    if (Number.isSafeInteger(numeric) && numeric > 0 && numeric <= 2_147_483_647) return numeric;
  }
  return documentId;
}

export async function saveAdminDocument(collectionName: string, id: string | number, data: Record<string, unknown>) {
  const documentId = normalizedDocumentId(id);
  await setDoc(doc(db(), collectionName, documentId), {
    ...data,
    id: storedId(collectionName, documentId),
    updatedAt: Date.now(),
    updatedAtServer: serverTimestamp(),
    source: "pwa",
  }, { merge: true });
  return documentId;
}

export async function replaceAdminDocument(collectionName: string, id: string | number, data: Record<string, unknown>) {
  const documentId = normalizedDocumentId(id);
  await setDoc(doc(db(), collectionName, documentId), {
    ...data,
    id: storedId(collectionName, documentId),
    updatedAt: Date.now(),
    updatedAtServer: serverTimestamp(),
    source: "pwa",
  });
  return documentId;
}

export async function deleteAdminDocument(collectionName: string, id: string | number) {
  await deleteDoc(doc(db(), collectionName, normalizedDocumentId(id)));
}

export async function saveAdminSetting(documentName: string, data: Record<string, unknown>) {
  await setDoc(doc(db(), "settings", documentName), {
    ...data,
    updatedAt: Date.now(),
    updatedAtServer: serverTimestamp(),
    source: "pwa",
  }, { merge: true });
}

export async function forceAdminSync() {
  await setDoc(doc(db(), "settings", "sync_trigger"), {
    timestamp: Date.now(),
    source: "pwa",
  }, { merge: true });
}

export function createAdminDocumentId(collectionName: string) {
  if (collectionName === "bible_news") {
    const random = new Uint32Array(1);
    crypto.getRandomValues(random);
    // Faixa reservada para a PWA, sempre positiva e compatível com Int do Android.
    return String(1_000_000 + (random[0] % 2_000_000_000));
  }
  return doc(collection(db(), collectionName)).id;
}
