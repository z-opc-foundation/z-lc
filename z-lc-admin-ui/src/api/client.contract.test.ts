import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError, DEFAULT_TENANT_CODE, request } from '@/api/client';
import { setActor } from '@/api/actor';

/**
 * Result 信封的拆包约定 —— 整个前端都建立在它之上。
 *
 * 关键不变式：z-lc 绝大多数业务失败都是 **HTTP 200 + success:false**。
 * 任何按 HTTP 状态码分支的客户端都会把这些失败当成功，
 * 所以这里把"200 且 success:false 必须抛"钉死。
 */
function mockJson(payload: unknown, status = 200) {
  vi.stubGlobal('fetch', vi.fn(async () => ({
    ok: status >= 200 && status < 300,
    status,
    json: async () => payload,
    text: async () => JSON.stringify(payload),
  })));
}

describe('request 信封', () => {
  beforeEach(() => {
    window.localStorage.clear();
    vi.unstubAllGlobals();
  });

  it('成功时把 data 拆出来给调用方', async () => {
    mockJson({ data: { status: 'UP' }, success: true, code: 200, message: null });
    await expect(request<{ status: string }>('/health')).resolves.toEqual({ status: 'UP' });
  });

  it('HTTP 200 但 success:false 必须抛 ApiError 并带上业务 code', async () => {
    mockJson({ data: null, success: false, code: 400, message: 'appCode is required' });
    await expect(request('/runtime/list')).rejects.toMatchObject({
      name: 'ApiError',
      code: 400,
      message: 'appCode is required',
    });
  });

  it('success:false 且 code 缺失时不退化成"成功"', async () => {
    mockJson({ data: null, success: false });
    await expect(request('/runtime/list')).rejects.toBeInstanceOf(ApiError);
  });

  it('每个请求都带租户与身份头（undo 归属依赖它）', async () => {
    mockJson({ data: null, success: true, code: 200 });
    setActor('web-tester');
    await request('/runtime/list');
    const fetchMock = globalThis.fetch as unknown as ReturnType<typeof vi.fn>;
    const call = fetchMock.mock.calls[0];
    expect(call, 'fetch 应该被调用过').toBeTruthy();
    const init = (call as [string, RequestInit])[1];
    const headers = init.headers as Record<string, string>;
    expect(headers['X-Tenant-Code']).toBe(DEFAULT_TENANT_CODE);
    expect(headers['X-User-Code']).toBe('web-tester');
  });

  it('响应不是 JSON（网关错误页等）时报错而不是把 undefined 当数据返回', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => ({
      ok: true,
      status: 200,
      json: async () => {
        throw new Error('not json');
      },
      text: async () => '<html>502 Bad Gateway</html>',
    })));
    await expect(request('/health')).rejects.toBeInstanceOf(ApiError);
  });

  it('网络层直接失败时也是 ApiError，调用方只处理一种异常', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => {
      throw new Error('Failed to fetch');
    }));
    await expect(request('/health')).rejects.toBeInstanceOf(ApiError);
  });
});

describe('变更归属身份', () => {
  // actor.ts 是模块级单例（cached + override），跨用例必须真正重置模块，
  // 只清 localStorage 不会清掉内存里的缓存
  let getActor: () => string;
  let setActorLocal: (actor: string | null | undefined) => void;
  beforeEach(async () => {
    window.localStorage.clear();
    // actor.ts 是模块级单例（cached + override），只清 localStorage 不够，必须重置模块
    vi.resetModules();
    ({ getActor, setActor: setActorLocal } = await import('@/api/actor'));
  });

  it('同一会话内稳定，并写进 localStorage（否则 undo 栈每刷新就换人）', () => {
    const first = getActor();
    expect(first).toMatch(/^web-/);
    expect(getActor()).toBe(first);
    expect(window.localStorage.getItem('zlc:actor')).toBe(first);
  });

  it('网关/登录身份到位后覆盖本地生成值', () => {
    getActor();
    setActorLocal('alice');
    expect(getActor()).toBe('alice');
    expect(window.localStorage.getItem('zlc:actor')).toBe('alice');
  });

  it('空身份不覆盖已有值', () => {
    setActorLocal('bob');
    setActorLocal('   ');
    expect(getActor()).toBe('bob');
    setActorLocal(null);
    expect(getActor()).toBe('bob');
  });
});
