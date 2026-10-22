/// <reference types="vite/client" />

/**
 * vite 环境类型引用（lead 008 §7）。
 *
 * 为什么需要：`import docMarkdown from '../DOC.md?raw'` 这类 `?raw` 导入，
 * 类型来自 vite 客户端类型（把 `*?raw` 声明成 string 模块）。没有这段引用时
 * TS 会报 "Cannot find module '../DOC.md?raw'"。
 *
 * 本仓还没有 tsconfig.json（构建走 vite/esbuild，不做类型检查），
 * 这份声明是为将来补 tsconfig / 开 `tsc --noEmit` 时预留的。
 */
