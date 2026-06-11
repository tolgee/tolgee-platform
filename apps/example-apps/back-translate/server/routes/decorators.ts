import type { Express, Request, Response } from 'express'
import { getAnalysis } from '../store'

type DecoratorsRequest = {
  projectId?: number
  installId?: number
  keyIds?: number[]
  languageTags?: string[]
}

type DecoratorItem = {
  keyId: number
  languageTag: string
  actionKey: 'warning' | 'error'
}

/**
 * Returns one decorator per (keyId, languageTag) we've already analysed
 * whose verdict isn't a clean match. `match` produces no decorator —
 * the cell is fine.
 */
export const registerDecoratorsRoute = (app: Express): void => {
  app.post('/decorators', (req: Request, res: Response) => {
    const body = (req.body ?? {}) as DecoratorsRequest
    const keyIds = Array.isArray(body.keyIds) ? body.keyIds : []
    const languageTags = Array.isArray(body.languageTags)
      ? body.languageTags
      : []

    const items: DecoratorItem[] = []
    for (const keyId of keyIds) {
      for (const tag of languageTags) {
        const cached = getAnalysis(keyId, tag)
        if (!cached) continue
        if (cached.verdict === 'match') continue
        items.push({
          keyId,
          languageTag: tag,
          actionKey: cached.verdict === 'close' ? 'warning' : 'error',
        })
      }
    }
    res.json({ items })
  })
}
