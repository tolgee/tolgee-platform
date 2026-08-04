import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv } from 'vite'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const serverTarget = `http://localhost:${Number(env.SERVER_PORT || {{serverPort}})}`
  return {
    plugins: [react()],
    server: {
      port: Number(env.VITE_PORT || {{vitePort}}),
      // Tolgee embeds this origin by URL from the manifest, so a silent
      // fallback to another port would break the iframe instead of the boot.
      strictPort: true,
      // A Cloudflare quick tunnel exposes one port — this one. Forwarding the
      // manifest path here is what makes it reachable through the tunnel.
      proxy: {
        '/manifest.json': serverTarget,
      },
      // Quick tunnels rewrite the Host header to *.trycloudflare.com.
      allowedHosts: true,
    },
  }
})
