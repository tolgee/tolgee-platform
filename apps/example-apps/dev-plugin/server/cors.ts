import type { RequestHandler } from 'express'

/**
 * Permissive CORS for the dev plugin: allows any origin to call
 * /decorators and /api/* with bearer tokens. Production plugins should
 * restrict the allowed origin to the Tolgee webapp.
 */
export const cors: RequestHandler = (_req, res, next) => {
  res.setHeader('Access-Control-Allow-Origin', '*')
  res.setHeader('Access-Control-Allow-Methods', 'GET,POST,PUT,DELETE,OPTIONS')
  res.setHeader('Access-Control-Allow-Headers', 'Authorization,Content-Type')
  res.setHeader('Access-Control-Max-Age', '600')
  next()
}
