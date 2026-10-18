import {defineConfig} from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

/**
 * z-lc 组件层构建（lead 005 §8.2 组件层 / §8.6 #3 双入口）。
 *
 *   .      → dist/index.js     默认出口 LcRoutes（QueryClientProvider + 完整路由表）
 *   ./store → dist/store/index.js  ui store
 *   ./pages → dist/pages.js     routes + appMeta 清单
 *
 * 两处历史包袱，本次一并收口：
 *  - `src/index.jsx`（`export {LcApp} from './App'`）指向的 ./App 从不存在，删掉；
 *    真入口是 `src/index.tsx`，它的 default 就是 z-team DevWorkbench 需要的那个组件。
 *  - `src/{project,subscribe,views,layouts,...}` 里的 @/lc/* 实际指向 src 根
 *    （仓内没有 src/lc/ 目录），所以 alias 必须是 '@/lc' → src 而不是 '@' → src/lc。
 *
 * src/components/TopBar.tsx 用 `@yuku123/z-lc-component/store` 自引用本包子路径，
 * standalone 构建时包还没装进自己的 node_modules，必须显式 alias 到 src。
 */
const srcDir = path.resolve(__dirname, './src')

export default defineConfig({
    plugins: [react()],
    resolve: {
        alias: {
            '@/lc': srcDir,
            '@': srcDir,
            '@yuku123/z-lc-component/store': path.resolve(srcDir, 'store/index.ts'),
        },
    },
    build: {
        lib: {
            entry: {
                'index': path.resolve(srcDir, 'index.tsx'),
                'store/index': path.resolve(srcDir, 'store/index.ts'),
                'pages': path.resolve(srcDir, 'pages.ts'),
            },
            formats: ['es'],
        },
        outDir: 'dist',
        emptyOutDir: true,
        sourcemap: false,
        rollupOptions: {
            external: (id) =>
                id === 'react' ||
                id === 'react-dom' ||
                id === 'react-dom/client' ||
                id === 'react/jsx-runtime' ||
                id === 'react-router-dom' ||
                id === 'zustand' ||
                id === 'zustand/react' ||
                id.startsWith('@tanstack/') ||
                id.startsWith('@dnd-kit/') ||
                id.startsWith('@yuku123/') ||
                id === 'antd' ||
                id === '@ant-design/icons' ||
                id === 'axios' ||
                id === 'dayjs' ||
                id === 'papaparse',
        },
    },
})
