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
 * Provision 的真实结果（对齐后端 `ProvisionReport`）。
 *
 * 旧口径这里写的是 `Promise<string>` / `Record<string,string>`，装的是**那条 DDL 文本**。
 * 而 `CREATE TABLE IF NOT EXISTS` 对一张已经在的表是空操作 —— 不报错和建成了是两件事
 * （缺陷 #43：两个实体抢同一张物理表时，服务端把没建成的列报成了成功）。
 * 所以判断成败只能看 `status`，`ddl` 只是给用户看"服务端尝试执行了什么"。
 *
 * `ALTERED` 是缺陷 #47 之后多出来的第四态：表本来就在、但定义跑到表前面了（加过栏），
 * 这一次真的执行了 `ADD COLUMN` 把它补上。它既不是"新建"也不是"什么都没做"，
 * 界面把这三态混成一句"表好好的"就又是第二句假话。
 */
export type ProvisionStatus = 'CREATED' | 'EXISTS_INTACT' | 'ALTERED' | 'FAILED';

export interface ProvisionItem {
  entityCode: string;
  tableName?: string;
  status: ProvisionStatus;
  ddl?: string;
  message?: string;
  missingColumns?: string[];
  /** 这一次真的被 ALTER 加出来的列。`status === 'ALTERED'` 时它不该为空。 */
  addedColumns?: string[];
}

export interface ProvisionReport {
  appCode?: string;
  total: number;
  created: number;
  unchanged: number;
  /** 补过列的表数。少了这一格，"3 张新表 + 2 张补了列"会被说成"3 张新表"。 */
  altered: number;
  failedCount: number;
  allOk: boolean;
  items: ProvisionItem[];
}

/**
 * Provision (create/alter) the physical table for one entity.
 *
 * `SchemaAdminController` maps this at `POST /admin/entity/provision?id=`; the
 * documented alias `POST /admin/app/entity/provision?id=` is attempted as a
 * fallback so either backend revision works.
 */
export async function provisionEntity(id: number): Promise<ProvisionItem> {
  try {
    return await request<ProvisionItem>('/admin/entity/provision', {
      method: 'POST',
      query: { id },
      silent: true,
    });
  } catch (err) {
    if (isApiError(err) && (err.status === 404 || err.status === 405)) {
      return request<ProvisionItem>('/admin/app/entity/provision', { method: 'POST', query: { id } });
    }
    throw err;
  }
}

/** Provision every entity of an app; one broken entity only reddens itself. */
export function provisionAllEntities(
  appCode: string,
  tenantCode: string = DEFAULT_TENANT_CODE,
): Promise<ProvisionReport> {
  return request<ProvisionReport>('/admin/app/provision-all', {
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
