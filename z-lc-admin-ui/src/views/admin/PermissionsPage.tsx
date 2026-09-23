import { useCallback, useState } from 'react';
import { Alert, Button, Input, Select, Space, Table, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { CheckCircleOutlined, PlusOutlined } from '@ant-design/icons';
import type { PermissionEntity } from '@/api/types';
import {
  PERMISSION_KEYS,
  checkPermission,
  grantPermission,
  listPermissions,
  revokePermission,
} from '@/api/permission';
import { DEFAULT_TENANT_CODE } from '@/api/client';
import { AdminScaffold, ListBanner } from './_shared';
import { entityNotFoundContent, listEmptyText, useAppSelection, useEntityOptions, useResourceList } from './_scope';

const { Text } = Typography;

/**
 * Role × permission grants for one app. Granting is idempotent server-side, so
 * the matrix flips straight to the backend and reflects the real row.
 */
export function PermissionsPage() {
  const { appCode, setAppCode, options, error: appError, reload: reloadApps } = useAppSelection();
  const entitiesSource = useEntityOptions(appCode);
  const entityOptions = entitiesSource.options;
  const [roleCode, setRoleCode] = useState('');
  const [entityCode, setEntityCode] = useState('');
  const [probeRole, setProbeRole] = useState('');
  const [probeResult, setProbeResult] = useState<string | null>(null);

  const { rows, state, error, loading, reload } = useResourceList<PermissionEntity>(
    () => listPermissions({ appCode, entityCode: entityCode || undefined, roleCode: roleCode || undefined }),
    appCode ? `${appCode}|${entityCode}|${roleCode}` : null,
  );

  const grant = useCallback(
    async (targetRole: string, permission: string) => {
      try {
        await grantPermission({
          appCode,
          entityCode: entityCode || undefined,
          roleCode: targetRole,
          permission,
          tenantCode: DEFAULT_TENANT_CODE,
        } as PermissionEntity);
        message.success(`已授予 ${targetRole} · ${permission}`);
        reload();
      } catch (err) {
        message.error(err instanceof Error ? err.message : '授权失败');
      }
    },
    [appCode, entityCode, reload],
  );

  const columns: ColumnsType<PermissionEntity> = [
    { title: '角色', dataIndex: 'roleCode', width: 160, render: (value: string) => <Text code>{value}</Text> },
    { title: '权限', dataIndex: 'permission', width: 120, render: (value: string) => <Tag color="blue">{value}</Tag> },
    {
      title: '作用范围',
      dataIndex: 'entityCode',
      render: (value?: string | null) => (value ? <Tag>{value}</Tag> : <Text type="secondary">整个应用</Text>),
    },
    {
      title: '',
      key: 'revoke',
      width: 80,
      render: (_v, row) => (
        <Button
          size="small"
          type="link"
          danger
          disabled={!row.id}
          onClick={async () => {
            if (!row.id) return;
            await revokePermission(row.id);
            message.success('已回收');
            reload();
          }}
        >
          回收
        </Button>
      ),
    },
  ];

  const roles = Array.from(new Set(rows.map((row) => row.roleCode).filter(Boolean))) as string[];

  return (
    <AdminScaffold
      title="权限"
      description="按「应用 / 实体 / 角色」授予操作权限。z-lc 自身不做拦截，鉴权由上游网关 z-ctc 统一负责，这里维护的是策略数据。"
      appCode={appCode}
      onAppCode={setAppCode}
      appOptions={options}
      appError={appError}
      onRefresh={() => {
        reloadApps();
        reload();
      }}
    >
      <Space wrap style={{ marginBottom: 12 }} size={10}>
        <Select
          allowClear
          style={{ minWidth: 190 }}
          placeholder="按实体过滤"
          value={entityCode || undefined}
          options={entityOptions}
          notFoundContent={entityNotFoundContent(entitiesSource)}
          onChange={(value) => setEntityCode(value ?? '')}
        />
        <Input
          style={{ width: 190 }}
          placeholder="按角色过滤"
          allowClear
          value={roleCode}
          onChange={(event) => setRoleCode(event.target.value)}
          onPressEnter={() => reload()}
        />
        <Button onClick={() => reload()}>查询</Button>
      </Space>

      {entityOptions.length > 0 && roles.length > 0 ? (
        <Table
          size="small"
          pagination={false}
          style={{ marginBottom: 12 }}
          rowKey="role"
          dataSource={roles.map((role) => ({ role }))}
          columns={[
            { title: '角色 \\ 权限', dataIndex: 'role', width: 170 },
            ...PERMISSION_KEYS.map((key) => ({
              title: key,
              key,
              width: 110,
              render: (_v: unknown, record: { role: string }) => {
                const granted = rows.some(
                  (row) =>
                    row.roleCode === record.role &&
                    row.permission === key &&
                    (entityCode ? row.entityCode === entityCode : true),
                );
                return (
                  <Button
                    size="small"
                    type={granted ? 'primary' : 'default'}
                    icon={granted ? <CheckCircleOutlined /> : <PlusOutlined />}
                    onClick={() => void grant(record.role, key)}
                  >
                    {granted ? '已授予' : '授予'}
                  </Button>
                );
              },
            })),
          ]}
        />
      ) : null}

      <ListBanner state={state} error={error} onRetry={reload} label="权限" />
      <Table<PermissionEntity>
        size="small"
        rowKey={(row) => String(row.id ?? `${row.roleCode}-${row.permission}`)}
        loading={loading}
        columns={columns}
        dataSource={rows}
        pagination={{ pageSize: 20 }}
        locale={{ emptyText: listEmptyText(state, '权限配置') }}
      />

      <Alert
        style={{ marginTop: 12 }}
        type="info"
        showIcon
        message="校验某条权限是否成立"
        description={
          <Space wrap size={8}>
            <Input
              style={{ width: 170 }}
              placeholder="角色"
              value={probeRole}
              onChange={(event) => setProbeRole(event.target.value)}
            />
            <Select
              style={{ minWidth: 170 }}
              placeholder="实体"
              value={entityCode || undefined}
              options={entityOptions}
              notFoundContent={entityNotFoundContent(entitiesSource)}
              onChange={(value) => setEntityCode(value ?? '')}
            />
            <Select
              id="probe-permission"
              style={{ minWidth: 120 }}
              defaultValue="VIEW"
              options={PERMISSION_KEYS.map((key) => ({ value: key, label: key }))}
              onChange={(value) => {
                if (!probeRole || !value || !entityCode) {
                  setProbeResult('请先填齐角色与实体');
                  return;
                }
                void (async () => {
                  try {
                    const allowed = await checkPermission({ appCode, entityCode, roleCode: probeRole, permission: String(value) });
                    setProbeResult(`${probeRole} 对 ${entityCode} 的 ${value}：${allowed ? '允许' : '拒绝'}`);
                  } catch (err) {
                    setProbeResult(err instanceof Error ? err.message : '校验失败');
                  }
                })();
              }}
            />
            {probeResult ? <Text code>{probeResult}</Text> : null}
          </Space>
        }
      />
    </AdminScaffold>
  );
}
