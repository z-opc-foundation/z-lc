import type { ReactNode } from 'react';
import { EmptyState, ErrorState, LoadingState } from '@yuku123/z-frontend-common/ui';
import { isApiError } from '@/lc/api/client';

/**
 * 全仓统一的加载 / 空 / 错误三态。
 *
 * 这三态的「皮肉」全部交给共享基座（@yuku123/z-frontend-common/ui），本文件只留
 * z-lc 自己的语义：
 *   - 错误标题要区分「接口返回错误 (code)」和「加载失败」；
 *   - 空态文案由调用方给，基座只负责排版。
 *
 * 为什么保留这层薄封装而不是让每个页面直接 import 基座：z-lc 有二十来个数据面，
 * 「读失败」与「读到空」必须说的是两句话 —— 这个判断集中在一处，才不会哪天某个页面
 * 把 ErrorState 换成一句 toast 又把「我没读到」讲成「库里没有」。
 */

export function LoadingBlock({ label = '加载中' }: { label?: string }) {
  return (
    <div className="zlc-empty-block">
      {/* 骨架屏比转圈圈专业，也提前占住位置，避免数据到位时整页跳动（CLS）。 */}
      <LoadingState variant="table" rows={4} style={{ width: '100%', maxWidth: 680 }} />
      <span>{label}</span>
    </div>
  );
}

export function EmptyBlock({
  title = '暂无数据',
  description,
  actionText,
  onAction,
}: {
  title?: string;
  description?: string;
  actionText?: string;
  onAction?: () => void;
}) {
  return (
    <EmptyState
      title={title}
      description={description}
      actionText={actionText}
      onAction={onAction}
    />
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
  const text = error instanceof Error ? error.message : String(error);
  return (
    <ErrorState
      // 基座会优先取后端响应体里的 message；这里自己给 description，
      // 是因为 z-lc 的错误对象是 ApiError（自带 code），基座取不到 code 那一句。
      error={error instanceof Error ? error : text}
      title={title ?? (apiError ? `接口返回错误 (${error.code})` : '加载失败')}
      description={text}
      onRetry={onRetry}
      retryText="重试"
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
