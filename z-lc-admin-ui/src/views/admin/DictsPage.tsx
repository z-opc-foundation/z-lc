import { useCallback, useEffect, useState } from 'react';
import {
  Button,
  Card,
  Input,
  List,
  Modal,
  Popconfirm,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { ArrowDownOutlined, ArrowUpOutlined, DeleteOutlined, PlusOutlined } from '@ant-design/icons';
import type { DictDTO, DictItemDTO } from '@/api/types';
import {
  createDict,
  createDictItem,
  deleteDict,
  deleteDictItem,
  listDictItems,
  listDicts,
  updateDict,
  updateDictItem,
} from '@/api/dict';
import { DEFAULT_TENANT_CODE } from '@/api/client';
import { StateBlock } from '@/components/StateBlock';
import { AdminScaffold, ListBanner } from './_shared';
import { listEmptyText, useAppSelection, useResourceList } from './_scope';

const { Text } = Typography;

/**
 * Dictionaries. Items are edited one at a time on purpose: `items/create` and
 * `items/update` are single-record operations (they used to route through the
 * whole-set `saveItems`, which silently deleted every sibling item).
 */
export function DictsPage() {
  const {
    appCode,
    setAppCode,
    options,
    error: appError,
    reload: reloadApps,
  } = useAppSelection();
  const dictList = useResourceList<DictDTO>(() => listDicts(appCode), appCode || null);
  const { rows: dicts, state: dictState, error: dictError, loading, reload: reloadDicts } = dictList;
  const [active, setActive] = useState<DictDTO | null>(null);
  const itemList = useResourceList<DictItemDTO>(
    () => listDictItems(active?.dictCode ?? ''),
    active?.dictCode ?? null,
  );
  const {
    state: itemState,
    error: itemError,
    loading: itemsLoading,
    reload: reloadItems,
    setRows: setItems,
  } = itemList;
  const items = itemList.rows;
  const [editorOpen, setEditorOpen] = useState(false);
  const [draft, setDraft] = useState<DictDTO>({ dictCode: '', dictName: '', status: 'ENABLED' });
  const [isNew, setIsNew] = useState(true);

  // Keep the selection on the same dictionary across a reload; a failed read
  // clears `rows`, and the active dictionary must go with it.
  useEffect(() => {
    setActive((prev) => dicts.find((item) => item.dictCode === prev?.dictCode) ?? dicts[0] ?? null);
  }, [dicts]);

  const saveDict = useCallback(async () => {
    if (!draft.dictCode?.trim() || !draft.dictName?.trim()) {
      message.warning('字典编码与名称都不能为空');
      return;
    }
    const payload: DictDTO = {
      ...draft,
      tenantCode: DEFAULT_TENANT_CODE,
      status: draft.status ?? 'ENABLED',
    };
    try {
      if (isNew) {
        await createDict(payload);
        message.success('字典已创建');
      } else {
        await updateDict(payload);
        message.success('字典已更新');
      }
      setEditorOpen(false);
      reloadDicts();
    } catch (err) {
      message.error(err instanceof Error ? err.message : '保存失败');
    }
  }, [draft, isNew, reloadDicts]);

  const patchItem = useCallback(
    async (index: number, patch: Partial<DictItemDTO>) => {
      const next = [...items];
      const target = next[index];
      if (!target) return;
      next[index] = { ...target, ...patch };
      setItems(next);
    },
    [items, setItems],
  );

  const commitItem = useCallback(
    async (item: DictItemDTO, index: number) => {
      if (!active?.dictCode) return;
      if (!item.itemCode?.trim()) {
        message.warning('itemCode 不能为空');
        return;
      }
      try {
        if (item.id) {
          await updateDictItem(active.dictCode, item);
        } else {
          const created = await createDictItem(active.dictCode, {
            ...item,
            dictCode: active.dictCode,
            tenantCode: DEFAULT_TENANT_CODE,
          });
          const next = [...items];
          next[index] = created;
          setItems(next);
        }
        message.success('字典项已保存');
        reloadItems();
      } catch (err) {
        message.error(err instanceof Error ? err.message : '保存字典项失败');
      }
    },
    [active?.dictCode, items, reloadItems, setItems],
  );

  const addItem = useCallback(() => {
    setItems((prev) => [
      ...prev,
      {
        dictCode: active?.dictCode ?? '',
        itemCode: `ITEM${prev.length + 1}`,
        itemLabel: '',
        itemValue: '',
        sortOrder: prev.length + 1,
      },
    ]);
  }, [active?.dictCode, setItems]);

  const moveItem = useCallback(
    (index: number, delta: number) => {
      const to = index + delta;
      if (to < 0 || to >= items.length) return;
      const next = [...items];
      const [moved] = next.splice(index, 1);
      if (!moved) return;
      next.splice(to, 0, moved);
      const reordered = next.map((item, i) => ({ ...item, sortOrder: i + 1 }));
      setItems(reordered);
      void Promise.all(
        reordered
          .filter((item) => item.id)
          .map((item) => updateDictItem(active?.dictCode ?? '', item).catch(() => null)),
      ).then(() => message.success('顺序已保存'));
    },
    [items, active?.dictCode, setItems],
  );

  const itemColumns = [
    {
      title: '编码',
      dataIndex: 'itemCode',
      width: 130,
      render: (_v: unknown, row: DictItemDTO, index: number) => (
        <Input size="small" value={row.itemCode ?? ''} onChange={(e) => patchItem(index, { itemCode: e.target.value })} />
      ),
    },
    {
      title: '显示标签',
      dataIndex: 'itemLabel',
      width: 150,
      render: (_v: unknown, row: DictItemDTO, index: number) => (
        <Input
          size="small"
          value={row.itemLabel ?? ''}
          placeholder="网格里显示的文字"
          onChange={(e) => patchItem(index, { itemLabel: e.target.value })}
          onBlur={() => void commitItem(row, index)}
        />
      ),
    },
    {
      title: '存储值',
      dataIndex: 'itemValue',
      width: 150,
      render: (_v: unknown, row: DictItemDTO, index: number) => (
        <Input size="small" value={row.itemValue ?? ''} onChange={(e) => patchItem(index, { itemValue: e.target.value })} />
      ),
    },
    {
      title: '排序',
      dataIndex: 'sortOrder',
      width: 70,
      render: (value: number | null | undefined) => <Tag>{value ?? '-'}</Tag>,
    },
    {
      title: '说明',
      dataIndex: 'description',
      render: (_v: unknown, row: DictItemDTO, index: number) => (
        <Input
          size="small"
          value={row.description ?? ''}
          onChange={(e) => patchItem(index, { description: e.target.value })}
          onBlur={() => void commitItem(row, index)}
        />
      ),
    },
    {
      title: '',
      key: 'ops',
      width: 112,
      render: (_v: unknown, row: DictItemDTO, index: number) => (
        <Space size={0}>
          <Button size="small" type="text" icon={<ArrowUpOutlined />} onClick={() => moveItem(index, -1)} />
          <Button size="small" type="text" icon={<ArrowDownOutlined />} onClick={() => moveItem(index, 1)} />
          <Popconfirm
            title="删除这个字典项？"
            onConfirm={async () => {
              if (!row.id) {
                setItems((prev) => prev.filter((_, i) => i !== index));
                return;
              }
              await deleteDictItem(row.id);
              message.success('已删除');
              if (active?.dictCode) reloadItems();
            }}
          >
            <Button size="small" type="text" danger icon={<DeleteOutlined />} />
          </Popconfirm>
          <Button size="small" type="text" onClick={() => void commitItem(row, index)}>
            存
          </Button>
        </Space>
      ),
    },
  ] as ColumnsType<DictItemDTO>;

  return (
    <AdminScaffold
      title="数据字典"
      description="字段的可选值集合。实体字段绑定 dictCode 后，表格里会自动显示 itemLabel。"
      appCode={appCode}
      onAppCode={setAppCode}
      appOptions={options}
      appError={appError}
      onRefresh={() => {
        reloadApps();
        reloadDicts();
      }}
      actions={
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={() => {
            setIsNew(true);
            setDraft({ dictCode: '', dictName: '', status: 'ENABLED', tenantCode: DEFAULT_TENANT_CODE });
            setEditorOpen(true);
          }}
        >
          新建字典
        </Button>
      }
    >
      <div style={{ display: 'flex', gap: 12, alignItems: 'flex-start' }}>
        <Card size="small" title={`字典（${dicts.length}）`} style={{ width: 280, flex: '0 0 280px' }}>
          <StateBlock
            isLoading={loading}
            isError={dictState === 'error'}
            error={dictError}
            onRetry={reloadDicts}
            isEmpty={dicts.length === 0}
            errorTitle="字典列表没有读到"
            empty={
              <Text type="secondary">
                {appCode ? '该应用还没有字典' : '选择应用后再看这里'}
              </Text>
            }
          >
            <List
              size="small"
              dataSource={dicts}
              renderItem={(item) => (
                <List.Item
                  style={{
                    cursor: 'pointer',
                    background: item.dictCode === active?.dictCode ? '#eef4ff' : undefined,
                    paddingLeft: 8,
                  }}
                  onClick={() => setActive(item)}
                  actions={[
                    <Button
                      key="edit"
                      size="small"
                      type="text"
                      onClick={(event) => {
                        event.stopPropagation();
                        setIsNew(false);
                        setDraft(item);
                        setEditorOpen(true);
                      }}
                    >
                      编辑
                    </Button>,
                    <Popconfirm
                      key="del"
                      title="删除字典会连同其字典项一起失效，确认？"
                      onConfirm={async () => {
                        if (!item.id) return;
                        await deleteDict(item.id);
                        message.success('已删除');
                        if (active?.dictCode === item.dictCode) setActive(null);
                        reloadDicts();
                      }}
                    >
                      <Button
                        size="small"
                        type="text"
                        danger
                        icon={<DeleteOutlined />}
                        onClick={(event) => event.stopPropagation()}
                      />
                    </Popconfirm>,
                  ]}
                >
                  <List.Item.Meta
                    title={<Text style={{ fontSize: 13 }}>{item.dictName || item.dictCode}</Text>}
                    description={<Text type="secondary" style={{ fontSize: 12 }}>{item.dictCode}</Text>}
                  />
                </List.Item>
              )}
            />
          </StateBlock>
        </Card>

        <Card
          size="small"
          style={{ flex: 1, minWidth: 0 }}
          title={active ? `字典项 · ${active.dictName || active.dictCode}` : '字典项'}
          extra={
            <Button size="small" icon={<PlusOutlined />} disabled={!active} onClick={addItem}>
              添加一项
            </Button>
          }
        >
          <ListBanner state={itemState} error={itemError} onRetry={reloadItems} label="字典项" />
          <Table<DictItemDTO>
            size="small"
            rowKey={(row, index) => String(row.id ?? `new-${index}`)}
            loading={itemsLoading}
            columns={itemColumns}
            dataSource={items}
            pagination={false}
            scroll={{ x: 'max-content' }}
            locale={{
              emptyText: active
                ? listEmptyText(itemState, '字典项')
                : '请先选择左侧字典',
            }}
          />
        </Card>
      </div>

      <Modal
        open={editorOpen}
        title={isNew ? '新建字典' : '编辑字典'}
        onOk={() => void saveDict()}
        onCancel={() => setEditorOpen(false)}
        okText="保存"
        cancelText="取消"
      >
        <Space direction="vertical" style={{ width: '100%' }} size={10}>
          <Input
            addonBefore="字典编码"
            disabled={!isNew}
            value={draft.dictCode ?? ''}
            onChange={(event) => setDraft({ ...draft, dictCode: event.target.value })}
          />
          <Input
            addonBefore="名称"
            value={draft.dictName ?? ''}
            onChange={(event) => setDraft({ ...draft, dictName: event.target.value })}
          />
          <Input
            addonBefore="说明"
            value={draft.description ?? ''}
            onChange={(event) => setDraft({ ...draft, description: event.target.value })}
          />
          <Select
            style={{ width: '100%' }}
            value={draft.status ?? 'ENABLED'}
            options={[
              { value: 'ENABLED', label: '启用' },
              { value: 'DISABLED', label: '停用' },
            ]}
            onChange={(status) => setDraft({ ...draft, status })}
          />
        </Space>
      </Modal>
    </AdminScaffold>
  );
}
