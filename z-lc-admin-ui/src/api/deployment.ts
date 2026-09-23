import { request } from './client';
import type { DeploymentCreateReq, DeploymentDTO } from './types';

export function listDeployments(appCode: string): Promise<DeploymentDTO[]> {
  return request<DeploymentDTO[]>('/deployment/list', { query: { appCode } });
}

export function getDeployment(id: number): Promise<DeploymentDTO> {
  return request<DeploymentDTO>('/deployment/detail', { query: { id } });
}

/** Creates a deployment task; execution is asynchronous server-side. */
export function createDeployment(payload: DeploymentCreateReq): Promise<DeploymentDTO> {
  return request<DeploymentDTO>('/deployment/create', { method: 'POST', body: payload });
}
