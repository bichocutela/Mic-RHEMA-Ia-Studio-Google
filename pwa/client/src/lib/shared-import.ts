export type SharedDocumentKind = "pdf" | "docx" | "epub";

export type PendingSharedDocument = {
  id: string;
  name: string;
  type: string;
  size: number;
  blob: Blob;
  kind: SharedDocumentKind;
  sharedTitle?: string;
  sharedText?: string;
  sharedUrl?: string;
  createdAt: number;
};

const DB_NAME = "mic-rhema-share-import";
const STORE_NAME = "pending";
const RECORD_ID = "latest";

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

export function sharedDocumentFile(document: PendingSharedDocument) {
  return new File([document.blob], document.name, { type: document.type || (document.kind === "pdf" ? "application/pdf" : document.kind === "epub" ? "application/epub+zip" : "application/vnd.openxmlformats-officedocument.wordprocessingml.document") });
}
