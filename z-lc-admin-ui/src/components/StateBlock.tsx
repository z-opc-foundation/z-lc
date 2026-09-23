import type { ReactNode } from 'react';
import { Alert, Button, Empty, Spin } from 'antd';
import { isApiError } from '@/api/client';

/**
 * The three states every data surface must handle. Used by every page so the
 * empty/loading/error treatment is identical across the console.
 */

export function LoadingBlock({ label = '加载中' }: { label?: string }) {
  return (
    <div className="zlc-empty-block">
      <Spin />
      <span>{label}</span>
    </div>
  );
}

export function EmptyBlock({
  title = '暂无数据',
  description,
  action,
}: {
  title?: string;
  description?: ReactNode;
  action?: ReactNode;
}) {
  return (
    <Empty
      image={Empty.PRESENTED_IMAGE_SIMPLE}
      description={
        <div>
          <div>{title}</div>
          {description ? <div style={{ fontSize: 12, color: '#7b8794', marginTop: 4 }}>{description}</div> : null}
        </div>
      }
    >
      {action}
    </Empty>
  );
}

export function ErrorBlock({
  error,
  onRetry,
  title,
}: {
  error: unknown;
  onRetry?: () => void;
  title?: string;
}) {
  const apiError = isApiError(error);
  const message = error instanceof Error ? error.message : String(error);
  return (
    <Alert
      type="error"
      showIcon
      message={title ?? (apiError ? `接口返回错误${apiError ? ` (${error.code})` : ''}` : '加载失败')}
      description={message}
      action={
        onRetry ? (
          <Button size="small" onClick={onRetry}>
            重试
          </Button>
        ) : null
      }
      style={{ margin: 12 }}
    />
  );
}

/** Renders exactly one of loading / error / empty / content. */
export function StateBlock({
  isLoading,
  isError,
  error,
  isEmpty,
  empty,
  errorTitle,
  onRetry,
  children,
}: {
  isLoading: boolean;
  isError: boolean;
  error?: unknown;
  isEmpty: boolean;
  empty?: ReactNode;
  errorTitle?: string;
  onRetry?: () => void;
  children: ReactNode;
}) {
  if (isLoading) return <LoadingBlock />;
  if (isError) return <ErrorBlock error={error} onRetry={onRetry} title={errorTitle} />;
  if (isEmpty) return <>{empty ?? <EmptyBlock />}</>;
  return <>{children}</>;
}
