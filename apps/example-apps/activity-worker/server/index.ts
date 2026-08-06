import { readFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import express from 'express'
import {
  ensureAppCredentialsFresh,
  mountTolgeeLifecycle,
  renderManifest,
  selfRegisterApp,
  tolgeeAppCorsHeaders,
} from '@tolgee/apps-sdk/server'
import { startActivityWorker } from './activityWorker'
import { applyUrlOverrides, config, currentUrls, workerConfig } from './config'
import { resolveDevUrls, tunnelNeeded } from './devTunnel'
import { handleFeedRequest } from './feedRoute'
import { forgetTolgeeClient } from './tolgeeAccess'

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

app.get('/api/feed', (req, res) => {
  handleFeedRequest(req)
    .then((result) => res.status(result.status).json(result.body))
    .catch((error: unknown) => {
      console.error('[server] /api/feed failed', error)
      res.status(502).json({ error: 'Could not reach Tolgee.' })
    })
})

/**
 * The one call that receives everything Tolgee pushes about this app: the
 * app-level credentials at registration, the per-install credentials whenever
 * an organization installs it, and every later rotation. The SDK verifies each
 * delivery against the app's webhook secret and stores what it carries.
 */
mountTolgeeLifecycle(app, {
  tolgeeUrl: config.tolgeeUrl,
  on: {
    registered: (event) => {
      console.log(
        `Lifecycle: Tolgee registered this app as "${event.app?.appId ?? 'unknown'}" — ` +
          'its app-level credentials are stored (never printed).'
      )
    },
    installed: (event) => {
      console.log(
        `Lifecycle: installed by ${event.organization?.slug ?? 'no organization'} ` +
          `(install ${event.install?.installId}) — credentials stored.`
      )
    },
    uninstalled: (event) => {
      console.log(
        `Lifecycle: uninstalled (install ${event.install?.installId ?? 'all'}) — ` +
          'the stored credentials were dropped.'
      )
    },
    secretRotated: (event) => {
      // The cached client holds a token minted from the previous secret.
      forgetTolgeeClient()
      console.log(
        `Lifecycle: Tolgee rotated the ${event.rotatedLayer ?? 'app'}-level secret — ` +
          'the worker re-authenticates with the new one.'
      )
    },
  },
  onRejected: (rejected) => {
    console.warn(`Lifecycle: refused a delivery — ${rejected.message}`)
  },
})

const connect = async (manifestUrl: string): Promise<void> => {
  if (!config.registrationSecret) {
    console.log(
      'Manual mode — register the app yourself in Tolgee:\n' +
        '  Administration → Apps → Add app, then paste this manifest URL:\n' +
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
        `  Its credentials are stored in ${result.credentialsPath} (gitignored) — nothing to copy.` +
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
 * Ages the app's own client secret out on its own, so nobody ever has to copy a
 * new one: Tolgee mints it, the SDK stores it, and the secret in use until now
 * keeps working until an operator revokes it. No-op when the credentials are
 * injected through the environment.
 */
const refreshCredentials = async (): Promise<void> => {
  try {
    const result = await ensureAppCredentialsFresh({
      tolgeeUrl: config.tolgeeUrl,
    })
    if (result.rotated) {
      console.log(
        'Auto-connect: this app issued itself a fresh client secret and stored it. ' +
          'The previous one still authenticates — revoke it in Tolgee once you see ' +
          'it go idle.'
      )
    }
  } catch (error) {
    // The credential in use was not touched, so this is a warning, not a failure.
    console.warn(
      'Could not refresh the stored client secret: ' +
        (error instanceof Error ? error.message : String(error))
    )
  }
}

/**
 * Registration is what tells Tolgee where to fetch the manifest, and the dev
 * tunnel gets a fresh hostname on every `npm run dev` — so the URLs have to be
 * settled before registering, never after. The worker starts after that: until
 * the app is registered it has no credentials to ask Tolgee anything with.
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
  console.log(
    `activity-worker serving ${urls.manifestUrl} (app baseUrl ${urls.baseUrl})`
  )
  await connect(urls.manifestUrl)
  await refreshCredentials()

  console.log(
    `Watching for translation changes — installations every ${workerConfig.installationsRefreshIntervalMs}ms, ` +
      `activity every ${workerConfig.activityPollIntervalMs}ms.`
  )
  startActivityWorker()
}

app.listen(config.serverPort, () => {
  console.log(
    `activity-worker server listening on http://localhost:${config.serverPort}`
  )
  if (tunnelNeeded(config.tolgeeUrl)) {
    console.log(`Waiting for the dev tunnel — ${config.tolgeeUrl} is not local…`)
  }
  void start()
})
