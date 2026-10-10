import { HomeOutlined, MonitorOutlined } from '@ant-design/icons'
import HomePage from './HomePage'
import StatusPage from './StatusPage'

/** 菜单 + 路由清单（lead 008 §10/§14/§16 批量落地，suit 内联形态）。 */
export const appMeta = { title: 'z-lc 低代码平台', short: 'z-lc' }

export const menuItems = [
    { key: '/z-lc/home', label: '首页', icon: <HomeOutlined /> },
    { key: '/z-lc/status', label: '服务状态', icon: <MonitorOutlined /> },
]

export const routeTable = [
    { path: '/z-lc/home', Component: HomePage },
    { path: '/z-lc/status', Component: StatusPage },
]

export { default as HomePage } from './HomePage'
export { default as LoginPage } from './LoginPage'
