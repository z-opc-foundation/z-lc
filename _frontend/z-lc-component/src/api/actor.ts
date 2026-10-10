/**
 * Stable identity used for change attribution (who wrote / who may undo).
 *
 * This is **attribution only** — z-lc runs in 直连 mode and does no authorization;
 * behind a gateway the real user id arrives as `X-User-Code` and should win.
 * Without either, we fall back to a per-browser persisted id so undo stacks are still
 * per-person rather than shared by everyone hitting the same entity.
 */
const STORAGE_KEY = 'zlc:actor';

let cached: string | null = null;

/** Override for a logged-in identity supplied by the host app / gateway. */
let override: string | null = null;

export function setActor(actor: string | null | undefined): void {
  const next = actor && actor.trim() ? actor.trim() : null;
  override = next;
  if (next) {
    try {
      window.localStorage.setItem(STORAGE_KEY, next);
    } catch {
      /* storage may be unavailable; the in-memory override still holds */
    }
  }
}

export function getActor(): string {
  if (override) {
    return override;
  }
  if (cached) {
    return cached;
  }
  let stored: string | null = null;
  try {
    stored = window.localStorage.getItem(STORAGE_KEY);
  } catch {
    stored = null;
  }
  if (stored && stored.trim()) {
    cached = stored.trim();
    return cached;
  }
  const generated = `web-${randomToken()}`;
  cached = generated;
  try {
    window.localStorage.setItem(STORAGE_KEY, generated);
  } catch {
    /* ephemeral id still works for this session */
  }
  return generated;
}

function randomToken(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID().slice(0, 12);
  }
  return Math.random().toString(36).slice(2, 10) + Date.now().toString(36);
}
