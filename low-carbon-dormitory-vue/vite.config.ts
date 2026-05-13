import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

export default defineConfig({
  base: '/',
  plugins: [
    vue(),
    AutoImport({
      resolvers: [ElementPlusResolver({ importStyle: 'css' })],
    }),
    Components({
      resolvers: [ElementPlusResolver({ importStyle: 'css' })],
    }),
  ],
  server: {
    proxy: {
      '/api/restaurant': {
        target: 'http://39.98.69.153:8081',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/restaurant/, '/api'),
      },
      '/api/dorm': {
        target: 'http://localhost:3000',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/dorm/, ''),
      },
      '/api': {
        target: 'http://localhost:3000',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) {
            return
          }

          if (id.includes('element-plus')) {
            return 'element-plus'
          }

          if (id.includes('vue-router')) {
            return 'vue-router'
          }

          if (id.includes('pinia')) {
            return 'pinia'
          }

          if (id.includes('/vue/') || id.includes('\\vue\\')) {
            return 'vue-core'
          }
        },
      },
    },
  },
})
