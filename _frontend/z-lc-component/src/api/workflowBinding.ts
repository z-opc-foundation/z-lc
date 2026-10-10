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

/** `/fires` 的一页。`total` 是库里真行数，`records` 只是这一页 —— 两者不是一回事（缺陷 #65）。 */
export interface FireWindow {
  records: WorkflowFireEntity[];
  total: number;
  pageNum: number;
  pageSize: number;
}

/** 某条绑定在某条记录上实际发起的结果 —— 绑定不等于发出去了，这一格才是账。 */
export function listWorkflowFires(
  appCode: string,
  entityCode?: string,
  recordId?: number,
  page = 1,
  size = 20,
): Promise<FireWindow> {
  return request<unknown>('/workflow-binding/fires', {
    query: { appCode, entityCode, recordId, page, size },
    silent: true,
  }).then((raw) => readFireWindow(raw));
}

/**
 * 把 `/fires` 的 `data` 归成一页；读不出形状就<b>抛</b>，不要退化成"0 行"或"总数=这一页的行数"。
 *
 * 为什么较真：这张表是"绑定到底兑现过没有"的唯一证据面，而缺陷 #65 的形状正是
 * 接口只回一个裸数组、读到 200 条时没人知道库里其实有 250 条 —— 于是"第 201 条之后没读出来"
 * 会被界面说成"这个实体还没有发起记录"。所以：
 * - **裸数组一律判为读不出来**（那正是 #65 之前的形状；把它接住并当场算 `total = length`
 *   等于把静默截断原地复活，还给它配了一句看起来可信的总数）；
 * - `total` 缺席/非数字 ⇒ 抛，绝不默认成 `records.length`；
 * - 与 {@link readVocabulary} 同一口径：形状漂移不能读成"能力下线"。
 */
export function readFireWindow(raw: unknown): FireWindow {
  if (!raw || typeof raw !== 'object' || Array.isArray(raw)) {
    throw new Error(
      `/fires 没有回分页信封（拿到的是${Array.isArray(raw) ? '裸数组 = #65 之前那个不带总数的形状' : typeof raw}），` +
        '不能把"这一页读到的条数"当成"库里一共有多少条"',
    );
  }
  const value = raw as Record<string, unknown>;
  if (!Array.isArray(value.records)) {
    throw new Error(
      `/fires 的 records 不是数组（实际=${value.records === null ? 'null' : typeof value.records}），` +
        '不能当成"这个实体没有发起记录"',
    );
  }
  if (typeof value.total !== 'number' || !Number.isFinite(value.total) || value.total < 0) {
    throw new Error(
      `/fires 没有如实回总数（total=${String(value.total)}）—— 少了这一格，一页读不满就等于"没有账"，` +
        '那正是缺陷 #65',
    );
  }
  const numOf = (v: unknown, name: string): number => {
    if (typeof v !== 'number' || !Number.isFinite(v) || v < 1) {
      throw new Error(`/fires 的 ${name} 不是合法的页码/页大小（${String(v)}），不能拿它去标"第几页"`);
    }
    return v;
  };
  return {
    records: value.records as WorkflowFireEntity[],
    total: value.total,
    pageNum: numOf(value.pageNum, 'pageNum'),
    pageSize: numOf(value.pageSize, 'pageSize'),
  };
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
