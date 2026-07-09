import express, { json } from 'express'
import { ANTHROPIC_API_KEY, SERVER_PORT, WEBHOOK_SECRET } from './config'
import { cors } from './cors'
import { registerManifestRoute } from './routes/manifest'
import { registerWebhookRoute } from './routes/webhook'
import { registerDecoratorsRoute } from './routes/decorators'
import { registerTranslateBackRoute } from './routes/translateBack'

const app = express()

// /webhook is registered before the json() middleware: the SDK's HMAC
// verifier needs the raw POST body, not a parsed object.
registerWebhookRoute(app)

app.use(cors)
app.options('*', (_req, res) => {
  res.status(204).end()
})
app.use(json())

registerManifestRoute(app)
registerDecoratorsRoute(app)
registerTranslateBackRoute(app)

app.listen(SERVER_PORT, () => {
  console.log(`back-translate server listening on http://localhost:${SERVER_PORT}`)
  if (!WEBHOOK_SECRET) {
    console.warn(
      'TOLGEE_WEBHOOK_SECRET is not set; webhook signatures will not be verified.'
    )
  }
  if (!ANTHROPIC_API_KEY) {
    console.warn(
      'ANTHROPIC_API_KEY is not set; /translate-back will return 503.'
    )
  }
})
