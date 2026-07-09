import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

// Ports are env-overridable so multiple plugins can run side-by-side.
// Defaults match the historical 5180/5181 pair.
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const vitePort = Number(env.VITE_PORT ?? 5180)
  const serverPort = Number(env.SERVER_PORT ?? 5181)
  const serverTarget = `http://localhost:${serverPort}`
  return {
    plugins: [react()],
    server: {
      port: vitePort,
      strictPort: true,
      // Cloudflare quick tunnels expose a single port. Vite is the
      // tunnel target; these proxies forward Tolgee → Express endpoints
      // through the one public hostname.
      proxy: {
        '/manifest.json': serverTarget,
        '/webhook': serverTarget,
        '/decorators': serverTarget,
        '/translate-back': serverTarget,
      },
      // Quick tunnels rewrite the Host header to the trycloudflare.com
      // hostname; Vite's default host check would reject those requests.
      allowedHosts: true,
    },
  }
})
