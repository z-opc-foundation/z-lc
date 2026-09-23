import { Menu } from 'antd';
import {
  AppstoreOutlined,
  ApartmentOutlined,
  BranchesOutlined,
  DatabaseOutlined,
  DeploymentUnitOutlined,
  EyeOutlined,
  FunctionOutlined,
  RobotOutlined,
  SafetyCertificateOutlined,
  TableOutlined,
} from '@ant-design/icons';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { TopBar } from '@/components/TopBar';

interface NavItem {
  key: string;
  label: string;
  icon: React.ReactNode;
}

const NAV_GROUPS: { title: string; items: NavItem[] }[] = [
  {
    title: '应用',
    items: [{ key: '/apps', label: '应用列表', icon: <AppstoreOutlined /> }],
  },
  {
    title: '建模',
    items: [
      { key: '/designer', label: 'Schema 设计器', icon: <TableOutlined /> },
      { key: '/db-import', label: '从数据库导入', icon: <DatabaseOutlined /> },
      { key: '/admin/ai', label: 'AI 建模', icon: <RobotOutlined /> },
    ],
  },
  {
    title: '配置',
    items: [
      { key: '/admin/dicts', label: '数据字典', icon: <FunctionOutlined /> },
      { key: '/admin/relations', label: '实体关系', icon: <ApartmentOutlined /> },
      { key: '/admin/views', label: '视图配置', icon: <EyeOutlined /> },
      { key: '/admin/pipelines', label: 'Pipeline', icon: <BranchesOutlined /> },
      { key: '/admin/workflows', label: '流程绑定', icon: <DeploymentUnitOutlined /> },
    ],
  },
  {
    title: '运行',
    items: [
      { key: '/admin/permissions', label: '权限矩阵', icon: <SafetyCertificateOutlined /> },
      { key: '/admin/deployments', label: '部署记录', icon: <DeploymentUnitOutlined /> },
    ],
  },
];

/** Global console shell for the non-workspace pages. */
export function ConsoleLayout() {
  const navigate = useNavigate();
  const location = useLocation();

  const flatItems = NAV_GROUPS.flatMap((group) => group.items);
  const selected =
    flatItems
      .map((item) => item.key)
      .filter((key) => location.pathname === key || location.pathname.startsWith(`${key}/`))
      .sort((a, b) => b.length - a.length)[0] ?? '/apps';

  return (
    <div className="zlc-shell">
      <TopBar />
      <div className="zlc-body">
        <aside className="zlc-nav">
          <div className="zlc-nav__scroll">
            {NAV_GROUPS.map((group) => (
              <div key={group.title}>
                <div className="zlc-nav__section">{group.title}</div>
                <Menu
                  theme="dark"
                  mode="inline"
                  style={{ paddingInline: 0 }}
                  selectedKeys={[selected]}
                  onClick={({ key }) => navigate(String(key))}
                  items={group.items.map((item) => ({ key: item.key, icon: item.icon, label: item.label }))}
                />
              </div>
            ))}
          </div>
        </aside>
        <main className="zlc-main">
          <div className="zlc-content">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
}
