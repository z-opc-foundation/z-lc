import Papa from 'papaparse';
import { MAX_PAGE_SIZE, listRecords } from '@/api/runtime';
import { asText } from '@/fields';
import type { Conjunction, LcRow, QueryCondition, QuerySort } from '@/api/types';
import type { ResolvedField } from '@/fields';

/**
 * Client-side CSV import/export.
 *
 * Export walks every page under the *current* filter + sort so the file matches
 * what the user sees, and renders each cell through the field registry's
 * `toCellText` (so dict codes become labels, dates keep the console format).
 */

export const EXPORT_ROW_CAP = 20_000;

export interface ExportParams {
  entityCode: string;
  appCode: string;
  tenantCode: string;
  columns: ResolvedField[];
  conditions: QueryCondition[];
  conjunction: Conjunction;
  sorts: QuerySort[];
  cap?: number;
}

export interface ExportResult {
  rows: LcRow[];
  truncated: boolean;
  total: number;
}

export async function fetchAllForExport(params: ExportParams): Promise<ExportResult> {
  const cap = params.cap ?? EXPORT_ROW_CAP;
  const collected: LcRow[] = [];
  let total = 0;
  let page = 1;

  for (;;) {
    const result = await listRecords(
      params.entityCode,
      { appCode: params.appCode, tenantCode: params.tenantCode },
      {
        page,
        size: MAX_PAGE_SIZE,
        conditions: params.conditions,
        conjunction: params.conjunction,
        sorts: params.sorts,
      },
    );
    total = Number(result.total ?? collected.length);
    const records = result.records ?? [];
    collected.push(...records);
    if (records.length === 0) break;
    if (collected.length >= Math.min(total, cap)) break;
    page += 1;
    if (page > 500) break;
  }

  return {
    rows: collected.slice(0, cap),
    truncated: collected.length > cap || (total > 0 && collected.length < total),
    total,
  };
}

export function buildCsv(
  columns: ResolvedField[],
  rows: LcRow[],
): string {
  const header = columns.map((resolved) => headerOf(resolved));
  const body = rows.map((row) =>
    columns.map((resolved) =>
      resolved.def.toCellText({
        ctx: resolved.ctx,
        row,
        value: row[resolved.valueKey],
        displayValue: resolved.displayKey ? row[resolved.displayKey] : undefined,
      }),
    ),
  );
  return Papa.unparse({ fields: header, data: body }, { quotes: true });
}

export function headerOf(resolved: ResolvedField): string {
  const field = resolved.ctx.field;
  return field.fieldName && field.fieldName !== field.fieldCode
    ? `${field.fieldName}(${field.fieldCode})`
    : field.fieldCode;
}

export function downloadText(filename: string, content: string): void {
  // BOM so Excel on Windows reads UTF-8 Chinese correctly.
  const blob = new Blob([`\ufeff${content}`], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename;
  document.body.appendChild(anchor);
  anchor.click();
  document.body.removeChild(anchor);
  URL.revokeObjectURL(url);
}

/* ------------------------------------------------------------------ */
/* Import                                                              */
/* ------------------------------------------------------------------ */

export interface ParsedCsv {
  headers: string[];
  rows: string[][];
  /** Rows the parser flagged (ragged lines etc). */
  errors: string[];
}

export async function parseCsvFile(file: File): Promise<ParsedCsv> {
  const text = await file.text();
  const parsed = Papa.parse<string[]>(text, {
    skipEmptyLines: 'greedy',
    // Keep everything as text; type coercion is an explicit user step.
    header: false,
    dynamicTyping: false,
  });

  const matrix = (parsed.data as unknown[][])
    .filter((row) => Array.isArray(row))
    .map((row) => row.map((cell) => asText(cell)));

  const headers = matrix.shift() ?? [];
  const width = headers.length;
  const rows = matrix.map((row) =>
    row.length === width ? row : [...row, ...Array(Math.max(0, width - row.length)).fill('')],
  );

  return {
    headers,
    rows,
    errors: (parsed.errors ?? []).slice(0, 20).map((error) => `第 ${(error.row ?? 0) + 1} 行: ${error.message}`),
  };
}

/** Guess which CSV column maps to which field code (exact, then fuzzy). */
export function autoMap(
  headers: string[],
  candidates: { fieldCode: string; fieldName: string }[],
): Record<string, number | null> {
  const map: Record<string, number | null> = {};
  const normalised = headers.map((header) => header.replace(/[()\s]/g, '').toLowerCase());

  for (const candidate of candidates) {
    const code = candidate.fieldCode.toLowerCase();
    const name = (candidate.fieldName || '').toLowerCase();
    let index = normalised.indexOf(code);
    if (index < 0 && name) index = normalised.indexOf(name);
    if (index < 0 && name) index = normalised.findIndex((header) => header === `${name}${code}` || header === `${code}${name}`);
    if (index < 0 && name) index = normalised.findIndex((header) => header.includes(name) && name.length > 1);
    map[candidate.fieldCode] = index >= 0 ? index : null;
  }
  return map;
}
