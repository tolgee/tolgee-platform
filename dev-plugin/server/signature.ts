import { createHmac, timingSafeEqual } from 'node:crypto'
import type { Request } from 'express'
import { WEBHOOK_SECRET } from './config'

type ParsedSignature = { timestamp: number; signature: string }

/**
 * Verifies the `Tolgee-Signature` HMAC header against the raw POST body.
 * When TOLGEE_WEBHOOK_SECRET is unset, accepts the request and logs a
 * warning — convenient for local development.
 */
export const verifySignature = (req: Request, raw: string): boolean => {
  if (!WEBHOOK_SECRET) {
    console.warn(
      'TOLGEE_WEBHOOK_SECRET is not set — accepting webhook without verification.'
    )
    return true
  }
  const header = req.header('Tolgee-Signature')
  if (!header) return false
  const parsed = parseHeader(header)
  if (!parsed) return false
  return matchesHmac(parsed, raw, WEBHOOK_SECRET)
}

const parseHeader = (header: string): ParsedSignature | null => {
  let data: { timestamp?: number; signature?: string }
  try {
    data = JSON.parse(header)
  } catch {
    return null
  }
  if (typeof data.timestamp !== 'number') return null
  if (typeof data.signature !== 'string') return null
  return { timestamp: data.timestamp, signature: data.signature }
}

const matchesHmac = (
  parsed: ParsedSignature,
  raw: string,
  secret: string
): boolean => {
  const expected = createHmac('sha256', secret)
    .update(`${parsed.timestamp}.${raw}`)
    .digest('hex')
  const a = Buffer.from(expected, 'utf8')
  const b = Buffer.from(parsed.signature, 'utf8')
  if (a.length !== b.length) return false
  return timingSafeEqual(a, b)
}
