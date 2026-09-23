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
        target: process.env.VITE_BACKEND_URL || 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
        onError: (err, _req, res) => {
          console.warn('[Vite Proxy Warning] Local backend (localhost:8080) unreachable. Ensure Spring Boot is running in IntelliJ IDEA.');
          if (!res.headersSent) {
            res.writeHead(503, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ error: 'Local backend service unavailable on port 8080. Please start WellnessApplication in IntelliJ IDEA.' }));
          }
        },
      },
      '/ws': {
        target: process.env.VITE_BACKEND_URL || 'http://localhost:8080',
        ws: true,
        changeOrigin: true,
        secure: false,
      },
    },
  },
});
