import { request } from './client';
import type { WorkflowBindingEntity } from './types';

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
