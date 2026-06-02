import Anthropic from '@anthropic-ai/sdk'
import type { Express, Request, Response } from 'express'
import { ANTHROPIC_API_KEY, ANTHROPIC_MODEL } from '../config'

type Verdict = 'match' | 'close' | 'mismatch'

type RequestBody = {
  text: string
  sourceLang: string
  baseText: string
  baseLang: string
}

type Analysis = {
  backTranslation: string
  verdict: Verdict
  explanation: string
}

const client = ANTHROPIC_API_KEY
  ? new Anthropic({ apiKey: ANTHROPIC_API_KEY })
  : null

const cache = new Map<string, Analysis>()

const isValidBody = (b: unknown): b is RequestBody => {
  if (typeof b !== 'object' || b === null) return false
  const o = b as Record<string, unknown>
  return (
    typeof o.text === 'string' &&
    typeof o.sourceLang === 'string' &&
    typeof o.baseText === 'string' &&
    typeof o.baseLang === 'string'
  )
}

const prompt = (body: RequestBody): string =>
  `You evaluate a translation by back-translating it and comparing semantically.

Original (${body.baseLang}): "${body.baseText}"
Translation (${body.sourceLang}): "${body.text}"

Steps:
1. Translate the ${body.sourceLang} text back to ${body.baseLang}.
2. Compare your back-translation to the original ${body.baseLang} text.
   - "match"    — identical meaning, no nuance lost
   - "close"    — same intent, minor word-choice differences
   - "mismatch" — different meaning, lost in translation

Respond ONLY with valid JSON, no preamble, no markdown:
{
  "backTranslation": "...",
  "verdict": "match" | "close" | "mismatch",
  "explanation": "one short sentence"
}`

const extractJson = (raw: string): Analysis => {
  const cleaned = raw.replace(/```(?:json)?/g, '').trim()
  const start = cleaned.indexOf('{')
  const end = cleaned.lastIndexOf('}')
  if (start < 0 || end < 0) throw new Error('No JSON in model response')
  return JSON.parse(cleaned.slice(start, end + 1)) as Analysis
}

const cacheKey = (b: RequestBody): string =>
  `${b.baseLang}|${b.baseText}|${b.sourceLang}|${b.text}`

export const registerTranslateBackRoute = (app: Express): void => {
  app.post('/translate-back', async (req: Request, res: Response) => {
    if (!client) {
      res
        .status(503)
        .json({ error: 'ANTHROPIC_API_KEY is not set in the server environment.' })
      return
    }
    if (!isValidBody(req.body)) {
      res.status(400).json({ error: 'Expected { text, sourceLang, baseText, baseLang } in body.' })
      return
    }
    const body = req.body

    const key = cacheKey(body)
    const cached = cache.get(key)
    if (cached) {
      res.json({ ...cached, cached: true })
      return
    }

    try {
      const response = await client.messages.create({
        model: ANTHROPIC_MODEL,
        max_tokens: 400,
        messages: [{ role: 'user', content: prompt(body) }],
      })
      const block = response.content[0]
      const rawText = block && block.type === 'text' ? block.text : ''
      const analysis = extractJson(rawText)
      cache.set(key, analysis)
      res.json(analysis)
    } catch (err) {
      console.error('[translate-back] anthropic error', err)
      res.status(502).json({
        error: 'Anthropic call failed',
        message: err instanceof Error ? err.message : String(err),
      })
    }
  })
}
