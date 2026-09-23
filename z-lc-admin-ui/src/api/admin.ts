import { DEFAULT_TENANT_CODE, isApiError, request } from './client';
import type { AppDTO, EntityDefDTO, PageResult } from './types';

/* ------------------------------------------------------------------ */
/* Apps (schema admin) — /api/lc/admin/app                             */
/* ------------------------------------------------------------------ */

export function listAdminApps(
  page = 1,
  size = 20,
  tenantCode: string = DEFAULT_TENANT_CODE,
): Promise<PageResult<AppDTO>> {
  return request<PageResult<AppDTO>>('/admin/app/list', {
    method: 'POST',
    query: { page, size, tenantCode },
  });
}

/** Second create path (`/admin/app/create` takes a full `AppDTO`). */
export function adminCreateApp(payload: Partial<AppDTO>): Promise<AppDTO> {
  return request<AppDTO>('/admin/app/create', { method: 'POST', body: payload });
}

export function adminGetApp(id: number): Promise<AppDTO> {
  return request<AppDTO>('/admin/app', { query: { id } });
}

export function adminUpdateApp(id: number, payload: Partial<AppDTO>): Promise<number> {
  return request<number>('/admin/app', { method: 'PUT', query: { id }, body: payload });
}

export function adminDeleteApp(id: number): Promise<number> {
  return request<number>('/admin/app', { method: 'DELETE', query: { id } });
}

/* ------------------------------------------------------------------ */
/* Entities — /api/lc/admin/{app/entity,entity}                        */
/* ------------------------------------------------------------------ */

export function createEntity(
  appCode: string,
  payload: EntityDefDTO,
  tenantCode: string = DEFAULT_TENANT_CODE,
): Promise<EntityDefDTO> {
  return request<EntityDefDTO>('/admin/app/entity/create', {
    method: 'POST',
    query: { appCode, tenantCode },
    body: payload,
  });
}

export function listAdminEntities(
  appCode: string,
  tenantCode: string = DEFAULT_TENANT_CODE,
): Promise<EntityDefDTO[]> {
  return request<EntityDefDTO[]>('/admin/app/entity/list', { query: { appCode, tenantCode } });
}

export function getEntity(id: number): Promise<EntityDefDTO> {
  return request<EntityDefDTO>('/admin/entity', { query: { id } });
}

export function updateEntity(id: number, payload: EntityDefDTO): Promise<number> {
  return request<number>('/admin/entity', { method: 'PUT', query: { id }, body: payload });
}

export function deleteEntity(id: number): Promise<number> {
  return request<number>('/admin/entity', { method: 'DELETE', query: { id } });
}

/**
 * Provision (create/alter) the physical table for one entity, returning the
 * executed DDL.
 *
 * `SchemaAdminController` maps this at `POST /admin/entity/provision?id=`; the
 * documented alias `POST /admin/app/entity/provision?id=` is attempted as a
 * fallback so either backend revision works.
 */
export async function provisionEntity(id: number): Promise<string> {
  try {
    return await request<string>('/admin/entity/provision', {
      method: 'POST',
      query: { id },
      silent: true,
    });
  } catch (err) {
    if (isApiError(err) && (err.status === 404 || err.status === 405)) {
      return request<string>('/admin/app/entity/provision', { method: 'POST', query: { id } });
    }
    throw err;
  }
}

/** Provision every entity of an app. Key = entityCode, value = DDL. */
export function provisionAllEntities(
  appCode: string,
  tenantCode: string = DEFAULT_TENANT_CODE,
): Promise<Record<string, string>> {
  return request<Record<string, string>>('/admin/app/provision-all', {
    method: 'POST',
    query: { appCode, tenantCode },
  });
}

/* ------------------------------------------------------------------ */
/* Reverse claiming — /api/lc/admin/db                                 */
/* ------------------------------------------------------------------ */

export function scanDbTables(prefix = '', schema?: string): Promise<EntityDefDTO[]> {
  return request<EntityDefDTO[]>('/admin/db/tables', { query: { prefix, schema } });
}

export function previewDbTable(tableName: string, schema?: string): Promise<EntityDefDTO> {
  return request<EntityDefDTO>('/admin/db/table', { query: { tableName, schema } });
}

export function importDbTable(params: {
  tableName: string;
  appCode: string;
  tenantCode?: string;
  schema?: string;
  entityCode?: string;
  autoProvision?: boolean;
}): Promise<EntityDefDTO> {
  return request<EntityDefDTO>('/admin/db/table/import', {
    method: 'POST',
    query: {
      tableName: params.tableName,
      tenantCode: params.tenantCode ?? DEFAULT_TENANT_CODE,
      appCode: params.appCode,
      schema: params.schema,
      entityCode: params.entityCode,
      autoProvision: params.autoProvision ?? false,
    },
  });
}

export function batchImportDbTables(params: {
  appCode: string;
  prefix: string;
  tenantCode?: string;
  schema?: string;
  autoProvision?: boolean;
}): Promise<EntityDefDTO[]> {
  return request<EntityDefDTO[]>('/admin/db/tables/batch-import', {
    method: 'POST',
    query: {
      tenantCode: params.tenantCode ?? DEFAULT_TENANT_CODE,
      appCode: params.appCode,
      prefix: params.prefix,
      schema: params.schema,
      autoProvision: params.autoProvision ?? false,
    },
  });
}
