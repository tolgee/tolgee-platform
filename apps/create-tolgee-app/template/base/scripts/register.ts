/**
 * One-time install bootstrap. Registers the plugin against a chosen
 * Tolgee organization and stores the install id + PAT in
 * `.tolgee-dev/install.json` so subsequent `npm run dev` runs can
 * re-point the manifest URL via PATCH on every restart.
 *
 * When Tolgee is local (localhost / 127.0.0.1 / ::1), the manifest is
 * served straight from the local Express server — no tunnel needed.
 * For remote Tolgee, a Cloudflare quick tunnel is booted so Tolgee can
 * fetch the manifest from outside the user's machine. Either way, the
 * local dev server must be running (`npm run dev` in another terminal)
 * so the manifest is reachable when Tolgee fetches it.
 */
import { createInterface } from 'node:readline/promises'
import { existsSync } from 'node:fs'
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

const isLocalHost = (urlString: string): boolean => {
  try {
    const host = new URL(urlString).hostname
    return host === 'localhost' || host === '127.0.0.1' || host === '::1'
  } catch {
    return false
  }
}

const prompt = async (question: string): Promise<string> => {
  const rl = createInterface({ input: process.stdin, output: process.stdout })
  try {
    return (await rl.question(question)).trim()
  } finally {
    rl.close()
  }
}

const fetchOrgs = async (tolgeeUrl: string, pat: string): Promise<Org[]> => {
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
}

const registerApp = async (
  tolgeeUrl: string,
  pat: string,
  orgId: number,
  manifestUrl: string
): Promise<number> => {
  const res = await fetch(`${tolgeeUrl}/v2/organizations/${orgId}/apps`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-API-Key': pat },
    body: JSON.stringify({ manifestUrl }),
  })
  if (!res.ok) {
    throw new Error(`Register failed (${res.status}): ${await res.text()}`)
  }
  const json = (await res.json()) as { id: number }
  return json.id
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

const main = async (): Promise<void> => {
  if (existsSync(INSTALL_FILE)) {
    console.log(
      `Install record already exists at ${INSTALL_FILE}. Delete it to re-register, ` +
        'or just run `npm run dev` to keep using the existing install.'
    )
    return
  }

  const tolgeeUrlInput = await prompt(
    `Tolgee URL [${process.env.TOLGEE_URL ?? 'https://app.tolgee.io'}]: `
  )
  const tolgeeUrl =
    tolgeeUrlInput || process.env.TOLGEE_URL || 'https://app.tolgee.io'
  const pat = await prompt('Tolgee PAT (tgpat_…): ')
  if (!pat.startsWith('tgpat_')) {
    throw new Error('PAT must start with tgpat_')
  }

  const orgs = await fetchOrgs(tolgeeUrl, pat)
  if (orgs.length === 0) {
    throw new Error('No organizations visible to this PAT.')
  }
  console.log('\nOrganizations:')
  orgs.forEach((o, i) => console.log(`  ${i + 1}. ${o.name} (${o.slug})`))
  const idxStr = await prompt(`Pick one [1-${orgs.length}]: `)
  const idx = Number.parseInt(idxStr, 10) - 1
  const org = orgs[idx]
  if (!org) throw new Error('Invalid selection')

  // Pick the manifest URL Tolgee will fetch:
  //   local Tolgee  → http://localhost:5181/manifest.json (Express direct)
  //   remote Tolgee → Cloudflare quick tunnel pointing at vite
  let manifestUrl: string
  let tunnelBaseUrl: string | null = null
  if (isLocalHost(tolgeeUrl)) {
    manifestUrl = LOCAL_MANIFEST_URL
    console.log(`Skipping tunnel — Tolgee is local; using ${manifestUrl}`)
    writeTunnelState({ baseUrl: `http://localhost:${VITE_PORT}` })
  } else {
    tunnelBaseUrl = await startTunnel()
    manifestUrl = `${tunnelBaseUrl}/manifest.json`
    writeTunnelState({ baseUrl: tunnelBaseUrl })
  }

  await verifyManifestReachable(manifestUrl)

  const installId = await registerApp(tolgeeUrl, pat, org.id, manifestUrl)
  const state: InstallState = {
    tolgeeUrl,
    organizationId: org.id,
    installId,
    pat,
  }
  writeJson(INSTALL_FILE, state)
  console.log(
    `\nRegistered. Install ${installId} for org ${org.name} (${org.id}). ` +
      `Saved to ${INSTALL_FILE}.`
  )
  if (tunnelBaseUrl) {
    console.log(
      'Keep this script running (or your dev server up) until you enable the ' +
        'plugin on a project in Tolgee — otherwise the tunnel URL stops resolving.'
    )
  }
}

main().catch((err) => {
  console.error('register failed:', err)
  process.exit(1)
})
