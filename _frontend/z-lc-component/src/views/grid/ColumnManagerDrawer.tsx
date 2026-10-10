import { useMemo, useState } from 'react';
import {
  DndContext,
  KeyboardSensor,
  PointerSensor,
  closestCenter,
  useSensor,
  useSensors,
  type DragEndEvent,
} from '@dnd-kit/core';
import {
  SortableContext,
  arrayMove,
  sortableKeyboardCoordinates,
  useSortable,
  verticalListSortingStrategy,
} from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { Button, Drawer, InputNumber, Space, Switch, Tooltip, Typography } from 'antd';
import { EyeInvisibleOutlined, EyeOutlined, HolderOutlined, LockOutlined } from '@ant-design/icons';
import type { ColumnMeta } from '@/lc/api/types';
import type { ResolvedField } from '@yuku123/render/fields';
import { headerOf } from './csv';
import { projectColumns } from './gridModel';

const { Text } = Typography;

interface RowEntry {
  fieldCode: string;
  label: string;
  typeLabel: string;
  meta: ColumnMeta;
  pinned: boolean;
  sortable: boolean;
}

/**
 * Column manager: drag to reorder, toggle visibility, set width.
 *
 * The order/visibility/width it emits is exactly what gets persisted into the
 * saved view's `columnMeta`, so this drawer is the single writer of layout.
 */
export function ColumnManagerDrawer({
  open,
  onClose,
  resolvedFields,
  columnMeta,
  onChange,
}: {
  open: boolean;
  onClose: () => void;
  resolvedFields: ResolvedField[];
  columnMeta: ColumnMeta[];
  onChange: (next: ColumnMeta[]) => void;
}) {
  const [dragging, setDragging] = useState(false);

  const rows = useMemo<RowEntry[]>(() => {
    // 必须走表格用的同一份投影：以前这里直接 map(resolvedFields)，于是抽屉按 schema 排、
    // 表格按 columnMeta 排，同一个抽屉里"看到的顺序"和"写出去的顺序"是两套。
    // 后果有两层：排完一次列表纹丝不动（用户以为没生效），而下一次拖拽是从 schema 顺序
    // 起算的，上一次的结果被整份覆盖掉（实测两次键盘排序得到完全相同的表头）。
    const projected = projectColumns(resolvedFields, columnMeta);
    return projected.map(({ resolved, meta }) => {
      const code = resolved.ctx.field.fieldCode;
      return {
        fieldCode: code,
        label: headerOf(resolved),
        typeLabel: resolved.def.label,
        meta,
        pinned: code === 'id',
        sortable: resolved.sortable,
      };
    });
  }, [resolvedFields, columnMeta]);

  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 3 } }),
    useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates }),
  );

  const patch = (fieldCode: string, next: Partial<ColumnMeta>) => {
    // 以"当前列出的完整顺序"为基底整份写回。以前是往 columnMeta 尾巴追加缺失项，而表格把
    // columnMeta 的顺序当首选顺序 —— 实测：一个没配过列的视图（columnMeta 为默认的空数组）
    // 里只改一下"工时"的宽度，表头就从 标题|优先级|工时 变成 工时|标题|优先级。
    onChange(rows.map((row) => (row.fieldCode === fieldCode ? { ...row.meta, ...next } : row.meta)));
  };

  const handleDragEnd = (event: DragEndEvent) => {
    setDragging(false);
    const { active, over } = event;
    if (!over || active.id === over.id) return;

    // Reorder against the *currently rendered* order (schema order + meta).
    const order = rows.map((row) => row.fieldCode);
    const from = order.indexOf(String(active.id));
    const to = order.indexOf(String(over.id));
    if (from < 0 || to < 0) return;
    const nextOrder = arrayMove(order, from, to);
    const byCode = new Map(rows.map((row) => [row.fieldCode, row.meta]));
    onChange(nextOrder.map((code) => byCode.get(code) ?? { fieldCode: code }));
  };

  return (
    <Drawer
      title="列设置"
      placement="right"
      width={420}
      open={open}
      onClose={onClose}
      styles={{ body: { padding: '12px 16px' } }}
      extra={
        <Space>
          <Button size="small" onClick={() => onChange([])}>
            恢复默认
          </Button>
          <Button size="small" type="primary" onClick={onClose}>
            完成
          </Button>
        </Space>
      }
    >
      <Text type="secondary" style={{ fontSize: 12, display: 'block', marginBottom: 10 }}>
        拖动排序，使用 <span className="zlc-kbd">空格</span> + 方向键可键盘排序。隐藏列仍保留在 schema 中。
      </Text>

      <DndContext
        sensors={sensors}
        collisionDetection={closestCenter}
        onDragStart={() => setDragging(true)}
        onDragEnd={handleDragEnd}
        onDragCancel={() => setDragging(false)}
      >
        <SortableContext items={rows.map((row) => row.fieldCode)} strategy={verticalListSortingStrategy}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
            {rows.map((row) => (
              <SortableRow
                key={row.fieldCode}
                row={row}
                dragging={dragging}
                onToggleHidden={(hidden) => patch(row.fieldCode, { hidden })}
                onWidthChange={(width) => patch(row.fieldCode, { width: width ?? undefined })}
              />
            ))}
          </div>
        </SortableContext>
      </DndContext>
    </Drawer>
  );
}

function SortableRow({
  row,
  dragging,
  onToggleHidden,
  onWidthChange,
}: {
  row: RowEntry;
  dragging: boolean;
  onToggleHidden: (hidden: boolean) => void;
  onWidthChange: (width: number | null) => void;
}) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
    id: row.fieldCode,
    disabled: row.pinned,
  });

  const hidden = row.meta.hidden === true;

  return (
    <div
      ref={setNodeRef}
      style={{
        transform: CSS.Translate.toString(transform),
        transition: dragging ? transition : undefined,
        display: 'flex',
        alignItems: 'center',
        gap: 8,
        padding: '6px 8px',
        borderRadius: 4,
        border: '1px solid #eceef1',
        background: isDragging ? '#eaf1ff' : hidden ? '#fafbfc' : '#fff',
        opacity: isDragging ? 0.9 : 1,
      }}
    >
      <Tooltip title={row.pinned ? '主键列固定在第一列' : '拖动排序'}>
        <span
          {...attributes}
          {...listeners}
          style={{
            cursor: row.pinned ? 'not-allowed' : 'grab',
            color: '#9aa4b1',
            display: 'inline-flex',
            padding: '0 2px',
          }}
        >
          {row.pinned ? <LockOutlined /> : <HolderOutlined />}
        </span>
      </Tooltip>

      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 13, fontWeight: hidden ? 400 : 500, color: hidden ? '#9aa4b1' : undefined }}>
          {row.label}
        </div>
        <Text type="secondary" style={{ fontSize: 11 }}>
          {row.typeLabel}
          {row.sortable ? '' : ' · 不可排序'}
        </Text>
      </div>

      <InputNumber
        size="small"
        style={{ width: 74 }}
        placeholder="宽度"
        min={60}
        max={900}
        controls={false}
        value={row.meta.width}
        onChange={(next) => onWidthChange(next === null ? null : Number(next))}
      />

      <Tooltip title={hidden ? '显示该列' : '隐藏该列'}>
        <Switch
          size="small"
          checked={!hidden}
          disabled={row.pinned}
          onChange={(checked) => onToggleHidden(!checked)}
          checkedChildren={<EyeOutlined />}
          unCheckedChildren={<EyeInvisibleOutlined />}
        />
      </Tooltip>
    </div>
  );
}
