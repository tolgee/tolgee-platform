import type { AppContextClaims } from '../shared/contextTypes'

/**
 * Decodes (does NOT cryptographically verify) the Tolgee-issued context
 * JWT into typed claims. Plugin backends generally don't have access to
 * the platform's signing key, so verification happens server-side at
 * Tolgee — passing the same token as a bearer on REST calls back to
 * Tolgee is what authenticates the plugin.
 *
 * Use this when you need the install/user/project ids out of the token
 * for logging, routing, or persistence.
 */
export const decodeContextToken = (jwt: string): AppContextClaims => {
  const parts = jwt.split('.')
  if (parts.length !== 3) {
    throw new Error('Malformed JWT: expected 3 segments')
  }
  const payload = JSON.parse(base64UrlDecode(parts[1])) as Record<string, unknown>

  const installId = payload['tg.app.inst']
  const projectId = payload['tg.app.proj']
  if (typeof installId !== 'number' || typeof projectId !== 'number') {
    throw new Error('Token missing tg.app.inst or tg.app.proj claim')
  }
  return {
    installId,
    projectId,
    userId: Number(payload.sub),
    audience: String(payload.aud),
    expiresAt: Number(payload.exp),
  }
}

const base64UrlDecode = (s: string): string => {
  const normalized = s.replace(/-/g, '+').replace(/_/g, '/')
  const pad = (4 - (normalized.length % 4)) % 4
  return atob(normalized + '='.repeat(pad))
}
