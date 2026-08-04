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
import { bin, install, Tunnel } from 'cloudflared'
import { config } from '../server/config'
import {
  localDevUrls,
  tunnelDevUrls,
  tunnelNeeded,
  writeDevUrls,
} from '../server/devTunnel'

const ensureBinary = async (): Promise<void> => {
  if (existsSync(bin)) return
  console.log('[tunnel] downloading the cloudflared binary…')
  await install(bin)
}

const startTunnel = async (port: number): Promise<string> => {
  const tunnel = Tunnel.quick(`http://localhost:${port}`)
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

  await ensureBinary()
  const urls = tunnelDevUrls(await startTunnel(config.vitePort))
  writeDevUrls(urls)
  console.log(`[tunnel] app:      ${urls.baseUrl}`)
  console.log(`[tunnel] manifest: ${urls.manifestUrl}`)
  await parkUntilSignal()
}

main().catch((error) => {
  console.error('[tunnel]', error)
  process.exit(1)
})
