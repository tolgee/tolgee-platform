import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

/**
 * tsup bundles a single dist/index.js next to the package root, and `npm run
 * dev` runs src/index.ts — one level down either way.
 */
export const PACKAGE_ROOT = resolve(
  dirname(fileURLToPath(import.meta.url)),
  '..'
)

/** `template/` ships inside the published package. */
export const TEMPLATE_ROOT = join(PACKAGE_ROOT, 'template')
