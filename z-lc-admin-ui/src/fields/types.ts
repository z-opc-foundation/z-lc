import type { ReactNode } from 'react';
import type {
  DictDTO,
  DictItemDTO,
  EntityDefDTO,
  FieldDefDTO,
  FieldTypeDescriptor,
  FilterOperator,
  LcRow,
  ViewConfigDTO,
} from '@/api/types';

/**
 * Field registry contracts.
 *
 * Modeled on Baserow's `fieldType` object: a single registry entry per field
 * type owns every type-dependent behaviour (cell render, inline edit, form
 * control, filtering, export, coercion). **No view component may branch on a
 * type string** — they all call through `resolveField()`.
 */

/** Everything a definition needs to render one field inside one workspace. */
export interface FieldContext {
  field: FieldDefDTO;
  entity: EntityDefDTO;
  appCode: string;
  tenantCode: string;
  /** Resolved descriptor: server-provided (`/meta/field-types`) or frontend default. */
  descriptor: FieldTypeDescriptor;
  /** Dict items for `field.dictCode`, when applicable. */
  dict?: DictDTO;
  /**
   * Items of `field.dictCode`. Loaded eagerly from `/meta/bundle` when it
   * carries them, otherwise lazily by the editor via `/dict/items`.
   */
  dictItems?: DictItemDTO[];
  /** Entity def for `field.refEntity`, when applicable. */
  refEntity?: EntityDefDTO;
  /** Field of `refEntity` used as the human label in reference pickers. */
  refLabelField?: FieldDefDTO;
  dicts: DictDTO[];
  entities: EntityDefDTO[];
  views: ViewConfigDTO[];
}

export interface CellProps {
  ctx: FieldContext;
  row: LcRow;
  /** Raw cell value from the row (snake_case column). */
  value: unknown;
  /** Derived `{code}_label` / `{code}_name` column when the backend provided one. */
  displayValue?: unknown;
  compact?: boolean;
}

export interface EditorProps extends CellProps {
  onCommit: (next: unknown) => void;
  onCancel: () => void;
}

export interface FilterInputProps {
  ctx: FieldContext;
  operator: FilterOperator;
  value: unknown;
  onChange: (next: unknown) => void;
}

export interface FormInputProps {
  ctx: FieldContext;
  value: unknown;
  onChange: (next: unknown) => void;
  disabled?: boolean;
  /** Read-only detail mode. */
  preview?: boolean;
}

/**
 * Declarative description of the per-type config sub-form shown in the schema
 * designer. Keeping it data (not JSX) means the designer renders it generically
 * and never has to branch on a type string.
 */
export interface ConfigFieldSpec {
  /** Property on `FieldDefDTO`. */
  key: 'fieldLength' | 'scale' | 'defaultValue' | 'dictCode' | 'refEntity' | 'description';
  label: string;
  kind: 'number' | 'text' | 'textarea' | 'dictSelect' | 'entitySelect';
  help?: string;
  min?: number;
  max?: number;
  step?: number;
  required?: boolean;
}

/**
 * A registry entry. `toFieldValue` / `toFormValue` are the single place where
 * wire-format knowledge lives (see `DynamicSqlBuilder.coerce`).
 */
export interface FieldDefinition {
  /** Registry key: a `fieldType` (`STRING`…) or a synthetic kind (`dict`, `ref`). */
  key: string;
  label: string;
  /** Ordering hint for the designer's "add field" palette. */
  order: number;
  /** Icon element rendered in the entity nav / designer palette. */
  icon?: ReactNode;
  /** Default capability metadata; overridden by the server descriptor. */
  descriptor: FieldTypeDescriptor;
  /**
   * Operators this type supports, already intersected with the backend's real
   * whitelist — the only operators that reach the wire.
   */
  operators: readonly FilterOperator[];
  /** Which `FieldDefDTO` properties this type lets the designer edit. */
  configFields: readonly ConfigFieldSpec[];

  renderCell: (props: CellProps) => ReactNode;
  renderEditor: (props: EditorProps) => ReactNode;
  renderFormInput: (props: FormInputProps) => ReactNode;
  renderFilterInput: (props: FilterInputProps) => ReactNode;

  /** CSV export / clipboard text. */
  toCellText: (props: CellProps) => string;
  /** Row value -> controlled component value. */
  toFormValue: (raw: unknown, ctx: FieldContext) => unknown;
  /** Controlled component value -> request payload, honouring `coerce`. */
  toFieldValue: (formValue: unknown, ctx: FieldContext) => unknown;

  /** Inline editing is disabled for expensive/ambiguous editors. */
  supportsInlineEdit?: (ctx: FieldContext) => boolean;
  /** Grouping label for `groupable` columns. */
  groupValue?: (props: CellProps) => string;
}

/** A field resolved against a workspace: definition + effective capabilities. */
export interface ResolvedField {
  ctx: FieldContext;
  def: FieldDefinition;
  /** `field_*` column key holding the value. */
  valueKey: string;
  /** `field_label` / `field_name` derived column key, when the type has one. */
  displayKey?: string;
  sortable: boolean;
  groupable: boolean;
  filterable: boolean;
  inlineEditable: boolean;
  operators: readonly FilterOperator[];
}
