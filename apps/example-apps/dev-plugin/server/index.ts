import express, { json } from 'express'
import { SERVER_PORT, WEBHOOK_SECRET } from './config'
import { cors } from './cors'
import { registerWebhookRoute } from './routes/webhook'
import { registerDecoratorsRoute } from './routes/decorators'
import { registerStateRoute } from './routes/state'
import { registerEmojiRoute } from './routes/emoji'
import { registerManifestRoute } from './routes/manifest'

const app = express()

// /webhook is registered BEFORE the json() middleware: HMAC signature
// verification needs the raw POST body, not a parsed object.
registerWebhookRoute(app)

app.use(cors)
app.options('*', (_req, res) => {
  res.status(204).end()
})
app.use(json())

registerManifestRoute(app)
registerDecoratorsRoute(app)
registerStateRoute(app)
registerEmojiRoute(app)

app.listen(SERVER_PORT, () => {
  console.log(`dev-plugin server listening on http://localhost:${SERVER_PORT}`)
  if (!WEBHOOK_SECRET) {
    console.warn(
      'TOLGEE_WEBHOOK_SECRET is not set; webhook signatures will not be verified.'
    )
  }
})
