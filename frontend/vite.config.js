import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      '/api': {
        target: process.env.BACKEND_URL || 'http://127.0.0.1:8081',
        changeOrigin: true,
        timeout: 240000,
        proxyTimeout: 240000,
      },
    },
  },
  preview: {
    port: 5173,
    proxy: { '/api': { target: process.env.BACKEND_URL || 'http://127.0.0.1:8081', changeOrigin: true } },
  },
});
