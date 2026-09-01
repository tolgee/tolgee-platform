/**
 * Publishes the URL Tolgee reaches this app at, for as long as `npm run dev`
 * runs.
 *
 * With Tolgee on another host, a Cloudflare quick tunnel puts the Vite dev
 * server (and, through its proxy, `/manifest.json`) on a public hostname. With
 * Tolgee on localhost — or with `TOLGEE_DEV_TUNNEL=none` — the local URLs are
 * published as they are.
 *
 * Either way the URLs land in `.tolgee-dev/tunnel.json` before the server
 * registers the app, so registration never points Tolgee at a dead URL.
 */
import { existsSync } from 'node:fs'
import { config } from '../server/config'
import {
  localDevUrls,
  tunnelDevUrls,
  tunnelNeeded,
  writeDevUrls,
} from '../server/devTunnel'

/** `cloudflared` is a dev-only dependency; this script is its only user. */
type Cloudflared = typeof import('cloudflared')

const main = async (): Promise<void> => {
  if (!tunnelNeeded(config.tolgeeUrl)) {
    const urls = localDevUrls(config.vitePort, config.serverPort)
    writeDevUrls(urls)
    console.log(
      process.env.TOLGEE_DEV_TUNNEL === 'none'
        ? `[tunnel] off (TOLGEE_DEV_TUNNEL=none) — publishing ${urls.baseUrl}; ` +
            `${config.tolgeeUrl} has to reach that itself.`
        : `[tunnel] not needed — ${config.tolgeeUrl} reaches ${urls.baseUrl} directly.`
    )
    await parkUntilSignal()
    return
  }

  const cloudflared = await loadCloudflared()
  if (!cloudflared) {
    console.error(
      `[tunnel] cloudflared is not installed, so ${config.tolgeeUrl} has no public URL ` +
        'to reach this app at and the app will not register. Install it with ' +
        '`npm install -D cloudflared`, or set TOLGEE_DEV_TUNNEL=none if Tolgee can ' +
        'already reach this machine. Vite and the app server keep running.'
    )
    await parkUntilSignal()
    return
  }

  await ensureBinary(cloudflared)
  const urls = tunnelDevUrls(await startTunnel(cloudflared, config.vitePort))
  writeDevUrls(urls)
  console.log(`[tunnel] app:      ${urls.baseUrl}`)
  console.log(`[tunnel] manifest: ${urls.manifestUrl}`)
  await parkUntilSignal()
}

/** Null when the optional dependency was pruned or never installed. */
const loadCloudflared = async (): Promise<Cloudflared | null> => {
  try {
    return await import('cloudflared')
  } catch {
    return null
  }
}

const ensureBinary = async (cloudflared: Cloudflared): Promise<void> => {
  if (existsSync(cloudflared.bin)) return
  console.log('[tunnel] downloading the cloudflared binary…')
  await cloudflared.install(cloudflared.bin)
}

const startTunnel = async (
  cloudflared: Cloudflared,
  port: number
): Promise<string> => {
  const tunnel = cloudflared.Tunnel.quick(`http://localhost:${port}`)
  const stop = (): void => {
    tunnel.stop()
  }
  process.once('SIGINT', stop)
  process.once('SIGTERM', stop)
  tunnel.on('exit', (code) => {
    console.log(`[tunnel] cloudflared exited with code ${code}`)
  })
  return new Promise<string>((resolve) => {
    tunnel.once('url', resolve)
  })
}

/** Keeps this process alive so `concurrently -k` does not tear down its siblings. */
const parkUntilSignal = async (): Promise<void> => {
  const heartbeat = setInterval(() => {}, 1 << 30)
  await new Promise<void>((resolve) => {
    process.once('SIGINT', () => resolve())
    process.once('SIGTERM', () => resolve())
  })
  clearInterval(heartbeat)
}

main().catch((error) => {
  console.error('[tunnel]', error)
  process.exit(1)
})
