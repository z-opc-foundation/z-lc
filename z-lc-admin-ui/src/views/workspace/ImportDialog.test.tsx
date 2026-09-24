import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { createWorkspaceContext, resolveEntityFields } from '@yuku123/render/fields';
import { ImportDialog } from '@/views/workspace/ImportDialog';
import type { EntityDefDTO, FieldDefDTO } from '@/api/types';

/**
 * 导入向导"结果"那一步的账目测试。
 *
 * 这里守的是一条会伤到数据判断的谎：commit 返回 applied=false（写入中断、补偿回滚还没做完）时，
 * 旧代码只看 `applied` 就把每一行标成"成功"，页头于是渲染出绿色的「共 3 行，成功 3 行，失败 0 行」，
 * 真正的失败原因只在一闪而过的 toast 里。除此之外还钉住两点：
 * 服务端每批最多回 50 条错误，"多少行不合法"必须用 total/validCount 而不是 errors.length；
 * 以及 insertedCount 小于提交行数时不能替服务端猜哪几行进去了。
 */

const APP = 'crm';
const ENT = 'customer';

function respond(payload: unknown) {
  const text = JSON.stringify(payload);
  return {
    ok: true,
    status: 200,
    // client.ts 读的是 response.text()，不是 json()
    text: async () => text,
    json: async () => payload,
  };
}

function entityDef(): EntityDefDTO {
  const fields: FieldDefDTO[] = [
    { fieldCode: 'customer_name', fieldName: '客户名称', fieldType: 'STRING', required: true, fieldLength: 128, sortOrder: 1 },
    { fieldCode: 'level', fieldName: '客户等级', fieldType: 'STRING', dictCode: 'lvl', fieldLength: 16, sortOrder: 2 },
    { fieldCode: 'balance', fieldName: '余额', fieldType: 'DECIMAL', fieldLength: 18, scale: 2, sortOrder: 3 },
  ];
  return {
    id: 7, entityCode: ENT, entityName: '客户', tableName: 't_customer',
    tenantCode: 'default', appCode: APP, fields,
  } as EntityDefDTO;
}

function resolvedFields() {
  const ws = createWorkspaceContext({ appCode: APP, tenantCode: 'default', entities: [] });
  return resolveEntityFields(entityDef(), ws);
}

interface ImportResultPayload {
  total: number;
  validCount: number;
  insertedCount: number;
  applied: boolean;
  rolledBack: boolean;
  ids: number[];
  errors: { index: number; message: string }[];
  warnings: { index: number; fieldCode: string | null; message: string }[];
  message: string | null;
}

function result(over: Partial<ImportResultPayload>): ImportResultPayload {
  return {
    total: 0, validCount: 0, insertedCount: 0, applied: false, rolledBack: false,
    ids: [], errors: [], warnings: [], message: null, ...over,
  };
}

let previewPayload: ImportResultPayload = result({});
let commitPayload: ImportResultPayload = result({});
const calls: { url: string; body: Record<string, unknown> }[] = [];

function stubBackend() {
  calls.length = 0;
  vi.stubGlobal('fetch', vi.fn(async (input: unknown, init?: RequestInit) => {
    const url = String(input);
    const body = (init?.body ? JSON.parse(String(init.body)) : {}) as Record<string, unknown>;
    calls.push({ url, body });
    if (url.includes('/runtime/import/preview')) {
      return respond({ success: true, code: 200, message: null, data: previewPayload });
    }
    if (url.includes('/runtime/import/commit')) {
      return respond({ success: true, code: 200, message: null, data: commitPayload });
    }
    return respond({ success: true, code: 200, message: null, data: null });
  }));
}

/** 表头 + n 条数据行。第 1 列是必填的 customer_name。 */
function csvWith(rows: number): string {
  const body = Array.from({ length: rows }, (_, i) => `客户${i + 1},${i + 1}0.5`).join('\n');
  return `customer_name,balance\n${body}\n`;
}

async function openAndMap(rows: number) {
  render(
    <ImportDialog
      open
      onClose={() => undefined}
      entity={entityDef()}
      resolvedFields={resolvedFields()}
      appCode={APP}
      tenantCode="default"
      onImported={onImported}
    />,
  );
  const input = document.getElementById('zlc-csv-file') as HTMLInputElement;
  fireEvent.change(input, {
    target: { files: [new File([csvWith(rows)], 'in.csv', { type: 'text/csv' })] },
  });
  await waitFor(() => expect(screen.getByText('实体字段')).toBeInTheDocument());
}

let onImported = () => undefined;
const imported: number[] = [];

async function runImport() {
  // 「导入 N 行」要等 CSV 解析完才出现，而 openAndMap 等的 '实体字段' 是 Steps 的静态标题 ——
  // 它在解析完成之前就在 DOM 里了。所以这里必须再等一步：不等待时单跑侥幸能过，
  // 一旦同机并行把 CPU 抢满（本仓库 12 个用例文件），这一句就会以
  // "Unable to find role button /导入 \d+ 行/" 红掉。等的是文案，点的是 role。
  await waitFor(() => expect(screen.queryAllByText(/导入 \d+ 行/).length).toBeGreaterThan(0));
  fireEvent.click(screen.getByRole('button', { name: /导入 \d+ 行/ }));
  await waitFor(() => expect(document.querySelector('[data-testid="zlc-import-summary"]')).toBeTruthy());
}

function summaryText(): string {
  return (document.querySelector('[data-testid="zlc-import-summary"]')?.textContent ?? '');
}

function summaryTone(): string {
  return (document.querySelector('[data-testid="zlc-import-summary"]')?.className ?? '');
}

function tagTexts(): string[] {
  return Array.from(document.querySelectorAll('.ant-tag')).map((node) => String(node.textContent ?? ''));
}

beforeEach(() => {
  window.localStorage.clear();
  stubBackend();
  imported.length = 0;
  onImported = () => {
    imported.push(1);
  };
});

describe('ImportDialog 结果口径', () => {
  it('写入中断且回滚没做完：不许出现一个绿色「成功」', async () => {
    previewPayload = result({ total: 3, validCount: 3 });
    commitPayload = result({
      total: 3,
      validCount: 3,
      applied: false,
      rolledBack: false,
      message: '写入中断：连接断开，回滚未完成，请人工核对',
    });
    await openAndMap(3);
    await runImport();

    expect(summaryText()).toContain('回滚未完成');
    expect(summaryTone()).toContain('ant-alert-error');
    expect(summaryText()).not.toContain('已导入');
    // 一行都不许被判成成功；状态未知才是诚实的答案
    expect(tagTexts().filter((text) => text.includes('成功'))).toEqual([]);
    expect(tagTexts().filter((text) => text.includes('未确认')).length).toBe(3);
    expect(imported.length).toBe(0);
  });

  it('回滚做完了就说"未写入"，仍然不算成功', async () => {
    previewPayload = result({ total: 3, validCount: 3 });
    commitPayload = result({
      total: 3,
      validCount: 3,
      applied: false,
      rolledBack: true,
      message: '写入中断：连接断开，已回滚本批 2 行',
    });
    await openAndMap(3);
    await runImport();

    expect(summaryText()).toContain('已回滚本批 2 行');
    expect(tagTexts().filter((text) => text.includes('成功'))).toEqual([]);
    expect(tagTexts().filter((text) => text.includes('失败')).length).toBe(3);
  });

  it('错误列表被服务端截到 50 条时，"多少行不合法"要用 total 减 validCount', async () => {
    previewPayload = result({
      total: 200,
      validCount: 140,
      errors: Array.from({ length: 50 }, (_, index) => ({ index, message: `字段 [客户名称] 不能为空 #${index}` })),
      warnings: Array.from({ length: 25 }, (_, index) => ({
        index, fieldCode: 'level', message: `值不在字典 lvl 里 ${index}`,
      })),
    });
    await openAndMap(200);
    await runImport();

    // 旧代码报的是 errors.length（50），把 60 行坏数据说成 50 行
    expect(summaryText()).toContain('200 行里有 60 行不合法');
    expect(summaryText()).toContain('整批一行都没写入');
    expect(summaryText()).toContain('另有 10 行未列出');
    // 警告列表只画 20 条，剩下的数量也要说出来
    expect(screen.getByText('25 处需要注意（值不在字典中等，不影响写入）')).toBeInTheDocument();
    expect(document.body.textContent).toContain('另有 5 处');
    // 有错就不该发 commit
    expect(calls.some((call) => call.url.includes('/runtime/import/commit'))).toBe(false);
  });

  it('服务端只登记了部分 id：不许按顺序猜哪几行进去了', async () => {
    previewPayload = result({ total: 3, validCount: 3 });
    commitPayload = result({
      total: 3, validCount: 3, insertedCount: 2, applied: true, ids: [11, 12],
    });
    await openAndMap(3);
    await runImport();

    expect(summaryText()).toContain('只登记了 2 个 id');
    expect(summaryTone()).toContain('ant-alert-warning');
    expect(tagTexts().filter((text) => text.includes('成功'))).toEqual([]);
    expect(tagTexts().filter((text) => text.includes('未确认')).length).toBe(3);
    // 数据可能已经进去一部分，列表必须刷新，否则用户看到的还是旧行数
    expect(imported.length).toBe(1);
  });

  it('全部写入才报成功，并且提交的是映射后的 fieldCode', async () => {
    previewPayload = result({ total: 3, validCount: 3 });
    commitPayload = result({
      total: 3, validCount: 3, insertedCount: 3, applied: true, ids: [11, 12, 13],
    });
    await openAndMap(3);
    await runImport();

    expect(summaryText()).toContain('已导入 3 行');
    expect(summaryTone()).toContain('ant-alert-success');
    expect(tagTexts().filter((text) => text.includes('成功')).length).toBe(3);
    expect(imported.length).toBe(1);

    const preview = calls.find((call) => call.url.includes('/runtime/import/preview'));
    expect(preview?.url).toContain('entityCode=customer');
    const records = preview?.body.records as Record<string, unknown>[];
    expect(records.length).toBe(3);
    expect(records[0]?.customer_name).toBe('客户1');
    expect(String(records[0]?.balance)).toBe('10.5');
  });
});
