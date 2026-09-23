import { useQuery, keepPreviousData } from '@tanstack/react-query';
import { fetchWorkspaceMeta } from '@/api/meta';
import { getApp } from '@/api/app';
import { createWorkspaceContext } from '@/fields';
import { DEFAULT_TENANT_CODE } from '@/api/client';

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
      })
    : createWorkspaceContext({ appCode: appCode ?? '', tenantCode });

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
