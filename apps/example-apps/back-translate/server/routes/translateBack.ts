import type { Express, Request, Response } from 'express'
import { analyse, isAnthropicConfigured } from '../backTranslation'
import { getAnalysis, upsertAnalysis } from '../store'

type RequestBody = {
  text: string
  sourceLang: string
  baseText: string
  baseLang: string
  keyId?: number
  languageTag?: string
}

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

export const registerTranslateBackRoute = (app: Express): void => {
  app.post('/translate-back', async (req: Request, res: Response) => {
    if (!isAnthropicConfigured()) {
      res
        .status(503)
        .json({ error: 'ANTHROPIC_API_KEY is not set in the server environment.' })
      return
    }
    if (!isValidBody(req.body)) {
      res.status(400).json({
        error:
          'Expected { text, sourceLang, baseText, baseLang, keyId?, languageTag? } in body.',
      })
      return
    }
    const body = req.body

    // Reuse a stored analysis when text + base are unchanged. Lets the
    // panel skip the model call for already-analysed cells (incl. those
    // hydrated by the webhook).
    if (body.keyId != null && body.languageTag) {
      const cached = getAnalysis(body.keyId, body.languageTag)
      if (
        cached &&
        cached.text === body.text &&
        cached.baseText === body.baseText &&
        cached.baseLang === body.baseLang
      ) {
        res.json({
          backTranslation: cached.backTranslation,
          verdict: cached.verdict,
          explanation: cached.explanation,
          cached: true,
        })
        return
      }
    }

    try {
      const analysis = await analyse({
        text: body.text,
        sourceLang: body.sourceLang,
        baseText: body.baseText,
        baseLang: body.baseLang,
      })
      if (body.keyId != null && body.languageTag) {
        upsertAnalysis({
          ...analysis,
          keyId: body.keyId,
          languageTag: body.languageTag,
          baseLang: body.baseLang,
          baseText: body.baseText,
          text: body.text,
          analysedAt: new Date().toISOString(),
        })
      }
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
