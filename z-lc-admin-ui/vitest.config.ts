/// <reference types="vitest" />
import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import { fileURLToPath, URL } from 'node:url';

/**
 * 测试专用配置。
 *
 * 为什么单独一个文件而不是塞进 vite.config.ts：dev/build 的配置里混着 proxy 与
 * chunk 拆分策略，跑测试时不需要、也不该把那些副作用一起载进来。
 * 别名必须和 vite.config.ts 保持一致，否则 `@/...` 在测试里解析不到。
 */
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
    include: ['src/**/*.test.{ts,tsx}'],
    css: false,
    restoreMocks: true,
    // 默认 5s 在满并行时会误杀：ImportDialog / KanbanView 的 userEvent + antd 交互
    // 单跑就要 2.6–3.6s，14 个文件一起抢 CPU 时直接顶到 5s 超时（实测两次红）。
    // 这是墙钟额度，不是断言放宽。
    testTimeout: 20_000,
  },
});
