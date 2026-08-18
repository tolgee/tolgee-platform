import { readFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import express from 'express'
import {
  ensureAppCredentialsFresh,
  mountTolgeeLifecycle,
  renderManifest,
  selfRegisterAppWithRetry,
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
          `(install ${event.install?.installId}) — recorded; the app credentials mint its tokens.`
      )
    },
    uninstalled: (event) => {
      console.log(
        `Lifecycle: uninstalled (install ${event.install?.installId ?? 'all'}) — ` +
          'the stored install record was dropped.'
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
  if (!config.registrationToken) {
    console.log(
      'Manual mode — register the app yourself in Tolgee:\n' +
        '  Organization → Apps → Register app, then paste this manifest URL:\n' +
        `    ${manifestUrl}\n` +
        '  Set TOLGEE_APP_REGISTRATION_TOKEN to auto-connect instead ' +
        '(the secret comes from the Tolgee administrator).'
    )
    return
  }

  // Retry with backoff: the app may boot before Tolgee — or before its token
  // exists — and must still connect once Tolgee is ready, without restarting.
  const result = await selfRegisterAppWithRetry(
    {
      tolgeeUrl: config.tolgeeUrl,
      registrationToken: config.registrationToken,
      manifestUrl,
    },
    {
      onRetry: (error, attempt) =>
        console.warn(
          `Auto-connect attempt ${attempt} against ${config.tolgeeUrl} failed ` +
            `(${error.message}); retrying — still serving the manifest.`
        ),
    }
  )

  if (!result.created) {
    console.log(
      `Auto-connect: install ${result.installId} already existed on ${config.tolgeeUrl}; ` +
        `its manifest URL now points at ${manifestUrl}.`
    )
    return
  }

  console.log(
    `Auto-connect: registered install ${result.installId} on ${config.tolgeeUrl}.\n` +
      `  Its credentials are stored in ${result.credentialsPath} (gitignored) — nothing to copy.`
  )
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
