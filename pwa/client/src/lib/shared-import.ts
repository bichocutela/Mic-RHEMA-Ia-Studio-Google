export type SharedDocumentKind = "pdf" | "docx" | "epub" | "unknown";

export type PendingSharedDocument = {
  id: string;
  name: string;
  type: string;
  size: number;
  blob: Blob | null;
  kind: SharedDocumentKind;
  sharedTitle?: string;
  sharedText?: string;
  sharedUrl?: string;
  createdAt: number;
};

const DB_NAME = "mic-rhema-share-import";
const STORE_NAME = "pending";
const RECORD_ID = "latest";
const MAX_BYTES = 50 * 1024 * 1024;

function openDb(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, 1);
    request.onupgradeneeded = () => {
      const db = request.result;
      if (!db.objectStoreNames.contains(STORE_NAME)) db.createObjectStore(STORE_NAME, { keyPath: "id" });
    };
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error || new Error("Não foi possível acessar o arquivo compartilhado."));
  });
}

function inferKind(value: string): SharedDocumentKind {
  const fingerprint = value.toLowerCase();
  if (fingerprint.includes("application/pdf") || /\.pdf(?:\b|$)/.test(fingerprint)) return "pdf";
  if (fingerprint.includes("application/epub+zip") || /\.epub(?:\b|$)/.test(fingerprint)) return "epub";
  if (fingerprint.includes("wordprocessingml") || fingerprint.includes("msword") || /\.docx(?:\b|$)/.test(fingerprint)) return "docx";
  return "unknown";
}

export async function loadPendingSharedDocument(): Promise<PendingSharedDocument | null> {
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const transaction = db.transaction(STORE_NAME, "readonly");
    const request = transaction.objectStore(STORE_NAME).get(RECORD_ID);
    request.onsuccess = () => resolve((request.result as PendingSharedDocument | undefined) || null);
    request.onerror = () => reject(request.error || new Error("Não foi possível ler o arquivo compartilhado."));
    transaction.oncomplete = () => db.close();
  });
}

export async function clearPendingSharedDocument() {
  const db = await openDb();
  await new Promise<void>((resolve, reject) => {
    const transaction = db.transaction(STORE_NAME, "readwrite");
    transaction.objectStore(STORE_NAME).delete(RECORD_ID);
    transaction.oncomplete = () => resolve();
    transaction.onerror = () => reject(transaction.error || new Error("Não foi possível limpar o compartilhamento."));
  });
  db.close();
}

export async function detectPendingSharedKind(document: PendingSharedDocument): Promise<SharedDocumentKind> {
  const immediate = document.kind !== "unknown" ? document.kind : inferKind(`${document.name} ${document.type} ${document.sharedTitle || ""} ${document.sharedText || ""} ${document.sharedUrl || ""}`);
  if (immediate !== "unknown") return immediate;
  const remote = document.sharedUrl?.trim();
  if (!remote) return "unknown";
  try {
    const response = await fetch(remote, { method: "HEAD", redirect: "follow", cache: "no-store" });
    const disposition = response.headers.get("content-disposition") || "";
    return inferKind(`${response.headers.get("content-type") || ""} ${disposition} ${response.url}`);
  } catch {
    return "unknown";
  }
}

function mimeForKind(kind: Exclude<SharedDocumentKind, "unknown">) {
  return kind === "pdf" ? "application/pdf" : kind === "epub" ? "application/epub+zip" : "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
}

export async function sharedDocumentFile(document: PendingSharedDocument, forcedKind?: Exclude<SharedDocumentKind, "unknown">) {
  const kind = forcedKind || await detectPendingSharedKind(document);
  if (kind === "unknown") throw new Error("Não foi possível identificar se o arquivo é PDF, EPUB ou Word (.docx).");
  if (document.blob && document.blob.size > 0) {
    if (document.blob.size > MAX_BYTES) throw new Error("O arquivo compartilhado ultrapassa 50 MB.");
    return new File([document.blob], document.name || `material.${kind}`, { type: document.type || mimeForKind(kind) });
  }
  const remote = document.sharedUrl?.trim();
  if (!remote) throw new Error("O compartilhamento não trouxe um arquivo nem um link utilizável.");
  const response = await fetch(remote, { redirect: "follow", cache: "no-store" });
  if (!response.ok) throw new Error(`Não foi possível copiar o arquivo compartilhado (HTTP ${response.status}).`);
  const blob = await response.blob();
  if (blob.size <= 0 || blob.size > MAX_BYTES) throw new Error("O arquivo compartilhado está vazio ou ultrapassa 50 MB.");
  const extension = kind === "docx" ? "docx" : kind;
  const baseName = document.name && /\.(pdf|docx|epub)$/i.test(document.name) ? document.name : `material-compartilhado.${extension}`;
  return new File([blob], baseName, { type: blob.type || mimeForKind(kind) });
}
