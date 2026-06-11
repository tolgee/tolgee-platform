import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

// Ports are env-overridable so multiple example plugins can run side-by-side.
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const vitePort = Number(env.VITE_PORT ?? 5182)
  const serverPort = Number(env.SERVER_PORT ?? 5183)
  const serverTarget = `http://localhost:${serverPort}`
  return {
    plugins: [react()],
    server: {
      port: vitePort,
      strictPort: true,
      // Cloudflare quick tunnels expose a single port. Vite is the tunnel target;
      // these proxies forward Tolgee → Express endpoints through the one hostname.
      proxy: {
        '/manifest.json': serverTarget,
        '/webhook': serverTarget,
        '/suggestions': serverTarget,
        '/accept': serverTarget,
        '/accept-all': serverTarget,
      },
      // Quick tunnels rewrite the Host header to trycloudflare.com.
      allowedHosts: true,
    },
  }
})
