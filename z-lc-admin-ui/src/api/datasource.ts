import { request } from './client';
import type { EntityDefDTO } from './types';

/* ------------------------------------------------------------------ */
/* 数据源管理域（V1 孵化，2026-10-26）— /api/lc/admin/db/datasources     */
/* 目录来自 z-dms（lead 009/013 §六拍板）；只有非敏感元信息会透出。        */
/* ------------------------------------------------------------------ */

export interface DataSourceCatalogItem {
  id: string;
  name: string;
  /** z-dms 类型字面量：JDBC / MEMORY / CSV / HTTP / SQL */
  type: string;
  /** 凭据引用（<group>/<key>），不是口令 */
  credentialRef: string | null;
}

export interface DataSourceCatalog {
  /** z-dms 是否已装配（false 时目录为空，页面降级提示） */
  available: boolean;
  items: DataSourceCatalogItem[];
}

export function listDataSources(): Promise<DataSourceCatalog> {
  return request<DataSourceCatalog>('/admin/db/datasources');
}

/** 按数据源目录 id 扫表（dataSourceId 省略 = z-lc 主库，历史行为）。 */
export function scanDbTablesOn(
  dataSourceId: string,
  prefix = '',
  schema?: string,
): Promise<EntityDefDTO[]> {
  return request<EntityDefDTO[]>('/admin/db/tables', {
    query: { prefix, schema, dataSourceId },
  });
}

/** 按数据源目录 id 预览单表结构。 */
export function previewDbTableOn(
  dataSourceId: string,
  tableName: string,
  schema?: string,
): Promise<EntityDefDTO> {
  return request<EntityDefDTO>('/admin/db/table', {
    query: { tableName, schema, dataSourceId },
  });
}
