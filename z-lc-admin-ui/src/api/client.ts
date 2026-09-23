import { getActor } from './actor';
import { publishApiError } from '@/utils/errorBus';
import type { Result } from './types';

/** Default tenant; every z-lc endpoint resolves a tenant and defaults to this. */
export const DEFAULT_TENANT_CODE = 'default';

/** All z-lc endpoints live under this prefix (proxied to :18086 in dev). */
export const API_BASE = '/api/lc';

/**
 * Error carrying the backend's business `code`.
 *
 * The z-lc backend reports most failures as **HTTP 200 + `success:false`**, so
 * `status` alone is not enough — always branch on `code`/`message`.
 */
export class ApiError extends Error {
  override readonly name = 'ApiError';
  /** Business code from the `Result` envelope (400/404/409/500...). */
  readonly code: number;
  /** HTTP status, when the transport itself failed. */
  readonly status: number | undefined;
  /** The full envelope, useful for conflict UI (e.g. event parent mismatch). */
  readonly payload: Result<unknown> | undefined;

  constructor(
    message: string,
    code = -1,
    status?: number,
    payload?: Result<unknown>,
  ) {
    super(message);
    this.code = code;
    this.status = status;
    this.payload = payload;
  }
}

export function isApiError(err: unknown): err is ApiError {
  return err instanceof ApiError;
}

/** True when the endpoint is absent (undeployed feature) rather than failing. */
export function isMissingEndpoint(err: unknown): boolean {
  if (!isApiError(err)) return false;
  return err.status === 404 || err.code === 404 || err.code === 405;
}

export type QueryValue = string | number | boolean | null | undefined;
export type QueryParams = Record<string, QueryValue>;

export function buildQuery(params: QueryParams): string {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value === undefined || value === null) continue;
    search.append(key, String(value));
  }
  const qs = search.toString();
  return qs.length > 0 ? `?${qs}` : '';
}

export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE';

export interface RequestOptions {
  method?: HttpMethod;
  /** Appended to the path as `?a=b`. */
  query?: QueryParams;
  /** JSON body. z-lc often takes a body *and* a query param for the same value. */
  body?: unknown;
  tenantCode?: string;
  /** Suppress the global error toast for probes/expected failures. */
  silent?: boolean;
  signal?: AbortSignal;
  /** Pass through an `AbortSignal` created by the caller. */
  headers?: Record<string, string>;
}

function isEnvelope(value: unknown): value is Result<unknown> {
  return (
    typeof value === 'object' &&
    value !== null &&
    'success' in value &&
    'code' in value &&
    'data' in value
  );
}

/**
 * Single entry point for every z-lc call.
 *
 * Unwraps the `Result<T>` envelope and throws {@link ApiError} when
 * `success === false` — which the backend returns with HTTP 200.
 */
export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const {
    method = 'GET',
    query,
    body,
    tenantCode = DEFAULT_TENANT_CODE,
    silent = false,
    signal,
    headers,
  } = options;

  const url = `${API_BASE}${path}${buildQuery(query ?? {})}`;

  const init: RequestInit = {
    method,
    signal,
    headers: {
      Accept: 'application/json',
      'X-Tenant-Code': tenantCode,
      // 变更归属: 后端 ActorResolver 认这个头, 只用于 undo 归属与历史署名, 不做鉴权.
      'X-User-Code': getActor(),
      ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
      ...headers,
    },
    ...(body !== undefined ? { body: JSON.stringify(body) } : {}),
  };

  let response: Response;
  try {
    response = await fetch(url, init);
  } catch (err) {
    if (err instanceof DOMException && err.name === 'AbortError') throw err;
    const message = err instanceof Error ? err.message : String(err);
    const apiError = new ApiError(`网络请求失败: ${message}`, -1, undefined);
    if (!silent) publishApiError(apiError.message, apiError.code);
    throw apiError;
  }

  const text = await response.text();
  let parsed: unknown = null;
  if (text.length > 0) {
    try {
      parsed = JSON.parse(text) as unknown;
    } catch {
      parsed = null;
    }
  }

  const fail = (message: string, code: number, payload?: Result<unknown>): never => {
    const apiError = new ApiError(message, code, response.status, payload);
    if (!silent) publishApiError(apiError.message, apiError.code);
    throw apiError;
  };

  if (!response.ok) {
    // Spring's exception handler still emits a Result envelope on 4xx/5xx.
    if (isEnvelope(parsed)) {
      return fail(
        parsed.message ?? `请求失败 (HTTP ${response.status})`,
        parsed.code ?? response.status,
        parsed,
      );
    }
    return fail(`请求失败: HTTP ${response.status} ${response.statusText}`, response.status);
  }

  if (!isEnvelope(parsed)) {
    return fail(
      `响应不是预期的 Result 结构 (${method} ${url})`,
      -1,
    );
  }

  if (!parsed.success) {
    return fail(parsed.message ?? '后端返回失败', parsed.code, parsed);
  }

  return parsed.data as T;
}

/**
 * For `/api/lc/app/materialize*`, which returns a raw `{success,data}` map
 * instead of the `Result` envelope (no `code`, no `message`).
 */
export async function requestRaw<T>(
  path: string,
  options: RequestOptions = {},
): Promise<T> {
  const { method = 'GET', query, body, tenantCode = DEFAULT_TENANT_CODE, silent = false, signal } =
    options;
  const url = `${API_BASE}${path}${buildQuery(query ?? {})}`;
  const init: RequestInit = {
    method,
    signal,
    headers: {
      Accept: 'application/json',
      'X-Tenant-Code': tenantCode,
      'X-User-Code': getActor(),
      ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
    },
    ...(body !== undefined ? { body: JSON.stringify(body) } : {}),
  };

  const response = await fetch(url, init).catch((err: unknown) => {
    const message = err instanceof Error ? err.message : String(err);
    const apiError = new ApiError(`网络请求失败: ${message}`, -1);
    if (!silent) publishApiError(apiError.message, apiError.code);
    throw apiError;
  });

  const raw = (await response.json().catch(() => null)) as {
    success?: boolean;
    data?: T;
  } | null;

  if (!response.ok || raw === null) {
    const apiError = new ApiError(`物化接口调用失败: HTTP ${response.status}`, response.status);
    if (!silent) publishApiError(apiError.message, apiError.code);
    throw apiError;
  }

  if (raw.success === false) {
    const apiError = new ApiError('物化任务不存在或未返回数据', -1);
    if (!silent) publishApiError(apiError.message, apiError.code);
    throw apiError;
  }

  return raw.data as T;
}
