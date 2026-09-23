import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { fileURLToPath, URL } from 'node:url';

/**
 * Backend target for the z-lc admin API.
 * z-lc-web is started standalone with:
 *   --server.port=18090
 * No auth layer sits in front of it in dev, so the X-Tenant-Code header is
 * injected by the request client instead of a proxy header rewrite.
 */
const LC_API_TARGET = process.env.VITE_LC_API_TARGET ?? 'http://localhost:18090';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  // 真浏览器 E2E 跑的是**生产构建 + preview**，不是 dev server：
  // dev + HMR 在我反复改代码后会残留多个已卸载的 React 根（实测同一页里数出 5 份表格 DOM、48 个
  // 本应只有 5 个的可编辑单元格），点上去打到的就是死掉的旧实例 —— 既会假绿也会假红。
  preview: {
    port: 5274,
    proxy: {
      '/api/lc': { target: process.env.VITE_LC_API_TARGET ?? 'http://localhost:18090', changeOrigin: true },
    },
  },
  server: {
    port: 5273,
    strictPort: false,
    proxy: {
      '/api/lc': {
        target: LC_API_TARGET,
        changeOrigin: true,
        // The backend is frequently not running during frontend-only work; keep
        // the dev server alive with a 502 instead of crashing the proxy.
        configure: (proxy) => {
          proxy.on('error', (err, _req, res) => {
            if ('writeHead' in res && !res.headersSent) {
              res.writeHead(502, { 'Content-Type': 'application/json' });
            }
            const body = JSON.stringify({
              success: false,
              code: 502,
              message: `z-lc backend unreachable at ${LC_API_TARGET}: ${err.message}`,
              data: null,
            });
            if ('end' in res) res.end(body);
          });
        },
      },
    },
  },
  build: {
      rollupOptions: {
        output: {
          // antd + pro-components dominate the bundle; splitting them keeps the
          // app chunk small and lets a code change reuse the vendor cache.
          manualChunks: {
            react: ['react', 'react-dom', 'react-router-dom'],
            antd: ['antd', '@ant-design/icons'],
            pro: ['@ant-design/pro-components'],
            query: ['@tanstack/react-query', 'zustand'],
            grid: ['@dnd-kit/core', '@dnd-kit/sortable', '@dnd-kit/utilities', 'papaparse', 'dayjs'],
          },
        },
      },
    outDir: 'dist',
    sourcemap: true,
    chunkSizeWarningLimit: 1400,
  },
});
