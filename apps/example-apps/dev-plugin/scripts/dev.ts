/**
 * Boots a Cloudflare Quick Tunnel pointing at the local Vite dev server,
 * writes its public URL to `.tolgee-dev/tunnel.json` so the manifest
 * route picks it up, and (if an install was registered) PATCHes the
 * Tolgee install's manifest URL so the new tunnel URL takes effect
 * immediately.
 *
 * Skips the tunnel entirely when the registered Tolgee instance lives
 * on localhost — Tolgee can reach the dev plugin directly, no public
 * URL needed. Force-disable via `TOLGEE_DEV_TUNNEL=none`.
 */
import { existsSync } from 'node:fs'
import { bin, install, Tunnel } from 'cloudflared'
import {
  clearTunnelState,
  patchManifestUrl,
  readInstallState,
  writeTunnelState,
  type InstallState,
} from './lib'

const VITE_PORT = Number(process.env.VITE_PORT ?? 5180)
const LOCAL_BASE_URL = `http://localhost:${VITE_PORT}`

const isLocalHost = (urlString: string): boolean => {
  try {
    const host = new URL(urlString).hostname
    return host === 'localhost' || host === '127.0.0.1' || host === '::1'
  } catch {
    return false
  }
}

const shouldSkipTunnel = (installState: InstallState | null): boolean => {
  if (process.env.TOLGEE_DEV_TUNNEL === 'none') return true
  if (installState && isLocalHost(installState.tolgeeUrl)) return true
  if (process.env.TOLGEE_URL && isLocalHost(process.env.TOLGEE_URL)) return true
  return false
}

const ensureBinary = async (): Promise<void> => {
  if (!existsSync(bin)) {
    console.log('[tunnel] installing cloudflared binary…')
    await install(bin)
  }
}

const startTunnel = async (): Promise<string> => {
  const tunnel = Tunnel.quick(`http://localhost:${VITE_PORT}`)
  const url = await new Promise<string>((resolve) => {
    tunnel.once('url', resolve)
  })
  tunnel.on('exit', (code) => {
    console.log(`[tunnel] cloudflared exited with code ${code}`)
  })
  process.once('SIGINT', () => tunnel.stop())
  process.once('SIGTERM', () => tunnel.stop())
  return url
}

const main = async (): Promise<void> => {
  clearTunnelState()
  const installState = readInstallState()

  if (shouldSkipTunnel(installState)) {
    console.log(
      `[tunnel] skipping cloudflared — Tolgee reaches the plugin at ${LOCAL_BASE_URL}`
    )
    writeTunnelState({ baseUrl: LOCAL_BASE_URL })
    await maybePatchManifestUrl(installState, LOCAL_BASE_URL)
    // Park so concurrently -k doesn't tear the other processes down.
    // A long-lived interval keeps the event loop alive until SIGINT/SIGTERM.
    const heartbeat = setInterval(() => {}, 1 << 30)
    await new Promise<void>((resolve) => {
      process.once('SIGINT', () => resolve())
      process.once('SIGTERM', () => resolve())
    })
    clearInterval(heartbeat)
    return
  }

  await ensureBinary()
  const baseUrl = await startTunnel()
  console.log(`[tunnel] live at ${baseUrl}`)
  writeTunnelState({ baseUrl })
  await maybePatchManifestUrl(installState, baseUrl)
}

const maybePatchManifestUrl = async (
  installState: InstallState | null,
  baseUrl: string
): Promise<void> => {
  if (!installState) {
    console.log(
      '[tunnel] no install registered — run `npm run register` to ' +
        'install the dev plugin against a Tolgee organization.'
    )
    return
  }
  try {
    await patchManifestUrl(installState, `${baseUrl}/manifest.json`)
    console.log(
      `[tunnel] PATCHed install ${installState.installId} to use ${baseUrl}/manifest.json`
    )
  } catch (err) {
    console.error('[tunnel] manifest-url update failed:', err)
  }
}

main().catch((err) => {
  console.error('[tunnel] fatal:', err)
  process.exit(1)
})
