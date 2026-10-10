import { Alert, Button, Space, Tag, Tooltip, Typography } from 'antd';
import { Link } from 'react-router-dom';
import { DeleteOutlined, ExpandAltOutlined, ReloadOutlined } from '@ant-design/icons';
import type { Conjunction, QueryCondition } from '@/lc/api/types';
import type { ResolvedField } from '@yuku123/render/fields';
import { StateBlock } from '@/lc/components/StateBlock';
import { BarChart, LineChart, NumberCard, PieChart } from './Charts';
import { CHART_KIND_LABELS, chartCaption, formatNumber, type ChartConfig } from './chartModel';
import { useChartQuery } from './chartQuery';
import type { TileWidth } from './dashboardModel';

const { Text } = Typography;

interface ChartTileProps {
  appCode: string;
  tenantCode: string;
  entityCode: string;
  entityLabel: string;
  resolvedFields: ResolvedField[];
  title: string;
  config: ChartConfig;
  conditions: QueryCondition[];
  conjunction: Conjunction;
  width: TileWidth;
  /** 编辑态才给：改宽度与移除。只读展示时这两个不传，组件上也不会冒出按钮。 */
  onWidthChange?: (width: TileWidth) => void;
  onRemove?: () => void;
}

/**
 * 仪表盘上的一张图：读一份已落库的命名图表视图，不带工具栏。
 *
 * 取数走 useChartQuery（和 ChartView 同一条链路），所以"命名视图在图表页里
 * 是什么数，在仪表盘上就是什么数"。这也是仪表盘不自己内联图表配置的原因。
 */
export function ChartTile({
  appCode,
  tenantCode,
  entityCode,
  entityLabel,
  resolvedFields,
  title,
  config,
  conditions,
  conjunction,
  width,
  onWidthChange,
  onRemove,
}: ChartTileProps) {
  const chart = useChartQuery({
    appCode,
    tenantCode,
    entityCode,
    resolvedFields,
    config,
    conditions,
    conjunction,
  });
  const { candidates, query, series, data, fieldLabel, metricName, timeEnabled } = chart;

  const total = series.points.reduce((acc, point) => acc + (point.value ?? 0), 0);

  return (
    <section
      className={`zlc-dash-tile zlc-dash-tile--w${width}`}
      data-entity={entityCode}
      data-kind={query.kind}
    >
      <header className="zlc-dash-tile-head">
        <div className="zlc-dash-tile-heading">
          <Text strong className="zlc-dash-tile-title">
            {title}
          </Text>
          <Text type="secondary" className="zlc-dash-tile-caption">
            {chartCaption({
              kind: query.kind,
              fieldLabel,
              metricName,
              timeEnabled,
              timeGroup: query.timeGroup,
              conditionCount: conditions.length,
            })}
          </Text>
        </div>
        <Space size={4} className="zlc-dash-tile-tools">
          <Tag bordered={false}>{entityLabel}</Tag>
          <Tag bordered={false}>{CHART_KIND_LABELS[query.kind]}</Tag>
          {onWidthChange ? (
            <Tooltip title={width === 1 ? '占整行' : '占半行'}>
              <Button
                size="small"
                type="text"
                className="zlc-dash-tile-width"
                icon={<ExpandAltOutlined />}
                onClick={() => onWidthChange(width === 1 ? 2 : 1)}
              />
            </Tooltip>
          ) : null}
          {onRemove ? (
            <Tooltip title="从仪表盘移除（不删除图表视图本身）">
              <Button
                size="small"
                type="text"
                className="zlc-dash-tile-remove"
                icon={<DeleteOutlined />}
                onClick={onRemove}
              />
            </Tooltip>
          ) : null}
          <Tooltip title="刷新">
            <Button
              size="small"
              type="text"
              className="zlc-dash-tile-refresh"
              icon={<ReloadOutlined />}
              onClick={() => void data.refetch()}
            />
          </Tooltip>
          <Link to={`/lc/${appCode}/${entityCode}/CHART`} className="zlc-dash-tile-open">
            打开源视图
          </Link>
        </Space>
      </header>

      {candidates.length === 0 ? (
        <Alert
          type="warning"
          showIcon
          message="这张图引用的实体已经没有可分组的字段了"
          description="字段被删掉或改名之后图就取不到数了。到模型设计器补回字段，或者把这张图从仪表盘里移除。"
        />
      ) : (
        <StateBlock
          isLoading={data.isLoading}
          isError={data.isError}
          error={data.error}
          isEmpty={!data.isFetching && series.points.length === 0}
          empty={<Text type="secondary">当前筛选条件下没有可统计的记录。</Text>}
          onRetry={() => void data.refetch()}
        >
          <div className="zlc-chart-card">
            {query.kind === 'line' ? <LineChart points={series.points} metricLabel={metricName} /> : null}
            {query.kind === 'pie' ? <PieChart points={series.points} metricLabel={metricName} /> : null}
            {query.kind === 'number' ? <NumberCard points={series.points} metricLabel={metricName} /> : null}
            {query.kind === 'bar' ? <BarChart points={series.points} metricLabel={metricName} /> : null}
            {series.points.length > 0 ? (
              <div className="zlc-chart-foot">
                合计 {formatNumber(total)} · {series.points.length} 组
                {series.folded > 0 ? ` · 已折叠 ${series.folded} 组` : ''}
              </div>
            ) : null}
          </div>
        </StateBlock>
      )}
    </section>
  );
}

/**
 * 引用失效时的占位：视图被删了、或它所属的实体被删了。
 * 必须指名道姓说清是哪一个丢了 —— 只让组件少一块，用户只能猜。
 */
export function MissingTile({
  message,
  width,
  onRemove,
  // 默认这段建议只对"视图真的没了"成立。加载故障时必须换一套说法：照默认文案去移除组件，
  // 用户会因为一次接口故障亲手删掉自己仪表盘里的图。
  description = '仪表盘记的是视图编号，所以这个组件不会自动换成别的图。重新存一份同名视图再加回来，或者直接移除这个组件。',
}: {
  message: string;
  width: TileWidth;
  onRemove?: () => void;
  description?: string;
}) {
  return (
    <section className={`zlc-dash-tile zlc-dash-tile--w${width}`}>
      <Alert
        type="warning"
        showIcon
        message={message}
        description={description}
        action={
          onRemove ? (
            <Button size="small" onClick={onRemove}>
              移除
            </Button>
          ) : null
        }
      />
    </section>
  );
}
