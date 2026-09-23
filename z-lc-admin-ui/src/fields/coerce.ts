import dayjs, { type Dayjs } from 'dayjs';
import type { LcRow } from '@/api/types';

/**
 * Value coercion shared by every field definition.
 *
 * The wire format is dictated by `FieldTypeRegistry.coerceValue` (reached via
 * `DynamicSqlBuilder.coerce`), which is unforgiving:
 *   - INT/LONG/REF : `Long.parseLong(raw.toString())` -> "" or "abc" throws
 *   - DECIMAL      : `Double.parseDouble(...)`        -> same
 *   - BOOLEAN      : `Boolean.parseBoolean(...)`      -> anything but "true" is FALSE
 *   - DATE/DATETIME: `SimpleDateFormat("yyyy-MM-dd"[,"yyyy-MM-dd HH:mm:ss"]).parse(...)`
 *                    -> ISO-8601 ("...T...Z") and epoch millis both FAIL
 *   - STRING/TEXT/JSON: `raw.toString()` -> a JS object becomes "[object Object]"
 * So the client must pre-format dates, pre-stringify JSON and send real numbers.
 */

export const DATE_FORMAT = 'YYYY-MM-DD';
export const DATETIME_FORMAT = 'YYYY-MM-DD HH:mm:ss';

/** Read a cell out of a snake_case row. */
export function readCell(row: LcRow, fieldCode: string): unknown {
  return row[fieldCode];
}

export function isBlank(value: unknown): boolean {
  return value === null || value === undefined || value === '';
}

export function asNumber(value: unknown): number | null {
  if (isBlank(value)) return null;
  if (typeof value === 'number') return Number.isFinite(value) ? value : null;
  if (typeof value === 'boolean') return value ? 1 : 0;
  const parsed = Number(String(value).trim());
  return Number.isFinite(parsed) ? parsed : null;
}

export function asText(value: unknown): string {
  if (isBlank(value)) return '';
  if (typeof value === 'object') return JSON.stringify(value);
  return String(value);
}

/** Mirrors `Boolean.parseBoolean`: only the literal "true" (any case) is true. */
const TRUTHY = new Set(['true', '1', 'y', 'yes', 'on', 't']);
const FALSY = new Set(['false', '0', 'n', 'no', 'off', 'f', '']);

/**
 * 与后端 BooleanTypeHandler.coerce 同一套口径。
 * 原来这里只有 `=== 'true'`，于是字典里存 '1'、CSV 里写 1、整数式布尔都会被静默判成 false ——
 * 和后端刚修掉的是同一个"校验放行/写入篡改"类问题。
 */
export function asBoolean(value: unknown): boolean {
  if (typeof value === 'boolean') return value;
  if (typeof value === 'number') return value !== 0;
  if (value === null || value === undefined) return false;
  const text = String(value).trim().toLowerCase();
  if (TRUTHY.has(text)) return true;
  if (FALSY.has(text)) return false;
  return false;
}

/** Accept epoch millis, ISO strings and the server's `yyyy-MM-dd[ HH:mm:ss]`. */
export function parseDate(value: unknown): Dayjs | null {
  if (isBlank(value)) return null;
  if (dayjs.isDayjs(value)) return value.isValid() ? value : null;
  if (typeof value === 'number' || /^\d{11,}$/.test(String(value))) {
    const millis = typeof value === 'number' ? value : Number(value);
    const fromMillis = dayjs(millis);
    return fromMillis.isValid() ? fromMillis : null;
  }
  const text = String(value).trim().replace(' ', 'T');
  const direct = dayjs(String(value).trim());
  if (direct.isValid()) return direct;
  const normalised = dayjs(text);
  return normalised.isValid() ? normalised : null;
}

export function formatDate(value: unknown, pattern: string): string {
  const parsed = parseDate(value);
  return parsed ? parsed.format(pattern) : asText(value);
}

/** Form-control value -> wire value, following the type table in the header. */
export function toWireValue(fieldType: string, formValue: unknown): unknown {
  if (isBlank(formValue)) {
    // Empty string is normalised to NULL so numeric/date columns do not choke
    // on `Long.parseLong("")` / `SimpleDateFormat.parse("")`.
    return null;
  }
  switch (fieldType.toUpperCase()) {
    case 'INT':
    case 'LONG':
    case 'REF':
      return asNumber(formValue);
    case 'DECIMAL':
      return asNumber(formValue);
    case 'BOOLEAN':
      return asBoolean(formValue);
    case 'DATE':
      return formatDate(formValue, DATE_FORMAT);
    case 'DATETIME':
      return formatDate(formValue, DATETIME_FORMAT);
    case 'JSON':
      return typeof formValue === 'string' ? formValue : JSON.stringify(formValue);
    case 'TEXT':
    case 'STRING':
    default:
      if (typeof formValue === 'object') return JSON.stringify(formValue);
      return String(formValue);
  }
}

/** Deep-ish equality good enough for dirty-flagging a form. */
export function valuesEqual(a: unknown, b: unknown): boolean {
  if (a === b) return true;
  if (isBlank(a) && isBlank(b)) return true;
  return JSON.stringify(a ?? null) === JSON.stringify(b ?? null);
}
