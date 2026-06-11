import type { RequestHandler } from 'express'

/**
 * Permissive CORS for /decorators and /translate-back. The webapp's
 * dev origin (e.g. http://localhost:3824) differs from the Tolgee
 * backend origin (http://localhost:8824), so a single allowed origin
 * is fragile. The decorator JWT travels in the Authorization header
 * (not a credentialed cookie), so wildcard is safe.
 */
export const cors: RequestHandler = (_req, res, next) => {
  res.setHeader('Access-Control-Allow-Origin', '*')
  res.setHeader('Access-Control-Allow-Methods', 'GET,POST,PUT,DELETE,OPTIONS')
  res.setHeader('Access-Control-Allow-Headers', 'Authorization,Content-Type')
  res.setHeader('Access-Control-Max-Age', '600')
  next()
}
