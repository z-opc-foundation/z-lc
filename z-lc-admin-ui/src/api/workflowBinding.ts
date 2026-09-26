import { request } from './client';
import type { WorkflowBindingEntity, WorkflowFireEntity, WorkflowVocabulary } from './types';

export function listWorkflowBindings(
  appCode: string,
  entityCode?: string,
): Promise<WorkflowBindingEntity[]> {
  return request<WorkflowBindingEntity[]>('/workflow-binding/list', {
    query: { appCode, entityCode },
  });
}

export function createWorkflowBinding(
  payload: WorkflowBindingEntity,
): Promise<WorkflowBindingEntity> {
  return request<WorkflowBindingEntity>('/workflow-binding/create', {
    method: 'POST',
    body: payload,
  });
}

export function updateWorkflowBinding(
  payload: WorkflowBindingEntity,
): Promise<WorkflowBindingEntity> {
  return request<WorkflowBindingEntity>('/workflow-binding/update', {
    method: 'POST',
    body: payload,
  });
}

export function deleteWorkflowBinding(id: number): Promise<boolean> {
  return request<boolean>('/workflow-binding/delete', { method: 'POST', body: { id } });
}

/**
 * 引擎真正兑现的触发时机。界面上那一份清单必须从这里长出来（缺陷 #61）：
 * 此前 WorkflowsPage 手抄了三个时机，其中两个引擎根本没有挂接点，而写入口现在把它们拒成 400。
 */
export function getWorkflowVocabulary(): Promise<WorkflowVocabulary> {
  return request<WorkflowVocabulary>('/workflow-binding/vocabulary', { silent: true }).then((raw) =>
    readVocabulary(raw),
  );
}

/** 某条绑定在某条记录上实际发起的结果 —— 绑定不等于发出去了，这一格才是账。 */
export function listWorkflowFires(
  appCode: string,
  entityCode?: string,
  recordId?: number,
): Promise<WorkflowFireEntity[]> {
  return request<WorkflowFireEntity[]>('/workflow-binding/fires', {
    query: { appCode, entityCode, recordId },
    silent: true,
  });
}

/**
 * 把接口给的 `data` 归一成能用的一份词表；读不出形状就<b>抛</b>，不要返回空清单。
 *
 * 为什么较真：`{implemented: null}`、整个 data 缺席、或者只回了 `implemented` 没回 `rejected` ——
 * 这些形状若被当成"引擎一个事件都不支持"，界面上就是"没有可选的触发时机"，
 * 一次接口形状漂移会被读成"这个能力下线了"（本仓 #19/#22/#23 同一族的错法）。
 */
export function readVocabulary(raw: unknown): WorkflowVocabulary {
  if (!raw || typeof raw !== 'object') {
    throw new Error('流程词表接口没有返回对象（data 为空或形状不对）');
  }
  const value = raw as Record<string, unknown>;
  if (!Array.isArray(value.implemented)) {
    throw new Error(
      `流程词表接口里 implemented 不是数组（实际=${value.implemented === null ? 'null' : typeof value.implemented}），` +
        '不能当成"引擎没有可兑现的事件"',
    );
  }
  const implemented = value.implemented
    .filter((item): item is string => typeof item === 'string' && item.trim().length > 0)
    .map((item) => item.trim());
  const rejectedRaw = Array.isArray(value.rejected) ? value.rejected : [];
  const rejected: { event: string; reason: string }[] = [];
  for (const item of rejectedRaw) {
    if (!item || typeof item !== 'object') continue;
    const one = item as Record<string, unknown>;
    if (typeof one.event !== 'string' || !one.event.trim()) continue;
    rejected.push({
      event: one.event.trim(),
      // 原因缺格时不许留空：界面上"这个时机不支持"后面跟着一句空白，等于没说。
      reason: typeof one.reason === 'string' && one.reason.trim() ? one.reason.trim() : '引擎没有说原因',
    });
  }
  return { implemented, rejected };
}
