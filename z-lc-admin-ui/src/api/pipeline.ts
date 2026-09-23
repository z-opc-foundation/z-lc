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

/** `stages` travels as a JSON string; parse defensively. */
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
      }));
  } catch {
    return [];
  }
}

export function stringifyStages(stages: PipelineStage[]): string {
  return JSON.stringify(
    stages.map((stage, index) => ({
      type: stage.type,
      config: stage.config ?? {},
      order: stage.order ?? index,
    })),
  );
}

export function newPipelineDraft(appCode: string): PipelineConfigEntity {
  return {
    appCode,
    entityCode: '',
    triggerEvent: 'AFTER_CREATE',
    stages: stringifyStages([]),
    enabled: 1,
    tenantCode: DEFAULT_TENANT_CODE,
  };
}
