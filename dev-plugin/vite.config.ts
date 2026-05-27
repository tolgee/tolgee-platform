import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5180,
    strictPort: true,
    // Cloudflare quick tunnels expose a single port, so the public
    // tunnel URL has to reach every backend endpoint too. Vite proxies
    // these paths through to the Express server on 5181 so one tunnel
    // URL serves both module iframes and webhook/decorator/manifest
    // traffic.
    proxy: {
      '/api': 'http://localhost:5181',
      '/manifest.json': 'http://localhost:5181',
      '/webhook': 'http://localhost:5181',
      '/decorators': 'http://localhost:5181',
      '/state': 'http://localhost:5181',
      '/emoji': 'http://localhost:5181',
    },
    // Cloudflare quick tunnels rewrite the Host header to the tunnel
    // hostname; Vite's default host check would reject those requests.
    allowedHosts: true,
  },
})
