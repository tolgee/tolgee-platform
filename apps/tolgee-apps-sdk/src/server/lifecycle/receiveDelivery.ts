import { normalizeTolgeeUrl } from '../../shared/url'
import { DEFAULT_TOLGEE_URL } from '../config'
import {
  forgetAppInstall,
  forgetTolgeeInstance,
  hasStoredCredentials,
  readStoredApp,
  saveApp,
  saveAppInstall,
  type AppInstallStoreOptions,
} from '../installStore'
import type { TolgeeLifecycleEvent } from './events'
import { parseDelivery, type ParsedDelivery } from './parseDelivery'
import {
  DEFAULT_SIGNATURE_TOLERANCE_MS,
  TolgeeSignatureError,
  verifyTolgeeSignature,
  type TolgeeSignatureEnvelope,
} from './signature'

export type DeliveryRejection =
  | 'missing-signature'
  | 'malformed-signature'
  | 'bad-signature'
  | 'stale-timestamp'
  | 'replayed'
  | 'unreadable-body'
  | 'unknown-event'
  | 'unverifiable'
  | 'unverified-credentials'
  | 'rate-limited'
  | 'credentials-already-held'

/** Called for a delivery that verified; throwing makes Tolgee retry it. */
export type TolgeeLifecycleListener = (
  event: TolgeeLifecycleEvent
) => void | Promise<void>

export type TolgeeLifecycleListeners = {
  registered?: TolgeeLifecycleListener
  installed?: TolgeeLifecycleListener
  uninstalled?: TolgeeLifecycleListener
  secretRotated?: TolgeeLifecycleListener
  /** Runs for every accepted delivery, after the type-specific listener. */
  any?: TolgeeLifecycleListener
}

export type TolgeeLifecycleOptions = AppInstallStoreOptions & {
  /**
   * Tolgee instance whose deliveries these are, and the key credentials are
   * stored under. Defaults to `TOLGEE_URL`.
   */
  tolgeeUrl?: string
  /**
   * Verify against this secret instead of the stored one — the way an operator
   * hands a deployment the webhook secret shown at registration, and the way
   * out of a refused first delivery. Falls back to `TOLGEE_APP_WEBHOOK_SECRET`.
   */
  webhookSecret?: string | null
  /** How far a delivery's timestamp may lie from this clock; defaults to 5 minutes. */
  toleranceMs?: number
  /**
   * Refuse every delivery this app cannot check against a secret it already
   * holds, instead of trusting a first one. For a deployment whose webhook
   * secret is injected — there is no first delivery to trust.
   */
  requireKnownSecret?: boolean
  /**
   * On a first delivery (no secret held yet), confirm the delivered app
   * credentials against the configured Tolgee before trusting them. Defaults to
   * true. Set false to fall back to trust-on-first-use — accept the self-signed
   * delivery as-is, marked `trusted: false`.
   */
  verifyCredentials?: boolean
  /** Set false to dispatch without writing anything to the state file. */
  persist?: boolean
  on?: TolgeeLifecycleListeners
  /** Current epoch ms; injectable so tests need no clock. */
  now?: () => number
  /**
   * Signatures already accepted, with the epoch ms they stop being replayable
   * at. Defaults to one cache per process; pass your own to isolate several
   * receivers (or a test) from each other.
   */
  seenSignatures?: Map<string, number>
}

export type DeliveryInput = TolgeeLifecycleOptions & {
  /** The body exactly as it arrived — re-serialising it changes the signed bytes. */
  rawBody: string
  /** Contents of the `Tolgee-Signature` header. */
  signatureHeader: string | string[] | null | undefined
}

export type DeliveryResult =
  | { accepted: true; status: 200; event: TolgeeLifecycleEvent }
  | {
      accepted: false
      status: number
      rejection: DeliveryRejection
      /** Safe to log and to return: it never names a secret. */
      message: string
    }

const REJECTION_STATUS: Record<DeliveryRejection, number> = {
  'missing-signature': 401,
  'malformed-signature': 401,
  'bad-signature': 401,
  'stale-timestamp': 401,
  replayed: 401,
  'unreadable-body': 400,
  'unknown-event': 400,
  unverifiable: 401,
  'unverified-credentials': 401,
  'rate-limited': 429,
  'credentials-already-held': 409,
}

/** How many first-contact verifications the SDK makes to one Tolgee per window. */
const VERIFY_LIMIT = 10
const VERIFY_WINDOW_MS = 60_000
const verifyTimestamps = new Map<string, number[]>()

/**
 * A first delivery signs with the secret it carries, which proves nothing, so the SDK confirms the
 * delivered credentials against the **configured** Tolgee before trusting them — the URL is always
 * the one this app was configured with, never anything from the payload. The call is rate-limited so
 * a flood of forged first deliveries cannot turn this app into an outbound amplifier against Tolgee.
 */
const allowVerify = (
  tolgeeUrl: string,
  now: number
): boolean => {
  const recent = (verifyTimestamps.get(tolgeeUrl) ?? []).filter(
    (t) => t > now - VERIFY_WINDOW_MS
  )
  if (recent.length >= VERIFY_LIMIT) {
    verifyTimestamps.set(tolgeeUrl, recent)
    return false
  }
  recent.push(now)
  verifyTimestamps.set(tolgeeUrl, recent)
  return true
}

const verifyDeliveredCredentials = async (
  tolgeeUrl: string,
  clientId: string,
  clientSecret: string
): Promise<boolean> => {
  try {
    const response = await fetch(`${tolgeeUrl}/v2/public/apps/app-secrets/list`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ client_id: clientId, client_secret: clientSecret }),
    })
    return response.ok
  } catch {
    return false
  }
}

const processSeenSignatures = new Map<string, number>()

/**
 * Verifies one lifecycle delivery, stores the credentials it carries and hands
 * it to the listeners — the whole receiver, without any HTTP framework in it.
 *
 * **How a delivery is trusted.** Tolgee signs the body with the app's webhook
 * secret, which it discloses exactly once: in the `app.registered` delivery
 * itself. So there are two cases, and they are not the same strength:
 *
 * - This app already holds a webhook secret (stored, or injected through
 *   `webhookSecret` / `TOLGEE_APP_WEBHOOK_SECRET`). The signature then proves
 *   the delivery came from Tolgee, and a rotation may replace anything held.
 * - It holds none. The only key available is the one inside the body, which
 *   proves nothing but its own integrity — anybody can sign with a secret they
 *   invented. That is accepted only while the app holds no credentials for the
 *   instance at all, and refused the moment it does, so a stranger cannot
 *   overwrite a live install by posting a self-signed "you were just
 *   registered".
 *
 * Returns a rejection rather than throwing for anything the caller should turn
 * into a status code. It throws only when a listener does — that is a real
 * failure, and Tolgee retrying is the right answer.
 */
export const receiveTolgeeDelivery = async (
  input: DeliveryInput
): Promise<DeliveryResult> => {
  const tolgeeUrl = normalizeTolgeeUrl(
    input.tolgeeUrl ?? process.env.TOLGEE_URL ?? DEFAULT_TOLGEE_URL
  )
  const storeOptions: AppInstallStoreOptions = input.stateDir
    ? { stateDir: input.stateDir }
    : {}

  const parsed = parseDelivery(input.rawBody)
  if (parsed === null) {
    return reject(
      'unreadable-body',
      'Delivery body is not a JSON object naming a lifecycle event.'
    )
  }

  const verified = await verify(input, parsed, tolgeeUrl, storeOptions)
  if ('rejection' in verified) return verified

  const seen = input.seenSignatures ?? processSeenSignatures
  const now = input.now?.() ?? Date.now()
  const tolerance = input.toleranceMs ?? DEFAULT_SIGNATURE_TOLERANCE_MS
  if (!remember(seen, `${tolgeeUrl}\n${verified.envelope.signature}`, now, tolerance)) {
    return reject(
      'replayed',
      'This exact delivery was already accepted — ignoring the replay.'
    )
  }

  const event: TolgeeLifecycleEvent = {
    type: parsed.type,
    deliveryId: parsed.deliveryId,
    timestamp: verified.envelope.timestamp,
    tolgeeUrl,
    app: parsed.app,
    install: parsed.install,
    organization: parsed.organization,
    rotatedLayer: parsed.rotatedLayer,
    trusted: verified.trusted,
    payload: parsed.payload,
  }

  if (input.persist !== false) persist(event, storeOptions)
  await dispatch(event, input.on)
  return { accepted: true, status: 200, event }
}

type Verified = { envelope: TolgeeSignatureEnvelope; trusted: boolean }

const verify = async (
  input: DeliveryInput,
  parsed: ParsedDelivery,
  tolgeeUrl: string,
  storeOptions: AppInstallStoreOptions
): Promise<Verified | Extract<DeliveryResult, { accepted: false }>> => {
  const known =
    input.webhookSecret ??
    process.env.TOLGEE_APP_WEBHOOK_SECRET ??
    readStoredApp(tolgeeUrl, storeOptions)?.webhookSecret ??
    null

  const check = (secret: string): TolgeeSignatureEnvelope =>
    verifyTolgeeSignature({
      payload: input.rawBody,
      header: input.signatureHeader,
      secret,
      toleranceMs: input.toleranceMs,
      now: input.now?.(),
    })

  if (known !== null) {
    try {
      return { envelope: check(known), trusted: true }
    } catch (error) {
      return rejectSignature(error)
    }
  }

  if (input.requireKnownSecret === true) {
    return reject(
      'unverifiable',
      'This app holds no webhook secret for this Tolgee instance and is configured to trust no ' +
        'first delivery. Set TOLGEE_APP_WEBHOOK_SECRET to the secret shown when the app was ' +
        'registered.'
    )
  }

  const carried = parsed.app?.webhookSecret ?? null
  if (carried === null) {
    return reject(
      'unverifiable',
      'This app holds no webhook secret for this Tolgee instance and the delivery carries none, ' +
        'so nothing about it can be checked.'
    )
  }

  let envelope: TolgeeSignatureEnvelope
  try {
    envelope = check(carried)
  } catch (error) {
    return rejectSignature(error)
  }

  // The signature was made with a key the body handed over, so it proves only
  // that the body is internally consistent. Never overwrite live credentials on
  // the strength of that.
  if (hasStoredCredentials(tolgeeUrl, storeOptions)) {
    return reject(
      'credentials-already-held',
      `This app already holds credentials for ${tolgeeUrl}, so a delivery that brings its own ` +
        'webhook secret cannot be trusted to replace them. If this is a legitimate ' +
        're-registration, set TOLGEE_APP_WEBHOOK_SECRET to the secret Tolgee showed, or clear the ' +
        'stored state deliberately.'
    )
  }

  // Verify by use: confirm the delivered credentials against the CONFIGURED Tolgee before trusting
  // them. Skipping the check falls back to trust-on-first-use — accepting the self-signed delivery
  // as-is, marked untrusted.
  if (input.verifyCredentials === false) {
    return { envelope, trusted: false }
  }

  const clientId = parsed.app?.clientId
  const clientSecret = parsed.app?.clientSecret
  if (!clientId || !clientSecret) {
    return reject(
      'unverifiable',
      'This app holds no credentials for this Tolgee instance and the first delivery carries no ' +
        'app credentials to verify against it.'
    )
  }

  const now = input.now?.() ?? Date.now()
  if (!allowVerify(tolgeeUrl, now)) {
    return reject(
      'rate-limited',
      `Too many unverified first deliveries for ${tolgeeUrl} — refusing to check more this minute.`
    )
  }

  if (!(await verifyDeliveredCredentials(tolgeeUrl, clientId, clientSecret))) {
    return reject(
      'unverified-credentials',
      `${tolgeeUrl} did not accept the credentials this delivery carried, so it did not come from ` +
        'Tolgee. Ignoring it.'
    )
  }

  // Tolgee vouched for the credentials, so this first delivery is genuinely trusted.
  return { envelope, trusted: true }
}

const rejectSignature = (
  error: unknown
): Extract<DeliveryResult, { accepted: false }> => {
  if (error instanceof TolgeeSignatureError) {
    return reject(error.failure, error.message)
  }
  throw error
}

const reject = (
  rejection: DeliveryRejection,
  message: string
): Extract<DeliveryResult, { accepted: false }> => ({
  accepted: false,
  status: REJECTION_STATUS[rejection],
  rejection,
  message,
})

/**
 * Records a signature as used and reports whether it was new. Signatures older
 * than the tolerance are dropped: a delivery that old is rejected on its
 * timestamp anyway, so the cache never has to grow past one window.
 */
const remember = (
  seen: Map<string, number>,
  key: string,
  now: number,
  toleranceMs: number
): boolean => {
  for (const [entry, expiresAt] of seen) {
    if (expiresAt <= now) seen.delete(entry)
  }
  if (seen.has(key)) return false
  seen.set(key, now + toleranceMs)
  return true
}

const persist = (
  event: TolgeeLifecycleEvent,
  storeOptions: AppInstallStoreOptions
): void => {
  const { tolgeeUrl } = event

  if (event.type === 'app.uninstalled') {
    const installId = event.install?.installId
    if (installId != null) {
      forgetAppInstall(tolgeeUrl, installId, storeOptions)
      return
    }
    forgetTolgeeInstance(tolgeeUrl, storeOptions)
    return
  }

  if (event.app !== null) {
    saveApp(
      {
        tolgeeUrl,
        id: event.app.id,
        appId: event.app.appId,
        clientId: event.app.clientId,
        clientSecret: event.app.clientSecret,
        webhookSecret: event.app.webhookSecret,
      },
      storeOptions
    )
  }

  const installId = event.install?.installId
  if (event.install !== null && installId != null) {
    const organization = event.install.organization ?? event.organization
    saveAppInstall(
      {
        tolgeeUrl,
        installId,
        native: event.install.native,
        organizationId: organization?.id ?? null,
        organizationSlug: organization?.slug ?? null,
        organizationName: organization?.name ?? null,
        // An install another organization made must never displace the one this
        // app authenticates as; the store still adopts it when it has none.
        makeCurrent: false,
      },
      storeOptions
    )
  }
}

const dispatch = async (
  event: TolgeeLifecycleEvent,
  listeners: TolgeeLifecycleListeners | undefined
): Promise<void> => {
  if (!listeners) return
  const specific = {
    'app.registered': listeners.registered,
    'app.installed': listeners.installed,
    'app.uninstalled': listeners.uninstalled,
    'app.secret.rotated': listeners.secretRotated,
  }[event.type]
  await specific?.(event)
  await listeners.any?.(event)
}
