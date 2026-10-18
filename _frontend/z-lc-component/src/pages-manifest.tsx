/**
 * z-lc-component 路由清单（lead 005 §8.6 #3：./pages 命名导出 routes，非空数组）
 *
 * 占位说明：z-lc 的页面代码现仍住在 z-lc-suit/src/ 下（z-lc 域 95 个 tsx 仍在主壳），
 * 本 manifest 现阶段只列骨架路由供主壳的 domainRoutes 探测；正式消费请走 suit。
 * 下次重构把 page 文件搬入 component 后，Component 字段直接换成同模块 import 即可。
 *
 * 已纳入 component：ui store（zustand density）—— 本 manifest 的 App 路由即消费它。
 */
import {useUiStore} from './store'

/**
 * 探活首页：真的把 density 渲染出来。
 *
 * 原实现 `return {density, type: 'div'} as unknown as React.ReactElement`
 * 是把一个普通对象强转成 ReactElement —— 编译能过（断言绕过了检查），一旦主壳真去
 * 挂这条路由就会在渲染期炸掉，而错误信息指向 React 内部、跟真正的病灶隔了三层。
 */
export function LcHome() {
  const density = useUiStore(s => s.density)
  return <div data-testid="z-lc-home" data-density={density}>z-lc · {density}</div>
}

export const appMeta = { title: 'z-lc 控制台', short: 'z-lc' }

export const routes = [
  { path: '/z-lc/home', title: '首页', order: 1, Component: LcHome },
]

export {LcHome as HomePage}  // 兼容旧 suit import
