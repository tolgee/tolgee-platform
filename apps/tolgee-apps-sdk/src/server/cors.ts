/**
 * CORS headers a Tolgee App's server endpoints (decorators, custom APIs) must
 * return so the webapp — served from a different origin than the app — can call
 * them. Wildcard origin is safe here because the install token travels in the
 * `Authorization` header, not a credentialed cookie.
 *
 * Apply them in any framework, e.g. Express:
 *     for (const [k, v] of Object.entries(tolgeeAppCorsHeaders())) res.setHeader(k, v)
 */
export const tolgeeAppCorsHeaders = (): Record<string, string> => ({
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET,POST,PUT,DELETE,OPTIONS',
  'Access-Control-Allow-Headers': 'Authorization,Content-Type',
  'Access-Control-Max-Age': '600',
})
