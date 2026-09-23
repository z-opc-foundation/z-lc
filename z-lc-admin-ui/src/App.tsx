import { Navigate, Route, Routes } from 'react-router-dom';
import { ConsoleLayout } from '@/layouts/ConsoleLayout';
import { WorkspaceLayout } from '@/layouts/WorkspaceLayout';
import { AppListPage } from '@/views/apps/AppListPage';
import { AppOverviewPage } from '@/views/apps/AppOverviewPage';
import { WorkspaceViewPage } from '@/views/workspace/WorkspaceViewPage';
import { DashboardPage } from '@/views/workspace/DashboardPage';
import { DesignerIndexPage, DesignerPage } from '@/views/designer/DesignerPage';
import { DbImportPage } from '@/views/designer/DbImportPage';
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
export function AppRoutes() {
  return (
    <Routes>
      <Route element={<ConsoleLayout />}>
        <Route path="/" element={<Navigate to="/apps" replace />} />
        <Route path="/apps" element={<AppListPage />} />

        <Route path="/designer" element={<DesignerIndexPage />} />
        <Route path="/designer/:appCode" element={<DesignerPage />} />
        <Route path="/db-import" element={<DbImportPage />} />

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
