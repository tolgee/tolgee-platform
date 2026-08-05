import { readFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import express from 'express'
import {
  renderManifest,
  selfRegisterApp,
  tolgeeAppCorsHeaders,
} from '@tolgee/apps-sdk/server'
import { applyUrlOverrides, config, currentUrls } from './config'
import { resolveDevUrls, tunnelNeeded } from './devTunnel'

const here = dirname(fileURLToPath(import.meta.url))
const manifestTemplate = readFileSync(
  join(here, 'manifest.template.json'),
  'utf8'
)

const app = express()

app.use((_req, res, next) => {
  for (const [name, value] of Object.entries(tolgeeAppCorsHeaders())) {
    res.setHeader(name, value)
  }
  next()
})

app.get('/manifest.json', (_req, res) => {
  res
    .type('application/json')
    .send(renderManifest(manifestTemplate, currentUrls().baseUrl))
})

const connect = async (manifestUrl: string): Promise<void> => {
  if (!config.registrationSecret) {
    console.log(
      'Manual mode — register the app yourself in Tolgee:\n' +
        '  Organization → Apps → Add app, then paste this manifest URL:\n' +
        `    ${manifestUrl}\n` +
        '  Set TOLGEE_APP_REGISTRATION_SECRET to auto-connect instead.'
    )
    return
  }

  try {
    const result = await selfRegisterApp({
      tolgeeUrl: config.tolgeeUrl,
      registrationSecret: config.registrationSecret,
      // Omitted by default, so the app registers server-wide, owned by no
      // organization. Which organizations may use it is an admin decision.
      organizationSlug: config.organizationSlug,
      manifestUrl,
    })

    const where = result.native
      ? 'as a native (server-wide) app'
      : `in organization "${config.organizationSlug}"`

    if (!result.created) {
      console.log(
        `Auto-connect: install ${result.installId} already existed on ${config.tolgeeUrl} ${where}; ` +
          `its manifest URL now points at ${manifestUrl}.`
      )
      return
    }

    console.log(
      `Auto-connect: registered install ${result.installId} on ${config.tolgeeUrl} ${where}.\n` +
        `  Its credentials are stored in ${result.credentialsPath} (gitignored) — ` +
        'nothing to copy; `npm run token` reads them from there.' +
        (result.native
          ? '\n  Next: grant it to an organization in Tolgee under ' +
            'Administration → Apps, then enable it for a project.'
          : '')
    )
  } catch (error) {
    // A failed registration must not take the manifest endpoint down with it —
    // the manual flow still works, and the dev can fix the env and restart.
    console.error(
      `Auto-connect failed against ${config.tolgeeUrl}: ` +
        (error instanceof Error ? error.message : String(error)) +
        '\n  Check that Tolgee runs with tolgee.apps.enabled=true and ' +
        'tolgee.apps.allow-local-addresses=true, and that ' +
        'TOLGEE_APP_REGISTRATION_SECRET matches tolgee.apps.registration-secret.\n' +
        `  Still serving the manifest — you can register ${manifestUrl} by hand.`
    )
  }
}

/**
 * Registration is what tells Tolgee where to fetch the manifest, and the dev
 * tunnel gets a fresh hostname on every `npm run dev` — so the URLs have to be
 * settled before registering, never after.
 */
const start = async (): Promise<void> => {
  const resolved = await resolveDevUrls(config)

  if (!resolved) {
    console.error(
      `No tunnel came up, so ${config.tolgeeUrl} has no URL it can reach this app at — ` +
        'skipping registration. Is `npm run dev:tunnel` running? Set TOLGEE_DEV_TUNNEL=none ' +
        'if Tolgee can reach this machine directly.'
    )
    return
  }

  const urls = applyUrlOverrides(resolved)
  console.log(`keys-showcase serving ${urls.manifestUrl} (app baseUrl ${urls.baseUrl})`)
  await connect(urls.manifestUrl)
}

app.listen(config.serverPort, () => {
  console.log(`keys-showcase server listening on http://localhost:${config.serverPort}`)
  if (tunnelNeeded(config.tolgeeUrl)) {
    console.log(`Waiting for the dev tunnel — ${config.tolgeeUrl} is not local…`)
  }
  void start()
})
