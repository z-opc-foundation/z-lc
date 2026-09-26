import { request } from './client';
import type { PermissionEntity } from './types';

export interface PermissionFilter {
  appCode: string;
  entityCode?: string;
  roleCode?: string;
}

/**
 * `GET /permission/list`.
 * NOTE: the backend gives `roleCode` precedence over `entityCode`, and falls
 * back to app-wide when neither is provided.
 */
export function listPermissions(filter: PermissionFilter): Promise<PermissionEntity[]> {
  return request<PermissionEntity[]>('/permission/list', { query: { ...filter } });
}

export function checkPermission(params: {
  appCode: string;
  /** 不给 = 只问「整个应用」这一档。给了则应用级授权也算覆盖 (与后端口径一致)。 */
  entityCode?: string;
  roleCode: string;
  permission: string;
}): Promise<boolean> {
  return request<boolean>('/permission/check', { query: params, silent: true });
}

export function grantPermission(payload: PermissionEntity): Promise<PermissionEntity> {
  return request<PermissionEntity>('/permission/grant', { method: 'POST', body: payload });
}

export function revokePermission(id: number): Promise<boolean> {
  return request<boolean>('/permission/revoke', { method: 'POST', body: { id } });
}

/** Permission verbs used by `PermissionService`. */
export const PERMISSION_KEYS = ['VIEW', 'CREATE', 'UPDATE', 'DELETE', 'EXPORT'] as const;
export type PermissionKey = (typeof PERMISSION_KEYS)[number];
