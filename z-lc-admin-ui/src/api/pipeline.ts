import { DEFAULT_TENANT_CODE, request } from './client';
import type { PipelineConfigEntity, PipelineStage } from './types';

export function listPipelineConfigs(
  appCode: string,
  entityCode?: string,
): Promise<PipelineConfigEntity[]> {
  return request<PipelineConfigEntity[]>('/pipeline-config/list', { query: { appCode, entityCode } });
}

export function createPipelineConfig(payload: PipelineConfigEntity): Promise<PipelineConfigEntity> {
  return request<PipelineConfigEntity>('/pipeline-config/create', { method: 'POST', body: payload });
}

export function updatePipelineConfig(payload: PipelineConfigEntity): Promise<PipelineConfigEntity> {
  return request<PipelineConfigEntity>('/pipeline-config/update', { method: 'POST', body: payload });
}

export function deletePipelineConfig(id: number): Promise<boolean> {
  return request<boolean>('/pipeline-config/delete', { method: 'POST', body: { id } });
}

/**
 * Toggle uses the same entity body but reads only `id` and `enabled`, where
 * `enabled` is an **integer 0|1**, not a boolean.
 */
export function togglePipelineConfig(id: number, enabled: boolean): Promise<boolean> {
  return request<boolean>('/pipeline-config/toggle', {
    method: 'POST',
    body: { id, enabled: enabled ? 1 : 0 },
  });
}

/**
 * 前端这一侧的阶段口径必须与后端 `PipelineStages.java` 完全一致 ——
 * `pipelineVocabulary.test.ts` 直接读那个 java 文件对表, 清单一旦漂红就拦住。
 */
export interface PipelineStageType {
  type: string;
  label: string;
  /** 三道完整性闸门之一: 从链里摘掉会被写入口拒掉。 */
  mandatory: boolean;
  /** 今天在写路径上什么都不做 (只打一行日志), 配与不配不改变行为。 */
  noOpOnWrite: boolean;
}

/** 迭代顺序 = 后端没有配置时那条默认链的执行顺序。 */
export const PIPELINE_STAGE_TYPES: PipelineStageType[] = [
  { type: 'DICT_RESOLVE', label: '字典解析', mandatory: false, noOpOnWrite: true },
  { type: 'REF_CHECK', label: '引用检查', mandatory: false, noOpOnWrite: true },
  { type: 'REQUIRED_CHECK', label: '必填校验', mandatory: true, noOpOnWrite: false },
  { type: 'TYPE_CONVERT', label: '类型转换', mandatory: true, noOpOnWrite: false },
  { type: 'VALUE_VALIDATE', label: '值域校验', mandatory: true, noOpOnWrite: false },
];

/** 引擎真有挂接点的触发事件; AFTER_* 一个都不在这里。 */
export const PIPELINE_SUPPORTED_TRIGGERS = ['BEFORE_CREATE', 'BEFORE_UPDATE'] as const;

export const PIPELINE_TRIGGER_LABELS: Record<string, string> = {
  BEFORE_CREATE: '创建前',
  BEFORE_UPDATE: '更新前',
};

export function isPipelineTriggerSupported(trigger: string): boolean {
  return (PIPELINE_SUPPORTED_TRIGGERS as readonly string[]).includes(trigger);
}

export function stageTypeLabel(type: string): string {
  return PIPELINE_STAGE_TYPES.find((item) => item.type === type)?.label ?? type;
}

/** 新建配置默认带上三道闸门 (顺序即后端默认链的顺序), 而不是默认一份会被拒的空链。 */
export function defaultPipelineStages(): PipelineStage[] {
  return PIPELINE_STAGE_TYPES.filter((item) => item.mandatory).map((item, index) => ({
    type: item.type,
    config: {},
    order: index,
  }));
}

/**
 * `stages` travels as a JSON string; parse defensively.
 *
 * 按 `order` 排序 —— 后端 `PipelineStages.parseTypes` 就是按 (order, 数组位置) 排的,
 * 前端不排的话界面上看到的顺序会和真正执行的顺序是两套。
 */
export function parseStages(raw: string | null | undefined): PipelineStage[] {
  if (!raw) return [];
  try {
    const parsed = JSON.parse(raw) as unknown;
    if (!Array.isArray(parsed)) return [];
    return parsed
      .filter((item): item is Record<string, unknown> => typeof item === 'object' && item !== null)
      .map((item, index) => ({
        type: typeof item.type === 'string' ? item.type : '',
        config:
          typeof item.config === 'object' && item.config !== null
            ? (item.config as Record<string, unknown>)
            : {},
        order: typeof item.order === 'number' ? item.order : index,
      }))
      .sort((a, b) => (a.order ?? 0) - (b.order ?? 0));
  } catch {
    return [];
  }
}

/**
 * `order` 一律重写成数组下标: 编辑器里的上下移动只动数组位置, 若沿用 `stage.order ?? index`,
 * 移动完再保存会把旧 order 原样写回去, 界面顺序变了而执行顺序没变。
 */
export function stringifyStages(stages: PipelineStage[]): string {
  return JSON.stringify(
    stages.map((stage, index) => ({
      type: stage.type,
      config: stage.config ?? {},
      order: index,
    })),
  );
}

export function newPipelineDraft(appCode: string): PipelineConfigEntity {
  return {
    appCode,
    entityCode: '',
    triggerEvent: PIPELINE_SUPPORTED_TRIGGERS[0],
    stages: stringifyStages(defaultPipelineStages()),
    enabled: 1,
    tenantCode: DEFAULT_TENANT_CODE,
  };
}
