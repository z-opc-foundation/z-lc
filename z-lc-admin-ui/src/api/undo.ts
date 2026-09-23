import { DEFAULT_TENANT_CODE, request } from './client';
import { getActor } from './actor';

export interface UndoOutcome {
  applied: boolean;
  changeId: number | null;
  operation: string | null;
  recordId: number | null;
  message: string | null;
}

export interface ChangeEntry {
  id: number;
  entityCode: string;
  recordId: number;
  operation: 'CREATE' | 'UPDATE' | 'DELETE' | string;
  /** 改动归属; 后端未拿到身份时为 anonymous. */
  actor: string | null;
  createTime: string | null;
  undone: boolean;
  /** JSON string of the row before the change (null for CREATE). */
  beforeImage: string | null;
  /** JSON string of the row after the change (null for DELETE). */
  afterImage: string | null;
}

function scope(appCode: string, tenantCode?: string, opts?: { allUsers?: boolean }) {
  return {
    appCode,
    tenantCode: tenantCode ?? DEFAULT_TENANT_CODE,
    // 默认后端只让撤自己写的; 显式 allUsers 才跨人撤销
    scope: opts?.allUsers ? 'all' : 'mine',
    actor: getActor(),
  };
}

/**
 * Undo the most recent not-yet-undone change in this entity's scope.
 * Server-side: the backend replays the stored before-image, it does not guess.
 */
export function undoLast(
  entityCode: string,
  appCode: string,
  tenantCode?: string,
  allUsers = false,
): Promise<UndoOutcome> {
  return request<UndoOutcome>('/undo/undo', {
    method: 'POST',
    query: { entityCode },
    body: scope(appCode, tenantCode, { allUsers }),
  });
}

/** Redo the most recent undone change. */
export function redoLast(
  entityCode: string,
  appCode: string,
  tenantCode?: string,
  allUsers = false,
): Promise<UndoOutcome> {
  return request<UndoOutcome>('/undo/redo', {
    method: 'POST',
    query: { entityCode },
    body: scope(appCode, tenantCode, { allUsers }),
  });
}

/** Change log, newest first. */
export function changeHistory(
  appCode: string,
  entityCode?: string,
  tenantCode?: string,
  limit = 50,
  owner?: 'mine' | 'all',
): Promise<ChangeEntry[]> {
  return request<ChangeEntry[]>('/undo/history', {
    query: {
      appCode,
      entityCode,
      tenantCode: tenantCode ?? DEFAULT_TENANT_CODE,
      limit,
      owner,
    },
  });
}

/** Safe parse for the JSON-string snapshots. */
export function parseImage(raw: string | null): Record<string, unknown> {
  if (!raw) return {};
  try {
    return JSON.parse(raw) as Record<string, unknown>;
  } catch {
    return {};
  }
}
