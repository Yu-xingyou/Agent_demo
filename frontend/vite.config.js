import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    allowedHosts: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        configure: (proxy) => {
          // 关闭 SSE（text/event-stream）响应的缓冲：http-proxy 默认会整体缓冲分块响应，
          // 直到连接关闭才一次性转发，导致前端流式输出表现为一次性出现。
          // 仅对 event-stream 生效，普通 JSON 接口不受影响。
          proxy.on('proxyRes', (proxyRes) => {
            const headers = proxyRes.headers;
            const contentType = headers['content-type'] || '';
            if (contentType.includes('text/event-stream')) {
              headers['cache-control'] = 'no-cache, no-transform';
              delete headers['content-length'];
            }
          });
        },
      },
    },
  },
})
