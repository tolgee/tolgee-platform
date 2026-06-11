import express, { json } from 'express'
import { ANTHROPIC_API_KEY, SERVER_PORT, WEBHOOK_SECRET } from './config'
import { cors } from './cors'
import { registerManifestRoute } from './routes/manifest'
import { registerWebhookRoute } from './routes/webhook'
import { registerSuggestionsRoute } from './routes/suggestions'
import { registerAcceptRoutes } from './routes/accept'

const app = express()

// /webhook is registered before json(): the SDK's HMAC verifier needs the raw POST body.
registerWebhookRoute(app)

app.use(cors)
app.options('*', (_req, res) => {
  res.status(204).end()
})
app.use(json())

registerManifestRoute(app)
registerSuggestionsRoute(app)
registerAcceptRoutes(app)

app.listen(SERVER_PORT, () => {
  console.log(`glossary-keeper server listening on http://localhost:${SERVER_PORT}`)
  if (!WEBHOOK_SECRET) {
    console.warn(
      'TOLGEE_WEBHOOK_SECRET is not set; webhook signatures will not be verified.'
    )
  }
  if (!ANTHROPIC_API_KEY) {
    console.warn('ANTHROPIC_API_KEY is not set; the webhook will not produce suggestions.')
  }
})
