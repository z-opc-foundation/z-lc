import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { Alert, Button, Empty, Input, Popconfirm, Select, Space, Typography, message } from 'antd';
import { DeleteOutlined, PlusOutlined, SaveOutlined } from '@ant-design/icons';
import { useWorkspace } from '@/hooks/useWorkspace';
import { resolveEntityFields } from '@/fields';
import { createViewConfig, deleteViewConfig, newViewConfigDraft, updateViewConfig } from '@/api/viewConfig';
import type { ResolvedField } from '@/fields';
import { LoadingBlock, StateBlock, ErrorBlock } from '@/components/StateBlock';
import { unreadReason } from '@/api/meta';
import { namedViews, viewLabel } from './viewConfigModel';
import { ChartTile, MissingTile } from './ChartTile';
import {
  APP_LEVEL_ENTITY_CODE,
  DASHBOARD_VIEW_TYPE,
  DEFAULT_TILE_WIDTH,
  dashboardConfig,
  parseDashboard,
  pickableChartViews,
  resolveWidgets,
  type DashboardModel,
  type TileWidth,
} from './dashboardModel';

const { Text } = Typography;

const BLANK: DashboardModel = { name: '未命名仪表盘', widgets: [] };

interface Draft {
  /** null 表示这是一份还没落库的新仪表盘。 */
  id: number | null;
  model: DashboardModel;
}

/**
 * 仪表盘：把多张**已落库的命名图表视图**拼成一屏。
 *
 * 组件存的是图表视图的 id，不是图表配置的副本 —— 所以"在图表页改了口径"会同时
 * 反映到所有用到它的仪表盘上；反过来说，删掉一个图表视图会让仪表盘上的组件变成
 * 一块写着"视图 #id 已被删除"的牌子，而不是悄悄少一张图。
 *
 * 这一页自己也落库（viewType=DASHBOARD，entity_code 用应用级占位值），所以
 * 同应用成员打开同一个链接看到的是同一屏，换浏览器也不会只剩一个空壳。
 */
export function DashboardPage() {
  const { appCode = '', dashboardId } = useParams();
  const navigate = useNavigate();
  const { ctx, meta, isLoading, isError, error, refetch } = useWorkspace(appCode);
  const views = useMemo(() => meta?.views ?? [], [meta?.views]);

  const dashboards = useMemo(
    () => namedViews(views, APP_LEVEL_ENTITY_CODE, DASHBOARD_VIEW_TYPE),
    [views],
  );
  // URL 没带编号时取最新的一份（后端按 id 倒序返回），省掉"打开页面还要再选一次"
  const active = useMemo(
    () => dashboards.find((view) => view.id === Number(dashboardId)) ?? dashboards[0] ?? null,
    [dashboards, dashboardId],
  );
  const stored = useMemo(() => parseDashboard(active), [active]);

  const [draft, setDraft] = useState<Draft | null>(null);
  // 切仪表盘 / 切应用都必须丢掉上一份未保存的草稿：这条路由的组件实例是复用的，
  // 不重置的话"A 应用改了一半的布局"会出现在 B 应用的仪表盘上（图表页栽过同一个坑）。
  useEffect(() => {
    setDraft(null);
  }, [active?.id, appCode]);

  const model = draft?.model ?? stored;
  const editingId = draft ? draft.id : active?.id ?? null;
  const dirty = draft !== null;

  const edit = useCallback(
    (mutate: (model: DashboardModel) => DashboardModel) => {
      setDraft((prev) => {
        const base = prev?.model ?? stored ?? BLANK;
        return { id: prev ? prev.id : active?.id ?? null, model: mutate(base) };
      });
    },
    [stored, active?.id],
  );

  const fieldsByEntity = useMemo(() => {
    const map = new Map<string, ResolvedField[]>();
    for (const entity of meta?.entities ?? []) {
      if (entity.entityCode) map.set(entity.entityCode, resolveEntityFields(entity, ctx));
    }
    return map;
  }, [meta?.entities, ctx]);

  const entityNameByCode = useMemo(() => {
    const map = new Map<string, string>();
    for (const entity of meta?.entities ?? []) {
      if (entity.entityCode) map.set(entity.entityCode, entity.entityName || entity.entityCode);
    }
    return map;
  }, [meta?.entities]);

  const widgets = useMemo(() => resolveWidgets(model, views), [model, views]);
  const chartViews = useMemo(() => pickableChartViews(views), [views]);
  const addable = useMemo(
    () => chartViews.filter((view) => !widgets.some((widget) => widget.viewId === view.id)),
    [chartViews, widgets],
  );

  const addableOptions = useMemo(() => {
    const byEntity = new Map<string, { value: number; label: string }[]>();
    for (const view of addable) {
      const group = byEntity.get(view.entityCode) ?? [];
      group.push({ value: view.id as number, label: viewLabel(view) });
      byEntity.set(view.entityCode, group);
    }
    return Array.from(byEntity, ([entityCode, options]) => ({
      label: entityNameByCode.get(entityCode) ?? entityCode,
      options,
    }));
  }, [addable, entityNameByCode]);

  const save = useCallback(async () => {
    if (!model) return;
    const payload = JSON.stringify(dashboardConfig(model));
    try {
      if (editingId) {
        await updateViewConfig({ id: editingId, viewType: DASHBOARD_VIEW_TYPE, config: payload });
        setDraft(null);
        message.success('仪表盘已更新');
        void refetch();
      } else {
        const created = await createViewConfig(
          newViewConfigDraft({
            appCode,
            entityCode: APP_LEVEL_ENTITY_CODE,
            viewType: DASHBOARD_VIEW_TYPE,
            config: dashboardConfig(model),
            tenantCode: ctx.tenantCode,
          }),
        );
        setDraft(null);
        message.success('仪表盘已保存，同应用成员可见');
        if (created?.id) navigate(`/${appCode}/dashboard/${created.id}`, { replace: true });
        // 必须重拉一次 workspace：地址栏已经指向新编号，但 meta.views 里还没有这一行，
        // 不重拉的话页面会回落到"最新的一份"，用户看着像保存到了别的仪表盘上。
        void refetch();
      }
    } catch (err) {
      message.error(err instanceof Error ? err.message : '保存失败');
    }
  }, [model, editingId, appCode, ctx.tenantCode, navigate, refetch]);

  const removeDashboard = useCallback(async () => {
    if (!active?.id) return;
    try {
      await deleteViewConfig(active.id);
      setDraft(null);
      message.success('仪表盘已删除（图表视图本身没动）');
      navigate(`/${appCode}/dashboard`, { replace: true });
      void refetch();
    } catch (err) {
      message.error(err instanceof Error ? err.message : '删除失败');
    }
  }, [active?.id, appCode, navigate, refetch]);

  if (isLoading && !meta) return <LoadingBlock label="加载仪表盘" />;
  if (isError) return <StateBlock isLoading={false} isError error={error} isEmpty={false} onRetry={() => void refetch()}><span /></StateBlock>;

  // 这一整页都建立在 views 上：仪表盘列表是 views 里的行，每个组件又按 id 引用一条图表视图。
  // views 没读到时逐块渲染"视图 #id 已被删除"，等于把一次加载故障说成用户把自己存的图全删了。
  if (meta && !meta.read.views) {
    return (
      <ErrorBlock
        title="图表视图元数据没读到"
        error={new Error(
          `无法确认「${appCode}」里有哪些仪表盘和图表视图（${unreadReason(meta, 'views')}）。这是加载故障，不代表视图已被删除。`,
        )}
        onRetry={() => void refetch()}
      />
    );
  }

  return (
    <div className="zlc-dash">
      <Space wrap className="zlc-dash-toolbar" size={8}>
        <Select
          className="zlc-dash-switch"
          style={{ minWidth: 190 }}
          placeholder="选择仪表盘"
          value={active?.id}
          options={dashboards.map((view) => ({ value: view.id as number, label: viewLabel(view) }))}
          onChange={(id: number) => navigate(`/${appCode}/dashboard/${id}`)}
        />
        <Input
          className="zlc-dash-name"
          style={{ width: 200 }}
          value={model?.name ?? ''}
          placeholder="仪表盘名称"
          onChange={(event) => edit((current) => ({ ...current, name: event.target.value }))}
        />
        <Button
          className="zlc-dash-new"
          icon={<PlusOutlined />}
          onClick={() => setDraft({ id: null, model: { ...BLANK, name: `仪表盘 ${dashboards.length + 1}` } })}
        >
          新建
        </Button>
        <Select
          className="zlc-dash-add"
          style={{ minWidth: 220 }}
          placeholder={chartViews.length ? '添加图表视图' : '还没有命名图表视图'}
          value={undefined}
          options={addableOptions}
          disabled={addable.length === 0}
          onChange={(viewId: number) =>
            edit((current) => ({
              ...current,
              widgets: [...current.widgets, { viewId, width: DEFAULT_TILE_WIDTH }],
            }))
          }
        />
        <Button
          className="zlc-dash-save"
          type="primary"
          icon={<SaveOutlined />}
          disabled={!dirty || !model}
          onClick={() => void save()}
        >
          保存
        </Button>
        {active?.id ? (
          <Popconfirm
            title="删除这个仪表盘？"
            description="只删仪表盘本身，里面引用的图表视图会保留。"
            okText="删除"
            cancelText="取消"
            onConfirm={() => void removeDashboard()}
          >
            <Button className="zlc-dash-delete" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        ) : null}
        {dirty ? (
          <Text type="warning" className="zlc-dash-dirty">
            有未保存的改动
          </Text>
        ) : null}
      </Space>

      {active && !stored ? (
        <Alert
          type="error"
          showIcon
          message={`视图 #${active.id} 存的不是仪表盘配置`}
          description="这一行的 config 里没有组件清单（可能是管理页手改过的表格视图）。保存当前这份会覆盖它，请先在视图管理页确认。"
        />
      ) : null}

      {!meta?.read.entities ? (
        <Alert
          type="warning"
          showIcon
          message="实体元数据没读到，下面的图暂时取不到字段"
          description={`${unreadReason(meta, 'entities')}。这是加载故障，不代表实体或图被删除，刷新这一页即可再试。`}
        />
      ) : null}

      {!model || model.widgets.length === 0 ? (
        <Empty
          className="zlc-dash-empty"
          description={
            chartViews.length === 0
              ? '还没有可放进仪表盘的图表视图。先到某个实体的「图表」页把一张图存成命名视图。'
              : active
                ? '这个仪表盘还是空的，用上面的「添加图表视图」放几张图进来。'
                : // 一份仪表盘都没有时说"这个仪表盘是空的"是在指一个不存在的东西
                  '还没有仪表盘。放几张图进来再保存，就会新建一份；也可以先「新建」起个名字。'
          }
        >
          {chartViews.length === 0 ? (
            <Link to={`/${appCode}`}>
              <Button>去选一个实体</Button>
            </Link>
          ) : null}
        </Empty>
      ) : (
        <div className="zlc-dash-grid">
          {widgets.map((widget) => {
            const remove = () =>
              edit((current) => ({ ...current, widgets: current.widgets.filter((item) => item.viewId !== widget.viewId) }));
            const resize = (width: TileWidth) =>
              edit((current) => ({
                ...current,
                widgets: current.widgets.map((item) => (item.viewId === widget.viewId ? { ...item, width } : item)),
              }));

            if (!widget.view) {
              return (
                <MissingTile
                  key={widget.viewId}
                  width={widget.width}
                  message={`图表视图 #${widget.viewId} 已被删除`}
                  onRemove={remove}
                />
              );
            }
            if (!widget.snapshot) {
              return (
                <MissingTile
                  key={widget.viewId}
                  width={widget.width}
                  message={`图表视图「${viewLabel(widget.view)}」存的不是图表配置`}
                  onRemove={remove}
                />
              );
            }
            const resolvedFields = fieldsByEntity.get(widget.entityCode);
            if (!resolvedFields) {
              // 只有在实体列表确实读到的情况下，"取不到字段"才能归因于实体被删；否则这句
              // 就是猜的 —— 一次 /app/schema 故障会把每张图都判成"源实体已删除"。
              const entityGone = meta?.read.entities;
              return (
                <MissingTile
                  key={widget.viewId}
                  width={widget.width}
                  message={
                    entityGone
                      ? `实体「${widget.entityCode}」已被删除，图表视图「${viewLabel(widget.view)}」取不到字段`
                      : `实体「${widget.entityCode}」的字段没读到（${unreadReason(meta, 'entities')}），图表视图「${viewLabel(widget.view)}」取不到数`
                  }
                  description={
                    entityGone
                      ? undefined
                      : '图和视图都还在，只是这次没拿到实体字段。刷新页面就会重新加载，先别移除这个组件。'
                  }
                  onRemove={remove}
                />
              );
            }
            return (
              <ChartTile
                key={widget.viewId}
                appCode={appCode}
                tenantCode={ctx.tenantCode}
                entityCode={widget.entityCode}
                entityLabel={entityNameByCode.get(widget.entityCode) ?? widget.entityCode}
                resolvedFields={resolvedFields}
                title={viewLabel(widget.view)}
                config={widget.snapshot.config}
                conditions={widget.snapshot.conditions}
                conjunction={widget.snapshot.conjunction}
                width={widget.width}
                onWidthChange={resize}
                onRemove={remove}
              />
            );
          })}
        </div>
      )}
    </div>
  );
}
