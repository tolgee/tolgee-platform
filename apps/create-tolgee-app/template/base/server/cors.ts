import type { RequestHandler } from 'express'
import { tolgeeAppCorsHeaders } from '@tolgee/apps-sdk/server'

/**
 * Applies the SDK's standard Tolgee-app CORS headers so the webapp (a different
 * origin than this server) can call /decorators and any custom endpoints.
 */
export const cors: RequestHandler = (_req, res, next) => {
  for (const [name, value] of Object.entries(tolgeeAppCorsHeaders())) {
    res.setHeader(name, value)
  }
  next()
}
