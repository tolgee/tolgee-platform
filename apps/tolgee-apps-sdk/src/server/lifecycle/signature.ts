import { createHmac, timingSafeEqual } from 'node:crypto'

/** Header Tolgee puts `{"timestamp": …, "signature": "…"}` in. */
export const TOLGEE_SIGNATURE_HEADER = 'tolgee-signature'

/**
 * How far a delivery's timestamp may lie from this app's clock.
 *
 * Five minutes: wide enough to survive the queueing and retry backoff between
 * Tolgee deciding to deliver and this process reading the request, plus the
 * clock skew of two hosts that are only roughly NTP-synchronised; narrow enough
 * that a delivery captured off the wire stops being replayable within minutes
 * rather than for ever. It is the same window Stripe's webhook SDKs default to,
 * so it is a number operators already recognise.
 */
export const DEFAULT_SIGNATURE_TOLERANCE_MS = 5 * 60 * 1000

/** Contents of the signature header: epoch milliseconds and a hex HMAC. */
export type TolgeeSignatureEnvelope = {
  timestamp: number
  signature: string
}

export type SignatureFailure =
  | 'missing-signature'
  | 'malformed-signature'
  | 'bad-signature'
  | 'stale-timestamp'

export class TolgeeSignatureError extends Error {
  constructor(
    readonly failure: SignatureFailure,
    message: string
  ) {
    super(message)
    this.name = 'TolgeeSignatureError'
  }
}

/**
 * The signature Tolgee sends for `payload` at `timestamp` — HMAC-SHA256 over
 * `"$timestamp.$payload"`, hex encoded, exactly as `WebhookExecutor` computes
 * it on the server.
 */
export const computeTolgeeSignature = (
  secret: string,
  timestamp: number,
  payload: string
): string =>
  createHmac('sha256', secret).update(`${timestamp}.${payload}`).digest('hex')

/** Parses the signature header, throwing `TolgeeSignatureError` on anything unusable. */
export const parseSignatureHeader = (
  header: string | string[] | null | undefined
): TolgeeSignatureEnvelope => {
  const raw = Array.isArray(header) ? header[0] : header
  if (raw == null || raw.trim() === '') {
    throw new TolgeeSignatureError(
      'missing-signature',
      `No ${TOLGEE_SIGNATURE_HEADER} header — the request did not come from Tolgee.`
    )
  }

  let parsed: unknown
  try {
    parsed = JSON.parse(raw)
  } catch {
    throw new TolgeeSignatureError(
      'malformed-signature',
      `${TOLGEE_SIGNATURE_HEADER} is not the JSON object Tolgee sends.`
    )
  }
  if (typeof parsed !== 'object' || parsed === null) {
    throw new TolgeeSignatureError(
      'malformed-signature',
      `${TOLGEE_SIGNATURE_HEADER} is not the JSON object Tolgee sends.`
    )
  }

  const { timestamp, signature } = parsed as Record<string, unknown>
  if (
    typeof timestamp !== 'number' ||
    !Number.isFinite(timestamp) ||
    timestamp <= 0 ||
    typeof signature !== 'string' ||
    signature === ''
  ) {
    throw new TolgeeSignatureError(
      'malformed-signature',
      `${TOLGEE_SIGNATURE_HEADER} carries no usable timestamp and signature.`
    )
  }
  return { timestamp, signature }
}

export type VerifySignatureInput = {
  /** The request body exactly as it arrived — re-serialising changes the bytes signed. */
  payload: string
  header?: string | string[] | null
  envelope?: TolgeeSignatureEnvelope
  secret: string
  /** Defaults to `DEFAULT_SIGNATURE_TOLERANCE_MS`. */
  toleranceMs?: number
  /** Current epoch ms; injectable so tests need no clock. */
  now?: number
}

/**
 * Verifies a delivery against `secret` and returns its signature envelope,
 * throwing `TolgeeSignatureError` when the signature does not match or the
 * timestamp lies outside the tolerance in either direction.
 *
 * The future half of the window matters as much as the past one: without it a
 * captured delivery stamped far ahead would stay replayable until that time
 * arrives.
 */
export const verifyTolgeeSignature = (
  input: VerifySignatureInput
): TolgeeSignatureEnvelope => {
  const envelope = input.envelope ?? parseSignatureHeader(input.header)
  const expected = computeTolgeeSignature(
    input.secret,
    envelope.timestamp,
    input.payload
  )
  if (!equalsInConstantTime(expected, envelope.signature)) {
    throw new TolgeeSignatureError(
      'bad-signature',
      'Delivery signature does not match — it was not signed with the webhook secret this app holds.'
    )
  }

  const tolerance = input.toleranceMs ?? DEFAULT_SIGNATURE_TOLERANCE_MS
  const now = input.now ?? Date.now()
  if (Math.abs(now - envelope.timestamp) > tolerance) {
    throw new TolgeeSignatureError(
      'stale-timestamp',
      `Delivery is stamped ${Math.round(Math.abs(now - envelope.timestamp) / 1000)}s away from ` +
        `the clock of this app, outside the ${Math.round(tolerance / 1000)}s tolerance — ` +
        'replayed, or the two clocks disagree.'
    )
  }
  return envelope
}

const equalsInConstantTime = (expected: string, actual: string): boolean => {
  const a = Buffer.from(expected, 'utf8')
  const b = Buffer.from(actual, 'utf8')
  if (a.length !== b.length) return false
  return timingSafeEqual(a, b)
}
