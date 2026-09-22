import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  define: {
    global: 'window',
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'https://ai-wellness-jt1d.onrender.com',
        changeOrigin: true,
        secure: false,
      },
      '/ws': {
        target: 'https://ai-wellness-jt1d.onrender.com',
        ws: true,
        changeOrigin: true,
        secure: false,
      },
    },
  },
});
