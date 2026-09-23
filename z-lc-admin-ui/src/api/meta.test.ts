import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fetchWorkspaceMeta } from '@/api/meta';
import type { EntityDefDTO } from '@/api/types';

/**
 * fetchWorkspaceMeta 的降级口径。
 *
 * 这个函数从不抛出：每个端点的失败只被记进 degraded。所以"读到了空列表"和
 * "压根没读到"必须靠 read 区分开，否则页面会把加载故障说成实体/视图被删了
 * （WorkspaceViewPage 和 DashboardPage 的那两条谎话就是这么来的）。
 * 另外后端是可以回 `success:true, data:null` 的 —— 那不是失败，但拿 `.length`
 * 就是崩，所以出参必须已经归一成数组，同时 read 得是 false。
 */

interface Spec {
  status?: number;
  body?: unknown;
}

const requested: string[] = [];

function reply(spec: Spec, ok: boolean) {
  const text = JSON.stringify(spec.body);
  return {
    ok,
    status: spec.status ?? 200,
    statusText: ok ? 'OK' : 'Error',
    text: async () => text,
    json: async () => JSON.parse(text),
  };
}

function stub(routes: Record<string, Spec>) {
  requested.length = 0;
  vi.stubGlobal(
    'fetch',
    vi.fn(async (input: unknown) => {
      const url = String(input);
      requested.push(url);
      const hit = Object.entries(routes).find(([path]) => url.includes(path));
      if (!hit) {
        // 未登记的端点当作"接口不存在"：HTTP 404，走 fetchWorkspaceMeta 的降级分支
        return reply(
          { body: { success: false, code: 404, message: 'no such endpoint', data: null } },
          false,
        );
      }
      const ok = (hit[1].status ?? 200) < 400;
      return reply({ body: hit[1].body ?? { success: true, code: 200, message: null, data: [] } }, ok);
    }),
  );
}

function entity(code: string): EntityDefDTO {
  return {
    id: 1,
    entityCode: code,
    entityName: code,
    tableName: `t_${code}`,
    tenantCode: 'default',
    appCode: 'crm',
    fields: [{ fieldCode: 'name', fieldName: '名称', fieldType: 'STRING', sortOrder: 1 }],
  } as EntityDefDTO;
}

beforeEach(() => {
  vi.unstubAllGlobals();
});

describe('fetchWorkspaceMeta', () => {
  it('bundle 可用时只补一次 field-types，不回头打单接口清单', async () => {
    stub({
      '/meta/bundle': {
        body: {
          success: true,
          code: 200,
          message: null,
          data: { entities: [entity('task')], dicts: [], views: [], relations: [], fieldTypes: [] },
        },
      },
      '/meta/field-types': {
        body: {
          success: true,
          code: 200,
          message: null,
          data: [{ code: 'STRING', name: '文本' }],
        },
      },
    });

    const meta = await fetchWorkspaceMeta('crm');
    expect(meta.source).toBe('bundle');
    expect(meta.entities).toHaveLength(1);
    expect(meta.read.entities).toBe(true);
    expect(meta.read.views).toBe(true);
    expect(meta.degraded).toEqual([]);
    expect(meta.fieldTypes).toHaveLength(1);
    expect(requested.filter((url) => url.includes('/meta/field-types'))).toHaveLength(1);
    expect(requested.filter((url) => url.includes('/app/schema'))).toEqual([]);
  });

  it('schema 接口失败时 read.entities 为 false，且不被当成空列表', async () => {
    stub({
      '/app/schema': {
        status: 500,
        body: { success: false, code: 500, message: 'boom', data: null },
      },
    });

    const meta = await fetchWorkspaceMeta('crm');
    expect(meta.source).toBe('fallback');
    expect(meta.entities).toEqual([]);
    expect(meta.degraded).toContain('schema');
    expect(meta.read.entities).toBe(false);
  });

  it('接口回 success:true 但 data:null 时，出参已是数组且不算降级项', async () => {
    stub({
      '/app/schema': { body: { success: true, code: 200, message: null, data: [] } },
      '/dict/list': { body: { success: true, code: 200, message: null, data: null } },
      '/view-config/list': { body: { success: true, code: 200, message: null, data: null } },
      '/relation/list': { body: { success: true, code: 200, message: null, data: null } },
      '/meta/field-types': { body: { success: true, code: 200, message: null, data: null } },
    });

    const meta = await fetchWorkspaceMeta('crm');
    expect(meta.dicts).toEqual([]);
    expect(meta.views).toEqual([]);
    expect(meta.relations).toEqual([]);
    expect(meta.fieldTypes).toEqual([]);
    expect(meta.degraded).toEqual([]);
    expect(meta.read.entities).toBe(true);
    // data:null 不是"没有数据"而是"没读到"：页面据此决定说"空"还是说"加载故障"。
    expect(meta.read).toEqual({
      entities: true,
      dicts: false,
      views: false,
      relations: false,
      fieldTypes: false,
    });
  });

  it('data:null 的实体列表同样是"没读到"', async () => {
    stub({
      '/app/schema': { body: { success: true, code: 200, message: null, data: null } },
    });

    const meta = await fetchWorkspaceMeta('crm');
    expect(meta.entities).toEqual([]);
    expect(meta.read.entities).toBe(false);
  });

  it('bundle 路径里补拉 field-types 失败也要记进 degraded', async () => {
    stub({
      '/meta/bundle': {
        body: {
          success: true,
          code: 200,
          message: null,
          data: { entities: [entity('task')], dicts: [], views: [], relations: [], fieldTypes: [] },
        },
      },
    });

    const meta = await fetchWorkspaceMeta('crm');
    expect(meta.source).toBe('bundle');
    expect(meta.fieldTypes).toEqual([]);
    expect(meta.degraded).toContain('field-types');
    expect(meta.read.fieldTypes).toBe(false);
  });

  // MetaController#bundle 给每个子资源各自包了一层 try/catch + log.warn：坏掉的那个 key 会被
  // 直接**省略**，整个响应仍然是 success:true。所以"bundle 成功"从来不等于"每个资源都读到了"，
  // 缺哪个就得单独补拉哪个 —— 否则 DashboardPage 会把一次 views 故障渲染成"视图已被删除"。
  it('bundle 里少了 views 这个 key 时补拉单接口，补到了就不算没读到', async () => {
    stub({
      '/meta/bundle': {
        body: {
          success: true,
          code: 200,
          message: null,
          data: { entities: [entity('task')], dicts: [], relations: [], fieldTypes: [{ code: 'STRING' }] },
        },
      },
      '/view-config/list': {
        body: {
          success: true,
          code: 200,
          message: null,
          data: [{ id: 7, appCode: 'crm', entityCode: 'task', viewType: 'CHART', viewName: '按人统计', config: '{}' }],
        },
      },
    });

    const meta = await fetchWorkspaceMeta('crm');
    expect(meta.source).toBe('bundle');
    expect(meta.views.map((view) => view.id)).toEqual([7]);
    expect(meta.read.views).toBe(true);
    expect(meta.degraded).toEqual([]);
    expect(requested.filter((url) => url.includes('/view-config/list'))).toHaveLength(1);
  });

  it('bundle 少了 views 且补拉也失败时，read.views 必须是 false', async () => {
    stub({
      '/meta/bundle': {
        body: {
          success: true,
          code: 200,
          message: null,
          data: { entities: [entity('task')], dicts: [], relations: [], fieldTypes: [{ code: 'STRING' }] },
        },
      },
      '/view-config/list': {
        status: 500,
        body: { success: false, code: 500, message: 'boom', data: null },
      },
    });

    const meta = await fetchWorkspaceMeta('crm');
    expect(meta.views).toEqual([]);
    expect(meta.degraded).toContain('views');
    expect(meta.read.views).toBe(false);
  });
});
