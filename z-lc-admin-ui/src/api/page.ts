import { request } from './client';

/* ------------------------------------------------------------------ */
/* 页面域（V2，2026-10-27）— /api/lc/page                                */
/* Page 一等公民: schema 即 z-frontend-render 组件树 JSON（lead 013 §四）。 */
/* 渲染产物与 z-frontend-render 的 RenderData 逐字段对齐。                */
/* ------------------------------------------------------------------ */

export interface PageWidgetEncode {
  x?: string;
  y?: string;
  series?: string;
  category?: string;
  value?: string;
  columns?: string[];
}

export type PageWidgetType = 'LINE' | 'BAR' | 'PIE' | 'TABLE' | 'KPI' | 'RAW' | 'FORM';

/** 部件规格: 非 FORM 绑 dataset(datasetId 指向 schema.datasets), FORM 绑实体 */
export interface PageWidgetSpec {
  id: string;
  type: PageWidgetType;
  title?: string;
  datasetId?: string;
  entityCode?: string;
  encode?: PageWidgetEncode;
  /** 度量聚合方式, 缺省 SUM */
  agg?: 'SUM' | 'COUNT' | 'AVG' | 'MIN' | 'MAX';
}

export interface PageLayoutCell {
  widgetId: string;
  /** 12 栅格: x∈[0,11], w∈[1,12], x+w≤12 */
  x: number;
  y: number;
  w: number;
  h: number;
}

export interface PageDatasetBinding {
  sourceId: string;
  baseTable: string;
}

/** 页面 schema: datasets 内嵌（页面跨重启可渲染, 不依赖报表内存 store） */
export interface PageSchema {
  version: number;
  datasets: Record<string, PageDatasetBinding>;
  layout: PageLayoutCell[];
  widgets: PageWidgetSpec[];
}

export interface PageDTO {
  id: number | null;
  appCode: string;
  pageCode: string;
  pageName: string | null;
  /** JSON 字符串; 前端 parse/stringify PageSchema */
  schema: string | null;
  tenantCode?: string | null;
  createTime?: string | number | null;
  updateTime?: string | number | null;
}

export function createPage(req: {
  appCode: string;
  pageCode: string;
  pageName: string;
  schema: string;
}): Promise<PageDTO> {
  return request<PageDTO>('/page/create', { method: 'POST', body: req });
}

export function updatePage(req: { id: number; pageName?: string; schema?: string }): Promise<PageDTO> {
  return request<PageDTO>('/page/update', { method: 'POST', body: req });
}

export function deletePage(id: number): Promise<boolean> {
  return request<boolean>('/page/delete', { method: 'POST', body: { id } });
}

export function listPages(appCode: string): Promise<PageDTO[]> {
  return request<PageDTO[]>('/page/list', { query: { appCode } });
}

export function getPage(appCode: string, pageCode: string): Promise<PageDTO | null> {
  return request<PageDTO | null>(`/page/${appCode}/${pageCode}`);
}

/* ---------------- 渲染产物 (z-frontend-render RenderData + FORM 扩展) ---------------- */

export interface PageRenderField {
  code: string;
  name: string;
  type: string;
  required: boolean;
}

export interface PageRenderForm {
  entityCode: string;
  entityName?: string;
  title?: string;
  fields: PageRenderField[];
}

/** 与 z-frontend-render 的 WidgetOption 对齐 (只声明我们消费的字段) */
export interface PageWidgetOption {
  _widgetType?: string;
  _title?: string;
  columns?: { key: string; label?: string }[];
  rows?: Record<string, unknown>[];
  value?: unknown;
  label?: string;
  [key: string]: unknown;
}

export interface PageRenderData {
  pageCode: string;
  pageName: string | null;
  title?: string | null;
  version?: number;
  layout?: PageLayoutCell[];
  options?: Record<string, PageWidgetOption>;
  forms?: Record<string, PageRenderForm>;
}

export function renderPage(appCode: string, pageCode: string): Promise<PageRenderData> {
  return request<PageRenderData>(`/page/${appCode}/${pageCode}/render`);
}
