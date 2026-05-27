/**
 * Boots a Cloudflare Quick Tunnel pointing at the local Vite dev server,
 * writes its public URL to `.tolgee-dev/tunnel.json` so the manifest
 * route picks it up, and (if an install was registered) PATCHes the
 * Tolgee install's manifest URL so the new tunnel URL takes effect
 * immediately.
 */
import { existsSync } from 'node:fs'
import { bin, install, Tunnel } from 'cloudflared'
import {
  clearTunnelState,
  patchManifestUrl,
  readInstallState,
  writeTunnelState,
} from './lib'

const VITE_PORT = 5180

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
  await ensureBinary()
  const baseUrl = await startTunnel()
  console.log(`[tunnel] live at ${baseUrl}`)
  writeTunnelState({ baseUrl })

  const installState = readInstallState()
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
      `[tunnel] PATCHed install ${installState.installId} to use new tunnel URL`
    )
  } catch (err) {
    console.error('[tunnel] manifest-url update failed:', err)
  }
}

main().catch((err) => {
  console.error('[tunnel] fatal:', err)
  process.exit(1)
})
