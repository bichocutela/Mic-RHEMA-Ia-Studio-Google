import { arrayRemove, arrayUnion, collection, deleteDoc, doc, serverTimestamp, setDoc, writeBatch } from "firebase/firestore";
import { firestore } from "./firebase";

function db() {
  if (!firestore) throw new Error("Firebase indisponível nesta PWA.");
  return firestore;
}

function normalizedDocumentId(id: string | number | null | undefined) {
  return String(id ?? "").trim() || crypto.randomUUID();
}

function numericNewsId(documentId: string) {
  if (!/^\d+$/.test(documentId)) return null;
  const numeric = Number(documentId);
  return Number.isSafeInteger(numeric) && numeric > 0 && numeric <= 2_147_483_647 ? numeric : null;
}

function storedId(collectionName: string, documentId: string) {
  return collectionName === "bible_news" ? (numericNewsId(documentId) ?? documentId) : documentId;
}

function documentPayload(collectionName: string, documentId: string, data: Record<string, unknown>) {
  return {
    ...data,
    id: storedId(collectionName, documentId),
    updatedAt: Date.now(),
    updatedAtServer: serverTimestamp(),
    source: "pwa",
  };
}

export async function saveAdminDocument(collectionName: string, id: string | number, data: Record<string, unknown>) {
  const documentId = normalizedDocumentId(id);
  const payload = documentPayload(collectionName, documentId, data);
  const numericId = collectionName === "bible_news" ? numericNewsId(documentId) : null;
  if (numericId != null) {
    const batch = writeBatch(db());
    batch.set(doc(db(), collectionName, documentId), payload, { merge: true });
    batch.set(doc(db(), "settings", "bible_news_editorial"), { hiddenIds: arrayRemove(numericId) }, { merge: true });
    await batch.commit();
  } else {
    await setDoc(doc(db(), collectionName, documentId), payload, { merge: true });
  }
  return documentId;
}

export async function replaceAdminDocument(collectionName: string, id: string | number, data: Record<string, unknown>) {
  const documentId = normalizedDocumentId(id);
  const payload = documentPayload(collectionName, documentId, data);
  const numericId = collectionName === "bible_news" ? numericNewsId(documentId) : null;
  if (numericId != null) {
    const batch = writeBatch(db());
    batch.set(doc(db(), collectionName, documentId), payload);
    batch.set(doc(db(), "settings", "bible_news_editorial"), { hiddenIds: arrayRemove(numericId) }, { merge: true });
    await batch.commit();
  } else {
    await setDoc(doc(db(), collectionName, documentId), payload);
  }
  return documentId;
}

export async function deleteAdminDocument(collectionName: string, id: string | number) {
  const documentId = normalizedDocumentId(id);
  const numericId = collectionName === "bible_news" ? numericNewsId(documentId) : null;
  if (numericId != null) {
    const batch = writeBatch(db());
    batch.set(doc(db(), "settings", "bible_news_editorial"), { hiddenIds: arrayUnion(numericId) }, { merge: true });
    batch.delete(doc(db(), collectionName, documentId));
    await batch.commit();
    return;
  }
  await deleteDoc(doc(db(), collectionName, documentId));
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
