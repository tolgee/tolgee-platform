import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv } from 'vite'

const originOf = (url: string | undefined): string => {
  if (!url) return ''
  try {
    return new URL(url).origin
  } catch {
    return ''
  }
}

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const serverTarget = `http://localhost:${Number(env.SERVER_PORT || 5181)}`
  return {
    plugins: [react()],
    define: {
      // Baked in so the iframe can reject a `tolgee-app:init` — which carries an
      // API token — from any origin other than the Tolgee this app was built for.
      'import.meta.env.VITE_TOLGEE_ORIGIN': JSON.stringify(
        [originOf(env.TOLGEE_FRONTEND_URL), originOf(env.TOLGEE_URL)]
          .filter(Boolean)
          .join(',')
      ),
    },
    server: {
      port: Number(env.VITE_PORT || 5180),
      // Tolgee embeds this origin by URL from the manifest, so a silent
      // fallback to another port would break the iframe instead of the boot.
      strictPort: true,
      // A Cloudflare quick tunnel exposes one port — this one. Forwarding the
      // manifest path here is what makes it reachable through the tunnel.
      // Tolgee talks to the app through this one origin: the manifest it
      // fetches and the signed lifecycle deliveries both proxy through to the
      // app server.
      proxy: {
        '/manifest.json': serverTarget,
        '/tolgee/lifecycle': serverTarget,
      },
      // Quick tunnels rewrite the Host header to *.trycloudflare.com.
      allowedHosts: true,
    },
  }
})
