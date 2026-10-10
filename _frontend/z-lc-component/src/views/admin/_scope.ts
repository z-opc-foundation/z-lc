import { useCallback, useEffect, useRef, useState } from 'react';
import type { Dispatch, SetStateAction } from 'react';
import { useSearchParams } from 'react-router-dom';
import type { AppDTO } from '@/lc/api/types';
import { listApps } from '@/lc/api/app';
import { unreadReason } from '@/lc/api/meta';
import { useWorkspace } from '@/lc/hooks/useWorkspace';

/* eslint-disable react-refresh/only-export-components -- 纯 hook 与工具函数，没有组件 */

/** A list read has five outcomes, and the console must say which one it got. */
export type ListState = 'idle' | 'loading' | 'error' | 'empty' | 'ready';

/** Outcome of the last read, as stored by the hook. */
type ReadStatus = 'idle' | 'loading' | 'error' | 'done';

export interface ResourceList<T> {
  rows: T[];
  /** Outcome of the *last* read — not a synonym for `rows.length === 0`. */
  state: ListState;
  error: unknown;
  loading: boolean;
  setRows: Dispatch<SetStateAction<T[]>>;
  reload: () => void;
}

/**
 * Shared loader for every admin list.
 *
 * `error` and `empty` are different sentences, and a failed reload clears the
 * rows: stale rows sitting under a finished spinner read as "healthy", which is
 * worse than an error banner. A non-array body (`success:true, data:null`) is
 * also `error` — the endpoint answered, but it did not answer the question.
 *
 * `state` is derived from the read status plus `rows.length`, so the caller's
 * optimistic local edits (draft rows, reordering) stay consistent with it.
 *
 * `fetch` must close over the same values as `scope`; it is held in a ref so an
 * inline arrow cannot re-trigger the request on every render.
 */
export function useResourceList<T>(
  fetch: () => Promise<T[] | null | undefined>,
  scope: string | null | undefined,
): ResourceList<T> {
  const [rows, setRows] = useState<T[]>([]);
  const [status, setStatus] = useState<ReadStatus>('idle');
  const [error, setError] = useState<unknown>(null);
  const [attempt, setAttempt] = useState(0);
  /** Bumped per read, so a slow answer for the previous scope cannot land. */
  const seq = useRef(0);
  const fetchRef = useRef(fetch);

  useEffect(() => {
    fetchRef.current = fetch;
  }, [fetch]);

  useEffect(() => {
    const mine = ++seq.current;
    if (!scope) {
      setRows([]);
      setStatus('idle');
      setError(null);
      return;
    }
    setStatus('loading');
    setError(null);
    void (async () => {
      let next: T[] | null | undefined;
      let failure: unknown = null;
      try {
        next = await fetchRef.current();
      } catch (err) {
        failure = err;
      }
      if (mine !== seq.current) return;
      if (failure || !Array.isArray(next)) {
        setRows([]);
        setError(failure ?? new Error('接口没有返回列表'));
        setStatus('error');
        return;
      }
      setRows(next);
      setStatus('done');
    })();
  }, [scope, attempt]);

  /** Stable, so callers can keep it in their own `useCallback` deps. */
  const reload = useCallback(() => setAttempt((prev) => prev + 1), []);

  const state: ListState =
    status === 'idle'
      ? 'idle'
      : status === 'loading'
        ? 'loading'
        : status === 'error'
          ? 'error'
          : rows.length
            ? 'ready'
            : 'empty';

  return {
    rows,
    state,
    error,
    loading: status === 'loading',
    setRows,
    reload,
  };
}

/** Apps are the scope for every admin surface, so the picker lives here once. */
export function useAppSelection(): {
  appCode: string;
  setAppCode: (code: string) => void;
  apps: AppDTO[];
  options: { value: string; label: string }[];
  loading: boolean;
  /** The app list itself failed: an empty picker is then "读不到", not "没有应用". */
  error: unknown;
  reload: () => void;
} {
  const [params] = useSearchParams();
  const [appCode, setAppCode] = useState(params.get('appCode') ?? '');
  const apps = useResourceList<AppDTO>(() => listApps(), 'apps');

  useEffect(() => {
    setAppCode((prev) => prev || apps.rows[0]?.appCode || '');
  }, [apps.rows]);

  return {
    appCode,
    setAppCode,
    apps: apps.rows,
    options: apps.rows.map((app) => ({ value: app.appCode ?? '', label: app.appName || app.appCode })),
    loading: apps.loading,
    error: apps.error,
    reload: apps.reload,
  };
}

/**
 * 「库里真没有」和「接口没读到」必须是两句话 —— 前者让人去建数据，
 * 后者让人去查接口，合并成一句空态会让人往错误的方向修。
 */
export function listEmptyText(state: ListState, label: string): string {
  if (state === 'error') return '接口没有读到数据，无法判断有没有';
  if (state === 'idle') return '选择应用后再看这里';
  return `该应用还没有${label}`;
}

/** Entity codes of the selected app, for the `entityCode` column/filter. */
export interface EntityOptionSource {
  options: { value: string; label: string }[];
  /**
   * False means the model was never read, so an empty `options` is "couldn't ask"
   * rather than "this app has no entities" — the picker must not imply the latter.
   */
  read: boolean;
  reason: string;
  retry: () => void;
}

export function useEntityOptions(appCode: string): EntityOptionSource {
  const { meta, refetch } = useWorkspace(appCode);
  return {
    options: (meta?.entities ?? []).map((entity) => ({
      value: entity.entityCode ?? '',
      label: entity.entityName || entity.entityCode || '',
    })),
    read: Boolean(meta?.read.entities),
    reason: unreadReason(meta, 'entities'),
    retry: () => {
      void refetch();
    },
  };
}

/** Candidate list of a model-driven Select: name the failure instead of showing 「暂无数据」. */
export function entityNotFoundContent(source: EntityOptionSource): string | undefined {
  return source.read ? undefined : `实体列表没有读到：${source.reason}`;
}



/** 后端把时间统一序列化成 ISO 串；解析不了就原样显示，不假装是"无效数据"。 */
export function formatTime(value: string | number | null | undefined): string {
  if (value === null || value === undefined || value === '') return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  const pad = (input: number) => String(input).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}
