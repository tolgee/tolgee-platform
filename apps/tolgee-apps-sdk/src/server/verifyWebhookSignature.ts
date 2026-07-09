/**
 * Verifies the `Tolgee-Signature` HMAC header against the raw POST body.
 * Returns true when the signature matches the per-install webhook
 * secret. Uses WebCrypto so the same code runs on Node 20+, Cloudflare
 * Workers, Bun, and Deno.
 *
 * The caller is responsible for reading the raw request body before any
 * JSON parser sees it — Express's `express.text({ type: 'application/json' })`
 * is the standard way.
 *
 *     const ok = await verifyWebhookSignature({
 *       header: req.header('Tolgee-Signature'),
 *       rawBody: req.body,
 *       secret: process.env.TOLGEE_WEBHOOK_SECRET!,
 *     })
 */
export async function verifyWebhookSignature(opts: {
  header: string | null | undefined
  rawBody: string
  secret: string
}): Promise<boolean> {
  if (!opts.header) return false

  let parsed: { timestamp?: unknown; signature?: unknown }
  try {
    parsed = JSON.parse(opts.header)
  } catch {
    return false
  }
  const { timestamp, signature } = parsed
  if (typeof timestamp !== 'number' || typeof signature !== 'string') {
    return false
  }

  const encoder = new TextEncoder()
  const key = await crypto.subtle.importKey(
    'raw',
    encoder.encode(opts.secret),
    { name: 'HMAC', hash: 'SHA-256' },
    false,
    ['sign']
  )
  const sigBytes = await crypto.subtle.sign(
    'HMAC',
    key,
    encoder.encode(`${timestamp}.${opts.rawBody}`)
  )
  const expected = hexEncode(new Uint8Array(sigBytes))
  return timingSafeStringEqual(expected, signature)
}

const hexEncode = (bytes: Uint8Array): string => {
  let out = ''
  for (const b of bytes) out += b.toString(16).padStart(2, '0')
  return out
}

const timingSafeStringEqual = (a: string, b: string): boolean => {
  if (a.length !== b.length) return false
  let diff = 0
  for (let i = 0; i < a.length; i++) {
    diff |= a.charCodeAt(i) ^ b.charCodeAt(i)
  }
  return diff === 0
}
