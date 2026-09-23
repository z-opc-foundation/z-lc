import { useCallback, useEffect, useMemo, useState } from 'react';
import { Button, Card, Empty, Pagination, Tag, Typography } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { listRecords } from '@/api/runtime';
import { StateBlock } from '@/components/StateBlock';
import type { Conjunction, EntityDefDTO, LcRow, QueryCondition } from '@/api/types';
import type { ResolvedField } from '@/fields';
import { readFieldValue } from '@/fields';

const { Text } = Typography;

interface GalleryViewProps {
  entity: EntityDefDTO;
  resolvedFields: ResolvedField[];
  appCode: string;
  tenantCode: string;
  conditions: QueryCondition[];
  conjunction: Conjunction;
  onOpenRecord: (id: number) => void;
  onCreateRecord: () => void;
}

const CARD_COLORS = ['#f0f5ff', '#f6ffed', '#fff7e6', '#fff1f0', '#f9f0ff', '#e6fffb', '#fcffe6'];

function getCardColor(index: number) {
  return CARD_COLORS[index % CARD_COLORS.length];
}

function cellText(row: LcRow, field: ResolvedField): string {
  const text = field.def.toCellText({ ctx: field.ctx, row, ...readFieldValue(row, field) });
  return text || `#${String(row.id ?? '')}`;
}

export function GalleryView({
  entity,
  resolvedFields,
  appCode,
  tenantCode,
  conditions,
  conjunction,
  onOpenRecord,
  onCreateRecord,
}: GalleryViewProps) {
  const [rows, setRows] = useState<LcRow[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  // 挂载即发请求：首帧该是"在读"，不是"还没有记录"。
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<unknown>(null);

  const pageSize = 12;

  const loadPage = useCallback(async () => {
    setLoading(true);
    setLoadError(null);
    try {
      const res = await listRecords(entity.entityCode, { appCode, tenantCode }, {
        page,
        size: pageSize,
        conditions,
        conjunction,
      });
      setRows(res?.records ?? []);
      setTotal(res?.total ?? 0);
    } catch (err) {
      // 只弹一条会自己消失的 toast 等于没说的：toast 收走之后画廊会亮出
      // "暂无数据"，把"这次没读到"讲成"库里没有"。错误要留在原地并能重试。
      setLoadError(err);
    } finally {
      setLoading(false);
    }
  }, [entity.entityCode, appCode, tenantCode, page, conditions, conjunction]);

  useEffect(() => {
    void loadPage();
  }, [loadPage]);

  const displayFields = useMemo(() => resolvedFields.slice(0, 5), [resolvedFields]);
  const titleField = displayFields[0];

  if (displayFields.length === 0) {
    return <div style={{ padding: 48 }}><Empty description="没有可显示的字段" /></div>;
  }

  return (
    <div style={{ padding: 16 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Text strong>画廊视图</Text>
        <Button type="primary" icon={<PlusOutlined />} onClick={onCreateRecord}>新建记录</Button>
      </div>

      <StateBlock
        isLoading={loading && rows.length === 0}
        isError={Boolean(loadError)}
        error={loadError}
        errorTitle="记录没有读到"
        onRetry={() => void loadPage()}
        isEmpty={rows.length === 0}
        empty={
          <div style={{ padding: 24, textAlign: 'center' }}>
            <Text type="secondary">
              {conditions.length
                ? '没有符合条件的记录，试试放宽筛选条件'
                : '还没有记录，点击「新建记录」开始录入'}
            </Text>
          </div>
        }
      >
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(260px, 1fr))', gap: 16 }}>
          {rows.map((row, idx) => (
            <Card
              key={String(row.id)}
              hoverable
              style={{ background: getCardColor(idx), cursor: 'pointer' }}
              onClick={() => onOpenRecord(Number(row.id))}
            >
              <Card.Meta
                title={<Text ellipsis style={{ fontSize: 15 }}>{titleField ? cellText(row, titleField) : `#${row.id}`}</Text>}
                description={
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 4, marginTop: 8 }}>
                    {displayFields.slice(1).map((field) => {
                      const text = cellText(row, field);
                      const isDict = field.ctx.field.dictCode;
                      return (
                        <div key={field.ctx.field.fieldCode} style={{ fontSize: 12 }}>
                          <Text type="secondary">{field.ctx.field.fieldName}：</Text>
                          {isDict ? <Tag>{text}</Tag> : <Text>{text || '—'}</Text>}
                        </div>
                      );
                    })}
                  </div>
                }
              />
            </Card>
          ))}
        </div>

        {total > pageSize && (
          <div style={{ marginTop: 16, textAlign: 'center' }}>
            <Pagination
              current={page}
              pageSize={pageSize}
              total={total}
              onChange={setPage}
              showSizeChanger={false}
              showTotal={(t) => `共 ${t} 条`}
            />
          </div>
        )}
      </StateBlock>
    </div>
  );
}
