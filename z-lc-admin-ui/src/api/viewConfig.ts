import { DEFAULT_TENANT_CODE, request } from './client';
import { queryClient } from '@/queryClient';
import type { ViewConfigDTO } from './types';
import type { ViewConfigCreateReq, ViewConfigUpdateReq } from './types';

export function listViewConfigs(appCode: string, entityCode?: string): Promise<ViewConfigDTO[]> {
  return request<ViewConfigDTO[]>('/view-config/list', { query: { appCode, entityCode } });
}

/**
 * 写完视图配置一定要让 workspace 缓存失效：实体工作区的「视图」下拉读的是
 * `meta.views`，而 `useWorkspace` 的 staleTime 是 20s 且关了窗口聚焦重拉 ——
 * 不失效的话"已保存"的 toast 报了、下拉框里却没有，得手动刷新页面才出现。
 * 放在 api 层而不是各调用点，是因为写这一资源的入口有三个（表格保存视图、
 * 图表保存视图、管理页增删改），漏一个就是一个"看着存下了其实没生效"。
 */
async function afterWrite<T>(promise: Promise<T>): Promise<T> {
  const result = await promise;
  await queryClient.invalidateQueries({ queryKey: ['workspace'] });
  return result;
}

export function createViewConfig(payload: ViewConfigCreateReq): Promise<ViewConfigDTO> {
  return afterWrite(request<ViewConfigDTO>('/view-config/create', { method: 'POST', body: payload }));
}

export function updateViewConfig(payload: ViewConfigUpdateReq): Promise<ViewConfigDTO> {
  return afterWrite(request<ViewConfigDTO>('/view-config/update', { method: 'POST', body: payload }));
}

export function deleteViewConfig(id: number): Promise<boolean> {
  return afterWrite(request<boolean>('/view-config/delete', { method: 'POST', body: { id } }));
}

export function newViewConfigDraft(params: {
  appCode: string;
  entityCode: string;
  viewType: string;
  config: unknown;
  tenantCode?: string;
}): ViewConfigCreateReq {
  return {
    appCode: params.appCode,
    entityCode: params.entityCode,
    viewType: params.viewType,
    // `config` is transported as a JSON *string*.
    config: JSON.stringify(params.config),
    tenantCode: params.tenantCode ?? DEFAULT_TENANT_CODE,
  };
}

/** Parse `ViewConfigDTO.config`, returning `fallback` when it is not valid JSON. */
export function parseViewConfig<T>(raw: string | null | undefined, fallback: T): T {
  if (!raw) return fallback;
  try {
    return JSON.parse(raw) as T;
  } catch {
    return fallback;
  }
}
