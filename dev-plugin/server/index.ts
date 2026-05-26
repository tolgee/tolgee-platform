import { createHmac, timingSafeEqual } from 'node:crypto'
import { existsSync, readFileSync, writeFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import express, { json, type Request } from 'express'
import {
  onWebhook,
  type AppWebhookPayload,
} from './webhookHandler'

const PORT = Number(process.env.PORT ?? 5181)
const WEBHOOK_SECRET = process.env.TOLGEE_WEBHOOK_SECRET ?? null
const __dirname = dirname(fileURLToPath(import.meta.url))
const DATA_FILE = join(__dirname, 'data.json')

type Data = {
  emojis: Record<string, string>
  updatedAt: Record<string, string>
  /** ISO timestamps of every translation-update webhook, keyed by translation id. */
  events: Record<string, string[]>
}

const initialData: Data = { emojis: {}, updatedAt: {}, events: {} }
const ACTIVITY_RETENTION_DAYS = 14

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
    let payload: AppWebhookPayload
    try {
      payload = JSON.parse(raw) as AppWebhookPayload
    } catch {
      res.status(400).json({ error: 'invalid json' })
      return
    }

    onWebhook(payload, 'SET_TRANSLATIONS', (typed) => {
      const activityData = typed.activityData
      const timestamp = activityData?.timestamp
      const occurredAt =
        typeof timestamp === 'number'
          ? new Date(timestamp).toISOString()
          : new Date().toISOString()
      const translations = activityData?.modifiedEntities?.Translation ?? []
      const cutoffMs = Date.now() - ACTIVITY_RETENTION_DAYS * 24 * 60 * 60 * 1000
      let touched = 0
      for (const entity of translations) {
        if (typeof entity.entityId === 'number') {
          const id = String(entity.entityId)
          data.updatedAt[id] = occurredAt
          const log = data.events[id] ?? []
          log.push(occurredAt)
          data.events[id] = log.filter((iso) => Date.parse(iso) >= cutoffMs)
          touched++
        }
      }
      if (touched > 0) persist()
    })

    res.status(204).end()
  }
)

app.use((_req, res, next) => {
  res.setHeader('Access-Control-Allow-Origin', '*')
  res.setHeader('Access-Control-Allow-Methods', 'GET,POST,PUT,DELETE,OPTIONS')
  res.setHeader('Access-Control-Allow-Headers', 'Authorization,Content-Type')
  res.setHeader('Access-Control-Max-Age', '600')
  next()
})

app.options('*', (_req, res) => {
  res.status(204).end()
})

app.use(json())

type DecoratorRequest = {
  projectId?: number
  keyIds?: number[]
  languageTags?: string[]
}

type DecoratorItem = {
  keyId: number
  languageTag?: string
  actionKey: string
  url?: string
  count?: number
  visibility?: 'always' | 'on-hover'
}

app.post('/decorators', (req, res) => {
  const body = (req.body ?? {}) as DecoratorRequest
  const keyIds = Array.isArray(body.keyIds) ? body.keyIds.filter((id) => typeof id === 'number') : []
  const languageTags = Array.isArray(body.languageTags)
    ? body.languageTags.filter((t) => typeof t === 'string')
    : []
  const items: DecoratorItem[] = []
  for (const keyId of keyIds) {
    // Demo: every key gets the audit shortcut.
    items.push({ keyId, actionKey: 'open-audit' })

    // Demo: show-activity is emitted for every (keyId, languageTag) pair,
    // with a synthetic count varying 0..6. count===0 hides the badge but
    // keeps the icon. When count is 0 we also dynamically override the
    // visibility to 'on-hover' so quiet cells declutter the row.
    for (const tag of languageTags) {
      const count = (keyId * 3 + tag.charCodeAt(0)) % 7
      const item: DecoratorItem = {
        keyId,
        languageTag: tag,
        actionKey: 'show-activity',
        count,
      }
      if (count === 0) item.visibility = 'on-hover'
      items.push(item)
    }
  }
  res.json({ items })
})

app.get('/api/state', (req, res) => {
  const ids = String(req.query.ids ?? '')
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
  if (ids.length === 0) {
    res.json({ emojis: {}, updatedAt: {}, events: {} })
    return
  }
  const emojis: Record<string, string> = {}
  const updatedAt: Record<string, string> = {}
  const events: Record<string, string[]> = {}
  for (const id of ids) {
    if (data.emojis[id]) emojis[id] = data.emojis[id]
    if (data.updatedAt[id]) updatedAt[id] = data.updatedAt[id]
    if (data.events[id]?.length) events[id] = data.events[id]
  }
  res.json({ emojis, updatedAt, events })
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
