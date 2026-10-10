import { QueryClientProvider } from '@tanstack/react-query';
import { Navigate, Outlet, Route, Routes } from 'react-router-dom';
import { queryClient } from '@/lc/queryClient';
import { ErrorBridge } from '@/lc/components/ErrorBridge';
import { WorkspaceLayout } from '@/lc/layouts/WorkspaceLayout';
import { AppListPage } from '@/lc/views/apps/AppListPage';
import { AppOverviewPage } from '@/lc/views/apps/AppOverviewPage';
import { WorkspaceViewPage } from '@/lc/views/workspace/WorkspaceViewPage';
import { DashboardPage } from '@/lc/views/workspace/DashboardPage';
import { DesignerIndexPage, DesignerPage } from '@/lc/views/designer/DesignerPage';
import { DbImportPage } from '@/lc/views/designer/DbImportPage';
import { DictsPage } from '@/lc/views/admin/DictsPage';
import { RelationsPage } from '@/lc/views/admin/RelationsPage';
import { ViewConfigsPage } from '@/lc/views/admin/ViewConfigsPage';
import { PipelinesPage } from '@/lc/views/admin/PipelinesPage';
import { WorkflowsPage } from '@/lc/views/admin/WorkflowsPage';
import { PermissionsPage } from '@/lc/views/admin/PermissionsPage';
import { DeploymentsPage } from '@/lc/views/admin/DeploymentsPage';
import { AiModelingPage } from '@/lc/views/admin/AiModelingPage';
import '@/lc/styles/global.css';

/**
 * 控制台页外壳. 独立站里这层是 ConsoleLayout (自带深色左侧菜单 + 顶栏);
 * 进了 z-team 研发 tab 之后左侧菜单由 DevWorkbench 提供, 再套一层就是双侧栏,
 * 所以这里只保留内容区的滚动与留白。
 */
function LcConsole() {
  return (
    <div className="zlc-content" style={{ display: 'flex', flexDirection: 'column' }}>
      <Outlet />
    </div>
  );
}

/**
 * z-lc 低代码平台, 挂载在 z-team 工作台「研发」tab 下.
 *
 * 与 z-lc 独立站 (z-lc-admin-ui) 的差别只有一处: 路由从绝对路径改成相对路径,
 * 因为宿主 main.jsx 用 `<Route path="/lc/*">` 接住剩余段。
 *
 * BrowserRouter / ConfigProvider / AntdApp 都由主前端提供, 这里只补 z-lc 自己的
 * QueryClientProvider 与错误桥; 独立站的 main.tsx 没有一起搬过来, 因为它会
 * createRoot 第二个 React 根 (z-lc 自己的 vite.config 注释记过一次「同一页数出
 * 5 份表格 DOM、48 个本应只有 5 个的可编辑单元格」的事故)。
 *
 * 静态段 (apps / designer / admin/*) 在 v6 路由排序里压过下面的 :appCode 动态段,
 * 所以工作区路由可以留在同一张表里继续可分享 / 可收藏。
 */
function LcRoutes() {
  return (
    <QueryClientProvider client={queryClient}>
      <ErrorBridge />
      <Routes>
        <Route element={<LcConsole />}>
          <Route path="apps" element={<AppListPage />} />
          <Route path="designer" element={<DesignerIndexPage />} />
          <Route path="designer/:appCode" element={<DesignerPage />} />
          <Route path="db-import" element={<DbImportPage />} />
          <Route path="admin/dicts" element={<DictsPage />} />
          <Route path="admin/relations" element={<RelationsPage />} />
          <Route path="admin/views" element={<ViewConfigsPage />} />
          <Route path="admin/pipelines" element={<PipelinesPage />} />
          <Route path="admin/workflows" element={<WorkflowsPage />} />
          <Route path="admin/permissions" element={<PermissionsPage />} />
          <Route path="admin/deployments" element={<DeploymentsPage />} />
          <Route path="admin/ai" element={<AiModelingPage />} />
        </Route>

        {/* 应用工作区: 自带实体导航, 但不要 z-lc 顶栏 (宿主已有一层) */}
        <Route element={<WorkspaceLayout showTopBar={false} />}>
          <Route path=":appCode" element={<AppOverviewPage />} />
          {/* 仪表盘是应用级的 (组件跨实体), 路由里没有 entityCode 段 */}
          <Route path=":appCode/dashboard" element={<DashboardPage />} />
          <Route path=":appCode/dashboard/:dashboardId" element={<DashboardPage />} />
          <Route path=":appCode/:entityCode/:viewType" element={<WorkspaceViewPage />} />
          <Route path=":appCode/:entityCode/:viewType/:recordId" element={<WorkspaceViewPage />} />
        </Route>

        <Route path="*" element={<Navigate to="/lc/apps" replace />} />
      </Routes>
    </QueryClientProvider>
  );
}

export default LcRoutes;
