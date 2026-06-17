import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// Vite 配置：开发时把 /api 代理到后端 8080，避免跨域；@ 指向 src
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  // 单元测试（Vitest）：jsdom 环境模拟浏览器（localStorage 等），@ 别名沿用
  test: {
    environment: 'jsdom',
    globals: true,
    include: ['src/**/*.spec.js']
  }
})
