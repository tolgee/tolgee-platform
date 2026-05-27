import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5180,
    strictPort: true,
    // Cloudflare quick tunnels expose a single port. Vite is the
    // tunnel target; these proxies forward Tolgee → Express endpoints
    // through the one public hostname.
    proxy: {
      '/manifest.json': 'http://localhost:5181',
      '/webhook': 'http://localhost:5181',
      '/decorators': 'http://localhost:5181',
    },
    // Quick tunnels rewrite the Host header to the trycloudflare.com
    // hostname; Vite's default host check would reject those requests.
    allowedHosts: true,
  },
})
