import { tolgeeAppCorsHeaders } from '@tolgee/apps-sdk/server'
import type { RequestHandler } from 'express'

/**
 * Applies the SDK's standard Tolgee-app CORS headers so the webapp — a
 * different origin than this server — can call any endpoint added here.
 */
export const cors: RequestHandler = (_req, res, next) => {
  for (const [name, value] of Object.entries(tolgeeAppCorsHeaders())) {
    res.setHeader(name, value)
  }
  next()
}
