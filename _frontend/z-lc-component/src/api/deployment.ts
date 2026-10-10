import { request } from './client';
import type { DeploymentCreateReq, DeploymentDTO, DeploymentVocabulary } from './types';

export function listDeployments(appCode: string): Promise<DeploymentDTO[]> {
  return request<DeploymentDTO[]>('/deployment/list', { query: { appCode } });
}

export function getDeployment(id: number): Promise<DeploymentDTO> {
  return request<DeploymentDTO>('/deployment/detail', { query: { id } });
}

/**
 * 创建一次部署。返回的是<b>执行之后</b>的那一行：`status` 已经是 SUCCESS/FAILED，
 * `deployLog` 是这一次真的动了什么（几张表新建、几张补列、哪一支没建成以及为什么）。
 * 缺陷 #70：原先这里写着"execution is asynchronous server-side"，而服务器一个字都不执行，
 * PENDING 就是终态 —— 那句话是假的，界面据它弹的是「部署已创建」。
 */
export function createDeployment(payload: DeploymentCreateReq): Promise<DeploymentDTO> {
  return request<DeploymentDTO>('/deployment/create', { method: 'POST', body: payload });
}

/**
 * 服务器真正会执行的部署方式。界面上那一份清单必须从这里长出来（缺陷 #70，与 #61 同法）：
 * 页面此前自己抄了三种方式，而三种都不执行。
 */
export function getDeploymentVocabulary(): Promise<DeploymentVocabulary> {
  return request<DeploymentVocabulary>('/deployment/vocabulary', { silent: true }).then((raw) =>
    readVocabulary(raw),
  );
}

/**
 * 把接口给的 `data` 归一成能用的一份词表；读不出形状就<b>抛</b>，不要返回空清单。
 *
 * 为什么较真：`{executable: []}` 与"接口形状漂移了"在界面上长得一样 —— 都会被渲染成
 * "一个部署方式都不能选"，一次漂移就被读成"这个能力下线了"（#19/#22/#23 同一族错法，
 * 也与 `workflowBinding.readVocabulary` 同一条口径）。
 */
export function readVocabulary(raw: unknown): DeploymentVocabulary {
  if (!raw || typeof raw !== 'object') {
    throw new Error('部署词表接口没有返回对象（data 为空或形状不对）');
  }
  const value = raw as Record<string, unknown>;
  if (!Array.isArray(value.executable)) {
    throw new Error(
      `部署词表接口里 executable 不是数组（实际=${value.executable === null ? 'null' : typeof value.executable}），` +
        '不能当成"服务器没有可执行的部署方式"',
    );
  }
  const executable = value.executable
    .filter((item): item is string => typeof item === 'string' && item.trim().length > 0)
    .map((item) => item.trim());
  const rejectedRaw = Array.isArray(value.rejected) ? value.rejected : [];
  const rejected: { type: string; reason: string }[] = [];
  for (const item of rejectedRaw) {
    if (!item || typeof item !== 'object') continue;
    const one = item as Record<string, unknown>;
    if (typeof one.type !== 'string' || !one.type.trim()) continue;
    rejected.push({
      type: one.type.trim(),
      // 原因缺格时不许留空：界面上"这种方式不支持"后面跟着一句空白，等于没说。
      reason: typeof one.reason === 'string' && one.reason.trim() ? one.reason.trim() : '服务器没有说原因',
    });
  }
  return { executable, rejected };
}
