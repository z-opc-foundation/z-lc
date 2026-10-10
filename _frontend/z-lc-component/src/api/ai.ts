import { request } from './client';
import type { JsonMap } from './types';

/**
 * AI modeling endpoints. Bodies and payloads are plain maps server-side
 * (`Map<String,String>` / `Map<String,Object>`), so responses are surfaced as
 * {@link JsonMap} and narrowed by the UI after inspection.
 */

/** `description` is a natural-language prompt; returns a schema suggestion map. */
export function requestModelSuggestion(appCode: string, description: string): Promise<JsonMap> {
  return request<JsonMap>('/ai/model-suggestion', {
    method: 'POST',
    body: { appCode, description },
  });
}

/** Persist a (possibly user-edited) suggestion. The map must carry `appCode`. */
export function applyModelSuggestion(suggestion: JsonMap): Promise<JsonMap> {
  return request<JsonMap>('/ai/apply-suggestion', { method: 'POST', body: suggestion });
}

export function generateFormLayout(appCode: string, entityCode: string): Promise<JsonMap> {
  return request<JsonMap>('/ai/generate-form', { method: 'POST', body: { appCode, entityCode } });
}

export function analyzeData(appCode: string, entityCode: string): Promise<JsonMap> {
  return request<JsonMap>('/ai/analyze-data', { method: 'POST', body: { appCode, entityCode } });
}

export function oneClickProductize(appCode: string): Promise<JsonMap> {
  return request<JsonMap>('/ai/productize', { method: 'POST', body: { appCode } });
}

export function listMcpTools(): Promise<JsonMap[]> {
  return request<JsonMap[]>('/ai/mcp-tools');
}
