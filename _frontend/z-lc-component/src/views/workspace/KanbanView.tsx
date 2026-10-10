import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Button,
  Dropdown,
  Empty,
  Select,
  Space,
  Spin,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd';
import { ReloadOutlined, SwapOutlined } from '@ant-design/icons';
import type { Conjunction, EntityDefDTO, LcRow, QueryCondition } from '@/lc/api/types';
import { aggregateRecords, listRecords, updateRecord } from '@/lc/api/runtime';
import { readFieldValue } from '@yuku123/render/fields';
import { groupableFields } from './kanbanModel';
import type { ResolvedField } from '@yuku123/render/fields';
import { StateBlock } from '@/lc/components/StateBlock';

const { Text } = Typography;

/** 一列最多渲染多少张卡 —— 服务端 size 上限就是 200, 超出的部分显式提示而不是假装是全量. */
const CARDS_PER_ENTITY = 200;

export interface KanbanViewProps {
  entity: EntityDefDTO;
  resolvedFields: ResolvedField[];
  appCode: string;
  tenantCode: string;
  conditions: QueryCondition[];
  conjunction: Conjunction;
}

/**
 * antd 的 Dropdown 打开时**不会**把焦点交给菜单（它的 `autoFocus` 只作用在 Dropdown.Button 上），
 * 而方向键/Enter 那套处理挂在 rc-menu 的根节点上 —— 焦点还停在触发按钮时一条都收不到。
 * 实测症状（缺陷 #32）：Enter 打得开「移到」菜单，之后 ArrowDown + Enter 什么也不做，
 * 卡片留在原列，而卡片上的 tooltip 正写着"键盘可用"。
 * 这一层只做一件事：弹层挂上来就把焦点交给菜单根节点，让 rc-menu 自己的无障碍路径生效。
 * 要配合 `destroyOnHidden` —— 否则弹层关着也留在 DOM 里，第二次打开就不会再跑这个 effect。
 *
 * 焦点要推到下一帧才给得出去：effect 跑的那一刻弹层子树**还没接进 document**（实测
 * `isConnected=false`，下一帧才 true），而对未连接的节点 `focus()` 是空操作 —— 红过一次，
 * 报的是"焦点还在按钮上"，看着像桥完全没生效。推到下一帧两边都成立。
 */
function MoveMenuPortal({ children }: { children: React.ReactNode }) {
  const ref = useRef<HTMLDivElement>(null);
  useEffect(() => {
    const menu = ref.current?.querySelector<HTMLElement>('.ant-dropdown-menu');
    if (!menu) {
      return undefined;
    }
    const frame = requestAnimationFrame(() => menu.focus());
    return () => cancelAnimationFrame(frame);
  }, []);
  return <div ref={ref}>{children}</div>;
}


/**
 * 看板视图: 列 = 服务端 group-by 的结果, 卡片 = 当前筛选下的记录.
 *
 * 列头计数走 `/runtime/aggregate`（全量口径），卡片走 `/runtime/list`（受 200 上限约束），
 * 两者不一致时会明确标出"另有 N 条未加载"，避免把一页数据当成整个分组。
 * 拖卡片换列 = 一次普通的 updateRecord，所以自动进 undo 日志。
 */
export function KanbanView({
  entity,
  resolvedFields,
  appCode,
  tenantCode,
  conditions,
  conjunction,
}: KanbanViewProps) {
  const groupable = useMemo(() => groupableFields(resolvedFields), [resolvedFields]);
  const [groupField, setGroupField] = useState<string>(groupable[0]?.ctx.field.fieldCode ?? '');
  const [dragging, setDragging] = useState<number | null>(null);
  /** 正在移动的记录 id：给卡片上的下拉菜单禁用用，避免连点重复提交 */
  const [movingId, setMovingId] = useState<number | null>(null);
  // 打开菜单那一刻的触发按钮：菜单销毁后把焦点还给它，键盘用户才不会在一次移动之后丢了位置。
  const moveTriggerRef = useRef<HTMLElement | null>(null);
  /**
   * 拖拽中的记录 id 必须放 ref, 不能只靠 state:
   * dragstart 里 setDragging 之后 React 还没重渲染, 紧接着触发的 drop 读到的仍是旧闭包里的 null,
   * 于是"拖过去没反应"(实测如此). state 只用来画高亮.
   */
  const draggingId = useRef<number | null>(null);
  const queryClient = useQueryClient();

  const entityCode = entity.entityCode ?? '';
  const groupResolved = groupable.find((field) => field.ctx.field.fieldCode === groupField);

  const numericFields = useMemo(
    () => resolvedFields.filter((field) => field.ctx.descriptor.cellValueType === 'Number'),
    [resolvedFields],
  );
  const [sumField, setSumField] = useState<string | undefined>(numericFields[0]?.ctx.field.fieldCode);

  const buckets = useQuery({
    queryKey: ['kanban-buckets', appCode, entityCode, tenantCode, groupField, sumField, conditions, conjunction],
    queryFn: () => aggregateRecords(entityCode, { appCode, tenantCode }, {
      groupField,
      conjunction,
      conditions,
      aggregations: sumField ? { [sumField]: ['SUM'] } : undefined,
      limit: 60,
    }),
    enabled: Boolean(groupField),
  });

  const rows = useQuery({
    queryKey: ['kanban-rows', appCode, entityCode, tenantCode, conditions, conjunction],
    queryFn: () => listRecords(entityCode, { appCode, tenantCode }, {
      page: 1,
      size: CARDS_PER_ENTITY,
      conditions,
      conjunction,
    }),
    enabled: Boolean(groupField),
  });

  const cardFields = useMemo(() => resolvedFields.slice(0, 5), [resolvedFields]);
  const titleField = resolvedFields[0];

  const columns = useMemo(() => {
    const byGroupKey = new Map<string, LcRow[]>();
    for (const row of rows.data?.records ?? []) {
      const key = String(row[groupField] ?? '');
      const list = byGroupKey.get(key);
      if (list) {
        list.push(row);
      } else {
        byGroupKey.set(key, [row]);
      }
    }
    const meta = buckets.data ?? [];
    if (!meta.length) {
      return Array.from(byGroupKey.entries()).map(([key, cards]) => ({
        key,
        label: key || '（空）',
        count: cards.length,
        cards,
        sum: undefined,
      }));
    }
    return meta.map((bucket) => {
      const key = String(bucket.group_key ?? '');
      return {
        key,
        label: bucket.group_label || key || '（空）',
        count: Number(bucket.group_count ?? 0),
        sum: bucket[`sum_${sumField ?? ''}`] as number | undefined,
        cards: byGroupKey.get(key) ?? [],
      };
    });
  }, [buckets.data, rows.data, groupField, sumField]);

  const move = useCallback(
    async (rowId: number, targetKey: string) => {
      const field = groupResolved;
      if (!field) return;
      const wire = field.def.toFieldValue(targetKey, field.ctx);
      setMovingId(rowId);
      try {
        await updateRecord(entityCode, rowId, { [groupField]: wire }, { appCode, tenantCode });
        message.success('已移动');
        await queryClient.invalidateQueries({ queryKey: ['kanban-rows'] });
        await queryClient.invalidateQueries({ queryKey: ['kanban-buckets'] });
        await queryClient.invalidateQueries({ queryKey: ['grid', appCode, entityCode] });
      } catch (err) {
        message.error(err instanceof Error ? err.message : '移动失败');
      } finally {
        setMovingId(null);
      }
    },
    [entityCode, groupField, groupResolved, appCode, tenantCode, queryClient],
  );

  const refetch = () => {
    void buckets.refetch();
    void rows.refetch();
  };

  return (
    <div style={{ padding: 12, display: 'flex', flexDirection: 'column', gap: 10, minHeight: 0, flex: 1 }}>
      <Space wrap size={10}>
        <Text type="secondary">分组字段</Text>
        <Select
          size="small"
          style={{ minWidth: 200 }}
          value={groupField || undefined}
          options={groupable.map((field) => ({
            value: field.ctx.field.fieldCode,
            label: `${field.ctx.field.fieldName || field.ctx.field.fieldCode}（${field.ctx.field.fieldCode}）`,
          }))}
          onChange={setGroupField}
          showSearch
        />
        <Text type="secondary">列底汇总</Text>
        <Select
          size="small"
          style={{ minWidth: 170 }}
          allowClear
          placeholder="不汇总"
          value={sumField}
          options={numericFields.map((field) => ({
            value: field.ctx.field.fieldCode,
            label: `SUM · ${field.ctx.field.fieldName || field.ctx.field.fieldCode}`,
          }))}
          onChange={(value) => setSumField(value)}
        />
        <Tooltip title="重新载入">
          <Button size="small" icon={<ReloadOutlined />} onClick={refetch} />
        </Tooltip>
      </Space>

      {!groupable.length ? (
        <Empty description="这个实体没有适合分组的字段" />
      ) : (
        <StateBlock
          isLoading={(buckets.isLoading || rows.isLoading) && columns.length === 0}
          isError={buckets.isError || rows.isError}
          error={buckets.error ?? rows.error}
          onRetry={refetch}
          isEmpty={columns.length === 0}
          empty={<Empty description="当前筛选下没有记录" image={Empty.PRESENTED_IMAGE_SIMPLE} />}
        >
          <Spin spinning={buckets.isFetching || rows.isFetching}>
            <div style={{ display: 'flex', gap: 10, overflowX: 'auto', alignItems: 'flex-start', paddingBottom: 8 }}>
              {columns.map((column) => {
                const hidden = Math.max(0, column.count - column.cards.length);
                return (
                  <div
                    key={column.key}
                    className="zlc-kb-column"
                    data-group-key={column.key}
                    onDragOver={(event) => {
                      event.preventDefault();
                      event.dataTransfer.dropEffect = 'move';
                    }}
                    onDrop={(event) => {
                      event.preventDefault();
                      const rowId = draggingId.current ?? Number(event.dataTransfer.getData('text/plain'));
                      draggingId.current = null;
                      setDragging(null);
                      if (Number.isFinite(rowId) && rowId > 0) {
                        void move(rowId, column.key);
                      }
                    }}
                    style={{
                      width: 268,
                      flex: '0 0 268px',
                      background: '#f4f6fa',
                      borderRadius: 8,
                      border: '1px solid #e6eaf1',
                      display: 'flex',
                      flexDirection: 'column',
                      maxHeight: 'calc(100vh - 260px)',
                    }}
                  >
                    <div style={{ padding: '8px 10px', borderBottom: '1px solid #e6eaf1' }}>
                      <Space size={6} style={{ justifyContent: 'space-between', width: '100%' }}>
                        <Text strong ellipsis className="zlc-kb-column-label" style={{ maxWidth: 170 }}>
                          {column.label}
                        </Text>
                        <Tag>{column.count}</Tag>
                      </Space>
                      {column.sum !== undefined ? (
                        <Text type="secondary" style={{ fontSize: 12 }}>
                          合计 {Number(column.sum).toFixed(2)}
                        </Text>
                      ) : null}
                      {hidden > 0 ? (
                        <Alert
                          style={{ marginTop: 6, padding: 0 }}
                          type="warning"
                          showIcon
                          message={`另有 ${hidden} 条未加载`}
                        />
                      ) : null}
                    </div>
                    <div style={{ padding: 8, overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: 8 }}>
                      {column.cards.map((row) => (
                        <div
                          key={String(row.id)}
                          className="zlc-kb-card"
                          data-record-id={String(row.id)}
                          draggable
                          onDragStart={(event) => {
                            draggingId.current = Number(row.id);
                            setDragging(Number(row.id));
                            event.dataTransfer.effectAllowed = 'move';
                            // Firefox 需要真的写上一点数据才会触发 drop
                            event.dataTransfer.setData('text/plain', String(row.id));
                          }}
                          onDragEnd={() => {
                            draggingId.current = null;
                            setDragging(null);
                          }}
                          style={{
                            background: '#fff',
                            border: '1px solid #e6eaf1',
                            borderRadius: 6,
                            padding: '8px 10px',
                            cursor: 'grab',
                            boxShadow: dragging === Number(row.id) ? '0 0 0 2px #91beff' : undefined,
                          }}
                        >
                          <Space align="start" size={4} style={{ justifyContent: 'space-between', width: '100%' }}>
                            <Text strong style={{ fontSize: 13 }}>
                              {displayOf(row, titleField)}
                            </Text>
                            {/*
                              拖拽只有鼠标能用。同一件事必须还有一条键盘可达的路径，
                              否则看板对键盘用户和触屏设备就是"看得见改不了"。
                            */}
                            <Dropdown
                              trigger={['click']}
                              disabled={movingId !== null}
                              destroyOnHidden
                              popupRender={(node) => <MoveMenuPortal>{node}</MoveMenuPortal>}
                              onOpenChange={(open) => {
                                if (open) {
                                  moveTriggerRef.current = document.activeElement as HTMLElement | null;
                                } else {
                                  moveTriggerRef.current?.focus();
                                }
                              }}
                              menu={{
                                items: columns
                                  .filter((other) => other.key !== column.key)
                                  .map((other) => ({
                                    key: other.key,
                                    label: other.label || '（空）',
                                  })),
                                onClick: ({ key }) => void move(Number(row.id), String(key)),
                              }}
                            >
                              <Tooltip title="移到其他列（键盘可用）">
                                <Button
                                  size="small"
                                  type="text"
                                  aria-label={`移动 ${displayOf(row, titleField)} 到其他分组`}
                                  icon={<SwapOutlined />}
                                />
                              </Tooltip>
                            </Dropdown>
                          </Space>
                          {cardFields.slice(1).map((field) => (
                            <div key={field.ctx.field.fieldCode} style={{ fontSize: 12, marginTop: 2 }}>
                              <Text type="secondary">{field.ctx.field.fieldName || field.ctx.field.fieldCode}：</Text>
                              {field.def.renderCell({
                                ctx: field.ctx,
                                row,
                                ...readFieldValue(row, field),
                                compact: true,
                              })}
                            </div>
                          ))}
                        </div>
                      ))}
                      {!column.cards.length ? (
                        <Text type="secondary" style={{ fontSize: 12, padding: 4 }}>
                          拖卡片到这里
                        </Text>
                      ) : null}
                    </div>
                  </div>
                );
              })}
            </div>
          </Spin>
        </StateBlock>
      )}
    </div>
  );
}

function displayOf(row: LcRow, field?: ResolvedField): string {
  if (!field) {
    return String(row.id ?? '');
  }
  const text = field.def.toCellText({
    ctx: field.ctx,
    row,
    ...readFieldValue(row, field),
  });
  return text || `#${String(row.id ?? '')}`;
}
