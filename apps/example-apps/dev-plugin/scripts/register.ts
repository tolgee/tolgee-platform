/**
 * One-time install bootstrap.
 *
 * Default flow: open Tolgee in the user's browser, let them pick an org and
 * approve the install, then receive the install credentials back on a one-shot
 * localhost callback server.
 *
 * Headless fallback: pass `--pat=tgpat_…` to skip the browser and register
 * directly with a personal-access token (useful for CI).
 *
 * When Tolgee is local the manifest is served straight from Express — no
 * Cloudflare tunnel needed. For remote Tolgee a quick tunnel is booted so
 * Tolgee can fetch the manifest from outside the user's machine. Either way,
 * the local dev server must be running (`npm run dev` in another terminal).
 */
import { createInterface } from 'node:readline/promises'
import { existsSync } from 'node:fs'
import { createServer, type IncomingMessage, type ServerResponse } from 'node:http'
import { randomUUID, timingSafeEqual } from 'node:crypto'
import open from 'open'
import { bin, install, Tunnel } from 'cloudflared'
import {
  type InstallState,
  INSTALL_FILE,
  writeJson,
  writeTunnelState,
} from './lib'

type Org = { id: number; name: string; slug: string }

const VITE_PORT = Number(process.env.VITE_PORT ?? 5180)
const SERVER_PORT = Number(process.env.SERVER_PORT ?? 5181)
const LOCAL_MANIFEST_URL = `http://localhost:${SERVER_PORT}/manifest.json`
const BROWSER_TIMEOUT_MS = 5 * 60_000

const args = process.argv.slice(2)
const patFlag =
  args.find((a) => a.startsWith('--pat='))?.slice('--pat='.length) ?? null

const isLocalHost = (urlString: string): boolean => {
  try {
    const host = new URL(urlString).hostname
    return host === 'localhost' || host === '127.0.0.1' || host === '::1'
  } catch {
    return false
  }
}

const verifyManifestReachable = async (manifestUrl: string): Promise<void> => {
  let res: Response
  try {
    res = await fetch(manifestUrl)
  } catch (err) {
    throw new Error(
      `Could not reach ${manifestUrl}. Is \`npm run dev\` running in another terminal? (${
        err instanceof Error ? err.message : String(err)
      })`
    )
  }
  if (!res.ok) {
    throw new Error(`${manifestUrl} returned ${res.status}. Is dev running?`)
  }
}

const startTunnel = async (): Promise<string> => {
  if (!existsSync(bin)) {
    console.log('Installing cloudflared binary…')
    await install(bin)
  }
  console.log('Starting Cloudflare quick tunnel…')
  const tunnel = Tunnel.quick(`http://localhost:${VITE_PORT}`)
  const baseUrl = await new Promise<string>((resolve) =>
    tunnel.once('url', resolve)
  )
  console.log(`Tunnel live at ${baseUrl}`)
  return baseUrl
}

/**
 * Picks the manifest URL Tolgee will fetch + writes the tunnel state so
 * the local server serves the right baseUrl.
 *   local Tolgee  → http://localhost:5181/manifest.json (Express direct)
 *   remote Tolgee → Cloudflare quick tunnel pointing at Vite
 */
const resolveManifestUrl = async (
  tolgeeUrl: string
): Promise<string> => {
  if (isLocalHost(tolgeeUrl)) {
    writeTunnelState({ baseUrl: `http://localhost:${VITE_PORT}` })
    console.log(`Skipping tunnel — Tolgee is local; using ${LOCAL_MANIFEST_URL}`)
    return LOCAL_MANIFEST_URL
  }
  const tunnelBaseUrl = await startTunnel()
  writeTunnelState({ baseUrl: tunnelBaseUrl })
  return `${tunnelBaseUrl}/manifest.json`
}

const constantTimeEq = (a: string, b: string): boolean => {
  const ab = Buffer.from(a)
  const bb = Buffer.from(b)
  if (ab.length !== bb.length) return false
  return timingSafeEqual(ab, bb)
}

/**
 * Browser flow: spins up a one-shot HTTP server on 127.0.0.1, opens the
 * Tolgee install page, waits for the redirect callback.
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

const main = async (): Promise<void> => {
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

main().catch((err) => {
  console.error('register failed:', err instanceof Error ? err.message : err)
  process.exit(1)
})
