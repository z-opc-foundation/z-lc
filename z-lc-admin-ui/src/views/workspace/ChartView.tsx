import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Button,
  Input,
  InputNumber,
  Modal,
  Segmented,
  Select,
  Space,
  Tooltip,
  Typography,
  message,
} from 'antd';
import { ReloadOutlined, SaveOutlined } from '@ant-design/icons';
import type {
  Conjunction,
  EntityDefDTO,
  QueryCondition,
  ViewConfigDTO,
} from '@/api/types';
import { createViewConfig, newViewConfigDraft } from '@/api/viewConfig';
import type { TimeGroup } from '@/api/runtime';
import type { ResolvedField } from '@/fields';
import { StateBlock } from '@/components/StateBlock';
import type { FilterState } from '@/views/grid/FilterBar';
import { BarChart, LineChart, NumberCard, PieChart } from './Charts';
import { namedViews, viewLabel } from './viewConfigModel';
import { useChartQuery } from './chartQuery';
import {
  CHART_KIND_LABELS,
  METRIC_FUNCTIONS,
  METRIC_LABELS,
  TIME_GROUPS,
  TIME_GROUP_LABELS,
  chartCaption,
  chartViewSnapshot,
  formatNumber,
  normaliseConfig,
  sameChartConfig,
  type ChartConfig,
  type ChartKind,
  type MetricFunction,
} from './chartModel';

const { Text } = Typography;

interface ChartViewProps {
  entity: EntityDefDTO;
  resolvedFields: ResolvedField[];
  views: ViewConfigDTO[];
  appCode: string;
  tenantCode: string;
  conditions: QueryCondition[];
  conjunction: Conjunction;
  onFilterChange: (next: FilterState) => void;
}

function storageKey(appCode: string, entityCode: string): string {
  return `zlc:chart:${appCode}:${entityCode}`;
}

function readConfig(appCode: string, entityCode: string): Partial<ChartConfig> | null {
  try {
    const raw = window.localStorage.getItem(storageKey(appCode, entityCode));
    return raw ? (JSON.parse(raw) as Partial<ChartConfig>) : null;
  } catch {
    return null;
  }
}

/**
 * 图表视图: 一条 SQL 分组聚合 -> 一张图.
 *
 * 全部口径来自服务端 (`/runtime/aggregate`)，包括时间分桶 —— 把整表拉回前端
 * 自己数会受分页上限影响，画出"看着完整其实只有一页"的图。
 *
 * 配置分两层，和表格视图一样：localStorage 存当前这份临时状态（刷新不丢），
 * `z_lc_view_config` 里 viewType=CHART 的记录存**命名视图**（同应用成员可见、
 * 换浏览器也还在 —— 只靠 localStorage 的图表定义换台机器就没了）。
 */
export function ChartView({
  entity,
  resolvedFields,
  views,
  appCode,
  tenantCode,
  conditions,
  conjunction,
  onFilterChange,
}: ChartViewProps) {
  const entityCode = entity.entityCode ?? '';
  // 默认分组轴留给 useChartQuery 回落（候选排序在 chartModel：字典列优先，
  // 不能取 schema 第一个字段 —— 那通常是名称列，会画出一张每组只有一条记录的图）。
  // 这里不把默认轴烤进 localStorage，否则字段后来被改了名/删了，本机还会继续带着旧的轴。
  const [config, setConfig] = useState<ChartConfig>(() => normaliseConfig(readConfig(appCode, entityCode)));
  const [saveOpen, setSaveOpen] = useState(false);
  const [saveName, setSaveName] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    try {
      window.localStorage.setItem(storageKey(appCode, entityCode), JSON.stringify(config));
    } catch {
      // 隐私模式下 localStorage 会抛：图表还能看，不该整页崩
    }
  }, [appCode, entityCode, config]);

  // 取数、分组轴回落、序列构造都收在 useChartQuery 里：仪表盘上的同一份命名视图
  // 必须和这里画出同一张图，两处各写一遍口径迟早会分叉。
  const chart = useChartQuery({
    appCode,
    tenantCode,
    entityCode,
    resolvedFields,
    config,
    conditions,
    conjunction,
  });
  const { candidates, numericFields, query, timeEnabled, fieldLabel, metricName, needsMetricField, series, data } = chart;
  const groupField = query.groupField;
  const metricField = query.metricField;

  const chartViews = useMemo(() => namedViews(views, entityCode, 'CHART'), [views, entityCode]);
  // 选中态是从"配置 + 筛选"反推出来的，不是另存一个 id：动任何一个下拉或改了筛选，
  // 下拉框就该自动显示成未命名状态，否则会把没存出去的改动说成是这个视图的样子。
  const activeViewId = useMemo(
    () =>
      chartViews.find((view) => {
        const snapshot = chartViewSnapshot(view);
        return (
          !!snapshot &&
          sameChartConfig(query, snapshot.config) &&
          JSON.stringify(snapshot.conditions) === JSON.stringify(conditions) &&
          snapshot.conjunction === conjunction
        );
      })?.id,
    [chartViews, query, conditions, conjunction],
  );

  const saveView = useCallback(async () => {
    const name = saveName.trim();
    if (!name) {
      message.warning('请输入图表视图名称');
      return;
    }
    setSaving(true);
    try {
      await createViewConfig(
        newViewConfigDraft({
          appCode,
          entityCode,
          viewType: 'CHART',
          // 存 query 而不是 config：groupField/metricField 在这一步已经落到真实字段上，
          // 存原始 config 会把"还没被解析出来的默认轴"一起写进库里。
          config: { ...query, conditions, conjunction, name },
          tenantCode,
        }),
      );
      setSaveOpen(false);
      setSaveName('');
      message.success('图表视图已保存，同应用成员可见');
    } catch (err) {
      message.error(err instanceof Error ? err.message : '保存失败');
    } finally {
      setSaving(false);
    }
  }, [appCode, entityCode, tenantCode, query, conditions, conjunction, saveName]);

  const applyView = (id?: number) => {
    const view = chartViews.find((item) => item.id === id);
    const snapshot = chartViewSnapshot(view);
    if (!snapshot) {
      if (id) message.warning('这个视图存的不是图表配置，打不开');
      return;
    }
    setConfig(snapshot.config);
    onFilterChange({ conditions: snapshot.conditions, conjunction: snapshot.conjunction });
  };

  const chartProps = { points: series.points, metricLabel: metricName };

  if (candidates.length === 0) {
    return (
      <Alert
        type="info"
        showIcon
        message="这个实体还没有可分组的字段"
        description="图表需要一个分组维度（文本/字典/数值/日期字段都行）。在模型设计器里加上字段后即可出图。"
      />
    );
  }

  return (
    <div className="zlc-chart-view">
      <Space wrap className="zlc-chart-toolbar" size={8}>
        <Segmented
          value={query.kind}
          onChange={(value) => setConfig({ ...config, kind: value as ChartKind })}
          options={Object.entries(CHART_KIND_LABELS).map(([value, label]) => ({ value, label }))}
        />
        {query.kind !== 'number' ? (
          <Select
            className="zlc-chart-group"
            value={groupField}
            style={{ minWidth: 168 }}
            onChange={(value: string) => setConfig({ ...config, groupField: value })}
            options={candidates.map((field) => ({
              value: field.ctx.field.fieldCode,
              label: `分组：${field.ctx.field.fieldName || field.ctx.field.fieldCode}`,
            }))}
          />
        ) : null}
        {query.kind !== 'number' && timeEnabled ? (
          <Select
            className="zlc-chart-timegroup"
            value={query.timeGroup ?? 'MONTH'}
            style={{ width: 96 }}
            onChange={(value: TimeGroup) => setConfig({ ...config, timeGroup: value })}
            options={TIME_GROUPS.map((value) => ({ value, label: TIME_GROUP_LABELS[value] }))}
          />
        ) : null}
        <Select
          className="zlc-chart-metric-fn"
          value={query.metricFn}
          style={{ width: 112 }}
          onChange={(value: MetricFunction) => setConfig({ ...config, metricFn: value })}
          options={METRIC_FUNCTIONS.map((value) => ({ value, label: METRIC_LABELS[value] }))}
        />
        {needsMetricField ? (
          <Select
            className="zlc-chart-metric"
            value={metricField}
            style={{ minWidth: 152 }}
            disabled={numericFields.length === 0}
            placeholder={numericFields.length ? undefined : '没有数值字段'}
            onChange={(value: string) => setConfig({ ...config, metricField: value })}
            options={numericFields.map((field) => ({
              value: field.ctx.field.fieldCode,
              label: field.ctx.field.fieldName || field.ctx.field.fieldCode,
            }))}
          />
        ) : null}
        <Tooltip title="超过这个组数会折叠进「其他」（时间轴保留最近 N 段）">
          <InputNumber
            min={1}
            max={60}
            value={query.topN}
            style={{ width: 112 }}
            prefix="前"
            onChange={(value) => setConfig({ ...config, topN: Number(value) || 12 })}
          />
        </Tooltip>
        <Select
          className="zlc-chart-view-select"
          style={{ minWidth: 176 }}
          placeholder="命名视图"
          allowClear
          value={activeViewId}
          options={chartViews.map((view) => ({ value: view.id, label: viewLabel(view) }))}
          onChange={(id?: number) => applyView(id)}
        />
        <Tooltip title="保存为命名图表视图（存进服务端，同应用成员可见）">
          <Button
            className="zlc-chart-save-view"
            icon={<SaveOutlined />}
            onClick={() => setSaveOpen(true)}
          />
        </Tooltip>
        <Button icon={<ReloadOutlined />} onClick={() => void data.refetch()}>
          刷新
        </Button>
      </Space>

      <div className="zlc-chart-caption">
        <Text type="secondary">
          {chartCaption({
            kind: query.kind,
            fieldLabel,
            metricName,
            timeEnabled,
            timeGroup: query.timeGroup,
            conditionCount: conditions.length,
          })}
        </Text>
        {series.folded > 0 ? (
          <Text type="warning" className="zlc-chart-folded">
            {`已折叠 ${series.folded} 组`}
          </Text>
        ) : null}
        {timeEnabled && query.kind !== 'number' ? (
          <Text type="secondary" className="zlc-chart-null-note">
            未填写该日期的记录不进入时间轴
          </Text>
        ) : null}
      </div>

      <StateBlock
        isLoading={data.isLoading}
        isError={data.isError}
        error={data.error}
        isEmpty={!data.isFetching && series.points.length === 0}
        empty={<Text type="secondary">当前筛选条件下没有可统计的记录。</Text>}
        onRetry={() => void data.refetch()}
      >
        <div className="zlc-chart-card">
          {query.kind === 'line' ? <LineChart {...chartProps} /> : null}
          {query.kind === 'pie' ? <PieChart {...chartProps} /> : null}
          {query.kind === 'number' ? <NumberCard {...chartProps} /> : null}
          {query.kind === 'bar' ? <BarChart {...chartProps} /> : null}
          {series.points.length > 0 ? (
            <div className="zlc-chart-foot">
              合计 {formatNumber(series.points.reduce((acc, point) => acc + (point.value ?? 0), 0))} ·{' '}
              {series.points.length} 组
            </div>
          ) : null}
        </div>
      </StateBlock>

      <Modal
        title="保存为命名图表视图"
        open={saveOpen}
        confirmLoading={saving}
        onOk={() => void saveView()}
        onCancel={() => setSaveOpen(false)}
        okText="保存"
        cancelText="取消"
      >
        <Input
          autoFocus
          value={saveName}
          placeholder="例如：每月新增任务趋势"
          onChange={(event) => setSaveName(event.target.value)}
          onPressEnter={() => void saveView()}
        />
        <Text type="secondary" style={{ display: 'block', marginTop: 8 }}>
          将保存图表类型、分组轴、指标、折叠组数，以及当前 {conditions.length} 条筛选条件；
          存在服务端，同应用成员都能看到。
        </Text>
      </Modal>
    </div>
  );
}
