import { existsSync } from 'node:fs'
import { bin, install, Tunnel } from 'cloudflared'
import {
  clearTunnelState,
  isLocalHost,
  patchManifestUrl,
  readInstallState,
  writeTunnelState,
  type InstallState,
} from './lib'

const vitePort = (): number => Number(process.env.VITE_PORT ?? 5180)
const localBaseUrl = (): string => `http://localhost:${vitePort()}`

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
  const tunnel = Tunnel.quick(`http://localhost:${vitePort()}`)
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

const maybePatchManifestUrl = async (
  installState: InstallState | null,
  baseUrl: string
): Promise<void> => {
  if (!installState) {
    console.log(
      '[tunnel] no install registered — run `tolgee-app register` to ' +
        'install the plugin against a Tolgee organization.'
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

/**
 * Boots a Cloudflare quick tunnel pointing at the local Vite server, writes its
 * URL to `.tolgee-dev/tunnel.json` (read by the manifest route), and repoints
 * the registered install at it. Skips the tunnel when Tolgee is on localhost.
 */
export const runDev = async (): Promise<void> => {
  clearTunnelState()
  const installState = readInstallState()

  if (shouldSkipTunnel(installState)) {
    console.log(
      `[tunnel] skipping cloudflared — Tolgee reaches the plugin at ${localBaseUrl()}`
    )
    writeTunnelState({ baseUrl: localBaseUrl() })
    await maybePatchManifestUrl(installState, localBaseUrl())
    // Park so `concurrently -k` doesn't tear down the other processes.
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
