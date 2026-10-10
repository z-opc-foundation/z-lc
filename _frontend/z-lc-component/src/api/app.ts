import { DEFAULT_TENANT_CODE, request } from './client';
import type {
  AppCreateReq,
  AppDTO,
  AppUpdateReq,
  EntityDefDTO,
  EventAppendRequest,
  EventDTO,
} from './types';

/* ------------------------------------------------------------------ */
/* App lifecycle — /api/lc/app                                         */
/* ------------------------------------------------------------------ */

export function createApp(payload: AppCreateReq): Promise<AppDTO> {
  return request<AppDTO>('/app/create', { method: 'POST', body: payload });
}

export function updateApp(payload: AppUpdateReq): Promise<AppDTO> {
  return request<AppDTO>('/app/update', { method: 'POST', body: payload });
}

/** `POST /app/delete` takes `{appCode}` in the body and resolves the id server-side. */
export function deleteApp(appCode: string): Promise<boolean> {
  return request<boolean>('/app/delete', { method: 'POST', body: { appCode } });
}

/** Unpaged list (the backend pages internally at size 200). */
export function listApps(): Promise<AppDTO[]> {
  return request<AppDTO[]>('/app/list');
}

export function getApp(appCode: string): Promise<AppDTO> {
  return request<AppDTO>('/app/detail', { query: { appCode } });
}

export function publishApp(appCode: string): Promise<boolean> {
  return request<boolean>('/app/publish', { method: 'POST', body: { appCode } });
}

export function archiveApp(appCode: string): Promise<boolean> {
  return request<boolean>('/app/archive', { method: 'POST', body: { appCode } });
}

/**
 * Schema source of truth: replays the event chain into current entity defs.
 * Used by the runtime CRUD layer, so this is what the grid must trust.
 */
export function getAppSchema(
  appCode: string,
  tenantCode: string = DEFAULT_TENANT_CODE,
): Promise<EntityDefDTO[]> {
  return request<EntityDefDTO[]>('/app/schema', { query: { appCode, tenantCode } });
}

/* ------------------------------------------------------------------ */
/* Events — /api/lc/app/event                                          */
/* ------------------------------------------------------------------ */

/**
 * Append a schema event.
 *
 * CONTRACT QUIRK: `EventController.append` returns `Result<Result<EventDTO>>`.
 * The outer envelope is unwrapped by `request`; the inner one is unwrapped
 * here. A causal conflict is returned as outer-success + inner 409.
 */
/**
 * Append a schema event.
 *
 * The backend returns a single `Result` envelope, so `request()` already raises
 * `ApiError` (carrying `code`) on a causal conflict — do NOT expect a nested
 * Result; that double wrap used to report a rejected write as `success:true`.
 */
export async function appendEvent(
  appCode: string,
  payload: EventAppendRequest,
): Promise<EventDTO> {
  const event = await request<EventDTO>('/app/event', {
    method: 'POST',
    query: { appCode },
    body: payload,
  });
  if (!event) {
    throw new Error('事件追加成功但未返回事件体');
  }
  return event;
}

/**
 * Head of the event chain. `parentEventId` is required by the causal check, so
 * callers must read this before appending — otherwise every append conflicts.
 */
export function getLastEvent(appCode: string, tenantCode?: string): Promise<EventDTO | null> {
  return request<EventDTO | null>('/app/event/last', { query: { appCode, tenantCode } });
}
