import { request } from './client';
import type { RelationCreateReq, RelationDTO, RelationUpdateReq } from './types';

export function listRelations(appCode: string, entityCode?: string): Promise<RelationDTO[]> {
  return request<RelationDTO[]>('/relation/list', { query: { appCode, entityCode } });
}

export function createRelation(payload: RelationCreateReq): Promise<RelationDTO> {
  return request<RelationDTO>('/relation/create', { method: 'POST', body: payload });
}

export function updateRelation(payload: RelationUpdateReq): Promise<RelationDTO> {
  return request<RelationDTO>('/relation/update', { method: 'POST', body: payload });
}

export function deleteRelation(id: number): Promise<boolean> {
  return request<boolean>('/relation/delete', { method: 'POST', body: { id } });
}
