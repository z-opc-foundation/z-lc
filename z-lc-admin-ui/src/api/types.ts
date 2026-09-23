/**
 * Typed mirror of the z-lc backend contract.
 *
 * Source of truth: `z-lc-common/src/main/java/com/zifang/z/lc/common/dto/*.java`
 * and `z-lc-web/.../controller/*.java`. Field names below match the Java DTOs
 * exactly (Jackson default naming = camelCase on the wire).
 */

/* ------------------------------------------------------------------ */
/* Envelope                                                            */
/* ------------------------------------------------------------------ */

/**
 * `com.zifang.util.core.meta.Result<T>`.
 *
 * IMPORTANT: the backend returns most business failures as HTTP 200 with
 * `success: false`. Never trust the HTTP status alone.
 */
export interface Result<T> {
  data: T | null;
  success: boolean;
  code: number;
  message: string | null;
}

/** `com.zifang.util.core.meta.page.PageResult<T>` — note `pageNum`, not `page`. */
export interface PageResult<T> {
  records: T[];
  total: number;
  pageNum: number;
  pageSize: number;
}

/* ------------------------------------------------------------------ */
/* Enums / unions derived from backend constants                       */
/* ------------------------------------------------------------------ */

/** `FieldDefDTO.fieldType`. Unknown values must be tolerated at runtime. */
export const FIELD_TYPES = [
  'STRING',
  'INT',
  'LONG',
  'DECIMAL',
  'BOOLEAN',
  'DATE',
  'DATETIME',
  'TEXT',
  'JSON',
  'REF',
] as const;
export type FieldType = (typeof FIELD_TYPES)[number];

/** How a cell value is physically represented in a grid row. */
export type CellValueType = 'String' | 'Number' | 'Boolean' | 'DateTime';

/** `ViewConfigDTO.viewType`. */
export const VIEW_TYPES = ['LIST', 'FORM', 'DETAIL', 'KANBAN', 'GALLERY', 'CALENDAR', 'CHART'] as const;
export type ViewType = (typeof VIEW_TYPES)[number];

/** `RelationDTO.relationType`. */
export const RELATION_TYPES = ['ONE_TO_MANY', 'MANY_TO_ONE', 'MANY_TO_MANY'] as const;
export type RelationType = (typeof RELATION_TYPES)[number];

/** `PipelineConfigEntity.triggerEvent` / `WorkflowBindingEntity.triggerEvent`. */
export const TRIGGER_EVENTS = [
  'AFTER_CREATE',
  'AFTER_UPDATE',
  'AFTER_DELETE',
  'BEFORE_CREATE',
  'BEFORE_UPDATE',
] as const;
export type TriggerEvent = (typeof TRIGGER_EVENTS)[number];

/** `DeploymentDTO.deployType`. */
export const DEPLOY_TYPES = ['HOT_LOAD', 'DOCKER', 'GIT_PUSH'] as const;
export type DeployType = (typeof DEPLOY_TYPES)[number];

/** `DeploymentDTO.status`. */
export const DEPLOYMENT_STATUSES = ['PENDING', 'RUNNING', 'SUCCESS', 'FAILED'] as const;
export type DeploymentStatus = (typeof DEPLOYMENT_STATUSES)[number];

/** `AppDTO.status` — values come from AppAdminService lifecycle transitions. */
export const APP_STATUSES = ['DRAFT', 'PUBLISHED', 'ARCHIVED'] as const;
export type AppStatus = (typeof APP_STATUSES)[number];

/**
 * Filter operators implemented by the backend.
 *
 * Two dialects exist and the UI must know both:
 *  1. LEGACY `filters` map — operator is encoded in the key suffix and
 *     `DynamicSqlBuilder.appendLegacyFilter` implements only
 *     eq/like/gt/gte/lt/lte/in, silently degrading anything else to `eq`.
 *     Every condition is ANDed.
 *  2. STRUCTURED `conditions` + `conjunction` — implements the full list below
 *     and honours AND/OR. This is what the grid sends.
 */
export const STRUCTURED_OPERATORS = [
  'eq',
  'ne',
  'like',
  'notLike',
  'gt',
  'gte',
  'lt',
  'lte',
  'in',
  'notIn',
  'isNull',
  'isNotNull',
] as const;
export type FilterOperator = (typeof STRUCTURED_OPERATORS)[number];

/** Operators that the legacy `filters` map can express. */
export const LEGACY_OPERATORS: readonly FilterOperator[] = ['eq', 'like', 'gt', 'gte', 'lt', 'lte', 'in'];

/**
 * `/meta/field-types` may advertise aliases (`notNull`) that the query builder
 * spells differently (`isNotNull`). Normalise so the wire never sees a
 * silent-fallback operator.
 */
const OPERATOR_ALIASES: Record<string, FilterOperator> = {
  eq: 'eq', equals: 'eq',
  ne: 'ne', neq: 'ne', notEq: 'ne',
  like: 'like', contains: 'like',
  notLike: 'notLike', notContains: 'notLike',
  gt: 'gt', gte: 'gte', ge: 'gte',
  lt: 'lt', lte: 'lte', le: 'lte',
  in: 'in', notIn: 'notIn',
  isNull: 'isNull', null: 'isNull',
  isNotNull: 'isNotNull', notNull: 'isNotNull', notEmpty: 'isNotNull',
};

export function normalizeOperator(raw: string | null | undefined): FilterOperator | null {
  if (!raw) return null;
  return OPERATOR_ALIASES[raw] ?? OPERATOR_ALIASES[raw.toLowerCase()] ?? null;
}

/** Operators taking no value. */
export const NULL_OPERATORS: readonly FilterOperator[] = ['isNull', 'isNotNull'];

export type Conjunction = 'AND' | 'OR';

export interface QueryCondition {
  fieldCode: string;
  operator: FilterOperator;
  value?: unknown;
}

export interface QuerySort {
  fieldCode: string;
  dir: 'asc' | 'desc';
}

/* ------------------------------------------------------------------ */
/* Runtime rows                                                        */
/* ------------------------------------------------------------------ */

/** A row from `/api/lc/runtime/list`. Keys are snake_case DB columns. */
export type LcRow = Record<string, unknown>;

/**
 * Filter map sent to the runtime list endpoint. The KEY carries the operator
 * suffix: `name`, `name:like`, `age:gt`, `status:in`.
 */
export type LcFilters = Record<string, string | number | boolean | null | (string | number)[]>;

export interface RuntimeQuery {
  page: number;
  size: number;
  /** Legacy operator-suffixed filter map; always ANDed server-side. */
  filters?: LcFilters;
  /** Structured conditions — use these together with `conjunction`. */
  conditions?: QueryCondition[];
  conjunction?: Conjunction;
  /** Multi-column sort; takes precedence over `orderBy` when present. */
  sorts?: QuerySort[];
  /** Single legacy clause, e.g. `"create_time desc"`. Default `id DESC`. */
  orderBy?: string;
}

export interface RuntimeCrudBody {
  entityCode: string;
  appCode: string;
  tenantCode: string;
  fieldValues: Record<string, unknown>;
}

/* ------------------------------------------------------------------ */
/* App / Entity / Field                                                */
/* ------------------------------------------------------------------ */

export interface AppDTO {
  id: number | null;
  tenantCode: string | null;
  appCode: string;
  appName: string;
  description?: string | null;
  icon?: string | null;
  status?: AppStatus | string | null;
  currentVersion?: number | null;
  entityCount?: number | null;
  fieldCount?: number | null;
  createTime?: string | number | null;
  updateTime?: string | number | null;
}

export interface AppCreateReq {
  tenantCode?: string;
  appCode: string;
  appName: string;
  description?: string;
  icon?: string;
}

export interface AppUpdateReq {
  id: number;
  appName: string;
  description?: string;
  icon?: string;
}

export interface FieldDefDTO {
  id?: number | null;
  tenantCode?: string | null;
  entityId?: number | null;
  fieldCode: string;
  fieldName: string;
  /** FieldType, but typed as string: the backend accepts anything and defaults to STRING. */
  fieldType: FieldType | string;
  required?: boolean | null;
  defaultValue?: string | null;
  dictCode?: string | null;
  refEntity?: string | null;
  fieldLength?: number | null;
  scale?: number | null;
  sortOrder?: number | null;
  description?: string | null;
}

export interface EntityDefDTO {
  id?: number | null;
  tenantCode?: string | null;
  appCode: string;
  entityCode: string;
  entityName: string;
  tableName?: string | null;
  description?: string | null;
  currentVersion?: number | null;
  fields?: FieldDefDTO[];
}

/* ------------------------------------------------------------------ */
/* Events                                                              */
/* ------------------------------------------------------------------ */

export interface EventAppendRequest {
  tenantCode: string;
  entityCode?: string;
  eventType: string;
  eventData?: string;
  source?: string;
  parentEventId?: string;
}

export interface EventDTO {
  id?: number | null;
  tenantCode?: string | null;
  eventId: string;
  appCode?: string | null;
  entityCode?: string | null;
  eventType: string;
  eventData?: string | null;
  source?: string | null;
  parentEventId?: string | null;
  applySeq?: number | null;
  applyTime?: string | number | null;
}

/* ------------------------------------------------------------------ */
/* Dict                                                                */
/* ------------------------------------------------------------------ */

export interface DictItemDTO {
  id?: number | null;
  tenantCode?: string | null;
  dictCode: string;
  itemCode: string;
  itemLabel: string;
  itemValue: string;
  sortOrder?: number | null;
  description?: string | null;
}

export interface DictDTO {
  id?: number | null;
  tenantCode?: string | null;
  dictCode: string;
  dictName: string;
  description?: string | null;
  status?: string | null;
  createTime?: string | number | null;
  updateTime?: string | number | null;
  items?: DictItemDTO[];
}

/* ------------------------------------------------------------------ */
/* View config / Relation                                              */
/* ------------------------------------------------------------------ */

export interface ViewConfigDTO {
  id?: number | null;
  entityCode: string;
  appCode: string;
  viewType: ViewType | string;
  /**
   * JSON *string* — must be parsed on read and stringified on write.
   * Nullable because the `config` column is a nullable TEXT and the Java DTO
   * is a plain `String`: a view row saved without config really does come back
   * as `null`, so callers must handle it.
   */
  config: string | null;
  tenantCode?: string | null;
  createTime?: string | number | null;
  updateTime?: string | number | null;
}

export interface ViewConfigCreateReq {
  entityCode: string;
  appCode: string;
  viewType: ViewType | string;
  config: string;
  tenantCode?: string;
}

export interface ViewConfigUpdateReq {
  id: number;
  viewType?: ViewType | string;
  config?: string;
}

export interface RelationDTO {
  id?: number | null;
  relationCode: string;
  relationName?: string | null;
  sourceEntityCode: string;
  targetEntityCode: string;
  relationType: RelationType | string;
  sourceFieldCode?: string | null;
  throughTable?: string | null;
  tenantCode?: string | null;
  appCode: string;
  createTime?: string | number | null;
  updateTime?: string | number | null;
}

export interface RelationCreateReq {
  relationCode: string;
  relationName?: string;
  sourceEntityCode: string;
  targetEntityCode: string;
  relationType: RelationType | string;
  sourceFieldCode?: string;
  throughTable?: string;
  appCode: string;
  tenantCode?: string;
}

export interface RelationUpdateReq extends RelationCreateReq {
  id: number;
}

/* ------------------------------------------------------------------ */
/* Pipeline / Workflow / Permission / Deployment                       */
/* ------------------------------------------------------------------ */

/** One element of the JSON-string `PipelineConfigEntity.stages` array. */
export interface PipelineStage {
  type: string;
  config?: Record<string, unknown>;
  order?: number;
}

export interface PipelineConfigEntity {
  id?: number | null;
  entityCode: string;
  appCode: string;
  triggerEvent: TriggerEvent | string;
  /** JSON-string array of {@link PipelineStage}. */
  stages: string;
  /** Integer flag, NOT a boolean: 0 | 1. */
  enabled: number;
  tenantCode?: string | null;
  createTime?: string | number | null;
  updateTime?: string | number | null;
  deleted?: number | null;
}

export interface WorkflowBindingEntity {
  id?: number | null;
  entityCode: string;
  appCode: string;
  triggerEvent: TriggerEvent | string;
  processDefinitionKey: string;
  autoSubmit?: number | null;
  tenantCode?: string | null;
  createTime?: string | number | null;
  updateTime?: string | number | null;
  deleted?: number | null;
}

export interface PermissionEntity {
  id?: number | null;
  appCode: string;
  entityCode: string;
  roleCode: string;
  permission: string;
  tenantCode?: string | null;
  createTime?: string | number | null;
}

export interface DeploymentDTO {
  id?: number | null;
  appCode: string;
  materializationId?: number | null;
  deployType: DeployType | string;
  status: DeploymentStatus | string;
  deployLog?: string | null;
  version?: string | null;
  tenantCode?: string | null;
  createTime?: string | number | null;
  updateTime?: string | number | null;
}

export interface DeploymentCreateReq {
  appCode: string;
  materializationId?: number | null;
  deployType: DeployType | string;
  version?: string;
  tenantCode?: string;
}

/* ------------------------------------------------------------------ */
/* Materialization — returns a RAW {success,data} map, not Result      */
/* ------------------------------------------------------------------ */

export interface MaterializationFile {
  path?: string;
  relativePath?: string;
  changeType?: 'ADDED' | 'MODIFIED' | 'DELETED' | string;
  size?: number;
  contentPreview?: string;
}

export interface MaterializationResp {
  id?: number | null;
  appCode?: string;
  status?: string;
  totalFiles?: number;
  generatedFiles?: MaterializationFile[];
  errorMessage?: string | null;
  startTime?: string | number | null;
  endTime?: string | number | null;
  [key: string]: unknown;
}

/** Raw envelope used only by `/api/lc/app/materialize*`. */
export interface RawSuccessData<T> {
  success: boolean;
  data: T | null;
}

/* ------------------------------------------------------------------ */
/* Meta endpoints (added by the backend agent in parallel)             */
/* ------------------------------------------------------------------ */

export interface FieldTypeDescriptor {
  fieldType: string;
  cellValueType: CellValueType;
  label: string;
  dbType: string;
  widget: string;
  sortable: boolean;
  groupable: boolean;
  filterable: boolean;
  inlineEditable: boolean;
  operators: string[];
}

export interface MetaBundle {
  app: AppDTO;
  entities: EntityDefDTO[];
  dicts: DictDTO[];
  views: ViewConfigDTO[];
  relations: RelationDTO[];
  fieldTypes: FieldTypeDescriptor[];
}

/* ------------------------------------------------------------------ */
/* Persisted view configuration (`ViewConfigDTO.config`, JSON string)  */
/* ------------------------------------------------------------------ */

/** Per-column user preference, kept separate from the schema itself. */
export interface ColumnMeta {
  fieldCode: string;
  /** Pixel width; absent means auto. */
  width?: number;
  hidden?: boolean;
  /** Pin to the leading edge of the grid. */
  fixed?: 'left';
}

/** What a LIST-type view config stores. */
export interface GridViewConfig {
  /** Display name of this saved view. */
  name: string;
  columns: ColumnMeta[];
  conditions: QueryCondition[];
  conjunction: Conjunction;
  sorts: QuerySort[];
  pageSize?: number;
  /** Row grouping field, when the user turned it on. */
  groupBy?: string;
}

export const DEFAULT_GRID_VIEW_CONFIG: GridViewConfig = {
  name: '全部记录',
  columns: [],
  conditions: [],
  conjunction: 'AND',
  sorts: [],
  pageSize: 20,
};

/** What a FORM-type view config stores (field order/visibility for the form). */
export interface FormViewConfig {
  name: string;
  /** Ordered list of visible field codes; absent fields are hidden. */
  fields: string[];
  /** Two-column layout spans, keyed by field code. */
  spans?: Record<string, number>;
}

/* ------------------------------------------------------------------ */
/* AI modeling — the backend accepts/returns plain maps                */
/* ------------------------------------------------------------------ */

export type JsonMap = Record<string, unknown>;
