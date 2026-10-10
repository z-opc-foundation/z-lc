/**
 * z-lc-component 路由清单（lead 005 §8.6 #3：./pages 命名导出 routes，非空数组）
 *
 * 占位说明：z-lc 的页面代码现仍住在 z-lc-suit/src/ 下（z-lc 域 95 个 tsx 仍在主壳），
 * 本 manifest 现阶段只列骨架路由供主壳的 domainRoutes 探测；正式消费请走 suit。
 * 下次重构把 page 文件搬入 component 后，Component 字段直接换成同模块 import 即可。
 *
 * 已纳入 component：ui store（zustand density）—— 本 manifest 的 App 路由即消费它。
 */
import HomePage from './store/index'  // 占位：仅触发 ./store 的 default 不存在，App 端实际不渲染
import { useUiStore } from './store'

/** 占位首页：仅暴露 ui store 状态指示，方便主壳探活 */
function LcHome() {
  const density = useUiStore(s => s.density)
  return { density, type: 'div' } as unknown as React.ReactElement
}

export const appMeta = { title: 'z-lc 控制台', short: 'z-lc' }

export const routes = [
  { path: '/z-lc/home', title: '首页', order: 1, Component: LcHome as unknown as React.ComponentType },
]

export { default as HomePage } from './store/index'  // 兼容旧 suit import
