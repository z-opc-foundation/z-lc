import { request } from './client';
import type { JsonMap } from './types';

/** `GET /api/lc/health` — reachability probe used by the console status pill. */
export function getHealth(): Promise<JsonMap> {
  return request<JsonMap>('/health', { silent: true });
}

export function listAdapters(): Promise<JsonMap[]> {
  return request<JsonMap[]>('/health/adapters', { silent: true });
}

export function pingMeta(): Promise<boolean> {
  return request<boolean>('/health/meta-ping', { silent: true });
}

export function pingCtc(): Promise<boolean> {
  return request<boolean>('/health/ctc-ping', { silent: true });
}
