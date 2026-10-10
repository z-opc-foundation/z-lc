import { useQuery, keepPreviousData } from '@tanstack/react-query';
import { fetchWorkspaceMeta } from '@/lc/api/meta';
import { getApp } from '@/lc/api/app';
import { listRecords } from '@/lc/api/runtime';
import { createWorkspaceContext } from '@yuku123/render/fields';
import type { FieldServices } from '@yuku123/render/fields';
import { DEFAULT_TENANT_CODE } from '@/lc/api/client';

/**
 * 共享渲染引擎不持传输层：REF 选择器的候选记录由这里接回 z-lc 的运行时查询。
 * 关键字命中走 `like`，与 `DynamicSqlBuilder` 的 structured conditions 口径一致。
 */
const fieldServices: FieldServices = {
  loadReference: ({ entityCode, appCode, tenantCode, labelField, keyword, page, size }) =>
    listRecords(
      entityCode,
      { appCode, tenantCode },
      {
        page,
        size,
        conditions:
          keyword && labelField
            ? [{ fieldCode: labelField, operator: 'like' as const, value: keyword }]
            : [],
      },
    ).then((result) => result.records ?? []),
};

/**
 * Shared metadata loader for an app workspace.
 *
 * Backed by `/meta/bundle` when available and by the individual endpoints
 * otherwise (see `fetchWorkspaceMeta`). Because the same query key is used by
 * the layout, the grid and the form, React Query dedupes it into one request.
 */
export function useWorkspace(appCode: string | undefined, tenantCode = DEFAULT_TENANT_CODE) {
  const query = useQuery({
    queryKey: ['workspace', appCode, tenantCode],
    enabled: Boolean(appCode),
    placeholderData: keepPreviousData,
    staleTime: 20_000,
    queryFn: async () => {
      if (!appCode) throw new Error('appCode is required');
      const meta = await fetchWorkspaceMeta(appCode, tenantCode);
      // The bundle may omit `app`; the detail endpoint is a cheap complement.
      const app = meta.app ?? (await getApp(appCode).catch(() => null));
      return { ...meta, app };
    },
  });

  const context = appCode
    ? createWorkspaceContext({
        appCode,
        tenantCode,
        entities: query.data?.entities,
        dicts: query.data?.dicts,
        views: query.data?.views,
        fieldTypes: query.data?.fieldTypes,
        services: fieldServices,
      })
    : createWorkspaceContext({ appCode: appCode ?? '', tenantCode, services: fieldServices });

  return { ...query, meta: query.data, ctx: context };
}

/** Invalidate everything a workspace mutation can affect. */
export function workspaceInvalidations(appCode: string, tenantCode = DEFAULT_TENANT_CODE) {
  return [
    ['workspace', appCode, tenantCode],
    ['apps'],
    ['entity-schema', appCode],
  ];
}
