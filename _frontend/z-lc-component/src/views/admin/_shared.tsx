import { Select, Typography } from 'antd';
import { EmptyState, ErrorState, RefreshButton, TableToolbar } from '@yuku123/z-frontend-common/ui';
import { ErrorBlock } from '@/lc/components/StateBlock';
import { listEmptyText, type ListState } from './_scope';

const { Text, Title } = Typography;

/**
 * 列表空态：把「库里真没有」和「接口没读到」分成两句话。
 *
 * 分句的逻辑仍然住在 _scope.listEmptyText（它就是为了区分这两种情况才写的），
 * 这里只负责把那句话摆成共享基座的 EmptyState。
 */
export function listEmptyState(state: ListState, label: string) {
  return <EmptyState title={listEmptyText(state, label)} size="sm" />;
}

/** Persistent, unlike the transport toast: the empty table stays for minutes. */
export function ListBanner({
  state,
  error,
  onRetry,
  label,
}: {
  state: ListState;
  error: unknown;
  onRetry: () => void;
  label: string;
}) {
  if (state !== 'error' || !error) return null;
  return <ErrorBlock error={error} onRetry={onRetry} title={`${label}没有读到`} />;
}

export function AdminScaffold({
  title,
  description,
  appCode,
  onAppCode,
  appOptions,
  appError,
  onRefresh,
  actions,
  children,
}: {
  title: string;
  description?: string;
  appCode: string;
  onAppCode: (code: string) => void;
  appOptions: { value: string; label: string }[];
  appError?: unknown;
  onRefresh?: () => void;
  actions?: React.ReactNode;
  children: React.ReactNode;
}) {
  return (
    <div style={{ padding: 16, maxWidth: 1280 }}>
      <Title level={4} style={{ marginBottom: 2 }}>
        {title}
      </Title>
      {description ? (
        <Text type="secondary" style={{ display: 'block', marginBottom: 12 }}>
          {description}
        </Text>
      ) : null}
      {appError ? (
        // 应用列表读不到时，下面的空态全是假的（"该应用还没有 X"），
        // 所以这条错误必须常驻在页面上并给出重试，而不是弹一条会自己消失的 toast。
        <ErrorState
          error={appError instanceof Error ? appError : String(appError)}
          title="应用列表没有读到"
          description={
            appError instanceof Error
              ? appError.message
              : '接口没有返回应用列表，下方的空态不代表这些资源真的为空'
          }
          onRetry={onRefresh}
          retryText="重试"
          style={{ marginBottom: 12 }}
        />
      ) : null}
      <TableToolbar
        left={
          <>
            <Text type="secondary">应用</Text>
            <Select
              showSearch
              style={{ minWidth: 220 }}
              placeholder="选择应用"
              value={appCode || undefined}
              options={appOptions}
              notFoundContent={appError ? '应用列表没有读到' : undefined}
              onChange={onAppCode}
            />
          </>
        }
        right={
          <>
            {actions}
            {onRefresh ? <RefreshButton onClick={onRefresh} title="刷新" /> : null}
          </>
        }
      />
      {children}
    </div>
  );
}
