/**
 * Tiny pub/sub so the transport layer stays UI-free.
 * `request()` publishes every failure; a React bridge component renders it.
 */
type Listener = (message: string, code: number) => void;

const listeners = new Set<Listener>();

export function subscribeToApiErrors(listener: Listener): () => void {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

export function publishApiError(message: string, code: number): void {
  for (const listener of listeners) {
    listener(message, code);
  }
}
