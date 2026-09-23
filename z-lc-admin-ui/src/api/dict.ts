import { request } from './client';
import type { DictDTO, DictItemDTO } from './types';

export function listDicts(appCode?: string): Promise<DictDTO[]> {
  return request<DictDTO[]>('/dict/list', { query: { appCode } });
}

export function getDict(dictCode: string): Promise<DictDTO> {
  return request<DictDTO>('/dict/detail', { query: { dictCode } });
}

export function createDict(payload: DictDTO): Promise<DictDTO> {
  return request<DictDTO>('/dict/create', { method: 'POST', body: payload });
}

export function updateDict(payload: DictDTO): Promise<DictDTO> {
  return request<DictDTO>('/dict/update', { method: 'POST', body: payload });
}

export function deleteDict(id: number): Promise<boolean> {
  return request<boolean>('/dict/delete', { method: 'POST', body: { id } });
}

export function listDictItems(dictCode: string): Promise<DictItemDTO[]> {
  return request<DictItemDTO[]>('/dict/items', { query: { dictCode } });
}

/** `dictCode` is a query param AND forced into the body server-side. */
export function createDictItem(dictCode: string, payload: DictItemDTO): Promise<DictItemDTO> {
  return request<DictItemDTO>('/dict/items/create', {
    method: 'POST',
    query: { dictCode },
    body: payload,
  });
}

export function updateDictItem(dictCode: string, payload: DictItemDTO): Promise<DictItemDTO> {
  return request<DictItemDTO>('/dict/items/update', {
    method: 'POST',
    query: { dictCode },
    body: payload,
  });
}

export function deleteDictItem(id: number): Promise<boolean> {
  return request<boolean>('/dict/items/delete', { method: 'POST', body: { id } });
}
