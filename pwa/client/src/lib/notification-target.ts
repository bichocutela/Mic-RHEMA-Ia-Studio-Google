import { useEffect, useRef, useState } from "react";

export function useNotificationQuery() {
  const [query, setQuery] = useState(() => new URLSearchParams(window.location.search));
  useEffect(() => {
    const update = () => setQuery(new URLSearchParams(window.location.search));
    window.addEventListener("micrhema:notification-target", update);
    return () => window.removeEventListener("micrhema:notification-target", update);
  }, []);
  return query;
}
export function consumeNotificationTarget(...keys: string[]) {
  const url = new URL(window.location.href);
  keys.forEach(key => url.searchParams.delete(key));
  window.history.replaceState({}, "", url.pathname + url.search + url.hash);
}
/** Wait for live data, open each click once, and preserve normal Back navigation. */
export function useNotificationItem<T extends { id: string }>(items: T[], open: (item: T) => void) {
  const query = useNotificationQuery();
  const consumed = useRef<URLSearchParams | null>(null);
  useEffect(() => {
    const id = query.get("id");
    if (!id || consumed.current === query) return;
    const item = items.find(value => value.id === id);
    if (!item) return;
    consumed.current = query;
    consumeNotificationTarget("id", "type");
    open(item);
  }, [query, items, open]);
}
