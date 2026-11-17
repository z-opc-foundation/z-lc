import { Navigate, Route, Routes, useParams } from 'react-router-dom';
import { ConsoleLayout } from '@/layouts/ConsoleLayout';
import { WorkspaceLayout } from '@/layouts/WorkspaceLayout';
import { AppListPage } from '@/views/apps/AppListPage';
import { AppOverviewPage } from '@/views/apps/AppOverviewPage';
import { WorkspaceViewPage } from '@/views/workspace/WorkspaceViewPage';
import { DashboardPage } from '@/views/workspace/DashboardPage';
import { DesignerIndexPage, DesignerPage } from '@/views/designer/DesignerPage';
import { DesignerPage as VisualDesignerPage } from '@/views/designer2';
import { DbImportPage } from '@/views/designer/DbImportPage';
import { DataSourcesPage } from '@/views/admin/DataSourcesPage';
import { VapisPage } from '@/views/vapis/VapisPage';
import { PagesPage } from '@/views/pages/PagesPage';
import { DictsPage } from '@/views/admin/DictsPage';
import { RelationsPage } from '@/views/admin/RelationsPage';
import { ViewConfigsPage } from '@/views/admin/ViewConfigsPage';
import { PipelinesPage } from '@/views/admin/PipelinesPage';
import { WorkflowsPage } from '@/views/admin/WorkflowsPage';
import { PermissionsPage } from '@/views/admin/PermissionsPage';
import { DeploymentsPage } from '@/views/admin/DeploymentsPage';
import { AiModelingPage } from '@/views/admin/AiModelingPage';

/**
 * Route map.
 *
 * Static segments (`/apps`, `/admin/*`, `/designer/*`) outrank the dynamic app
 * workspace paths in v6 route ranking, so `/:appCode/:entityCode/:viewType`
 * can stay at the top level and remain shareable/bookmarkable.
 */
/** 设计器路由：路径参数 → 组件 props（设计器来自共享包，不认识宿主路由） */
function VisualDesignerRoute() {
  const { appCode = 'demo' } = useParams<{ appCode: string }>();
  return <VisualDesignerPage appCode={appCode} />;
}

export function AppRoutes() {
  return (
    <Routes>
      {/* 可视化设计器是全屏工作台，不套 ConsoleLayout 的导航外壳 */}
      <Route path="/designer2/:appCode" element={<VisualDesignerRoute />} />
      <Route element={<ConsoleLayout />}>
        <Route path="/" element={<Navigate to="/apps" replace />} />
        <Route path="/apps" element={<AppListPage />} />

        <Route path="/designer" element={<DesignerIndexPage />} />
        <Route path="/designer/:appCode" element={<DesignerPage />} />
        <Route path="/db-import" element={<DbImportPage />} />
        <Route path="/pages/:appCode" element={<PagesPage />} />
        <Route path="/pages" element={<PagesPage />} />
        <Route path="/admin/datasources" element={<DataSourcesPage />} />
        <Route path="/vapis" element={<VapisPage />} />

        <Route path="/admin/dicts" element={<DictsPage />} />
        <Route path="/admin/relations" element={<RelationsPage />} />
        <Route path="/admin/views" element={<ViewConfigsPage />} />
        <Route path="/admin/pipelines" element={<PipelinesPage />} />
        <Route path="/admin/workflows" element={<WorkflowsPage />} />
        <Route path="/admin/permissions" element={<PermissionsPage />} />
        <Route path="/admin/deployments" element={<DeploymentsPage />} />
        <Route path="/admin/ai" element={<AiModelingPage />} />
      </Route>

      {/* App workspace: url carries appCode + entityCode + viewType. */}
      <Route element={<WorkspaceLayout />}>
        <Route path="/:appCode" element={<AppOverviewPage />} />
        {/* 仪表盘是应用级的（组件可以跨实体），所以不挂在 entityCode 段之下。
            静态段 dashboard 在 v6 的排序里赢过 :entityCode，不会和下面两条抢。 */}
        <Route path="/:appCode/dashboard" element={<DashboardPage />} />
        <Route path="/:appCode/dashboard/:dashboardId" element={<DashboardPage />} />
        <Route path="/:appCode/:entityCode/:viewType" element={<WorkspaceViewPage />} />
        <Route path="/:appCode/:entityCode/:viewType/:recordId" element={<WorkspaceViewPage />} />
      </Route>

      <Route path="*" element={<Navigate to="/apps" replace />} />
    </Routes>
  );
}
