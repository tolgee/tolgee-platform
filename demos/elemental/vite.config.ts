import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  return {
    define: {
      'process.env.VITE_APP_TOLGEE_API_URL': JSON.stringify(env.VITE_APP_TOLGEE_API_URL),
      'process.env.VITE_APP_TOLGEE_API_KEY': JSON.stringify(env.VITE_APP_TOLGEE_API_KEY),
      'process.env.WEATHER_API_KEY': JSON.stringify(env.WEATHER_API_KEY),
    },
    plugins: [react()],
  }
})