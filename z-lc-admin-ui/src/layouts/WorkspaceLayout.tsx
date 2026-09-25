import { useMemo } from 'react';
import { Link, Outlet, useLocation, useNavigate, useParams } from 'react-router-dom';
import { Button, Select, Space, Tag, Tooltip, Typography } from 'antd';
import {
  AppstoreOutlined,
  ArrowLeftOutlined,
  BarChartOutlined,
  BorderInnerOutlined,
  CalendarOutlined,
  DashboardOutlined,
  FormOutlined,
  LayoutOutlined,
  ProfileOutlined,
  QuestionCircleOutlined,
  ReloadOutlined,
  TableOutlined,
} from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import { listApps } from '@/api/app';
import { unreadReason } from '@/api/meta';
import type { ViewType } from '@/api/types';
import { useWorkspace } from '@/hooks/useWorkspace';
import { TopBar } from '@/components/TopBar';
import { LoadingBlock } from '@/components/StateBlock';
import { namedViews } from '@/views/workspace/viewConfigModel';
import { APP_LEVEL_ENTITY_CODE, DASHBOARD_VIEW_TYPE } from '@/views/workspace/dashboardModel';

const { Text } = Typography;

const VIEW_TABS: { key: ViewType; label: string; icon: React.ReactNode }[] = [
  { key: 'LIST', label: '表格', icon: <TableOutlined /> },
  { key: 'FORM', label: '表单', icon: <FormOutlined /> },
  { key: 'DETAIL', label: '详情', icon: <ProfileOutlined /> },
  { key: 'KANBAN', label: '看板', icon: <AppstoreOutlined /> },
  { key: 'GALLERY', label: '画廊', icon: <LayoutOutlined /> },
  { key: 'CALENDAR', label: '日历', icon: <CalendarOutlined /> },
  { key: 'CHART', label: '图表', icon: <BarChartOutlined /> },
  { key: 'PIVOT', label: '交叉表', icon: <BorderInnerOutlined /> },
];

/**
 * App workspace shell: entity navigation on the left, view-type switcher on
 * top. Everything is URL-driven (`/:appCode/:entityCode/:viewType`) so any
 * state is copy-paste shareable.
 */
export function WorkspaceLayout() {
  const { appCode = '', entityCode, viewType } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const { meta, isLoading, isError, error, refetch } = useWorkspace(appCode);

  const appsQuery = useQuery({ queryKey: ['apps'], queryFn: listApps, staleTime: 30_000 });
  const entities = meta?.entities ?? [];
  // `fetchWorkspaceMeta` never throws, so `isError` cannot see a single resource
  // failing: an unread entity list arrives here as `entities: []`.
  const entitiesRead = Boolean(meta?.read.entities);
  const dashboardCount = useMemo(
    () => namedViews(meta?.views, APP_LEVEL_ENTITY_CODE, DASHBOARD_VIEW_TYPE).length,
    [meta?.views],
  );
  // 仪表盘链接是应用级的，路由里没有 entityCode，只能靠 pathname 判断选中态
  const onDashboard = location.pathname.startsWith(`/${appCode}/dashboard`);

  const appOptions = useMemo(
    () =>
      (appsQuery.data ?? []).map((app) => ({
        value: app.appCode,
        label: app.appName || app.appCode,
      })),
    [appsQuery.data],
  );

  const activeView: ViewType = VIEW_TABS.some((tab) => tab.key === viewType)
    ? (viewType as ViewType)
    : 'LIST';

  return (
    <div className="zlc-shell">
      <TopBar
        appSlot={
          <Space size={8} style={{ marginLeft: 4 }}>
            <Tooltip title="返回应用列表">
              <Button size="small" type="text" icon={<ArrowLeftOutlined />} onClick={() => navigate('/apps')} />
            </Tooltip>
            <Select
              size="small"
              style={{ minWidth: 180 }}
              value={appCode}
              options={appOptions}
              notFoundContent={appsQuery.isError ? '应用列表没有读到' : undefined}
              showSearch
              optionFilterProp="label"
              onChange={(next) => navigate(`/${next}`)}
              placeholder="切换应用"
            />
            {meta?.app?.status ? <Tag bordered={false}>{meta.app.status}</Tag> : null}
            {meta?.source === 'fallback' ? (
              <Tooltip
                title={
                  meta.degraded.length > 0
                    ? `/meta/bundle 不可用，已回退到单接口加载。降级项: ${meta.degraded.join(', ')}`
                    : '/meta/bundle 不可用，已回退到单接口加载'
                }
              >
                <Tag color="orange" bordered={false}>
                  回退加载 <QuestionCircleOutlined />
                </Tag>
              </Tooltip>
            ) : null}
          </Space>
        }
      />

      <div className="zlc-body">
        <aside className="zlc-nav">
          <div className="zlc-nav__section">分析</div>
          <Link
            to={`/${appCode}/dashboard`}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 8,
              padding: '6px 12px',
              color: onDashboard ? '#fff' : '#a9b6c6',
              background: onDashboard ? '#2f6feb' : 'transparent',
              fontSize: 13,
              textDecoration: 'none',
              borderLeft: onDashboard ? '2px solid #7aa7ff' : '2px solid transparent',
            }}
          >
            <DashboardOutlined style={{ fontSize: 12 }} />
            <span style={{ flex: 1, minWidth: 0 }}>仪表盘</span>
            {dashboardCount > 0 ? (
              <span style={{ fontSize: 11, opacity: 0.7 }}>{dashboardCount}</span>
            ) : null}
          </Link>
          <div className="zlc-nav__section">实体 ({entitiesRead ? entities.length : '—'})</div>
          <div className="zlc-nav__scroll">
            {isLoading ? (
              <LoadingBlock label="加载模型" />
            ) : isError ? (
              <div style={{ padding: 12 }}>
                <Text type="danger" style={{ fontSize: 12 }}>
                  模型加载失败
                </Text>
                <div style={{ fontSize: 11, color: '#7b8794', marginTop: 4 }}>
                  {error instanceof Error ? error.message : String(error)}
                </div>
              </div>
            ) : !entitiesRead ? (
              <div style={{ padding: 12 }}>
                <Text type="danger" style={{ fontSize: 12 }}>
                  实体列表没有读到
                </Text>
                <div style={{ fontSize: 11, color: '#7b8794', marginTop: 4 }}>
                  {unreadReason(meta, 'entities')}。这里的空不代表应用真的没有实体。
                </div>
                <div style={{ marginTop: 8 }}>
                  <Button size="small" icon={<ReloadOutlined />} onClick={() => void refetch()}>
                    重试
                  </Button>
                </div>
              </div>
            ) : entities.length === 0 ? (
              <div style={{ padding: '12px 12px' }}>
                <Text type="secondary" style={{ fontSize: 12 }}>
                  该应用还没有实体。
                </Text>
                <div style={{ marginTop: 8 }}>
                  <Link to={`/designer/${appCode}`}>打开 Schema 设计器</Link>
                </div>
              </div>
            ) : (
              entities.map((entity) => {
                const active = entity.entityCode === entityCode;
                return (
                  <Link
                    key={entity.entityCode}
                    to={`/${appCode}/${entity.entityCode}/LIST`}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: 8,
                      padding: '6px 12px',
                      color: active ? '#fff' : '#a9b6c6',
                      background: active ? '#2f6feb' : 'transparent',
                      fontSize: 13,
                      textDecoration: 'none',
                      borderLeft: active ? '2px solid #7aa7ff' : '2px solid transparent',
                    }}
                    title={`${entity.entityName || entity.entityCode} · ${entity.tableName ?? entity.entityCode}`}
                  >
                    <TableOutlined style={{ fontSize: 12 }} />
                    <span style={{ flex: 1, minWidth: 0, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {entity.entityName || entity.entityCode}
                    </span>
                    <span style={{ fontSize: 11, opacity: 0.7 }}>{entity.fields?.length ?? 0}</span>
                  </Link>
                );
              })
            )}
          </div>
          <div style={{ padding: 12, borderTop: '1px solid #141b23' }}>
            <Link to={`/designer/${appCode}`} style={{ color: '#a9b6c6', fontSize: 12 }}>
              编辑 Schema
            </Link>
          </div>
        </aside>

        <main className="zlc-main">
          {entityCode ? (
            <nav className="zlc-view-tabs">
              {VIEW_TABS.map((tab) => {
                const active = tab.key === activeView;
                return (
                  <Link
                    key={tab.key}
                    to={`/${appCode}/${entityCode}/${tab.key}`}
                    style={{
                      padding: '4px 10px',
                      borderRadius: 4,
                      fontSize: 12,
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: 6,
                      textDecoration: 'none',
                      color: active ? '#2f6feb' : '#5b6675',
                      background: active ? '#eaf1ff' : 'transparent',
                      fontWeight: active ? 600 : 400,
                    }}
                  >
                    {tab.icon}
                    {tab.label}
                  </Link>
                );
              })}
            </nav>
          ) : null}
          <div className="zlc-content zlc-content--flush">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
}
