import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import path from 'node:path'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    // '@/features/auth/api' 처럼 절대경로로 import 하기 위한 설정.
    // tsconfig.app.json 의 paths 와 항상 같이 맞춰야 한다.
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    // 백엔드 SecurityConfig 의 CORS 허용 목록이 localhost:3000, 3001 뿐이다.
    // 다른 포트로 뜨면 로그인 요청이 CORS 에서 막힌다.
    port: 3000,
    strictPort: true,
  },
})
