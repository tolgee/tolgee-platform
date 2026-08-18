import { readFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { renderManifest } from '@tolgee/apps-sdk/server'
import { LOCAL_URLS } from './config'
import { readDevUrls } from './devTunnel'

const TEMPLATE_PATH = join(
  dirname(fileURLToPath(import.meta.url)),
  'manifest.template.json'
)

/**
 * `manifest.template.json` is the source of truth for what this app
 * contributes. It is read per request, so edits show up on the next fetch
 * without restarting — and so does a tunnel URL that changed underneath.
 */
export const getManifest = (): string =>
  renderManifest(
    readFileSync(TEMPLATE_PATH, 'utf8'),
    readDevUrls()?.baseUrl ?? LOCAL_URLS.baseUrl
  )
