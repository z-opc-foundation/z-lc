import { DEFAULT_TENANT_CODE, requestRaw } from './client';
import type { MaterializationResp } from './types';

/**
 * Code-materialization endpoints.
 *
 * CONTRACT QUIRK: `MaterializationController` returns a raw `{success,data}`
 * map rather than the `Result` envelope used everywhere else, hence the
 * dedicated `requestRaw` transport.
 */

export function triggerMaterialization(
  appCode: string,
  payload: Jsonish = {},
  tenantCode: string = DEFAULT_TENANT_CODE,
): Promise<MaterializationResp> {
  return requestRaw<MaterializationResp>('/app/materialize', {
    method: 'POST',
    query: { appCode, tenantCode },
    body: payload,
  });
}

export function getMaterializationStatus(id: number, appCode?: string): Promise<MaterializationResp> {
  return requestRaw<MaterializationResp>('/app/materialize/status', { query: { id, appCode } });
}

export function listMaterializations(appCode: string, limit = 20): Promise<MaterializationResp[]> {
  return requestRaw<MaterializationResp[]>('/app/materialize/list', { query: { appCode, limit } });
}

export function getMaterializationDiff(id: number, appCode?: string): Promise<unknown[]> {
  return requestRaw<unknown[]>('/app/materialize/diff', { query: { id, appCode } });
}

type Jsonish = Record<string, unknown>;
