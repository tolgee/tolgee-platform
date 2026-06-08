import type { Express, Request, Response } from 'express'
import type { DecoratorsRequest, DecoratorsResponse } from '@tolgee/apps-sdk/server'

/**
 * Dynamic decorators endpoint. The Tolgee webapp POSTs the key/language rows
 * currently in view; you return icon decorations to render alongside the native
 * row icons (and to drive any `dynamic: true` action in the manifest).
 *
 * This demo flags every key in view with a lightbulb. Replace the body with
 * your own logic — e.g. fetch state from your backend and decorate selectively.
 */
export const registerDecoratorsRoute = (app: Express): void => {
  app.post('/decorators', (req: Request, res: Response) => {
    const body = (req.body ?? {}) as DecoratorsRequest
    const response: DecoratorsResponse = {
      items: (body.keyIds ?? []).map((keyId) => ({
        keyId,
        icon: 'Lightbulb01',
        tooltip: 'Flagged by {{name}}',
      })),
    }
    res.json(response)
  })
}
