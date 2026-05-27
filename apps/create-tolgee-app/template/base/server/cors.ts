import type { RequestHandler } from 'express'
import { TOLGEE_URL } from './config'

/**
 * Allows the Tolgee webapp (TOLGEE_URL) to call /decorators with the
 * install JWT. Loosens to `*` only when TOLGEE_URL isn't set.
 */
export const cors: RequestHandler = (_req, res, next) => {
  res.setHeader('Access-Control-Allow-Origin', TOLGEE_URL || '*')
  res.setHeader('Access-Control-Allow-Methods', 'GET,POST,PUT,DELETE,OPTIONS')
  res.setHeader('Access-Control-Allow-Headers', 'Authorization,Content-Type')
  res.setHeader('Access-Control-Max-Age', '600')
  next()
}
