/**
 * 基座 UI 桶（@yuku123/z-frontend-common/ui）的类型声明。
 *
 * 【为什么手写而不是等基座出 d.ts】
 * 基座是纯 JS 包（dist 里只有 js，没有 .d.ts），exports 的 './ui' 子路径也没配 types，
 * TS 直接 import 会报 TS7016 "Could not find a declaration file"。本仓 src/ 下是 .tsx
 * （TypeScript 源码），所以手写一份最小声明。
 *
 * 【维护约定】
 * 这里只声明本仓**真正用到**的组件；**props 以基座源码 src/components/ui/*.jsx 为准**，
 * 基座升版改了签名要回来同步（尤其是 ErrorState 的 variant / EmptyState 的 size /
 * TableToolbar 的 left-right-extra 三段式）。
 *
 * 【本仓用到清单】
 * EmptyState / ErrorState / LoadingState / RefreshButton / SearchInput / TableToolbar / ComponentDoc。
 */
declare module '@yuku123/z-frontend-common/ui' {
  import type { CSSProperties, ReactElement, ReactNode } from 'react'

  /** 统一空态：图标 + 文案 + 行动按钮 */
  export function EmptyState(props: {
    icon?: ReactNode
    title?: string
    description?: ReactNode
    actionText?: string
    onAction?: () => void
    size?: 'sm' | 'md' | 'lg'
    style?: CSSProperties
  }): ReactElement

  /** 统一错误态：原因说明 + 重试入口（页面级错误用，业务 toast 不走这里） */
  export function ErrorState(props: {
    error?: unknown
    title?: string
    description?: ReactNode
    onRetry?: () => void
    retryText?: string
    variant?: 'inline' | 'full'
    style?: CSSProperties
  }): ReactElement

  /** 统一骨架屏：cards / table / list / detail 四种预设 */
  export function LoadingState(props: {
    variant?: 'cards' | 'table' | 'list' | 'detail'
    rows?: number
    active?: boolean
    style?: CSSProperties
  }): ReactElement

  /** 刷新按钮（TableToolbar.jsx 里同文件导出） */
  export function RefreshButton(props: {
    onClick?: () => void
    loading?: boolean
    title?: string
  }): ReactElement

  /** 端侧防抖搜索框（300ms，Enter 提交） */
  export function SearchInput(props: {
    value?: string
    onChange?: (value: string) => void
    onSearch?: (value: string) => void
    placeholder?: string
    debounceMs?: number
    width?: number
    allowClear?: boolean
    [key: string]: any
  }): ReactElement

  /** 列表页工具条：左段 + 右段 + 附加段 */
  export function TableToolbar(props: {
    left?: ReactNode
    right?: ReactNode
    extra?: ReactNode
  }): ReactElement

  /**
   * 组件文档页（lead 008 §7）：左 240px TOC + 右正文两栏 + 滚动联动。
   * props 以基座源码 src/components/ui/ComponentDoc.jsx 为准。
   */
  export function ComponentDoc(props: {
    markdown?: string
    title?: string
    className?: string
    style?: CSSProperties
    [key: string]: any
  }): ReactElement
}
