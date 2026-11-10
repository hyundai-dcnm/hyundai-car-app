import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// 개발 중에는 /api, /ws 를 로컬 서버(server/, 8080)로 넘긴다.
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
      '/ws': { target: 'ws://localhost:8080', ws: true },
    },
  },
})
