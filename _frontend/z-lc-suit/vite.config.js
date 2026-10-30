import {defineConfig} from 'vite'
import react from '@vitejs/plugin-react'
import {safeEmptyOutDir} from '../../build-tools/safe-vite-plugin.mjs'

export default defineConfig({
  plugins: [safeEmptyOutDir(), react()],
  resolve: { dedupe: ['react', 'react-dom', 'react-router-dom', 'antd', '@ant-design/icons', 'axios'] ,
        alias: process.env.LOCAL_SIBLINGS === '1' ? { '@yuku123/z-lc-component': '../z-lc-component/src' } : {}},
  server: {
    port: 3010,
    fs: {allow: ['..']},
    proxy: {
      // 报表/低代码 API 归 z-lc-admin :18090（/api/lc/**），须排在通配 /api 之前
      '/api/lc': {target: 'http://localhost:18090', changeOrigin: true},
      '/api': {target: 'http://localhost:8888', changeOrigin: true},
    },
  },
})
