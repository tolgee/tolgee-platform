import { createInterface } from 'node:readline/promises'
import { existsSync } from 'node:fs'
import {
  createServer,
  type IncomingMessage,
  type ServerResponse,
} from 'node:http'
import { randomUUID, timingSafeEqual } from 'node:crypto'
import open from 'open'
import { bin, install, Tunnel } from 'cloudflared'
import {
  INSTALL_FILE,
  isLocalHost,
  readTunnelState,
  writeJson,
  writeTunnelState,
  type InstallState,
} from './lib'

const sleep = (ms: number): Promise<void> =>
  new Promise((resolve) => setTimeout(resolve, ms))

type Org = { id: number; name: string; slug: string }

const BROWSER_TIMEOUT_MS = 5 * 60_000

const vitePort = (): number => Number(process.env.VITE_PORT ?? 5180)
const serverPort = (): number =>
  Number(process.env.SERVER_PORT ?? process.env.PORT ?? 5181)

const verifyManifestReachable = async (manifestUrl: string): Promise<void> => {
  // A freshly-started quick tunnel can take a moment to become edge-routable,
  // so retry a few times before giving up.
  const attempts = 5
  let lastError: unknown
  for (let attempt = 1; attempt <= attempts; attempt++) {
    try {
      const res = await fetch(manifestUrl)
      if (res.ok) return
      lastError = new Error(`${manifestUrl} returned ${res.status}`)
    } catch (err) {
      lastError = err
    }
    if (attempt < attempts) await sleep(1000)
  }
  throw new Error(
    `Could not reach ${manifestUrl}. Is \`npm run dev\` running in another terminal? (${
      lastError instanceof Error ? lastError.message : String(lastError)
    })`
  )
}

const startTunnel = async (): Promise<string> => {
  if (!existsSync(bin)) {
    console.log('Installing cloudflared binary…')
    await install(bin)
  }
  console.log('Starting Cloudflare quick tunnel…')
  const tunnel = Tunnel.quick(`http://localhost:${vitePort()}`)
  const baseUrl = await new Promise<string>((resolve) =>
    tunnel.once('url', resolve)
  )
  console.log(`Tunnel live at ${baseUrl}`)
  return baseUrl
}

/**
 * Reuses a live tunnel that `tolgee-app dev` already opened, if any. Reads the
 * URL `dev` persisted to tunnel.json and confirms it actually serves the
 * manifest — tunnel.json can hold a stale/dead URL from a previous session, and
 * a localhost value means no tunnel is up. Returns null in those cases so the
 * caller starts its own tunnel.
 */
const reusableTunnel = async (): Promise<string | null> => {
  const baseUrl = readTunnelState()?.baseUrl
  if (!baseUrl || isLocalHost(baseUrl)) return null
  try {
    const res = await fetch(`${baseUrl}/manifest.json`)
    if (res.ok) {
      console.log(`Reusing live tunnel from \`tolgee-app dev\`: ${baseUrl}`)
      return baseUrl
    }
  } catch {
    // unreachable (stale URL) — fall back to starting our own tunnel
  }
  return null
}

/**
 * Picks the manifest URL Tolgee will fetch + writes the tunnel state so the
 * local server serves the right baseUrl.
 *   local Tolgee  → http://localhost:<server>/manifest.json (Express direct)
 *   remote Tolgee → reuse `dev`'s live tunnel, or open a fresh Cloudflare one
 */
const resolveManifestUrl = async (tolgeeUrl: string): Promise<string> => {
  const localManifestUrl = `http://localhost:${serverPort()}/manifest.json`
  if (isLocalHost(tolgeeUrl)) {
    writeTunnelState({ baseUrl: `http://localhost:${vitePort()}` })
    console.log(`Skipping tunnel — Tolgee is local; using ${localManifestUrl}`)
    return localManifestUrl
  }
  const base = (await reusableTunnel()) ?? (await startTunnel())
  writeTunnelState({ baseUrl: base })
  return `${base}/manifest.json`
}

const constantTimeEq = (a: string, b: string): boolean => {
  const ab = Buffer.from(a)
  const bb = Buffer.from(b)
  if (ab.length !== bb.length) return false
  return timingSafeEqual(ab, bb)
}

/**
 * Browser flow: spins up a one-shot HTTP server on 127.0.0.1, opens the Tolgee
 * install page, waits for the redirect callback.
 */
const browserRegister = async (
  tolgeeUrl: string,
  manifestUrl: string
): Promise<InstallState> => {
  const state = randomUUID()
  return await new Promise<InstallState>((resolve, reject) => {
    let settled = false
    const finish = (
      kind: 'resolve' | 'reject',
      value: InstallState | Error
    ) => {
      if (settled) return
      settled = true
      clearTimeout(timeout)
      server.close()
      if (kind === 'resolve') resolve(value as InstallState)
      else reject(value as Error)
    }

    const server = createServer((req: IncomingMessage, res: ServerResponse) => {
      const url = new URL(req.url ?? '', `http://127.0.0.1`)
      if (url.pathname !== '/cb') {
        res.statusCode = 404
        res.end('Not found')
        return
      }
      const incomingState = url.searchParams.get('state') ?? ''
      if (!constantTimeEq(incomingState, state)) {
        res.statusCode = 400
        res.end(htmlPage('State mismatch — refusing this callback.', false))
        finish('reject', new Error('Callback state did not match.'))
        return
      }
      const error = url.searchParams.get('error')
      if (error) {
        res.end(htmlPage(`Install ${error}. You can close this tab.`, false))
        finish('reject', new Error(`Install ${error}`))
        return
      }
      const installId = Number(url.searchParams.get('installId'))
      const organizationId = Number(url.searchParams.get('organizationId'))
      const clientId = url.searchParams.get('clientId') ?? ''
      const clientSecret = url.searchParams.get('clientSecret') ?? ''
      const webhookSecret = url.searchParams.get('webhookSecret') ?? ''
      if (!installId || !organizationId || !clientId || !clientSecret) {
        res.statusCode = 400
        res.end(htmlPage('Callback missing required fields.', false))
        finish('reject', new Error('Callback missing required fields.'))
        return
      }
      res.end(htmlPage('Installed. You can close this tab.', true))
      finish('resolve', {
        tolgeeUrl,
        organizationId,
        installId,
        clientId,
        clientSecret,
        webhookSecret,
      })
    })

    const timeout = setTimeout(
      () => finish('reject', new Error('Browser flow timed out.')),
      BROWSER_TIMEOUT_MS
    )

    server.on('error', (err) => finish('reject', err))
    server.listen(0, '127.0.0.1', async () => {
      const address = server.address()
      if (!address || typeof address === 'string') {
        finish('reject', new Error('Failed to bind localhost port'))
        return
      }
      const callback = `http://127.0.0.1:${address.port}/cb`
      const installUrl =
        `${tolgeeUrl.replace(/\/$/, '')}/install-app` +
        `?manifestUrl=${encodeURIComponent(manifestUrl)}` +
        `&callback=${encodeURIComponent(callback)}` +
        `&state=${encodeURIComponent(state)}`
      console.log(`\nOpening browser to install the plugin…`)
      console.log(`  ${installUrl}\n`)
      console.log(
        `If the browser doesn't open automatically, paste that URL into it.\n` +
          `Waiting up to ${BROWSER_TIMEOUT_MS / 1000}s for you to approve…`
      )
      try {
        await open(installUrl)
      } catch {
        // ignore — user can paste the URL manually
      }
    })
  })
}

const htmlPage = (message: string, ok: boolean): string => {
  const color = ok ? '#1b8c43' : '#c44343'
  return `<!doctype html>
<html><head><meta charset="utf-8"><title>Tolgee plugin install</title>
<style>
  body { font-family: -apple-system, system-ui, sans-serif; max-width: 480px; margin: 80px auto; text-align: center; color: #222; }
  .badge { display: inline-block; padding: 12px 18px; border-radius: 999px; color: white; background: ${color}; margin-bottom: 16px; font-weight: 600; }
</style></head><body>
  <div class="badge">${ok ? '✓ Done' : '✗ Stopped'}</div>
  <p>${message}</p>
</body></html>`
}

/** Headless fallback: prompts for org by listing them via PAT. */
const patRegister = async (
  tolgeeUrl: string,
  manifestUrl: string,
  pat: string
): Promise<InstallState> => {
  if (!pat.startsWith('tgpat_')) {
    throw new Error('--pat value must start with tgpat_')
  }
  const orgs = await (async () => {
    const res = await fetch(`${tolgeeUrl}/v2/organizations?size=100`, {
      headers: { 'X-API-Key': pat },
    })
    if (!res.ok) {
      throw new Error(
        `Could not list organizations (${res.status}): ${await res.text()}`
      )
    }
    const json = (await res.json()) as { _embedded?: { organizations?: Org[] } }
    return json._embedded?.organizations ?? []
  })()
  if (orgs.length === 0) {
    throw new Error('No organizations visible to this PAT.')
  }
  console.log('\nOrganizations:')
  orgs.forEach((o, i) => console.log(`  ${i + 1}. ${o.name} (${o.slug})`))
  const rl = createInterface({ input: process.stdin, output: process.stdout })
  const idxStr = (await rl.question(`Pick one [1-${orgs.length}]: `)).trim()
  rl.close()
  const idx = Number.parseInt(idxStr, 10) - 1
  const org = orgs[idx]
  if (!org) throw new Error('Invalid selection')

  const res = await fetch(`${tolgeeUrl}/v2/organizations/${org.id}/apps`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-API-Key': pat },
    body: JSON.stringify({ manifestUrl }),
  })
  if (!res.ok) {
    throw new Error(`Register failed (${res.status}): ${await res.text()}`)
  }
  const data = (await res.json()) as {
    id: number
    clientId: string
    clientSecret: string
    webhookSecret: string
  }
  return {
    tolgeeUrl,
    organizationId: org.id,
    installId: data.id,
    clientId: data.clientId,
    clientSecret: data.clientSecret,
    webhookSecret: data.webhookSecret,
  }
}

/** One-time install bootstrap. Pass `--pat=tgpat_…` for the headless flow. */
export const runRegister = async (args: string[]): Promise<void> => {
  const patFlag =
    args.find((a) => a.startsWith('--pat='))?.slice('--pat='.length) ?? null

  if (existsSync(INSTALL_FILE)) {
    console.log(
      `Install record already exists at ${INSTALL_FILE}. Delete it to re-register, ` +
        'or just run `npm run dev` to keep using the existing install.'
    )
    return
  }

  const tolgeeUrl = (process.env.TOLGEE_URL ?? 'https://app.tolgee.io').replace(
    /\/$/,
    ''
  )
  const manifestUrl = await resolveManifestUrl(tolgeeUrl)
  await verifyManifestReachable(manifestUrl)

  const state = patFlag
    ? await patRegister(tolgeeUrl, manifestUrl, patFlag)
    : await browserRegister(tolgeeUrl, manifestUrl)

  writeJson(INSTALL_FILE, state)
  console.log(
    `\nRegistered. Install ${state.installId} for org ${state.organizationId}. ` +
      `Saved to ${INSTALL_FILE}.`
  )
}
