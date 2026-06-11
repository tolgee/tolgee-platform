import { defineConfig } from 'tsup'

export default defineConfig({
  // Named entries so the three subpaths don't collide on `index.js`.
  entry: {
    index: 'src/index.ts',
    server: 'src/server/index.ts',
    browser: 'src/browser/index.ts',
  },
  outDir: 'dist',
  format: ['esm'],
  target: 'es2022',
  dts: true,
  clean: true,
  sourcemap: true,
  // @tginternal/client is a runtime dependency — keep it external.
  external: ['@tginternal/client'],
})
