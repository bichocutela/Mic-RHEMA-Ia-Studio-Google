type Observer<T> = { data: (value: T) => void; error?: (error: Error) => void };
type Subscription<T> = {
  observers: Set<Observer<T>>;
  stop: () => void;
  timer?: ReturnType<typeof setTimeout>;
  latest?: T;
  hasValue: boolean;
};

/** Reuse a live SDK subscription during navigation; keys must include app and uid. */
export class ListenerPool<T> {
  private readonly entries = new Map<string, Subscription<T>>();
  private readonly graceMillis: number;
  constructor(graceMillis = 30_000) { this.graceMillis = graceMillis; }

  subscribe(
    key: string,
    start: (data: (value: T) => void, error: (error: Error) => void) => () => void,
    data: (value: T) => void,
    error?: (error: Error) => void,
  ): () => void {
    const observer = { data, error };
    let entry = this.entries.get(key);
    if (!entry) {
      entry = { observers: new Set(), stop: () => {}, hasValue: false };
      this.entries.set(key, entry);
      const subscription = entry;
      subscription.observers.add(observer);
      try {
        subscription.stop = start(
          value => {
            subscription.latest = value;
            subscription.hasValue = true;
            for (const listener of subscription.observers) listener.data(value);
          },
          failure => {
            // Permission errors must never replay a previously authorized result.
            subscription.hasValue = false;
            subscription.latest = undefined;
            this.entries.delete(key);
            if (subscription.timer) clearTimeout(subscription.timer);
            for (const listener of subscription.observers) listener.error?.(failure);
          },
        );
      } catch (failure) {
        this.entries.delete(key);
        throw failure;
      }
    } else {
      if (entry.timer) clearTimeout(entry.timer);
      entry.timer = undefined;
      entry.observers.add(observer);
      if (entry.hasValue) data(entry.latest as T);
    }
    const subscription = entry;
    let removed = false;
    return () => {
      if (removed) return;
      removed = true;
      subscription.observers.delete(observer);
      if (!subscription.observers.size) {
        subscription.timer = setTimeout(() => {
          subscription.stop();
          if (this.entries.get(key) === subscription) this.entries.delete(key);
        }, this.graceMillis);
      }
    };
  }
}
