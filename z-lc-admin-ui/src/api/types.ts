/**
 * Typed mirror of the z-lc backend contract.
 *
 * Source of truth: `z-lc-common/src/main/java/com/zifang/z/lc/common/dto/*.java`
 * and `z-lc-web/.../controller/*.java`. Field names below match the Java DTOs
 * exactly (Jackson default naming = camelCase on the wire).
 */

/* ------------------------------------------------------------------ */
/* 字段契约 —— 权威定义在 @yuku123/render/fields（共享渲染引擎）          */
/*                                                                    */
/* 字段类型注册表搬进引擎后，DTO 与算子词表跟着走，避免两边各留一份长歪。   */
/* 本文件只做再导出，其余 z-lc 专属 DTO 仍然留在这里。                    */
/* ------------------------------------------------------------------ */

export {
  FIELD_TYPES,
  LEGACY_OPERATORS,
  NULL_OPERATORS,
  STRUCTURED_OPERATORS,
  normalizeOperator,
} from '@yuku123/render/fields';
export type {
  CellValueType,
  Conjunction,
  DictDTO,
  DictItemDTO,
  EntityDefDTO,
  FieldDefDTO,
  FieldType,
  FieldTypeDescriptor,
  FilterOperator,
  LcRow,
  QueryCondition,
  QuerySort,
  ViewConfigDTO,
} from '@yuku123/render/fields';

// 本文件下方的 z-lc 专属 DTO 仍要引用这几个形状；`export ... from` 不产生本地绑定，得单独 import。
import type {
  Conjunction,
  DictDTO,
  EntityDefDTO,
  FieldTypeDescriptor,
  QueryCondition,
  QuerySort,
  ViewConfigDTO,
} from '@yuku123/render/fields';

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

/**
 * `ViewConfigDTO.viewType` —— 视图词表暂时留在本仓：@yuku123/render 1.0.1 的 VIEW_TYPES
 * 还没有 PIVOT（交叉表是 z-lc 后长出来的视图），本仓的 VIEW_TABS/activeView 都按
 * `ViewType` 收窄比较，改用引擎的会直接把 typecheck 打断。
 * 等引擎 contract 补上 PIVOT 后，这两行跟着 FIELD_TYPES 一起再导出。
 */
export const VIEW_TYPES = ['LIST', 'FORM', 'DETAIL', 'KANBAN', 'GALLERY', 'CALENDAR', 'CHART', 'PIVOT'] as const;
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

/**
 * `DeploymentDTO.deployType` 的取值<b>不在前端钉</b>（缺陷 #70）：原先这里抄了一份三种方式的清单，
 * 而服务器一种都不执行。可选清单的唯一来源改成 `/deployment/vocabulary`
 * （服务器那一份 `DeploymentTypes.java`），见 {@link DeploymentVocabulary}。
 */
export type DeployType = string;

/** `/deployment/vocabulary`：服务器会执行哪几种、不会执行哪几种以及为什么。 */
export interface DeploymentVocabulary {
  executable: string[];
  rejected: { type: string; reason: string }[];
}

/** `DeploymentDTO.status`. */
export const DEPLOYMENT_STATUSES = ['PENDING', 'RUNNING', 'SUCCESS', 'FAILED'] as const;
export type DeploymentStatus = (typeof DEPLOYMENT_STATUSES)[number];

/** `AppDTO.status` — values come from AppAdminService lifecycle transitions. */
export const APP_STATUSES = ['DRAFT', 'PUBLISHED', 'ARCHIVED'] as const;
export type AppStatus = (typeof APP_STATUSES)[number];

/**
 * 字段类型集合、算子词表（legacy `filters` 与 structured `conditions` 两种方言）、
 * 别名归一与 `LcRow` / `QueryCondition` / `QuerySort` 的权威定义都在
 * `@yuku123/render/fields` —— 前端能用哪些算子由字段注册表决定，两处各留一份必然漂移。
 */

/* ------------------------------------------------------------------ */
/* Runtime rows                                                        */
/* ------------------------------------------------------------------ */

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

/** `DictDTO` / `DictItemDTO` / `ViewConfigDTO` 见文件头再导出。 */

/* ------------------------------------------------------------------ */
/* View config / Relation                                              */
/* ------------------------------------------------------------------ */

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

/**
 * `z_lc_workflow_fire` 的一行：某条绑定在某条记录上**实际**发起的结果。
 *
 * 界面以前没有这一格：绑定存下来之后，发没发、为什么没发只能去翻日志 —— 而写后触发是一次
 * 用户看不见的调用，看不见就等于没发生。
 */
export interface WorkflowFireEntity {
  id?: number | null;
  tenantCode?: string | null;
  appCode: string;
  entityCode: string;
  recordId: number;
  bindingId?: number | null;
  triggerEvent: string;
  processDefinitionKey: string;
  /** STARTED / FAILED —— 只有这两个值由写入方产生，别按它猜"在跑"。 */
  status: string;
  /** z-wf 返回的实例 id；失败行为空。 */
  instanceId?: string | null;
  /** 失败原因（引擎那句原话或"超过 Nms 没有回话"）；成功行为空。 */
  detail?: string | null;
  createTime?: string | number | null;
  updateTime?: string | number | null;
  deleted?: number | null;
}

/** `/workflow-binding/vocabulary`：引擎真正兑现的事件 + 兑现不了的那几个各自为什么。 */
export interface WorkflowVocabulary {
  implemented: string[];
  rejected: { event: string; reason: string }[];
}

export interface PermissionEntity {
  id?: number | null;
  appCode: string;
  /** null / 缺省 = 整个应用（库里 `entity_code IS NULL`）；给值则只覆盖那一个实体。 */
  entityCode?: string | null;
  roleCode: string;
  /** 只认 `PermissionKeys` 里的那几个词，写入口会当场拒掉别的一律 400。 */
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
