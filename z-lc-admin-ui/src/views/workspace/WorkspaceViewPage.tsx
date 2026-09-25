import { useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Alert, Button, Empty, Typography } from 'antd';
import { PartitionOutlined } from '@ant-design/icons';
import { VIEW_TYPES, type ViewType } from '@/api/types';
import { useWorkspace } from '@/hooks/useWorkspace';
import { resolveEntityFields } from '@/fields';
import { ErrorBlock, LoadingBlock, StateBlock } from '@/components/StateBlock';
import { unreadReason } from '@/api/meta';
import { GridView } from './GridView';
import { RecordForm } from './RecordForm';
import { DetailView } from './DetailView';
import { KanbanView } from './KanbanView';
import { GalleryView } from './GalleryView';
import { CalendarView } from './CalendarView';
import { ChartView } from './ChartView';
import { PivotView } from './PivotView';
import type { FilterState } from '@/views/grid/FilterBar';

const { Text } = Typography;

/**
 * The entity workspace: one screen per (app, entity, viewType), all three
 * modes reading the same metadata so switching never refetches.
 */
export function WorkspaceViewPage() {
  const { appCode = '', entityCode = '', viewType = 'LIST', recordId } = useParams();
  const navigate = useNavigate();
  const { ctx, meta, isLoading, isError, error, refetch } = useWorkspace(appCode);
  // 筛选状态放在页面级: 表格和看板共用同一份，切换视图不会把筛选弄丢
  const [filter, setFilter] = useState<FilterState>({ conditions: [], conjunction: 'AND' });

  const entity = useMemo(
    () => (meta?.entities ?? []).find((item) => item.entityCode === entityCode),
    [meta?.entities, entityCode],
  );

  const resolvedFields = useMemo(
    () => (entity ? resolveEntityFields(entity, ctx) : []),
    [entity, ctx],
  );

  const activeView: ViewType = VIEW_TYPES.includes(viewType as ViewType)
    ? (viewType as ViewType)
    : 'LIST';

  const goto = (nextView: ViewType, id?: number) => {
    const base = `/${appCode}/${entityCode}/${nextView}`;
    navigate(id ? `${base}/${id}` : base);
  };

  if (isLoading && !meta) return <LoadingBlock />;

  if (isError) {
    return (
      <div style={{ padding: 24 }}>
        <StateBlock isLoading={false} isError error={error} isEmpty={false} onRetry={() => void refetch()}>
          <span />
        </StateBlock>
      </div>
    );
  }

  if (!entity) {
    // fetchWorkspaceMeta 从不抛出：单接口失败只被记进 meta.degraded，所以 isError 永远抓不到
    // 元数据故障。此时 entities 是空的，"实体不存在"就是谎话 —— 用户会以为数据被删了。
    if (!meta?.read.entities) {
      const why = unreadReason(meta, 'entities');
      return (
        <div style={{ padding: 24 }}>
          <ErrorBlock
            title="实体元数据没读到"
            error={new Error(
              `无法确认「${appCode}」里是否存在实体「${entityCode}」（${why}）。这是加载故障，不代表实体已被删除。`,
            )}
            onRetry={() => void refetch()}
          />
        </div>
      );
    }
    return (
      <div style={{ padding: 48 }}>
        <Empty
          description={
            <Text type="secondary">
              实体「{entityCode}」不存在，可能已被删除，或在设计器里还没建。
            </Text>
          }
        >
          <Button icon={<PartitionOutlined />} onClick={() => navigate(`/designer/${appCode}`)}>
            去模型设计器
          </Button>
        </Empty>
      </div>
    );
  }

  if (resolvedFields.length === 0) {
    return (
      <div style={{ padding: 24 }}>
        <Alert
          type="info"
          showIcon
          message="这个实体还没有字段"
          description="在模型设计器里给它加上字段并 provision 物理表后，这里就能直接录入数据。"
          action={
            <Button size="small" onClick={() => navigate(`/designer/${appCode}`)}>
              去设计字段
            </Button>
          }
        />
      </div>
    );
  }

  if (activeView === 'FORM') {
    return (
      <RecordForm
        key={`form-${entityCode}-${recordId ?? 'new'}`}
        entity={entity}
        resolvedFields={resolvedFields}
        appCode={appCode}
        tenantCode={ctx.tenantCode}
        recordId={recordId ? Number(recordId) : undefined}
        onSaved={(id) => goto('DETAIL', id)}
        onCancel={() => goto('LIST')}
      />
    );
  }

  if (activeView === 'KANBAN') {
    return (
      <KanbanView
        key={`kb-${entityCode}`}
        entity={entity}
        resolvedFields={resolvedFields}
        appCode={appCode}
        tenantCode={ctx.tenantCode}
        conditions={filter.conditions}
        conjunction={filter.conjunction}
      />
    );
  }

  if (activeView === 'DETAIL') {
    return (
      <DetailView
        entity={entity}
        resolvedFields={resolvedFields}
        appCode={appCode}
        tenantCode={ctx.tenantCode}
        recordId={recordId ? Number(recordId) : undefined}
        onEdit={(id) => goto('FORM', id)}
        onBack={() => goto('LIST')}
      />
    );
  }

  if (activeView === 'GALLERY') {
    return (
      <GalleryView
        key={`gallery-${entityCode}`}
        entity={entity}
        resolvedFields={resolvedFields}
        appCode={appCode}
        tenantCode={ctx.tenantCode}
        conditions={filter.conditions}
        conjunction={filter.conjunction}
        onOpenRecord={(id) => goto('DETAIL', id)}
        onCreateRecord={() => goto('FORM')}
      />
    );
  }

  if (activeView === 'CALENDAR') {
    return (
      <CalendarView
        key={`cal-${entityCode}`}
        entity={entity}
        resolvedFields={resolvedFields}
        appCode={appCode}
        tenantCode={ctx.tenantCode}
        conditions={filter.conditions}
        conjunction={filter.conjunction}
        onOpenRecord={(id) => goto('DETAIL', id)}
        onCreateRecord={() => goto('FORM')}
      />
    );
  }

  // 图表配置也是"按实体存"的 localStorage 状态，必须和 GridView 一样带 entityCode
  // 做 key，否则切换实体会把上一个实体的分组列/图表类型漏过来。
  if (activeView === 'CHART') {
    return (
      <ChartView
        key={`chart-${entityCode}`}
        entity={entity}
        resolvedFields={resolvedFields}
        views={meta?.views ?? []}
        appCode={appCode}
        tenantCode={ctx.tenantCode}
        conditions={filter.conditions}
        conjunction={filter.conjunction}
        onFilterChange={setFilter}
      />
    );
  }

  // 交叉表与图表同为"按实体存"的本地配置，key 必须带 entityCode，
  // 否则切换实体时会把上一个实体的行/列维度漏过来（与 CHART 同一类缺陷）。
  if (activeView === 'PIVOT') {
    return (
      <PivotView
        key={`pivot-${entityCode}`}
        entity={entity}
        resolvedFields={resolvedFields}
        views={meta?.views ?? []}
        appCode={appCode}
        tenantCode={ctx.tenantCode}
        conditions={filter.conditions}
        conjunction={filter.conjunction}
      />
    );
  }

  // key 必须带上 entityCode：同一路由段切换实体时 React 会复用同一个组件实例，
  // 而 GridView 的列布局/统计是 useState 懒加载自 localStorage 的，只跑一次 ——
  // 不重新挂载就会把 A 实体的列顺序和统计带到 B 实体上（实测确认）。
  return (
    <GridView
      key={entityCode}
      entity={entity}
      resolvedFields={resolvedFields}
      views={meta?.views ?? []}
      appCode={appCode}
      tenantCode={ctx.tenantCode}
      onEditRecord={(id) => goto('FORM', id)}
      onOpenRecord={(id) => goto('DETAIL', id)}
      onCreateRecord={() => goto('FORM')}
      conditions={filter.conditions}
      conjunction={filter.conjunction}
      onFilterChange={setFilter}
    />
  );
}
