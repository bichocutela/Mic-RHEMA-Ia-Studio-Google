import { arrayRemove, arrayUnion, collection, deleteDoc, doc, getDocs, query, serverTimestamp, setDoc, where, writeBatch } from "firebase/firestore";
import { adminFirestore, firebaseAdminAuth } from "./firebase";

export async function saveDiscipuladoDocument(id: string, data: Record<string, unknown>) {
  const database = db();
  const scheduled = data.isPublished === false && Number(data.scheduledPublishAt) > 0;
  const target = scheduled ? "discipulado_schedules" : "discipulado_pdfs";
  const other = scheduled ? "discipulado_pdfs" : "discipulado_schedules";
  const batch = writeBatch(database);
  batch.set(doc(database, target, id), documentPayload(target, id, data), { merge: true });
  batch.delete(doc(database, other, id));
  await batch.commit();
  if (!scheduled && data.releaseNotificationPending === true) {
    // Publication is durable before the server sends the notification. The
    // persistent server queue retries if this immediate wake request fails.
    void (async () => {
      const token = await firebaseAdminAuth?.currentUser?.getIdToken();
      if (!token) return;
      const url = import.meta.env.VITE_SUPABASE_URL || "https://cwphbkdtorfpgmnlafqb.supabase.co";
      await fetch(`${url}/functions/v1/discipulado-release`, {
        method: "POST", headers: { "content-type": "application/json", authorization: `Bearer ${token}` },
        body: JSON.stringify({ action: "release" }), signal: AbortSignal.timeout(30000),
      });
    })().catch(() => undefined);
  }
}

/** Explicit administrative visibility, never inferred from editing content. */
export async function setDiscipuladoPublication(
  study: Record<string, unknown> & { id: string },
  publish: boolean,
) {
  const now = Date.now();
  await saveDiscipuladoDocument(study.id, {
    ...study,
    isPublished: publish,
    scheduledPublishAt: 0,
    releaseNotificationPending: publish,
    releaseNotificationState: publish ? "pending" : "",
    releaseNotificationLeaseUntil: "",
    publishedAt: publish ? now : Number(study.publishedAt || 0),
  });
}

export async function deleteDiscipuladoDocument(id: string) {
  const database = db();
  const batch = writeBatch(database);
  batch.delete(doc(database, "discipulado_pdfs", id));
  batch.delete(doc(database, "discipulado_schedules", id));
  await batch.commit();
}

function db() {
  if (!adminFirestore) throw new Error("Entre novamente como administrador.");
  return adminFirestore;
}

function normalizedDocumentId(id: string | number | null | undefined) {
  return String(id ?? "").trim() || crypto.randomUUID();
}

function normalizeMemberPhone(value: unknown) {
  const digits = String(value ?? "").replace(/\D/g, "");
  return digits.length >= 12 && digits.length <= 13 && digits.startsWith("55") ? digits.slice(2) : digits;
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

async function saveAdminMember(documentId: string, data: Record<string, unknown>) {
  const phone = normalizeMemberPhone(data.phone);
  const name = String(data.name ?? "").trim();
  if (!name) throw new Error("Informe o nome do membro.");
  if (phone.length < 10 || phone.length > 11) throw new Error("Informe um telefone válido com DDD.");

  for (const variant of [phone, `55${phone}`]) {
    const snapshot = await getDocs(query(collection(db(), "acessos_pendentes"), where("phone", "==", variant)));
    const collision = snapshot.docs.find((item) => item.id !== documentId && normalizeMemberPhone(item.data().phone) === phone);
    if (collision) throw new Error("Este telefone já pertence a outro cadastro.");
  }

  const now = Date.now();
  const accessPatch: Record<string, unknown> = {
    name,
    phone,
    email: String(data.email ?? "").trim(),
    address: String(data.address ?? "").trim(),
    birthDate: String(data.birthDate ?? "").trim(),
    isApproved: data.isApproved === true,
    isIbr: data.isIbr === true,
    isAdmin: data.isAdmin === true,
    status: data.isApproved === true ? "aprovado" : "pendente",
    updatedAt: now,
    updatedAtServer: serverTimestamp(),
    source: "pwa",
  };
  const profilePatch: Record<string, unknown> = {
    name,
    phone,
    email: String(data.email ?? "").trim(),
    address: String(data.address ?? "").trim(),
    birthDate: String(data.birthDate ?? "").trim(),
    updatedAt: now,
    updatedAtServer: serverTimestamp(),
    source: "pwa-admin",
  };

  if (typeof data.ibrCertificateName === "string" && data.ibrCertificateName.trim()) {
    accessPatch.ibrCertificateName = data.ibrCertificateName.trim();
    profilePatch.ibrCertificateName = data.ibrCertificateName.trim();
  }

  const batch = writeBatch(db());
  batch.set(doc(db(), "acessos_pendentes", documentId), accessPatch, { merge: true });
  batch.set(doc(db(), "users", documentId), profilePatch, { merge: true });
  await batch.commit();
  return documentId;
}

export async function saveAdminDocument(collectionName: string, id: string | number, data: Record<string, unknown>) {
  const documentId = normalizedDocumentId(id);
  if (collectionName === "acessos_pendentes") {
    return saveAdminMember(documentId, data);
  }

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
    return String(1_000_000 + (random[0] % 2_000_000_000));
  }
  return doc(collection(db(), collectionName)).id;
}
