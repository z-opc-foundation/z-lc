import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { WorkspaceViewPage } from '@/views/workspace/WorkspaceViewPage';
import type { EntityDefDTO, FieldDefDTO } from '@/api/types';

/**
 * WorkspaceViewPage 的三种"没内容"状态。
 *
 * 这里守的是一条谎话：元数据加载失败时 entities 会是空数组，页面原来一律渲染
 * 「实体「x」不存在，可能已被删除」—— 连接池被吃空的那次故障，就是把"读不到"
 * 说成了"被删了"。fetchWorkspaceMeta 从不抛出（失败只记进 degraded），所以
 * react-query 的 isError 抓不到这类故障，必须在页面级用 meta.read.entities 判。
 */

const APP = 'crm';
const ENT = 'task';

vi.mock('@/views/workspace/GridView', () => ({
  GridView: () => null,
}));

interface Call {
  url: string;
}

const calls: Call[] = [];

function envelope(data: unknown, success = true) {
  const text = JSON.stringify({ success, code: success ? 200 : 500, message: success ? null : 'boom', data });
  return {
    ok: success,
    status: success ? 200 : 500,
    statusText: success ? 'OK' : 'Server Error',
    text: async () => text,
    json: async () => JSON.parse(text),
  };
}

type SchemaAnswer = { body: unknown } | { fail: true };

function stubBackend(schema: SchemaAnswer) {
  calls.length = 0;
  vi.stubGlobal(
    'fetch',
    vi.fn(async (input: unknown) => {
      const url = String(input);
      calls.push({ url });
      if (url.includes('/meta/bundle')) return envelope(null, false);
      if (url.includes('/app/schema')) {
        return 'fail' in schema ? envelope(null, false) : envelope(schema.body);
      }
      if (url.includes('/dict/list') || url.includes('/view-config/list') || url.includes('/relation/list')) {
        return envelope([]);
      }
      if (url.includes('/meta/field-types')) return envelope([]);
      return envelope(null, false);
    }),
  );
}

function taskEntity(): EntityDefDTO {
  const fields: FieldDefDTO[] = [
    { fieldCode: 'title', fieldName: '标题', fieldType: 'STRING', required: true, fieldLength: 64, sortOrder: 1 },
  ];
  return {
    id: 11,
    entityCode: ENT,
    entityName: '任务',
    tableName: 't_task',
    tenantCode: 'default',
    appCode: APP,
    fields,
  } as EntityDefDTO;
}

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <MemoryRouter initialEntries={[`/${APP}/${ENT}/LIST`]}>
      <QueryClientProvider client={client}>
        <Routes>
          <Route path="/:appCode/:entityCode/:viewType" element={<WorkspaceViewPage />} />
        </Routes>
      </QueryClientProvider>
    </MemoryRouter>,
  );
}

function schemaCalls() {
  return calls.filter((call) => call.url.includes('/app/schema'));
}

beforeEach(() => {
  vi.unstubAllGlobals();
});

describe('WorkspaceViewPage 的元数据状态', () => {
  it('读不到实体列表时报"加载失败"，不能报"实体不存在"', async () => {
    stubBackend({ fail: true });
    renderPage();

    await waitFor(() => expect(screen.getByText('实体元数据没读到')).toBeInTheDocument());
    expect(screen.getByText(/这是加载故障，不代表实体已被删除/)).toBeInTheDocument();
    expect(screen.getByText(/失败接口: schema/)).toBeInTheDocument();
    expect(screen.queryByText(/可能已被删除/)).toBeNull();
  });

  it('接口返回 success:true 但 data 为 null 时同样算没读到', async () => {
    stubBackend({ body: null });
    renderPage();

    await waitFor(() => expect(screen.getByText('实体元数据没读到')).toBeInTheDocument());
    expect(screen.getByText(/接口返回的不是列表/)).toBeInTheDocument();
    expect(screen.queryByText(/可能已被删除/)).toBeNull();
  });

  it('重试按钮会重新拉元数据', async () => {
    stubBackend({ fail: true });
    renderPage();

    await waitFor(() => expect(screen.getByText('实体元数据没读到')).toBeInTheDocument());
    expect(schemaCalls()).toHaveLength(1);
    // antd 会在两个中文字之间插一个空格（"重 试"），所以用 \s* 匹配
    fireEvent.click(screen.getByRole('button', { name: /重\s*试/ }));
    await waitFor(() => expect(schemaCalls().length).toBeGreaterThan(1));
  });

  it('确实读到了、且 app 下没有实体时，才说"实体不存在"', async () => {
    stubBackend({ body: [] });
    renderPage();

    await waitFor(() => expect(screen.getByText(/可能已被删除/)).toBeInTheDocument());
    expect(screen.queryByText('实体元数据没读到')).toBeNull();
  });

  it('读到目标实体时不显示任何错误/空态', async () => {
    stubBackend({ body: [taskEntity()] });
    renderPage();

    await waitFor(() => expect(schemaCalls().length).toBeGreaterThan(0));
    await waitFor(() => expect(screen.queryByText('加载中')).toBeNull());
    expect(screen.queryByText(/可能已被删除/)).toBeNull();
    expect(screen.queryByText('实体元数据没读到')).toBeNull();
    expect(screen.queryByText('这个实体还没有字段')).toBeNull();
  });
});
