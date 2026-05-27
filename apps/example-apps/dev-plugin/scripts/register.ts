/**
 * One-time bootstrap for the dev plugin. Boots a short-lived Cloudflare
 * Quick Tunnel, registers the plugin against a chosen Tolgee
 * organization, and stores the install id + PAT in `.tolgee-dev/install.json`
 * so subsequent `npm run dev` runs can re-point the manifest URL via
 * PATCH on every restart.
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

const VITE_PORT = 5180

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
    throw new Error(`Could not list organizations (${res.status}): ${await res.text()}`)
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

const main = async (): Promise<void> => {
  if (existsSync(INSTALL_FILE)) {
    console.log(
      `Install record already exists at ${INSTALL_FILE}. Delete it to re-register, ` +
        'or just run `npm run dev` to keep using the existing install.'
    )
    return
  }

  const tolgeeUrlInput = await prompt(
    'Tolgee URL [https://app.tolgee.io]: '
  )
  const tolgeeUrl = tolgeeUrlInput || 'https://app.tolgee.io'
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
  writeTunnelState({ baseUrl })

  try {
    const installId = await registerApp(
      tolgeeUrl,
      pat,
      org.id,
      `${baseUrl}/manifest.json`
    )
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
    console.log(
      'You can now stop this script (Ctrl-C) and run `npm run dev` to ' +
        'start developing — each restart will PATCH the install with the ' +
        'fresh tunnel URL.'
    )
  } finally {
    // Keep tunnel open until the user kills the script, so Tolgee can
    // still reach manifest.json long enough to finish the install
    // round-trip and the user can confirm in the UI before quitting.
  }
}

main().catch((err) => {
  console.error('register failed:', err)
  process.exit(1)
})
