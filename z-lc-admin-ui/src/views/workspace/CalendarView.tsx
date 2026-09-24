import { useCallback, useEffect, useMemo, useState } from 'react';
import { Button, Empty, Pagination, Typography } from 'antd';
import { LeftOutlined, PlusOutlined, RightOutlined } from '@ant-design/icons';
import { listRecords } from '@/api/runtime';
import { StateBlock } from '@/components/StateBlock';
import type { Conjunction, EntityDefDTO, LcRow, QueryCondition, QuerySort } from '@/api/types';
import type { ResolvedField } from '@yuku123/render/fields';
import { readFieldValue } from '@yuku123/render/fields';
import dayjs from 'dayjs';

const { Text } = Typography;

interface CalendarViewProps {
  entity: EntityDefDTO;
  resolvedFields: ResolvedField[];
  appCode: string;
  tenantCode: string;
  conditions: QueryCondition[];
  conjunction: Conjunction;
  onOpenRecord: (id: number) => void;
  onCreateRecord: () => void;
}

const WEEKDAYS = ['一', '二', '三', '四', '五', '六', '日'];

function cellText(row: LcRow, field: ResolvedField): string {
  const text = field.def.toCellText({ ctx: field.ctx, row, ...readFieldValue(row, field) });
  return text || `#${String(row.id ?? '')}`;
}

/** 找到实体中第一个 DATE/DATETIME 类型的字段作为日历轴 */
function findDateField(fields: ResolvedField[]): ResolvedField | undefined {
  return fields.find((f) => {
    const t = f.ctx.field.fieldType;
    return t === 'DATE' || t === 'DATETIME';
  });
}

export function CalendarView({
  entity,
  resolvedFields,
  appCode,
  tenantCode,
  conditions,
  conjunction,
  onOpenRecord,
  onCreateRecord,
}: CalendarViewProps) {
  const [rows, setRows] = useState<LcRow[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [currentMonth, setCurrentMonth] = useState(() => dayjs());
  // 挂载即发请求，所以首帧就是"在读"而不是"这个月没排期"。
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<unknown>(null);

  const pageSize = 100; // 日历一次加载多一些

  const dateField = useMemo(() => findDateField(resolvedFields), [resolvedFields]);
  const titleField = useMemo(() => resolvedFields[0], [resolvedFields]);

  // 月份的第一天和最后一天。必须按 currentMonth 缓存：dayjs() 每次都造新对象，
  // 不 memo 的话下面的 monthConditions 每轮渲染都换个身份，loadPage 跟着换，
  // useEffect 就无限重发当月查询（挂载后 300ms 内实测 43 次 /runtime/list）。
  const monthStart = useMemo(() => currentMonth.startOf('month'), [currentMonth]);
  const monthEnd = useMemo(() => currentMonth.endOf('month'), [currentMonth]);

  // 构建日期条件：筛选当前月份的数据
  const monthConditions = useMemo(() => {
    if (!dateField) return conditions;
    // 右边界用「下月 1 号 + lt」而不是「月末 + lte 23:59:59」:
    // DATE 列拿到带时分秒的字符串会被 JDBC 当 DATE 常量解析直接报错, 半个视图空掉。
    const startStr = monthStart.format('YYYY-MM-DD');
    const exclusiveEndStr = monthStart.add(1, 'month').format('YYYY-MM-DD');
    const dateConditions: QueryCondition[] = [
      { fieldCode: dateField.ctx.field.fieldCode, operator: 'gte', value: startStr },
      { fieldCode: dateField.ctx.field.fieldCode, operator: 'lt', value: exclusiveEndStr },
    ];
    return [...conditions, ...dateConditions];
  }, [dateField, monthStart, conditions]);

  const sorts: QuerySort[] = useMemo(
    () => (dateField ? [{ fieldCode: dateField.ctx.field.fieldCode, dir: 'asc' }] : []),
    [dateField],
  );

  const loadPage = useCallback(async () => {
    setLoading(true);
    setLoadError(null);
    try {
      const res = await listRecords(entity.entityCode, { appCode, tenantCode }, {
        page,
        size: pageSize,
        conditions: monthConditions,
        conjunction,
        sorts,
      });
      setRows(res?.records ?? []);
      setTotal(res?.total ?? 0);
    } catch (err) {
      // 挂掉的月份和真的空的月份长得一样: 42 个空格子。所以错误必须挡住网格,
      // 而不是只弹一条三秒就消失的 toast。
      setLoadError(err);
    } finally {
      setLoading(false);
    }
  }, [entity.entityCode, appCode, tenantCode, page, monthConditions, conjunction, sorts]);

  useEffect(() => {
    void loadPage();
  }, [loadPage]);

  // 按日期分组行
  const rowsByDate = useMemo(() => {
    const map: Record<string, LcRow[]> = {};
    if (!dateField) return map;
    for (const row of rows) {
      const raw = row[dateField.valueKey];
      if (!raw) continue;
      const d = dayjs(String(raw));
      if (!d.isValid()) continue;
      const key = d.format('YYYY-MM-DD');
      if (!map[key]) map[key] = [];
      map[key].push(row);
    }
    return map;
  }, [rows, dateField]);

  // 生成日历网格
  const calendarDays = useMemo(() => {
    const firstDay = monthStart.day() === 0 ? 6 : monthStart.day() - 1; // 周一=0
    const daysInMonth = currentMonth.daysInMonth();
    const cells: { date: dayjs.Dayjs; isCurrentMonth: boolean }[] = [];
    // 上月填充
    for (let i = firstDay - 1; i >= 0; i--) {
      cells.push({ date: monthStart.subtract(i + 1, 'day'), isCurrentMonth: false });
    }
    // 本月
    for (let i = 0; i < daysInMonth; i++) {
      cells.push({ date: monthStart.add(i, 'day'), isCurrentMonth: true });
    }
    // 下月填充到42格（6行×7列）
    while (cells.length < 42) {
      cells.push({ date: monthEnd.add(cells.length - firstDay - daysInMonth + 1, 'day'), isCurrentMonth: false });
    }
    return cells;
  }, [monthStart, monthEnd, currentMonth]);

  if (!dateField) {
    return (
      <div style={{ padding: 48 }}>
        <Empty description="日历视图需要至少一个日期/时间字段来作为日历轴" />
      </div>
    );
  }

  return (
    <div style={{ padding: 16 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <Button icon={<LeftOutlined />} size="small" onClick={() => setCurrentMonth((m) => m.subtract(1, 'month'))} />
          <Text strong className="zlc-cal-month" style={{ fontSize: 16 }}>{currentMonth.format('YYYY年MM月')}</Text>
          <Button icon={<RightOutlined />} size="small" onClick={() => setCurrentMonth((m) => m.add(1, 'month'))} />
          <Button size="small" onClick={() => { setCurrentMonth(dayjs()); setPage(1); }}>今天</Button>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={onCreateRecord}>新建记录</Button>
      </div>

      <StateBlock
        isLoading={loading}
        isError={Boolean(loadError)}
        error={loadError}
        errorTitle="本月记录没有读到"
        onRetry={() => void loadPage()}
        isEmpty={false}
      >
        {/* 星期头 */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', borderBottom: '1px solid #f0f0f0' }}>
          {WEEKDAYS.map((d, idx) => (
            <div
              key={d}
              className="zlc-cal-weekday"
              data-weekday={idx}
              style={{ textAlign: 'center', padding: '6px 0', fontWeight: 500, fontSize: 12, color: '#666' }}
            >{d}</div>
          ))}
        </div>

        {/* 日期网格 */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', borderLeft: '1px solid #f0f0f0' }}>
          {calendarDays.map(({ date, isCurrentMonth }, idx) => {
            const key = date.format('YYYY-MM-DD');
            const dayRows = rowsByDate[key] ?? [];
            const isToday = date.isSame(dayjs(), 'day');
            return (
              <div
                key={idx}
                data-date={key}
                className={`zlc-cal-cell${isCurrentMonth ? ' zlc-cal-cell-current' : ''}`}
                style={{
                  minHeight: 90,
                  borderRight: '1px solid #f0f0f0',
                  borderBottom: '1px solid #f0f0f0',
                  padding: 4,
                  background: isCurrentMonth ? '#fff' : '#fafafa',
                }}
              >
                <div style={{
                  fontSize: 12,
                  fontWeight: isToday ? 700 : 400,
                  color: isToday ? '#1677ff' : isCurrentMonth ? '#333' : '#bbb',
                  marginBottom: 2,
                }}>
                  {date.date()}
                </div>
                {dayRows.slice(0, 3).map((row) => (
                  <div
                    key={String(row.id)}
                    className="zlc-cal-chip"
                    data-record-id={String(row.id)}
                    onClick={() => onOpenRecord(Number(row.id))}
                    style={{
                      fontSize: 11,
                      padding: '1px 4px',
                      marginBottom: 1,
                      borderRadius: 3,
                      background: '#e6f4ff',
                      cursor: 'pointer',
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      whiteSpace: 'nowrap',
                    }}
                  >
                    {titleField ? cellText(row, titleField) : `#${row.id}`}
                  </div>
                ))}
                {dayRows.length > 3 && (
                  <div style={{ fontSize: 10, color: '#999', paddingLeft: 4 }}>+{dayRows.length - 3}</div>
                )}
              </div>
            );
          })}
        </div>

        {total > pageSize && (
          <div style={{ marginTop: 12, textAlign: 'center' }}>
            <Pagination current={page} pageSize={pageSize} total={total} onChange={setPage} showTotal={(t) => `共 ${t} 条`} />
          </div>
        )}
      </StateBlock>
    </div>
  );
}
