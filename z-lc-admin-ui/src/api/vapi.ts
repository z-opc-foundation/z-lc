import { request } from './client';
import type { HttpMethod } from './client';

/* ------------------------------------------------------------------ */
/* 接口/脚本域（V3，2026-10-27）— /api/lc/vapi                           */
/* vApi 注册表: app+method+path 唯一, 实现即脚本 (EL/GROOVY/LUA/SQL/MOCK, */
/* 经 z-script ScriptEngine 执行, GROOVY/EL 带沙箱)。分发 POST /invoke。  */
/* ------------------------------------------------------------------ */

export interface VapiDTO {
  id: number | null;
  appCode: string;
  vapiCode: string | null;
  method: string;
  path: string;
  dslType: string;
  sourceCode: string | null;
  paramSchema: string | null;
  status: number;
  description: string | null;
  createTime?: string | number | null;
  updateTime?: string | number | null;
}

export function createVapi(req: {
  appCode: string;
  vapiCode?: string;
  method: string;
  path: string;
  dslType: string;
  sourceCode: string;
  paramSchema?: string;
  description?: string;
}): Promise<VapiDTO> {
  return request<VapiDTO>('/vapi/create', { method: 'POST', body: req });
}

export function updateVapi(req: {
  id: number;
  vapiCode?: string;
  method?: string;
  path?: string;
  dslType?: string;
  sourceCode?: string;
  paramSchema?: string;
  status?: number;
  description?: string;
}): Promise<VapiDTO> {
  return request<VapiDTO>('/vapi/update', { method: 'POST', body: req });
}

export function deleteVapi(id: number): Promise<boolean> {
  return request<boolean>('/vapi/delete', { method: 'POST', query: { id } });
}

export function getVapi(id: number): Promise<VapiDTO> {
  return request<VapiDTO>('/vapi/get', { query: { id } });
}

export function listVapis(appCode: string): Promise<VapiDTO[]> {
  return request<VapiDTO[]>('/vapi/list', { query: { appCode } });
}

export interface VapiInvokeResult {
  vapiCode: string | null;
  dslType: string;
  data: unknown;
}

/** 通用分发: 任意已注册接口按 method+path 调用; params 作为 JSON body (GET 走 query 由调用方拼) */
export function invokeVapi(
  appCode: string,
  path: string,
  method: HttpMethod,
  params?: Record<string, unknown>,
): Promise<VapiInvokeResult> {
  return request<VapiInvokeResult>(`/vapi/invoke/${appCode}/${path}`, {
    method,
    body: method === 'GET' ? undefined : params ?? {},
    silent: true,
  });
}
