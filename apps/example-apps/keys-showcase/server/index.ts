import express from 'express'
import { config } from './config'
import { cors } from './cors'
import { resolveDevUrls, tunnelNeeded } from './devTunnel'
import { selfRegisterIfConfigured } from './register'
import { registerLifecycleRoute } from './routes/lifecycle'
import { registerManifestRoute } from './routes/manifest'

const app = express()

app.use(cors)

// Before the JSON parser: lifecycle deliveries are verified against the exact
// bytes Tolgee signed, which a parser would consume.
registerLifecycleRoute(app)

app.use(express.json())

registerManifestRoute(app)

/**
 * Self-registration is what tells Tolgee where to fetch the manifest, and a dev
 * tunnel gets a fresh hostname on every `npm run dev` — so the URLs have to be
 * settled before registering, never after.
 */
const connect = async (): Promise<void> => {
  const urls = await resolveDevUrls(config)

  if (!urls) {
    console.error(
      `[dev] no tunnel came up, so ${config.tolgeeUrl} has no URL it can reach ` +
        'this app at — skipping registration. Is `npm run dev:tunnel` running? ' +
        'Set TOLGEE_DEV_TUNNEL=none if Tolgee can reach this machine directly.'
    )
    return
  }

  console.log(`  manifest: ${urls.manifestUrl}`)
  console.log(`  app UI:   ${urls.baseUrl}`)
  await selfRegisterIfConfigured(urls.manifestUrl)
}

app.listen(config.serverPort, () => {
  console.log(`Keys Showcase server listening on http://localhost:${config.serverPort}`)
  if (tunnelNeeded(config.tolgeeUrl)) {
    console.log(`  waiting for the dev tunnel — ${config.tolgeeUrl} is not local…`)
  }
  void connect()
})
