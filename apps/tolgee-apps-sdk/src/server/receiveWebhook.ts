import type { AppWebhookPayload } from '../shared/webhookTypes'
import { verifyWebhookSignature } from './verifyWebhookSignature'

export type ReceiveWebhookInput = {
  /** The exact bytes Tolgee signed — do NOT pass a re-serialized object. */
  rawBody: string
  /** Value of the `Tolgee-Signature` header. */
  signatureHeader: string | null | undefined
  /**
   * Per-install webhook secret. When null/undefined, signature verification
   * is skipped — only acceptable in local development.
   */
  secret: string | null | undefined
}

export type ReceiveWebhookResult =
  | { ok: true; payload: AppWebhookPayload }
  | { ok: false; status: 401 | 400; error: string }

/**
 * Framework-agnostic webhook intake: verifies the `Tolgee-Signature` over the
 * raw body and parses it into a typed payload. Returns a result the caller maps
 * to its HTTP framework; on success, dispatch with `onWebhook`.
 *
 *     const r = await receiveWebhook({ rawBody, signatureHeader, secret })
 *     if (!r.ok) return res.status(r.status).json({ error: r.error })
 *     onWebhook(r.payload, 'SET_TRANSLATIONS', (p) => { ... })
 */
export async function receiveWebhook(
  input: ReceiveWebhookInput
): Promise<ReceiveWebhookResult> {
  if (input.secret) {
    const valid = await verifyWebhookSignature({
      header: input.signatureHeader,
      rawBody: input.rawBody,
      secret: input.secret,
    })
    if (!valid) return { ok: false, status: 401, error: 'invalid signature' }
  }

  try {
    return { ok: true, payload: JSON.parse(input.rawBody) as AppWebhookPayload }
  } catch {
    return { ok: false, status: 400, error: 'invalid json' }
  }
}
