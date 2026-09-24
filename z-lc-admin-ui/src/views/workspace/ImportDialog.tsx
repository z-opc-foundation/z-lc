import { useCallback, useMemo, useState } from 'react';
import {
  Alert,
  Button,
  Modal,
  Select,
  Space,
  Steps,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { InboxOutlined } from '@ant-design/icons';
import type { EntityDefDTO } from '@/api/types';
import { IMPORT_MAX_ROWS_REPORTED, importCommit, importPreview } from '@/api/runtime';
import type { ImportRowWarning } from '@/api/runtime';
import { autoMap, parseCsvFile } from '@/views/grid/csv';
import type { ParsedCsv } from '@/views/grid/csv';
import type { ResolvedField } from '@yuku123/render/fields';

const { Text } = Typography;

const PREVIEW_ROWS = 10;

/**
 * 一行的结论。`null` 不是"还没算完"，而是服务端没给出这一行的判定 ——
 * 写入中断且补偿回滚没做完时，这一行可能已经在库里，猜"失败"和猜"成功"一样是谎。
 */
interface RowOutcome {
  line: number;
  ok: boolean | null;
  detail: string;
}

interface OutcomeSummary {
  tone: 'success' | 'warning' | 'error';
  headline: string;
  /** 列表装不下的部分（服务端单次最多回 50 条错误），必须显式说"另有几条"。 */
  note?: string;
}

/**
 * CSV import: parse → map columns (with a per-column type coming from the field
 * registry) → preview → commit → per-row result report.
 *
 * Type inference deliberately only *suggests* the mapping; the user confirms it,
 * so a guessed column is never silently written as the wrong type.
 */
export interface ImportDialogProps {
  open: boolean;
  onClose: () => void;
  entity: EntityDefDTO;
  resolvedFields: ResolvedField[];
  appCode: string;
  tenantCode: string;
  onImported: () => void;
}

export function ImportDialog({
  open,
  onClose,
  entity,
  resolvedFields,
  appCode,
  tenantCode,
  onImported,
}: ImportDialogProps) {
  const [step, setStep] = useState(0);
  const [parsed, setParsed] = useState<ParsedCsv | null>(null);
  const [mapping, setMapping] = useState<Record<string, number | null>>({});
  const [busy, setBusy] = useState(false);
  const [outcomes, setOutcomes] = useState<RowOutcome[] | null>(null);
  const [summary, setSummary] = useState<OutcomeSummary | null>(null);
  /** 值不在字典里这类提示：不阻断写入，但必须让导入的人看见。 */
  const [importWarnings, setImportWarnings] = useState<{ line: number; key: string; text: string }[]>([]);

  const importable = useMemo(
    () => resolvedFields.filter((resolved) => resolved.ctx.field.fieldCode !== 'id'),
    [resolvedFields],
  );

  const headerOptions = useMemo(() => {
    const base = [{ value: -1, label: '— 忽略此字段 —' }];
    if (!parsed) return base;
    return [
      ...base,
      ...parsed.headers.map((header, index) => ({ value: index, label: `${header} (${index + 1})` })),
    ];
  }, [parsed]);

  const reset = useCallback(() => {
    setStep(0);
    setParsed(null);
    setMapping({});
    setOutcomes(null);
    setSummary(null);
    setImportWarnings([]);
  }, []);

  const close = useCallback(() => {
    reset();
    onClose();
  }, [reset, onClose]);

  const onFile = useCallback(
    async (file: File) => {
      try {
        const result = await parseCsvFile(file);
        if (!result.headers.length) {
          message.error('没解析出表头，请确认第一行是列名');
          return;
        }
        setParsed(result);
        setMapping(
          autoMap(
            result.headers,
            importable.map((resolved) => ({
              fieldCode: resolved.ctx.field.fieldCode,
              fieldName: resolved.ctx.field.fieldName ?? '',
            })),
          ),
        );
        setStep(1);
      } catch (err) {
        message.error(err instanceof Error ? err.message : '解析文件失败');
      }
    },
    [importable],
  );

  const preview = useMemo(() => {
    if (!parsed) return [];
    return parsed.rows.slice(0, PREVIEW_ROWS).map((row, rowIndex) => {
      const record: Record<string, string> = { __line: String(rowIndex + 1) };
      for (const resolved of importable) {
        const code = resolved.ctx.field.fieldCode;
        const index = mapping[code];
        if (index === null || index === undefined || index < 0) continue;
        record[code] = row[index] ?? '';
      }
      return record;
    });
  }, [parsed, mapping, importable]);

  /** 把映射结果拼成后端要的 records（值仍走注册表的 toFieldValue，类型口径只有一处）。 */
  const buildRecords = useCallback((): Record<string, unknown>[] => {
    if (!parsed) {
      return [];
    }
    return parsed.rows.map((row) => {
      const fieldValues: Record<string, unknown> = {};
      for (const resolved of importable) {
        const code = resolved.ctx.field.fieldCode;
        const column = mapping[code];
        if (column === null || column === undefined || column < 0) continue;
        const raw = row[column];
        if (raw === undefined || raw === '') continue;
        fieldValues[code] = resolved.def.toFieldValue(raw, resolved.ctx);
      }
      return fieldValues;
    });
  }, [parsed, mapping, importable]);

  /** 不阻断的数据质量提示（值不在字典中等）。列表只画前 20 条，剩下的数量要写出来。 */
  const applyWarnings = useCallback((list: ImportRowWarning[] | undefined) => {
    setImportWarnings((list ?? []).map((item) => ({
      line: item.index + 1,
      key: `${item.index}-${item.fieldCode ?? ''}`,
      text: item.message,
    })));
  }, []);

  /**
   * 先让服务端把整批校验一遍，校验不过就不进提交，避免"提交了一半"。
   *
   * 结果口径只认服务端给的那几个计数：`errors` 最多回 50 条（`ImportDto.MAX_ERRORS_RETURNED`），
   * 拿它的长度当"多少行不合法"会在 500 行坏数据时报成 50 行 —— 少报的那 450 行没人看得见。
   */
  const preflight = useCallback(async () => {
    const records = buildRecords();
    if (!records.length) {
      message.warning('没有可导入的行');
      return;
    }
    setBusy(true);
    const hide = message.loading('正在校验…', 0);
    try {
      const checked = await importPreview(entity.entityCode ?? '', { appCode, tenantCode }, records);
      applyWarnings(checked.warnings);
      const listed = checked.errors.length;
      // 计数走 total/validCount；万一信封里没带这两个数（老服务或空批），退回已列出的条数，
      // 不能让 NaN 把"有一行不合法"判成"全都能过"。
      const bad = Number.isFinite(checked.total - checked.validCount) && checked.total > 0
        ? Math.max(0, checked.total - checked.validCount)
        : listed;
      if (bad > 0) {
        setOutcomes(checked.errors.map((error) => ({
          line: error.index + 1,
          ok: false,
          detail: error.message,
        })));
        setSummary({
          tone: 'warning',
          headline: `${records.length} 行里有 ${bad} 行不合法，整批一行都没写入`,
          note: listed < bad
            ? `这里只列出 ${listed} 条，另有 ${bad - listed} 行未列出（服务端每批最多回 ${IMPORT_MAX_ROWS_REPORTED} 条）`
            : '修好这些行后可以重新导入',
        });
        setStep(2);
        message.error(`有 ${bad} 行不合法，整批未写入`);
        return;
      }
      const result = await importCommit(entity.entityCode ?? '', { appCode, tenantCode }, records);
      applyWarnings(result.warnings);
      // 服务端说写了才算写了，而且只认它登记的 id 数：applied=false 时一行都不能标成成功
      // （历史上这里就是把 applied 当唯一依据，写入中断也会显示绿色"共 50 行，成功 50 行"）。
      const wrote = result.applied ? result.insertedCount : 0;
      if (result.applied && wrote >= records.length) {
        setOutcomes(records.map((_, index) => ({ line: index + 1, ok: true, detail: '已导入' })));
        setSummary({ tone: 'success', headline: `已导入 ${wrote} 行` });
        setStep(2);
        message.success(`已导入 ${wrote} 行`);
        onImported();
        return;
      }
      if (result.applied) {
        // 服务端只登记到部分 id，而且 ids 与 CSV 行的对应关系无从推断：整批标"未知"，
        // 按顺序前 N 行判成功就是猜 —— 猜错的用户会少一批数据还以为都进库了。
        setOutcomes(records.map((_, index) => ({
          line: index + 1,
          ok: null,
          detail: '服务端未逐行确认这一行',
        })));
        setSummary({
          tone: 'warning',
          headline: `服务端只登记了 ${wrote} 个 id，这批共 ${records.length} 行，其余 ${records.length - wrote} 行状态未知`,
          note: '请先刷新列表核对，再决定要不要重导，直接重导可能写进重复数据',
        });
        setStep(2);
        message.warning('部分行写入状态未知，请核对后再重试');
        onImported();
        return;
      }
      const reverted = result.rolledBack;
      setOutcomes(records.map((_, index) => ({
        line: index + 1,
        ok: reverted ? false : null,
        detail: reverted ? '整批已补偿回滚，未写入' : '写入中断且回滚未完成，这一行可能已经在库里',
      })));
      setSummary({
        tone: reverted ? 'warning' : 'error',
        headline: result.message ?? '写入失败，整批未生效',
        note: reverted
          ? '库是干净的，修正后可以重新导入'
          : '本批可能仍有数据留在库里：请刷新列表人工核对，不要直接重导',
      });
      setStep(2);
      message.error(result.message ?? '导入失败');
    } catch (err) {
      message.error(err instanceof Error ? err.message : '导入失败');
    } finally {
      hide();
      setBusy(false);
    }
  }, [appCode, buildRecords, entity.entityCode, onImported, tenantCode, applyWarnings]);

  const previewColumns = useMemo<ColumnsType<Record<string, string>>>(() => {
    const cols: ColumnsType<Record<string, string>> = [
      { title: '数据行', dataIndex: '__line', width: 84, fixed: 'left' },
    ];
    for (const resolved of importable) {
      const code = resolved.ctx.field.fieldCode;
      const index = mapping[code];
      if (index === null || index === undefined || index < 0) continue;
      cols.push({
        title: `${resolved.ctx.field.fieldName || code}`,
        dataIndex: code,
        render: (cellValue: string, row: Record<string, string>) => {
          const text = cellValue ?? row[code] ?? '';
          const { value: coerced } = { value: resolved.def.toFieldValue(text, resolved.ctx) };
          return (
            <Space size={4}>
              <Text style={{ fontSize: 12 }}>{text || '—'}</Text>
              <Tag>{String(coerced ?? '')}</Tag>
            </Space>
          );
        },
      });
    }
    return cols;
  }, [importable, mapping]);

  const outcomeColumns: ColumnsType<RowOutcome> = [
    { title: '数据行', dataIndex: 'line', width: 90 },
    {
      title: '结果',
      dataIndex: 'ok',
      width: 90,
      render: (value: boolean | null) => (
        value === null
          ? <Tag color="default">未确认</Tag>
          : (value ? <Tag color="green">成功</Tag> : <Tag color="red">失败</Tag>)
      ),
    },
    { title: '详情', dataIndex: 'detail' },
  ];

  return (
    <Modal
      open={open}
      title="导入 CSV"
      width={920}
      onCancel={close}
      footer={
        step === 2
          ? [<Button key="done" type="primary" onClick={close}>完成</Button>]
          : [
              step === 1 ? <Button key="back" onClick={reset}>重选文件</Button> : null,
              <Button key="cancel" onClick={close}>取消</Button>,
              step === 1 ? (
                <Button key="go" type="primary" loading={busy} onClick={() => void preflight()}>
                  导入 {parsed?.rows.length ?? 0} 行
                </Button>
              ) : null,
            ].filter(Boolean)
      }
    >
      <Steps
        size="small"
        current={step}
        style={{ marginBottom: 16 }}
        items={[{ title: '选择文件' }, { title: '列映射' }, { title: '导入结果' }]}
      />

      {step === 0 ? (
        <Space direction="vertical" style={{ width: '100%' }} size={10}>
          <Text type="secondary">
            第一行必须是列名。列名与字段编码或显示名相同会自动匹配，匹配结果在下一步可以改。
            <br />
            空行会被跳过，所以结果里的「数据行」是第几条记录，不是文件里的物理行号。
          </Text>
          <Button type="primary" ghost icon={<InboxOutlined />} onClick={() => document.getElementById('zlc-csv-file')?.click()}>
            选择 CSV 文件
          </Button>
          <input
            id="zlc-csv-file"
            type="file"
            accept=".csv,text/csv"
            style={{ display: 'none' }}
            onChange={(event) => {
              const file = event.target.files?.[0];
              if (file) void onFile(file);
              event.target.value = '';
            }}
          />
        </Space>
      ) : null}

      {step === 1 && parsed ? (
        <Space direction="vertical" style={{ width: '100%' }} size={12}>
          <Table
            size="small"
            pagination={false}
            rowKey="fieldCode"
            dataSource={importable.map((resolved) => ({
              fieldCode: resolved.ctx.field.fieldCode,
              label: resolved.ctx.field.fieldName || resolved.ctx.field.fieldCode,
              type: resolved.ctx.field.fieldType,
              required: resolved.ctx.field.required,
            }))}
            columns={[
              { title: '实体字段', dataIndex: 'label', width: 200 },
              { title: '类型', dataIndex: 'type', width: 110, render: (v: string) => <Tag>{v}</Tag> },
              {
                title: '必填',
                dataIndex: 'required',
                width: 70,
                render: (v: boolean) => (v ? <Tag color="orange">必填</Tag> : '—'),
              },
              {
                title: '对应 CSV 列',
                key: 'map',
                width: 260,
                render: (_v, row: { fieldCode: string }) => (
                  <Select
                    size="small"
                    style={{ width: '100%' }}
                    value={mapping[row.fieldCode] ?? -1}
                    options={headerOptions}
                    onChange={(value) => setMapping({ ...mapping, [row.fieldCode]: value })}
                  />
                ),
              },
            ]}
          />
          {parsed.errors.length ? (
            <Alert type="warning" showIcon message="解析告警" description={parsed.errors.join('；')} />
          ) : null}
          <div>
            <Text strong>前 {Math.min(PREVIEW_ROWS, preview.length)} 行预览（灰底为转换成目标类型后的值）</Text>
            <Table<Record<string, string>>
              size="small"
              style={{ marginTop: 8 }}
              rowKey="__line"
              columns={previewColumns}
              dataSource={preview}
              pagination={false}
              scroll={{ x: 'max-content' }}
            />
          </div>
        </Space>
      ) : null}

      {step === 2 && outcomes ? (
        <Space direction="vertical" style={{ width: '100%' }} size={10}>
          <Alert
            data-testid="zlc-import-summary"
            type={summary?.tone ?? 'warning'}
            showIcon
            message={summary?.headline ?? '导入已结束'}
            description={summary?.note}
          />
          {importWarnings.length > 0 ? (
            <Alert
              type="warning"
              showIcon
              message={`${importWarnings.length} 处需要注意（值不在字典中等，不影响写入）`}
              description={
                <>
                  <ul style={{ margin: 0, paddingLeft: 18, maxHeight: 160, overflowY: 'auto' }}>
                    {importWarnings.slice(0, 20).map((item) => (
                      <li key={item.key}>{`数据行 ${item.line}：${item.text}`}</li>
                    ))}
                  </ul>
                  {importWarnings.length > 20 ? (
                    <Text type="secondary" style={{ fontSize: 12 }}>
                      这里只列出前 20 处，另有 {importWarnings.length - 20} 处
                    </Text>
                  ) : null}
                </>
              }
            />
          ) : null}
          <Table<RowOutcome>
            size="small"
            rowKey="line"
            columns={outcomeColumns}
            dataSource={outcomes}
            pagination={{ pageSize: 10 }}
          />
        </Space>
      ) : null}
    </Modal>
  );
}
