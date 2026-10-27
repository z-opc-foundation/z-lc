import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Empty,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Select,
  Space,
  Table,
  message,
} from 'antd';
import { DeleteOutlined, EyeOutlined, PlusOutlined, SaveOutlined } from '@ant-design/icons';
import UniversalRenderer from '@yuku123/z-frontend-render';
import type { RenderData } from '@yuku123/z-frontend-render';
import { listApps } from '@/api/app';
import { createRecord } from '@/api/runtime';
import {
  createPage,
  deletePage,
  listPages,
  renderPage,
  updatePage,
} from '@/api/page';
import type {
  PageDTO,
  PageDatasetBinding,
  PageLayoutCell,
  PageRenderData,
  PageSchema,
  PageWidgetSpec,
  PageWidgetType,
} from '@/api/page';

/**
 * 页面编排 (V2 页面域, lead 013 §四): 选应用 → 页面列表 → 编辑(部件+12栅格) → 预览。
 * 渲染走 UniversalRenderer; FORM 部件按回放字段画表单, 提交走 RuntimeCrud
 * (零脚本缺省实现; V3 虚拟接口接入后可改绑 vApi)。
 * 第一步用坐标表单编排, 拖拽留到接入 dnd 后再加。
 */

const WIDGET_TYPES: { value: PageWidgetType; label: string }[] = [
  { value: 'TABLE', label: '表格' },
  { value: 'BAR', label: '柱状图' },
  { value: 'LINE', label: '折线图' },
  { value: 'PIE', label: '饼图' },
  { value: 'KPI', label: '指标卡' },
  { value: 'FORM', label: '表单(绑实体)' },
];

const AGGS = ['SUM', 'COUNT', 'AVG', 'MIN', 'MAX'] as const;

interface WidgetDraft {
  id: string;
  type: PageWidgetType;
  title: string;
  sourceId: string;
  baseTable: string;
  encodeX: string;
  encodeY: string;
  agg: string;
  entityCode: string;
  x: number;
  y: number;
  w: number;
  h: number;
}

let widgetSeq = 1;

function emptyWidget(): WidgetDraft {
  const id = `w${widgetSeq++}`;
  return { id, type: 'TABLE', title: '', sourceId: '', baseTable: '', encodeX: '', encodeY: '', agg: 'SUM', entityCode: '', x: 0, y: 0, w: 6, h: 4 };
}

/** PageDTO.schema(JSON 字符串) → 编辑草稿; 坏结构降级为空页 (保存时会被后端校验拦下) */
function parseDraft(dto: PageDTO): { pageName: string; widgets: WidgetDraft[] } {
  let schema: PageSchema | null = null;
  try {
    schema = dto.schema ? (JSON.parse(dto.schema) as PageSchema) : null;
  } catch {
    schema = null;
  }
  const widgets: WidgetDraft[] = [];
  if (schema && Array.isArray(schema.widgets)) {
    const datasets = schema.datasets ?? {};
    for (const w of schema.widgets) {
      const cell: PageLayoutCell | undefined = (schema.layout ?? []).find((c) => c.widgetId === w.id);
      const ds = w.datasetId ? datasets[w.datasetId] : undefined;
      widgets.push({
        id: w.id,
        type: w.type,
        title: w.title ?? '',
        sourceId: ds?.sourceId ?? '',
        baseTable: ds?.baseTable ?? '',
        encodeX: w.encode?.x ?? '',
        encodeY: w.encode?.y ?? '',
        agg: w.agg ?? 'SUM',
        entityCode: w.entityCode ?? '',
        x: cell?.x ?? 0,
        y: cell?.y ?? 0,
        w: cell?.w ?? 6,
        h: cell?.h ?? 4,
      });
    }
    const maxSeq = widgets.reduce((m, w) => Math.max(m, Number(/^w(\d+)$/.exec(w.id)?.[1] ?? 0)), 0);
    widgetSeq = Math.max(widgetSeq, maxSeq + 1);
  }
  return { pageName: dto.pageName ?? '', widgets };
}

/** 草稿 → schema JSON; 非法值在这里就拦, 不等后端 400 */
function draftToSchema(widgets: WidgetDraft[]): PageSchema {
  const datasets: Record<string, PageDatasetBinding> = {};
  const layout: PageLayoutCell[] = [];
  const specs: PageWidgetSpec[] = [];
  widgets.forEach((w) => {
    if (w.type !== 'FORM') {
      const key = `${w.sourceId}::${w.baseTable}`;
      datasets[key] = { sourceId: w.sourceId.trim(), baseTable: w.baseTable.trim() };
      const spec: PageWidgetSpec = { id: w.id, type: w.type, datasetId: key };
      if (w.title.trim()) spec.title = w.title.trim();
      if (w.encodeX.trim() || w.encodeY.trim()) {
        spec.encode = { x: w.encodeX.trim() || undefined, y: w.encodeY.trim() || undefined };
      }
      spec.agg = w.agg as PageWidgetSpec['agg'];
      specs.push(spec);
    } else {
      const spec: PageWidgetSpec = { id: w.id, type: 'FORM', entityCode: w.entityCode.trim() };
      if (w.title.trim()) spec.title = w.title.trim();
      specs.push(spec);
    }
    layout.push({ widgetId: w.id, x: w.x, y: w.y, w: w.w, h: w.h });
  });
  return { version: 1, datasets, layout, widgets: specs };
}

export function PagesPage() {
  const [apps, setApps] = useState<{ appCode: string; appName: string }[]>([]);
  const [appCode, setAppCode] = useState<string>('');
  const [pages, setPages] = useState<PageDTO[]>([]);
  const [selected, setSelected] = useState<PageDTO | null>(null);
  const [pageName, setPageName] = useState('');
  const [widgets, setWidgets] = useState<WidgetDraft[]>([]);
  const [preview, setPreview] = useState<PageRenderData | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    listApps()
      .then((list) => {
        setApps(list.map((a) => ({ appCode: a.appCode, appName: a.appName })));
        const first = list[0];
        if (first) setAppCode((cur) => cur || first.appCode);
      })
      .catch(() => setApps([]));
  }, []);

  const reloadPages = useCallback((code: string) => {
    if (!code) {
      setPages([]);
      return;
    }
    listPages(code)
      .then(setPages)
      .catch(() => setPages([]));
  }, []);

  useEffect(() => {
    setSelected(null);
    setPreview(null);
    reloadPages(appCode);
  }, [appCode, reloadPages]);

  const openPage = (dto: PageDTO) => {
    setSelected(dto);
    setPreview(null);
    const draft = parseDraft(dto);
    setPageName(draft.pageName);
    setWidgets(draft.widgets);
  };

  const newPage = () => {
    Modal.confirm({
      title: '新建页面',
      content: (
        <Form layout="vertical">
          <Form.Item label="页面标识 (pageCode)" required>
            <Input id="new-page-code" placeholder="如 p_main" defaultValue={`p_${Date.now() % 10000}`} />
          </Form.Item>
          <Form.Item label="页面名称">
            <Input id="new-page-name" placeholder="如 主页" defaultValue="新页面" />
          </Form.Item>
        </Form>
      ),
      onOk: async () => {
        const pageCode = (document.getElementById('new-page-code') as HTMLInputElement | null)?.value.trim();
        const name = (document.getElementById('new-page-name') as HTMLInputElement | null)?.value.trim();
        if (!pageCode || !appCode) {
          message.error('页面标识必填');
          return;
        }
        const dto = await createPage({
          appCode,
          pageCode,
          pageName: name || '新页面',
          schema: JSON.stringify(draftToSchema([])),
        });
        message.success('页面已创建');
        reloadPages(appCode);
        openPage(dto);
      },
    });
  };

  const savePage = async () => {
    if (!selected) return;
    try {
      const schema = draftToSchema(widgets);
      const dto = await updatePage({ id: selected.id as number, pageName: pageName || selected.pageName || '', schema: JSON.stringify(schema) });
      message.success('已保存');
      setSelected(dto);
      reloadPages(appCode);
    } catch {
      // 错误详情由全局错误总线吐司
    }
  };

  const renderPreview = async () => {
    if (!selected) return;
    setLoading(true);
    try {
      setPreview(await renderPage(appCode, selected.pageCode));
    } catch {
      setPreview(null);
    } finally {
      setLoading(false);
    }
  };

  const removePage = async () => {
    if (!selected) return;
    await deletePage(selected.id as number);
    message.success('已删除');
    setSelected(null);
    setPreview(null);
    reloadPages(appCode);
  };

  const updateWidget = (id: string, patch: Partial<WidgetDraft>) => {
    setWidgets((list) => list.map((w) => (w.id === id ? { ...w, ...patch } : w)));
  };

  const previewData: RenderData | null = useMemo(() => {
    if (!preview) return null;
    return { layout: preview.layout ?? [], options: preview.options ?? {} } as RenderData;
  }, [preview]);

  const widgetColumns = [
    {
      title: '类型', dataIndex: 'type', width: 120,
      render: (_: unknown, w: WidgetDraft) => (
        <Select size="small" value={w.type} style={{ width: 110 }}
          onChange={(v) => updateWidget(w.id, { type: v })}
          options={WIDGET_TYPES} />
      ),
    },
    {
      title: '标题', dataIndex: 'title', width: 120,
      render: (_: unknown, w: WidgetDraft) => (
        <Input size="small" value={w.title} onChange={(e) => updateWidget(w.id, { title: e.target.value })} />
      ),
    },
    {
      title: '数据源', dataIndex: 'sourceId', width: 130,
      render: (_: unknown, w: WidgetDraft) => w.type === 'FORM'
        ? <Input size="small" value={w.entityCode} placeholder="entityCode" onChange={(e) => updateWidget(w.id, { entityCode: e.target.value })} />
        : <Input size="small" value={w.sourceId} placeholder="z-dms 源 id" onChange={(e) => updateWidget(w.id, { sourceId: e.target.value })} />,
    },
    {
      title: '表 / 编码', dataIndex: 'baseTable', width: 130,
      render: (_: unknown, w: WidgetDraft) => w.type === 'FORM'
        ? <span style={{ color: '#999' }}>运行时实体</span>
        : <Input size="small" value={w.baseTable} placeholder="表名" onChange={(e) => updateWidget(w.id, { baseTable: e.target.value })} />,
    },
    {
      title: 'X 轴', dataIndex: 'encodeX', width: 100,
      render: (_: unknown, w: WidgetDraft) => w.type === 'FORM'
        ? null
        : <Input size="small" value={w.encodeX} placeholder="字段" onChange={(e) => updateWidget(w.id, { encodeX: e.target.value })} />,
    },
    {
      title: 'Y 轴', dataIndex: 'encodeY', width: 100,
      render: (_: unknown, w: WidgetDraft) => w.type === 'FORM'
        ? null
        : <Input size="small" value={w.encodeY} placeholder="度量" onChange={(e) => updateWidget(w.id, { encodeY: e.target.value })} />,
    },
    {
      title: '聚合', dataIndex: 'agg', width: 95,
      render: (_: unknown, w: WidgetDraft) => w.type === 'FORM'
        ? null
        : <Select size="small" value={w.agg} style={{ width: 80 }} onChange={(v) => updateWidget(w.id, { agg: v })}
            options={AGGS.map((a) => ({ value: a, label: a }))} />,
    },
    {
      title: '栅格 x/y/w/h', width: 210,
      render: (_: unknown, w: WidgetDraft) => (
        <Space size={4}>
          {(['x', 'y', 'w', 'h'] as const).map((k) => (
            <InputNumber key={k} size="small" min={k === 'x' ? 0 : k === 'w' ? 1 : 0} max={k === 'x' ? 11 : k === 'w' ? 12 : undefined}
              value={w[k]} style={{ width: 44 }}
              onChange={(v) => updateWidget(w.id, { [k]: v ?? 0 } as Partial<WidgetDraft>) } />
          ))}
        </Space>
      ),
    },
    {
      title: '', width: 50,
      render: (_: unknown, w: WidgetDraft) => (
        <Button size="small" type="text" danger icon={<DeleteOutlined />}
          onClick={() => setWidgets((list) => list.filter((x) => x.id !== w.id))} />
      ),
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Card style={{ marginBottom: 16 }}>
        <Space wrap>
          <span>应用：</span>
          <Select style={{ width: 220 }} value={appCode || undefined}
            onChange={(v) => setAppCode(v)}
            options={apps.map((a) => ({ value: a.appCode, label: `${a.appName} (${a.appCode})` }))}
            placeholder="选择应用" />
          <Button type="primary" icon={<PlusOutlined />} disabled={!appCode} onClick={newPage}>新建页面</Button>
        </Space>
      </Card>

      <div style={{ display: 'flex', gap: 16, alignItems: 'flex-start' }}>
        <Card title="页面列表" style={{ width: 320 }} styles={{ body: { padding: 0 } }}>
          {pages.length === 0 ? (
            <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无页面" style={{ padding: 24 }} />
          ) : (
            pages.map((p) => (
              <div key={p.id}
                onClick={() => openPage(p)}
                style={{
                  padding: '10px 16px', cursor: 'pointer',
                  background: selected?.id === p.id ? '#e6f4ff' : undefined,
                  borderBottom: '1px solid #f0f0f0',
                }}>
                <div style={{ fontWeight: 600 }}>{p.pageName || p.pageCode}</div>
                <div style={{ color: '#888', fontSize: 12 }}>{p.pageCode}</div>
              </div>
            ))
          )}
        </Card>

        <div style={{ flex: 1, minWidth: 0 }}>
          {!selected ? (
            <Card><Empty description="从左侧选择页面, 或新建一个" /></Card>
          ) : (
            <>
              <Card title={`编辑: ${selected.pageCode}`} style={{ marginBottom: 16 }}
                extra={
                  <Space>
                    <Input value={pageName} onChange={(e) => setPageName(e.target.value)} style={{ width: 140 }} placeholder="页面名称" />
                    <Button icon={<SaveOutlined />} type="primary" onClick={savePage}>保存</Button>
                    <Button icon={<EyeOutlined />} onClick={renderPreview} loading={loading}>预览</Button>
                    <Popconfirm title="确认删除该页面?" onConfirm={removePage}>
                      <Button danger>删除</Button>
                    </Popconfirm>
                  </Space>
                }>
                <Button size="small" icon={<PlusOutlined />} style={{ marginBottom: 8 }}
                  onClick={() => setWidgets((list) => [...list, emptyWidget()])}>添加部件</Button>
                <Table rowKey="id" size="small" pagination={false}
                  dataSource={widgets} columns={widgetColumns} />
              </Card>

              {preview && (
                <Card title={`预览: ${preview.pageName || selected.pageCode}`}>
                  <UniversalRenderer renderData={previewData} />
                  {Object.entries(preview.forms ?? {}).map(([widgetId, form]) => (
                    <FormPageCard key={widgetId} appCode={appCode} widgetId={widgetId} form={form} />
                  ))}
                </Card>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}

/** FORM 部件: 按回放字段清单画表单, 提交走 RuntimeCrud (零脚本缺省) */
function FormPageCard({ appCode, widgetId, form }: {
  appCode: string;
  widgetId: string;
  form: { entityCode: string; entityName?: string; title?: string; fields: { code: string; name: string; type: string; required: boolean }[] };
}) {
  const [formInst] = Form.useForm();
  const [submitting, setSubmitting] = useState(false);

  const submit = async (values: Record<string, unknown>) => {
    setSubmitting(true);
    try {
      await createRecord(form.entityCode, values, { appCode });
      message.success('已提交');
      formInst.resetFields();
    } catch {
      // 全局错误总线已提示
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Card size="small" title={form.title || `${form.entityName || form.entityCode} 表单`} style={{ marginTop: 16 }} data-widget-id={widgetId}>
      {form.fields.length === 0 ? (
        <Alert type="warning" showIcon message={`实体 ${form.entityCode} 未回放出字段 (确认应用内已建模)`} />
      ) : (
        <Form form={formInst} layout="inline" onFinish={submit} style={{ rowGap: 8 }}>
          {form.fields.map((f) => (
            <Form.Item key={f.code} name={f.code} label={f.name}
              rules={f.required ? [{ required: true, message: `${f.name} 必填` }] : []}>
              <Input placeholder={f.type} style={{ width: 140 }} />
            </Form.Item>
          ))}
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={submitting}>提交</Button>
          </Form.Item>
        </Form>
      )}
    </Card>
  );
}
