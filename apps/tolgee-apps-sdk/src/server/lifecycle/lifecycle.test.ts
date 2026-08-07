import assert from 'node:assert/strict'
import { mkdtempSync, rmSync } from 'node:fs'
import type { IncomingMessage, ServerResponse } from 'node:http'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { Readable } from 'node:stream'
import { afterEach, beforeEach, describe, it } from 'node:test'
import {
  readStoredApp,
  readStoredAppInstall,
  readStoredAppInstallById,
  readStoredAppInstalls,
  saveApp,
  saveAppInstall,
} from '../installStore'
import type { TolgeeLifecycleEvent } from './events'
import {
  createTolgeeLifecycleHandler,
  mountTolgeeLifecycle,
  TOLGEE_LIFECYCLE_PATHS,
} from './httpHandler'
import {
  receiveTolgeeDelivery,
  type DeliveryResult,
  type TolgeeLifecycleOptions,
} from './receiveDelivery'
import { computeTolgeeSignature, verifyTolgeeSignature } from './signature'

const TOLGEE_URL = 'http://localhost:8718'
const WEBHOOK_SECRET = 'tolgee-webhook-secret'
const NOW = 1_750_000_000_000

let stateDir: string
let seenSignatures: Map<string, number>
let envWebhookSecret: string | undefined

beforeEach(() => {
  stateDir = mkdtempSync(join(tmpdir(), 'tolgee-app-lifecycle-'))
  seenSignatures = new Map()
  envWebhookSecret = process.env.TOLGEE_APP_WEBHOOK_SECRET
  delete process.env.TOLGEE_APP_WEBHOOK_SECRET
})

afterEach(() => {
  rmSync(stateDir, { recursive: true, force: true })
  if (envWebhookSecret === undefined) {
    delete process.env.TOLGEE_APP_WEBHOOK_SECRET
    return
  }
  process.env.TOLGEE_APP_WEBHOOK_SECRET = envWebhookSecret
})

/** Signs exactly as `WebhookExecutor.generateSigHeader` does on the server. */
const sign = (
  payload: string,
  secret = WEBHOOK_SECRET,
  timestamp = NOW
): string =>
  `{"timestamp": ${timestamp}, "signature": "${computeTolgeeSignature(secret, timestamp, payload)}"}`

const deliver = async (
  payload: unknown,
  options: TolgeeLifecycleOptions & {
    secret?: string
    timestamp?: number
    signatureHeader?: string
  } = {}
): Promise<DeliveryResult> => {
  const {
    secret = WEBHOOK_SECRET,
    timestamp = NOW,
    signatureHeader,
    ...rest
  } = options
  const rawBody = JSON.stringify(payload)
  return receiveTolgeeDelivery({
    tolgeeUrl: TOLGEE_URL,
    stateDir,
    seenSignatures,
    now: () => NOW,
    ...rest,
    rawBody,
    signatureHeader: signatureHeader ?? sign(rawBody, secret, timestamp),
  })
}

/** The body `AppLifecyclePayload` serializes, field for field. */
const registered = (overrides: Record<string, unknown> = {}) => ({
  eventType: 'app.registered',
  deliveryId: 41,
  appId: 'my-company.glossary',
  tolgeeInstanceUrl: TOLGEE_URL,
  app: {
    clientId: 'tgpub_app-client',
    clientSecret: 'tgpubs_app-secret',
    webhookSecret: WEBHOOK_SECRET,
  },
  install: {
    id: 7,
    scopes: ['keys.view'],
  },
  organization: { id: 11, name: 'Acme', slug: 'acme' },
  ...overrides,
})

const assertRejected = (
  result: DeliveryResult
): Extract<DeliveryResult, { accepted: false }> => {
  assert.equal(result.accepted, false)
  if (result.accepted) throw new Error('unreachable')
  return result
}

const assertAccepted = (result: DeliveryResult): TolgeeLifecycleEvent => {
  assert.equal(
    result.accepted,
    true,
    result.accepted ? '' : `rejected: ${result.message}`
  )
  if (!result.accepted) throw new Error('unreachable')
  return result.event
}

/**
 * Golden vector over the exact body `AppLifecyclePayload` serializes, using the
 * scheme cross-checked against the JVM implementation the server signs with
 * (`HMAC-SHA256` over `"$timestamp.$payload"`, wrapped the way
 * `WebhookSigner.signatureHeader` does). Neither the serializer nor the scheme
 * changes without this failing.
 */
const GOLDEN_SIGNATURE =
  '3afdc1203505504d0073c7a7be845c2fa80f6cbe80e92ec99f754f255b6075e8'
const GOLDEN_HEADER = `{"timestamp": 1750000000000, "signature": "${GOLDEN_SIGNATURE}"}`

describe('signature', () => {
  it('matches the signature the server computes', () => {
    const payload = JSON.stringify(registered())

    assert.equal(
      computeTolgeeSignature(WEBHOOK_SECRET, NOW, payload),
      GOLDEN_SIGNATURE
    )
    assert.deepEqual(
      verifyTolgeeSignature({
        payload,
        header: GOLDEN_HEADER,
        secret: WEBHOOK_SECRET,
        now: NOW,
      }),
      { timestamp: NOW, signature: GOLDEN_SIGNATURE }
    )
  })

  it('accepts a delivery carrying that very header', async () => {
    const event = assertAccepted(
      await deliver(registered(), { signatureHeader: GOLDEN_HEADER })
    )

    assert.equal(event.type, 'app.registered')
    assert.equal(event.deliveryId, 41)
    assert.equal(event.organization?.slug, 'acme')
  })
})

describe('first delivery', () => {
  it('stores both credential layers and reports itself untrusted', async () => {
    const event = assertAccepted(await deliver(registered()))

    assert.equal(event.type, 'app.registered')
    assert.equal(event.trusted, false)
    assert.equal(event.timestamp, NOW)

    const app = readStoredApp(TOLGEE_URL, { stateDir })
    assert.equal(app?.appId, 'my-company.glossary')
    assert.equal(app?.clientId, 'tgpub_app-client')
    assert.equal(app?.clientSecret, 'tgpubs_app-secret')
    assert.equal(app?.webhookSecret, WEBHOOK_SECRET)

    const install = readStoredAppInstallById(TOLGEE_URL, 7, { stateDir })
    assert.equal(install?.organizationSlug, 'acme')
    assert.equal(readStoredAppInstall(TOLGEE_URL, { stateDir })?.installId, 7)
  })

  it('is refused when credentials for that instance already exist', async () => {
    saveApp(
      {
        tolgeeUrl: TOLGEE_URL,
        clientId: 'tgpub_app-client',
        clientSecret: 'tgpubs_the-real-secret',
      },
      { stateDir }
    )

    // Signed with the secret it carries, so it is internally consistent — which
    // is exactly all a stranger can manage, and exactly why it is not enough.
    const rejected = assertRejected(
      await deliver(
        registered({
          app: {
            clientId: 'tgpub_hijacked',
            clientSecret: 'tgpubs_hijacked',
            webhookSecret: 'attacker-invented-secret',
          },
        }),
        { secret: 'attacker-invented-secret' }
      )
    )

    assert.equal(rejected.rejection, 'credentials-already-held')
    assert.equal(rejected.status, 409)
    assert.ok(!rejected.message.includes('the-real-secret'))
    assert.equal(
      readStoredApp(TOLGEE_URL, { stateDir })?.clientSecret,
      'tgpubs_the-real-secret'
    )
  })

  it('is refused outright when the app is told to trust no first delivery', async () => {
    const rejected = assertRejected(
      await deliver(registered(), { requireKnownSecret: true })
    )

    assert.equal(rejected.rejection, 'unverifiable')
    assert.equal(readStoredApp(TOLGEE_URL, { stateDir }), null)
  })

  it('is refused when nothing in it can be checked', async () => {
    const rejected = assertRejected(
      await deliver({ eventType: 'app.installed', install: { id: 7 } })
    )

    assert.equal(rejected.rejection, 'unverifiable')
  })
})

describe('a delivery signed with the held secret', () => {
  beforeEach(async () => {
    assertAccepted(await deliver(registered()))
  })

  it('is rejected when signed with anything else', async () => {
    const rejected = assertRejected(
      await deliver(
        registered({
          app: {
            clientId: 'tgpub_hijacked',
            clientSecret: 'tgpubs_hijacked',
            webhookSecret: 'attacker-invented-secret',
          },
        }),
        { secret: 'attacker-invented-secret' }
      )
    )

    assert.equal(rejected.rejection, 'bad-signature')
    assert.equal(rejected.status, 401)
    assert.equal(
      readStoredApp(TOLGEE_URL, { stateDir })?.webhookSecret,
      WEBHOOK_SECRET
    )
  })

  it('replaces the app secret and the signing secret on an app-level rotation', async () => {
    assertAccepted(
      await deliver({
        eventType: 'app.secret_rotated',
        app: {
          clientId: 'tgpub_app-client',
          clientSecret: 'tgpubs_rotated',
          webhookSecret: 'rotated-webhook-secret',
        },
      })
    )

    const app = readStoredApp(TOLGEE_URL, { stateDir })
    assert.equal(app?.clientSecret, 'tgpubs_rotated')
    assert.equal(app?.webhookSecret, 'rotated-webhook-secret')

    // The rotated signing secret is the one the next delivery is checked against.
    assertRejected(
      await deliver(
        { eventType: 'app.uninstalled', install: { id: 7 } },
        { timestamp: NOW + 1 }
      )
    )
    assertAccepted(
      await deliver(
        { eventType: 'app.uninstalled', install: { id: 7 } },
        { secret: 'rotated-webhook-secret', timestamp: NOW + 2 }
      )
    )
  })

  it('adds another organization install without displacing the current one', async () => {
    assertAccepted(
      await deliver({
        eventType: 'app.installed',
        install: {
          id: 9,
          clientId: 'tgapp_other',
          clientSecret: 'tgapps_other',
          organization: { id: 12, name: 'Globex', slug: 'globex' },
        },
      })
    )

    assert.equal(readStoredAppInstalls(TOLGEE_URL, { stateDir }).length, 2)
    assert.equal(readStoredAppInstall(TOLGEE_URL, { stateDir })?.installId, 7)
    assert.equal(
      readStoredAppInstallById(TOLGEE_URL, 9, { stateDir })?.organizationName,
      'Globex'
    )
  })

  it('drops the install it uninstalls, and only that one', async () => {
    assertAccepted(
      await deliver({
        eventType: 'app.installed',
        install: { id: 9, clientSecret: 'tgapps_other' },
      })
    )
    assertAccepted(
      await deliver(
        { eventType: 'app.uninstalled', install: { id: 9 } },
        { timestamp: NOW + 1 }
      )
    )

    assert.equal(readStoredAppInstallById(TOLGEE_URL, 9, { stateDir }), null)
    assert.equal(
      readStoredAppInstallById(TOLGEE_URL, 7, { stateDir })?.installId,
      7
    )
  })

  it('rejects a stale timestamp', async () => {
    const rejected = assertRejected(
      await deliver(
        { eventType: 'app.installed', install: { id: 9 } },
        { timestamp: NOW - 6 * 60 * 1000 }
      )
    )

    assert.equal(rejected.rejection, 'stale-timestamp')
    assert.equal(readStoredAppInstallById(TOLGEE_URL, 9, { stateDir }), null)
  })

  it('rejects a timestamp from the future too', async () => {
    const rejected = assertRejected(
      await deliver(
        { eventType: 'app.installed', install: { id: 9 } },
        { timestamp: NOW + 6 * 60 * 1000 }
      )
    )

    assert.equal(rejected.rejection, 'stale-timestamp')
  })

  it('rejects the same delivery replayed inside the tolerance', async () => {
    const payload = { eventType: 'app.installed', install: { id: 9 } }
    assertAccepted(await deliver(payload))

    const rejected = assertRejected(await deliver(payload))
    assert.equal(rejected.rejection, 'replayed')
  })

  it('calls the listener for the event and the catch-all', async () => {
    const seen: string[] = []
    assertAccepted(
      await deliver(
        { eventType: 'app.installed', install: { id: 9 } },
        {
          on: {
            installed: (event) => {
              seen.push(`installed:${event.install?.installId}`)
            },
            uninstalled: () => {
              seen.push('uninstalled')
            },
            any: (event) => {
              seen.push(`any:${event.type}`)
            },
          },
        }
      )
    )

    assert.deepEqual(seen, ['installed:9', 'any:app.installed'])
  })

  it('lets a failing listener through so Tolgee retries', async () => {
    await assert.rejects(
      deliver(
        { eventType: 'app.installed', install: { id: 9 } },
        {
          on: {
            installed: () => {
              throw new Error('downstream is down')
            },
          },
        }
      ),
      /downstream is down/
    )
  })
})

describe('the HTTP handler', () => {
  type FakeResponse = {
    statusCode: number
    headers: Record<string, string>
    body: string
  }

  const post = async (
    rawBody: string,
    signatureHeader: string,
    options: TolgeeLifecycleOptions = {}
  ): Promise<FakeResponse> => {
    const request = Object.assign(Readable.from([rawBody]), {
      method: 'POST',
      headers: { 'tolgee-signature': signatureHeader },
    }) as unknown as IncomingMessage

    const captured: FakeResponse = { statusCode: 0, headers: {}, body: '' }
    const finished = new Promise<void>((resolve) => {
      const response = {
        set statusCode(value: number) {
          captured.statusCode = value
        },
        get statusCode() {
          return captured.statusCode
        },
        setHeader(name: string, value: string) {
          captured.headers[name] = value
        },
        end(body: string) {
          captured.body = body
          resolve()
        },
      } as unknown as ServerResponse

      createTolgeeLifecycleHandler({
        tolgeeUrl: TOLGEE_URL,
        stateDir,
        seenSignatures,
        now: () => NOW,
        ...options,
      })(request, response)
    })

    await finished
    return captured
  }

  it('answers 200 and stores the credentials of a good delivery', async () => {
    const rawBody = JSON.stringify(registered())

    const response = await post(rawBody, sign(rawBody))

    assert.equal(response.statusCode, 200)
    assert.deepEqual(JSON.parse(response.body), {
      received: true,
      event: 'app.registered',
    })
    assert.equal(
      readStoredApp(TOLGEE_URL, { stateDir })?.clientId,
      'tgpub_app-client'
    )
  })

  it('answers 401 without echoing anything secret', async () => {
    const rawBody = JSON.stringify(registered())

    const response = await post(rawBody, sign(rawBody, 'wrong-secret'))

    assert.equal(response.statusCode, 401)
    assert.equal(JSON.parse(response.body).rejection, 'bad-signature')
    assert.ok(!response.body.includes('tgpubs_'))
    assert.ok(!response.body.includes(WEBHOOK_SECRET))
  })

  it('refuses to guess at a body some parser already ate', async () => {
    const request = Object.assign(Readable.from(['']), {
      method: 'POST',
      headers: {},
      body: { eventType: 'app.registered' },
    }) as unknown as IncomingMessage
    let status = 0
    let body = ''
    await new Promise<void>((resolve) => {
      const response = {
        set statusCode(value: number) {
          status = value
        },
        get statusCode() {
          return status
        },
        setHeader() {},
        end(chunk: string) {
          body = chunk
          resolve()
        },
      } as unknown as ServerResponse
      createTolgeeLifecycleHandler({ tolgeeUrl: TOLGEE_URL, stateDir })(
        request,
        response
      )
    })

    assert.equal(status, 500)
    assert.match(JSON.parse(body).error, /before express\.json\(\)/)
  })

  it('mounts on every path a delivery could arrive at', () => {
    const mounted: string[] = []
    const paths = mountTolgeeLifecycle({
      post: (path) => mounted.push(path),
    })

    assert.deepEqual(paths, TOLGEE_LIFECYCLE_PATHS)
    assert.deepEqual(mounted, TOLGEE_LIFECYCLE_PATHS)
  })
})
