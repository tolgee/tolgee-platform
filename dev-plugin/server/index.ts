import { createHmac, timingSafeEqual } from 'node:crypto'
import { existsSync, readFileSync, writeFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import express, { json, type Request } from 'express'

const PORT = Number(process.env.PORT ?? 5181)
const WEBHOOK_SECRET = process.env.TOLGEE_WEBHOOK_SECRET ?? null
const __dirname = dirname(fileURLToPath(import.meta.url))
const DATA_FILE = join(__dirname, 'data.json')

type Data = {
  emojis: Record<string, string>
  updatedAt: Record<string, string>
}

const initialData: Data = { emojis: {}, updatedAt: {} }

const data: Data = (() => {
  if (!existsSync(DATA_FILE)) return { ...initialData }
  try {
    return { ...initialData, ...JSON.parse(readFileSync(DATA_FILE, 'utf8')) }
  } catch (err) {
    console.warn('Failed to parse data.json — starting empty:', err)
    return { ...initialData }
  }
})()

const persist = () => {
  writeFileSync(DATA_FILE, JSON.stringify(data, null, 2))
}

const verifySignature = (req: Request, raw: string): boolean => {
  if (!WEBHOOK_SECRET) {
    console.warn(
      'TOLGEE_WEBHOOK_SECRET is not set — accepting webhook without verification.'
    )
    return true
  }
  const header = req.header('Tolgee-Signature')
  if (!header) return false
  let parsed: { timestamp?: number; signature?: string }
  try {
    parsed = JSON.parse(header)
  } catch {
    return false
  }
  if (typeof parsed.timestamp !== 'number' || typeof parsed.signature !== 'string') {
    return false
  }
  const expected = createHmac('sha256', WEBHOOK_SECRET)
    .update(`${parsed.timestamp}.${raw}`)
    .digest('hex')
  const expectedBuf = Buffer.from(expected, 'utf8')
  const providedBuf = Buffer.from(parsed.signature, 'utf8')
  if (expectedBuf.length !== providedBuf.length) return false
  return timingSafeEqual(expectedBuf, providedBuf)
}

const app = express()

app.post(
  '/webhook',
  express.text({ type: 'application/json', limit: '5mb' }),
  (req, res) => {
    const raw = typeof req.body === 'string' ? req.body : ''
    if (!verifySignature(req, raw)) {
      res.status(401).json({ error: 'invalid signature' })
      return
    }
    let payload: {
      eventType?: string
      activityData?: {
        timestamp?: number
        modifiedEntities?: Array<{
          entityClass?: string
          entityId?: number
        }>
      }
    }
    try {
      payload = JSON.parse(raw)
    } catch {
      res.status(400).json({ error: 'invalid json' })
      return
    }
    const timestamp = payload.activityData?.timestamp
    const occurredAt =
      typeof timestamp === 'number'
        ? new Date(timestamp).toISOString()
        : new Date().toISOString()
    const translations =
      payload.activityData?.modifiedEntities?.filter(
        (entity) => entity.entityClass === 'Translation' && typeof entity.entityId === 'number'
      ) ?? []
    for (const entity of translations) {
      data.updatedAt[String(entity.entityId)] = occurredAt
    }
    if (translations.length > 0) persist()
    res.status(204).end()
  }
)

app.use(json())

app.get('/api/state', (req, res) => {
  const ids = String(req.query.ids ?? '')
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
  if (ids.length === 0) {
    res.json({ emojis: {}, updatedAt: {} })
    return
  }
  const emojis: Record<string, string> = {}
  const updatedAt: Record<string, string> = {}
  for (const id of ids) {
    if (data.emojis[id]) emojis[id] = data.emojis[id]
    if (data.updatedAt[id]) updatedAt[id] = data.updatedAt[id]
  }
  res.json({ emojis, updatedAt })
})

app.put('/api/emoji', (req, res) => {
  const { translationId, emoji } = req.body ?? {}
  if (typeof translationId !== 'number') {
    res.status(400).json({ error: 'translationId must be a number' })
    return
  }
  const key = String(translationId)
  if (emoji == null || emoji === '') {
    delete data.emojis[key]
  } else if (typeof emoji === 'string') {
    data.emojis[key] = emoji
  } else {
    res.status(400).json({ error: 'emoji must be a string or null' })
    return
  }
  persist()
  res.json({ ok: true })
})

app.listen(PORT, () => {
  console.log(`dev-plugin server listening on http://localhost:${PORT}`)
  if (!WEBHOOK_SECRET) {
    console.warn(
      'TOLGEE_WEBHOOK_SECRET is not set; webhook signatures will not be verified.'
    )
  }
})
