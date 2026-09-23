import { DEFAULT_TENANT_CODE, isMissingEndpoint, request } from './client';
import { getAppSchema } from './app';
import { listDicts } from './dict';
import { listViewConfigs } from './viewConfig';
import { listRelations } from './relation';
import type {
  AppDTO,
  DictDTO,
  EntityDefDTO,
  FieldTypeDescriptor,
  MetaBundle,
  RelationDTO,
  ViewConfigDTO,
} from './types';

/**
 * `GET /api/lc/meta/field-types`
 *
 * Not deployed yet — callers must treat a rejection as "unknown" and fall back
 * to {@link DEFAULT_FIELD_TYPES} so the UI keeps working.
 */
export function fetchFieldTypes(): Promise<FieldTypeDescriptor[]> {
  return request<FieldTypeDescriptor[]>('/meta/field-types', { silent: true });
}

/**
 * `GET /api/lc/meta/bundle?appCode=&tenantCode=`
 *
 * One round-trip for the whole workspace. Returns `null` (rather than throwing)
 * when the endpoint is missing or fails, because the workspace loader then
 * degrades to the individual endpoints.
 */
export async function fetchMetaBundle(
  appCode: string,
  tenantCode: string = DEFAULT_TENANT_CODE,
): Promise<MetaBundle | null> {
  try {
    return await request<MetaBundle>('/meta/bundle', {
      query: { appCode, tenantCode },
      silent: true,
    });
  } catch (err) {
    if (isMissingEndpoint(err)) return null;
    // Any other failure (500, validation) also degrades: the individual
    // endpoints give a more precise error surface per resource.
    return null;
  }
}

/** The resources a workspace needs, keyed as in {@link WorkspaceMeta.read}. */
export type MetaResource = 'entities' | 'dicts' | 'views' | 'relations' | 'fieldTypes';

/** Which `degraded` label corresponds to each resource. */
const DEGRADED_LABEL: Record<MetaResource, string> = {
  entities: 'schema',
  dicts: 'dicts',
  views: 'views',
  relations: 'relations',
  fieldTypes: 'field-types',
};

/** A workspace meta snapshot plus where it came from. */
export interface WorkspaceMeta {
  source: 'bundle' | 'fallback';
  app: AppDTO | null;
  entities: EntityDefDTO[];
  dicts: DictDTO[];
  views: ViewConfigDTO[];
  relations: RelationDTO[];
  fieldTypes: FieldTypeDescriptor[];
  /** Which individual endpoints failed while degrading. */
  degraded: string[];
  /**
   * Whether each resource was *actually read*. `degraded` alone is not enough:
   * an endpoint can resolve `success:true, data:null`, which `optional()` does
   * not record as a failure even though there is nothing to show.
   *
   * Pages need this to tell "no data" apart from "couldn't ask". Without it a
   * failed read looks exactly like an empty database, and the UI ends up
   * telling the user their views and entities were **deleted** — which is a
   * lie with consequences (they go looking for a delete they never did).
   */
  read: Record<MetaResource, boolean>;
}

/** Best-effort probe: never throws, returns `[]` when unavailable. */
async function optional<T>(label: string, p: Promise<T>, degraded: string[], fallback: T): Promise<T> {
  try {
    return await p;
  } catch {
    degraded.push(label);
    return fallback;
  }
}

function asArray<T>(value: T[] | null | undefined): T[] {
  return Array.isArray(value) ? value : [];
}

/**
 * "Did we actually read this resource?" — a rejection (recorded in `degraded`) and a
 * resolved non-array (an endpoint answering `success:true, data:null`) both mean no.
 */
function wasRead(degraded: string[], resource: MetaResource, value: unknown): boolean {
  return !degraded.includes(DEGRADED_LABEL[resource]) && Array.isArray(value);
}

/**
 * Why a resource came back unread, in words the user can act on. Pages use this so a
 * load failure never gets phrased as "the entity/view was deleted".
 */
export function unreadReason(meta: WorkspaceMeta | undefined, resource: MetaResource): string {
  if (!meta) return '元数据还没加载完';
  if (meta.read[resource]) return '';
  return meta.degraded.includes(DEGRADED_LABEL[resource])
    ? `失败接口: ${meta.degraded.join(', ')}`
    : `${DEGRADED_LABEL[resource]} 接口返回的不是列表`;
}

/**
 * Use what the bundle carried; only re-fetch a resource the bundle didn't deliver.
 *
 * `MetaController#bundle` wraps every sub-resource in its own try/catch + `log.warn`, so a
 * partially failing bundle still answers `success:true` with the broken key **missing**.
 * Treating "bundle worked" as "everything was read" is what let a views failure render as
 * "图表视图已被删除".
 */
async function bundledOr<T>(
  resource: MetaResource,
  value: T[] | null | undefined,
  load: () => Promise<T[]>,
  degraded: string[],
  treatEmptyAsMissing = false,
): Promise<T[]> {
  const present = Array.isArray(value) && !(treatEmptyAsMissing && value.length === 0);
  if (present) return value as T[];
  return optional<T[]>(DEGRADED_LABEL[resource], load(), degraded, []);
}

/**
 * Load everything the entity workspace needs.
 *
 * Strategy: try `/meta/bundle` first (1 request). If it is absent or returns
 * `success:false`, fan out to `/app/schema`, `/dict/list`, `/view-config/list`,
 * `/relation/list` and `/meta/field-types`, tolerating each failure
 * independently so one broken admin surface cannot blank the grid.
 */
export async function fetchWorkspaceMeta(
  appCode: string,
  tenantCode: string = DEFAULT_TENANT_CODE,
): Promise<WorkspaceMeta> {
  const degraded: string[] = [];
  const bundled = await fetchMetaBundle(appCode, tenantCode);
  if (bundled && Array.isArray(bundled.entities) && bundled.entities.length > 0) {
    const [dicts, views, relations, fieldTypes] = await Promise.all([
      bundledOr<DictDTO>('dicts', bundled.dicts, () => listDicts(appCode), degraded),
      bundledOr<ViewConfigDTO>('views', bundled.views, () => listViewConfigs(appCode), degraded),
      bundledOr<RelationDTO>('relations', bundled.relations, () => listRelations(appCode), degraded),
      bundledOr<FieldTypeDescriptor>(
        'fieldTypes',
        bundled.fieldTypes,
        fetchFieldTypes,
        degraded,
        true,
      ),
    ]);
    return {
      source: 'bundle',
      app: bundled.app ?? null,
      entities: bundled.entities,
      dicts,
      views,
      relations,
      fieldTypes,
      degraded,
      read: {
        // 只有 entities 是非空数组时才走这条分支，所以它必然是读到了。
        entities: true,
        dicts: wasRead(degraded, 'dicts', dicts),
        views: wasRead(degraded, 'views', views),
        relations: wasRead(degraded, 'relations', relations),
        fieldTypes: wasRead(degraded, 'fieldTypes', fieldTypes),
      },
    };
  }

  const entities = await optional<EntityDefDTO[]>(
    'schema',
    getAppSchema(appCode, tenantCode),
    degraded,
    [],
  );
  const [dicts, views, relations, fieldTypes] = await Promise.all([
    optional<DictDTO[]>('dicts', listDicts(appCode), degraded, []),
    optional<ViewConfigDTO[]>('views', listViewConfigs(appCode), degraded, []),
    optional<RelationDTO[]>('relations', listRelations(appCode), degraded, []),
    optional<FieldTypeDescriptor[]>('field-types', fetchFieldTypes(), degraded, []),
  ]);

  // `optional` only records *rejections*. An endpoint answering
  // `success:true, data:null` arrives as a resolved null, so normalize here
  // rather than letting every consumer guard `?.length` separately.
  const read: Record<MetaResource, boolean> = {
    entities: wasRead(degraded, 'entities', entities),
    dicts: wasRead(degraded, 'dicts', dicts),
    views: wasRead(degraded, 'views', views),
    relations: wasRead(degraded, 'relations', relations),
    fieldTypes: wasRead(degraded, 'fieldTypes', fieldTypes),
  };

  return {
    source: 'fallback',
    app: null,
    entities: asArray(entities),
    dicts: asArray(dicts),
    views: asArray(views),
    relations: asArray(relations),
    fieldTypes: asArray(fieldTypes),
    degraded,
    read,
  };
}
