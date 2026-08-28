import { collection, deleteDoc, doc, serverTimestamp, setDoc } from "firebase/firestore";
import { firestore } from "./firebase";

function db() {
  if (!firestore) throw new Error("Firebase indisponível nesta PWA.");
  return firestore;
}

export async function saveAdminDocument(collectionName: string, id: string, data: Record<string, unknown>) {
  const documentId = id.trim() || crypto.randomUUID();
  await setDoc(doc(db(), collectionName, documentId), {
    ...data,
    id: documentId,
    updatedAt: Date.now(),
    updatedAtServer: serverTimestamp(),
    source: "pwa",
  }, { merge: true });
  return documentId;
}

export async function replaceAdminDocument(collectionName: string, id: string, data: Record<string, unknown>) {
  const documentId = id.trim() || crypto.randomUUID();
  await setDoc(doc(db(), collectionName, documentId), {
    ...data,
    id: documentId,
    updatedAt: Date.now(),
    updatedAtServer: serverTimestamp(),
    source: "pwa",
  });
  return documentId;
}

export async function deleteAdminDocument(collectionName: string, id: string) {
  await deleteDoc(doc(db(), collectionName, id));
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
  return doc(collection(db(), collectionName)).id;
}
