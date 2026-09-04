import {
  collection,
  limit as firestoreLimit,
  onSnapshot,
  orderBy,
  query,
  type DocumentData,
  type OrderByDirection,
} from "firebase/firestore";
import { firestore } from "./firebase";

function mapSnapshot<T extends DocumentData>(snapshot: Parameters<Parameters<typeof onSnapshot>[1]>[0]) {
  return snapshot.docs.map((item) => ({ id: item.id, ...item.data() }) as T & { id: string });
}

/**
 * Home da PWA: escuta somente a quantidade de documentos que realmente pode aparecer na tela.
 * As telas completas continuam usando as coleções integrais quando o usuário entra nelas.
 */
export function listenToLimitedCollection<T extends DocumentData>(
  collectionName: string,
  maxItems: number,
  onData: (items: Array<T & { id: string }>) => void,
  onError?: (error: Error) => void,
) {
  if (!firestore) return () => undefined;
  const source = query(collection(firestore, collectionName), firestoreLimit(Math.max(1, maxItems)));
  return onSnapshot(source, (snapshot) => onData(mapSnapshot<T>(snapshot)), (error) => onError?.(error));
}

/**
 * Preferência por itens recentes. Se a coleção antiga não possuir o campo de ordenação,
 * cai automaticamente para uma leitura limitada em vez de baixar a coleção inteira.
 */
export function listenToRecentCollection<T extends DocumentData>(
  collectionName: string,
  field: string,
  direction: OrderByDirection,
  maxItems: number,
  onData: (items: Array<T & { id: string }>) => void,
  onError?: (error: Error) => void,
) {
  if (!firestore) return () => undefined;
  let fallback: (() => void) | null = null;
  const source = query(
    collection(firestore, collectionName),
    orderBy(field, direction),
    firestoreLimit(Math.max(1, maxItems)),
  );
  const primary = onSnapshot(
    source,
    (snapshot) => onData(mapSnapshot<T>(snapshot)),
    (error) => {
      onError?.(error);
      if (!fallback) fallback = listenToLimitedCollection<T>(collectionName, maxItems, onData, onError);
    },
  );
  return () => {
    primary();
    fallback?.();
  };
}
