import type { Express, Request, Response } from 'express'

type DecoratorsRequest = {
  installId?: number
  projectId?: number
  keyIds?: number[]
  languageTags?: string[]
}

/**
 * Dynamic decorators endpoint. Returns one item per key (or per
 * key+language) the plugin wants to surface with an icon decorator.
 *
 * This stub returns an empty list. Replace with real logic — the
 * Tolgee webapp will POST here with `{ installId, projectId, keyIds,
 * languageTags }` and render whatever you return alongside native row
 * icons.
 */
export const registerDecoratorsRoute = (app: Express): void => {
  app.post('/decorators', (req: Request, res: Response) => {
    const body = (req.body ?? {}) as DecoratorsRequest
    void body
    res.json({ items: [] })
  })
}
