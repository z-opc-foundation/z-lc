import { Alert, Button, Select, Space, Typography } from 'antd';
import { ReloadOutlined } from '@ant-design/icons';
import { ErrorBlock } from '@/components/StateBlock';
import type { ListState } from './_scope';

const { Text, Title } = Typography;

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
        <Alert
          type="error"
          showIcon
          style={{ marginBottom: 12 }}
          message="应用列表没有读到"
          description={
            appError instanceof Error
              ? appError.message
              : '接口没有返回应用列表，下方的空态不代表这些资源真的为空'
          }
          action={
            onRefresh ? (
              <Button size="small" onClick={onRefresh}>
                重试
              </Button>
            ) : null
          }
        />
      ) : null}
      <Space wrap style={{ marginBottom: 12, width: '100%', justifyContent: 'space-between' }}>
        <Space wrap size={8}>
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
        </Space>
        <Space size={8}>
          {actions}
          {onRefresh ? <Button icon={<ReloadOutlined />} onClick={onRefresh} /> : null}
        </Space>
      </Space>
      {children}
    </div>
  );
}
