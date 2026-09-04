import { firebaseAuth } from "./firebase";

const supabaseUrl = import.meta.env.VITE_SUPABASE_URL || "https://cwphbkdtorfpgmnlafqb.supabase.co";
const gateway = `${supabaseUrl}/functions/v1/storage-gateway`;

async function authToken() {
  const user = firebaseAuth?.currentUser;
  if (!user) throw new Error("Entre novamente como administrador para gerenciar arquivos.");
  return { uid: user.uid, token: await user.getIdToken() };
}

export async function uploadAdminMedia(file: File) {
  const { uid, token } = await authToken();
  const form = new FormData();
  form.append("file", file);
  form.append("bucket", "media-assets");
  form.append("targetUid", uid);
  const response = await fetch(gateway, { method: "POST", headers: { authorization: `Bearer ${token}` }, body: form });
  const payload = await response.json().catch(() => ({}));
  if (!response.ok || payload.ok !== true || !payload.signed_url) throw new Error(payload.error || "Não foi possível enviar o arquivo ao Supabase.");
  return { url: String(payload.signed_url), storagePath: String(payload.storage_path || "") };
}

function descriptorFromUrl(value: string) {
  const marker = "/storage/v1/object/public/media-assets/";
  const index = value.indexOf(marker);
  if (index < 0) return null;
  const objectPath = decodeURIComponent(value.slice(index + marker.length).split(/[?#]/)[0]);
  const targetUid = objectPath.split("/")[0];
  return objectPath && targetUid ? { targetUid, storagePath: `media-assets/${objectPath}` } : null;
}

function descriptorFromStoragePath(value: string) {
  const raw = value.trim();
  if (!raw.startsWith("media-assets/")) return null;
  const objectPath = raw.slice("media-assets/".length);
  const targetUid = objectPath.split("/")[0];
  return objectPath && targetUid ? { targetUid, storagePath: raw } : null;
}

export async function deleteAdminStoredAsset(value: string) {
  const descriptor = descriptorFromUrl(value) || descriptorFromStoragePath(value);
  if (!descriptor) return false;
  const { token } = await authToken();
  const response = await fetch(gateway, {
    method: "POST",
    headers: { "content-type": "application/json", authorization: `Bearer ${token}` },
    body: JSON.stringify({ operation: "delete", bucket: "media-assets", targetUid: descriptor.targetUid, storagePath: descriptor.storagePath }),
  });
  const payload = await response.json().catch(() => ({}));
  if (!response.ok || payload.ok !== true) throw new Error(payload.error || "Não foi possível excluir o arquivo do Supabase.");
  return true;
}

function collectStrings(value: unknown, output: Set<string>) {
  if (typeof value === "string") { output.add(value); return; }
  if (Array.isArray(value)) { value.forEach((item) => collectStrings(item, output)); return; }
  if (value && typeof value === "object") Object.values(value as Record<string, unknown>).forEach((item) => collectStrings(item, output));
}

/** Remove apenas objetos reconhecidos como media-assets do MIC Rhema. Links externos ficam intactos. */
export async function deleteAdminStoredAssetsFromDocument(document: Record<string, unknown>) {
  const strings = new Set<string>(); collectStrings(document, strings);
  const candidates = [...strings].filter((value) => descriptorFromUrl(value) || descriptorFromStoragePath(value));
  const unique = [...new Set(candidates)];
  for (const value of unique) await deleteAdminStoredAsset(value);
  return unique.length;
}
