import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

const originOf = (url: string | undefined): string => {
  if (!url) return ''
  try {
    return new URL(url).origin
  } catch {
    return ''
  }
}

// Ports are env-overridable so several example apps can run side by side.
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const vitePort = Number(env.VITE_PORT ?? 5180)
  const serverPort = Number(env.SERVER_PORT ?? 5181)
  return {
    plugins: [react()],
    define: {
      // Baked in so the iframe can reject a `tolgee-app:init` — which carries an
      // API token — from any origin other than the Tolgee this app was built for.
      'import.meta.env.VITE_TOLGEE_ORIGIN': JSON.stringify(
        originOf(env.TOLGEE_URL)
      ),
    },
    server: {
      port: vitePort,
      strictPort: true,
      // Convenience only: the manifest is served by Express on `serverPort`.
      // Proxying it here means either origin works as the manifest URL.
      proxy: {
        '/manifest.json': `http://localhost:${serverPort}`,
      },
      // A Cloudflare quick tunnel rewrites the Host header to *.trycloudflare.com.
      allowedHosts: true,
    },
  }
})
