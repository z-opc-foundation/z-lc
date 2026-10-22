import { AppstoreOutlined, HomeOutlined, MonitorOutlined, ReadOutlined, RobotOutlined } from '@ant-design/icons'
import HomePage from './HomePage'
import StatusPage from './StatusPage'
import ReportHome from './report/Home'
import ReportView from './report/ViewPage'
import ReportShare from './report/SharePage'
import ReportAi from './report/AiPage'
import ComponentDocPage from './pages/ComponentDoc.jsx'

/** 菜单 + 路由清单（lead 008 §10/§14/§16 批量落地，suit 内联形态）。
 *  报表域 2026-10-11 自 z-report 收编（CEO: 「报表低代码领域一起吃下来」），
 *  后端 /api/lc/report/** 由 z-lc-report-web 提供，dev proxy 指 z-lc-admin :18090。 */
export const appMeta = { title: 'z-lc 低代码平台', short: 'z-lc' }

export const menuItems = [
    { key: '/z-lc/home', label: '首页', icon: <HomeOutlined /> },
    { key: '/z-lc/status', label: '服务状态', icon: <MonitorOutlined /> },
    { key: '/z-lc/report', label: '报表模板', icon: <AppstoreOutlined /> },
    { key: '/z-lc/report/ai', label: 'AI 报表', icon: <RobotOutlined /> },
    { key: '/z-lc/docs', label: '组件文档', icon: <ReadOutlined /> },
]

export const routes = [
    { path: '/z-lc/home', Component: HomePage },
    { path: '/z-lc/status', Component: StatusPage },
    { path: '/z-lc/report', Component: ReportHome },
    { path: '/z-lc/report/ai', Component: ReportAi },
    { path: '/z-lc/docs', Component: ComponentDocPage },
    // 子路径参数页（§13 可写页内标题；不进菜单）
    { path: '/z-lc/report/view/:viewId', Component: ReportView },
    { path: '/z-lc/report/share/:publishId', Component: ReportShare },
]

export { default as HomePage } from './HomePage'
export { default as LoginPage } from './LoginPage'
// 主壳接线用：文档页统一以 ComponentDocPage 这个名字从主入口导出。
export { default as ComponentDocPage } from './pages/ComponentDoc.jsx'
