import { useCallback, useMemo, useState } from 'react';
import { Alert, Button, Input, Select, Space, Table, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { CheckCircleOutlined, PlusOutlined } from '@ant-design/icons';
import type { PermissionEntity } from '@/lc/api/types';
import {
  PERMISSION_KEYS,
  type PermissionKey,
  checkPermission,
  grantPermission,
  listPermissions,
  revokePermission,
} from '@/lc/api/permission';
import { DEFAULT_TENANT_CODE } from '@/lc/api/client';
import { AdminScaffold, ListBanner } from './_shared';
import { entityNotFoundContent, listEmptyText, useAppSelection, useEntityOptions, useResourceList } from './_scope';

const { Text } = Typography;

/**
 * 某一行对「实体 scope」是否成立：应用级那一行（`entityCode` 为空）覆盖所有实体。
 * 与后端 `PermissionService.hasPermission` 的 `entity_code = ? OR entity_code IS NULL` 同形。
 * scope 必须是个真实体 —— 问"整个应用"能不能干时不能拿某个实体的授权算数，那一档走
 * `isAppWide`（后端同一处走的是只比 `IS NULL` 的另一支）。
 */
const coversScope = (row: PermissionEntity, scope: string) =>
  !row.entityCode || row.entityCode === scope;

const isAppWide = (row: PermissionEntity) => !row.entityCode;

/**
 * Role × permission grants for one app.
 *
 * 矩阵格子问的是「`/permission/check` 会不会答允许」，所以它和那张列表必须用同一个
 * 作用范围口径（`coversScope` / `isAppWide`）：应用级那一行覆盖任何一个实体，某个实体的
 * 单独授权既不覆盖别的实体、也不覆盖「整个应用」那一档。以前两者各说一套 —— 一个实体的
 * 授权会把「整个应用」那格点亮，而 `/check` 对着应用级的行却回"拒绝"。
 *
 * 因此这里只按 `appCode` 拉一次全量，筛选放在客户端：`/list` 的 roleCode 优先级高于
 * entityCode，两个筛选一起给时实体那个会被后端静默丢掉（矩阵需要看到应用级那些行，
 * 本来也不能让后端替它筛）。
 */
export function PermissionsPage() {
  const { appCode, setAppCode, options, error: appError, reload: reloadApps } = useAppSelection();
  const entitiesSource = useEntityOptions(appCode);
  const entityOptions = entitiesSource.options;
  const [roleCode, setRoleCode] = useState('');
  const [entityCode, setEntityCode] = useState('');
  const [newRole, setNewRole] = useState('');
  const [addedRoles, setAddedRoles] = useState<string[]>([]);
  const [probeRole, setProbeRole] = useState('');
  const [probeEntity, setProbeEntity] = useState('');
  const [probePermission, setProbePermission] = useState<PermissionKey>('VIEW');
  const [probeResult, setProbeResult] = useState<string | null>(null);

  const { rows: allRows, state, error, loading, reload } = useResourceList<PermissionEntity>(
    () => listPermissions({ appCode }),
    appCode || null,
  );

  /** 列表：未选实体就是"不按实体筛"，选了实体则连带显示覆盖它的应用级授权。 */
  const visible = useMemo(() => {
    const scope = entityCode.trim();
    const role = roleCode.trim();
    return allRows.filter(
      (row) => (!scope || coversScope(row, scope)) && (!role || row.roleCode.includes(role)),
    );
  }, [allRows, entityCode, roleCode]);

  const grant = useCallback(
    async (targetRole: string, permission: string) => {
      const scope = entityCode.trim();
      try {
        await grantPermission({
          appCode,
          entityCode: scope || undefined,
          roleCode: targetRole,
          permission,
          tenantCode: DEFAULT_TENANT_CODE,
        } as PermissionEntity);
        message.success(`已授予 ${targetRole} · ${permission}（${scope || '整个应用'}）`);
        reload();
      } catch (err) {
        message.error(err instanceof Error ? err.message : '授权失败');
      }
    },
    [appCode, entityCode, reload],
  );

  /**
   * 一格的真值必须对整张表算（`allRows`），不能只对筛过的行算 —— 否则选了实体就再也看不见
   * 那条让它成立的应用级授权。未选实体时问的是「整个应用」那一档，口径同 `/check`。
   */
  const cellState = (role: string, key: string) => {
    const mine = allRows.filter((row) => row.roleCode === role && row.permission === key);
    const scope = entityCode.trim();
    return {
      granted: mine.some((row) => (scope ? coversScope(row, scope) : isAppWide(row))),
      fromAppWide: mine.some(isAppWide),
      otherEntities: new Set(
        mine
          .map((row) => row.entityCode)
          .filter((value): value is string => Boolean(value) && value !== scope),
      ).size,
    };
  };

  const columns: ColumnsType<PermissionEntity> = [
    { title: '角色', dataIndex: 'roleCode', width: 160, render: (value: string) => <Text code>{value}</Text> },
    { title: '权限', dataIndex: 'permission', width: 120, render: (value: string) => <Tag color="blue">{value}</Tag> },
    {
      title: '作用范围',
      dataIndex: 'entityCode',
      // `data-scope` 给浏览器层：整个应用 = 库里 `entity_code IS NULL`，这一格是那句
      // SQL 三值逻辑唯一在界面上的出口，光看中文说不清它到底钉没钉住 NULL 那一档。
      render: (value?: string | null) => (
        <span data-testid="perm-scope" data-scope={value || 'APP_WIDE'}>
          {value ? <Tag>{value}</Tag> : <Text type="secondary">整个应用</Text>}
        </span>
      ),
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
          data-testid={`perm-revoke-${row.id ?? 'none'}`}
          onClick={async () => {
            if (!row.id) return;
            try {
              await revokePermission(row.id);
              message.success('已回收');
            } catch (err) {
              // 回收回 400 有两种：那一行不是本租户的，或者已经不在。两种都不能说"已回收"，
              // 也不能让它变成一个未捕获的 rejection。
              message.error(err instanceof Error ? err.message : '回收失败');
            }
            reload();
          }}
        >
          回收
        </Button>
      ),
    },
  ];

  /**
   * 矩阵的行**不能**只从"已经有授权"的角色里来（那是 #50）：那样一个角色的第一条权限
   * 永远没有入口可授，回收掉它最后一条还会把整行连同再授的入口一起抹掉。
   * 行集 = 授权里出现过的角色 ∪ 手动加进来的角色，且按 `allRows` 而不是按筛过的行算 ——
   * 与 `cellState` 同一个口径；否则选了实体会把"只有别的实体有授权"的角色整行藏掉，
   * 而那正是「另有 N 个实体单独授予」这句话要说的角色。
   */
  const matrixRoles = useMemo(() => {
    const seen = Array.from(
      new Set(allRows.map((row) => row.roleCode).filter(Boolean) as string[]),
    );
    const pending = addedRoles.filter((role) => !seen.includes(role));
    const filter = roleCode.trim();
    return [...seen, ...pending].filter((role) => !filter || role.includes(filter));
  }, [allRows, addedRoles, roleCode]);

  const addMatrixRole = useCallback(() => {
    const role = newRole.trim();
    if (!role) return;
    setAddedRoles((prev) => (prev.includes(role) ? prev : [...prev, role]));
    setNewRole('');
  }, [newRole]);

  const probe = useCallback(async () => {
    const role = probeRole.trim();
    if (!role) {
      setProbeResult('先填角色');
      return;
    }
    const scope = probeEntity.trim();
    try {
      const allowed = await checkPermission({
        appCode,
        entityCode: scope || undefined,
        roleCode: role,
        permission: probePermission,
      });
      setProbeResult(`${role} 对 ${scope || '整个应用'} 的 ${probePermission}：${allowed ? '允许' : '拒绝'}`);
    } catch (err) {
      setProbeResult(err instanceof Error ? err.message : '校验失败');
    }
  }, [appCode, probeRole, probeEntity, probePermission]);

  return (
    <AdminScaffold
      title="权限"
      description="按「应用 / 实体 / 角色」授予操作权限。z-lc 自身不做拦截，鉴权由上游网关 z-ctc 统一负责，这里维护的是策略数据。"
      appCode={appCode}
      onAppCode={(value) => {
        // 手动加进矩阵的角色只属于当前这个应用：换应用还留着，上一轮填的名字会变成
        // 这一轮的一个"看起来能授"的角色行。
        setAddedRoles([]);
        setNewRole('');
        setAppCode(value);
      }}
      appOptions={options}
      appError={appError}
      onRefresh={() => {
        reloadApps();
        reload();
      }}
    >
      <Space wrap style={{ marginBottom: 12 }} size={10}>
        {/* data-entity 是「换档生效」的读数面：过滤框自己印的那行字是 rc-select 内部状态，
            value 传 undefined 时它压根不受控 —— 只证明"点中了某项"，证明不了"组件状态换了"。 */}
        <span data-testid="perm-filter-scope" data-entity={entityCode}>
          <Select
            id="permission-scope-filter"
            allowClear
            style={{ minWidth: 210 }}
            placeholder="按实体过滤（含应用级）"
            value={entityCode || undefined}
            options={entityOptions}
            notFoundContent={entityNotFoundContent(entitiesSource)}
            onChange={(value) => setEntityCode(value ?? '')}
          />
        </span>
        <Input
          style={{ width: 190 }}
          placeholder="按角色过滤"
          allowClear
          value={roleCode}
          onChange={(event) => setRoleCode(event.target.value)}
        />
        <Input
          id="permission-new-role"
          style={{ width: 210 }}
          placeholder="新角色：加进矩阵再授第一条"
          allowClear
          value={newRole}
          onChange={(event) => setNewRole(event.target.value)}
          onPressEnter={addMatrixRole}
        />
        <Button
          id="permission-add-role"
          icon={<PlusOutlined />}
          disabled={!newRole.trim()}
          onClick={addMatrixRole}
        >
          加入矩阵
        </Button>
      </Space>

      {matrixRoles.length > 0 ? (
        <>
          <Table
            size="small"
            pagination={false}
            style={{ marginBottom: 6 }}
            rowKey="role"
            dataSource={matrixRoles.map((role) => ({ role }))}
            columns={[
              { title: '角色 \\ 权限', dataIndex: 'role', width: 170 },
              ...PERMISSION_KEYS.map((key) => ({
                title: key,
                key,
                width: 128,
                render: (_v: unknown, record: { role: string }) => {
                  const { granted, fromAppWide, otherEntities } = cellState(record.role, key);
                  const hint = granted
                    ? fromAppWide
                      ? '来自整个应用'
                      : '该实体单独授予'
                    : otherEntities > 0
                      ? `另有 ${otherEntities} 个实体单独授予`
                      : null;
                  return (
                    <Space
                      size={2}
                      direction="vertical"
                      // 一格一个钩子：浏览器层要拿这一格的"授予/已授予"去对 `/permission/check`
                      // 的答案，靠按钮文案在整页里搜是搜不出"哪一格"的。
                      data-testid={`perm-cell-${record.role}-${key}`}
                      data-granted={granted ? '1' : '0'}
                    >
                      <Button
                        size="small"
                        type={granted ? 'primary' : 'default'}
                        icon={granted ? <CheckCircleOutlined /> : <PlusOutlined />}
                        disabled={granted}
                        onClick={() => void grant(record.role, key)}
                      >
                        {granted ? '已授予' : '授予'}
                      </Button>
                      {hint ? (
                        <Text type="secondary" style={{ fontSize: 11 }}>
                          {hint}
                        </Text>
                      ) : null}
                    </Space>
                  );
                },
              })),
            ]}
          />
          <Text type="secondary" style={{ fontSize: 12 }}>
            格子按当前「按实体过滤」的档位授予：未选实体时授的是整个应用。已授予的格子不再重复授予，回收在下面那张表里点。角色一条授权都没有时先「加入矩阵」，否则这一行根本不存在、第一条权限也就没有可点的格子。
          </Text>
        </>
      ) : null}

      <ListBanner state={state} error={error} onRetry={reload} label="权限" />
      <Table<PermissionEntity>
        size="small"
        rowKey={(row) => String(row.id ?? `${row.roleCode}-${row.permission}`)}
        loading={loading}
        columns={columns}
        dataSource={visible}
        pagination={{ pageSize: 20 }}
        locale={{
          emptyText:
            // 'ready' 只在「读成功且库里非空」时成立（读成功但零行是 'empty'，见 _scope.ts 的 ListState），
            // 所以这一支说的是「有授权，只是被筛掉了」—— 与「这个应用压根没配过权限」是两句话。
            state === 'ready' && allRows.length > 0
              ? '当前筛选下没有匹配的授权（应用级授权覆盖所有实体，所以按实体筛时不会被排除）'
              : listEmptyText(state, '权限配置'),
        }}
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
              id="probe-entity"
              allowClear
              style={{ minWidth: 190 }}
              placeholder="实体（留空问整个应用）"
              value={probeEntity || undefined}
              options={entityOptions}
              notFoundContent={entityNotFoundContent(entitiesSource)}
              onChange={(value) => setProbeEntity(value ?? '')}
            />
            <Select
              id="probe-permission"
              style={{ minWidth: 120 }}
              value={probePermission}
              options={PERMISSION_KEYS.map((key) => ({ value: key, label: key }))}
              onChange={(value) => setProbePermission(value as PermissionKey)}
            />
            <Button id="probe-run" onClick={() => void probe()}>
              校验
            </Button>
            {probeResult ? <Text code>{probeResult}</Text> : null}
          </Space>
        }
      />
    </AdminScaffold>
  );
}
