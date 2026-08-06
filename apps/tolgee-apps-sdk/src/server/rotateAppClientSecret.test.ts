import assert from 'node:assert/strict'
import { mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, beforeEach, describe, it } from 'node:test'
import { loadTolgeeAppConfig } from './config'
import { readStoredAppInstall, saveAppInstall } from './installStore'
import {
  ensureAppCredentialsFresh,
  rotateAppClientSecret,
} from './rotateAppClientSecret'

const TOLGEE_URL = 'http://localhost:8718'

let stateDir: string
const originalFetch = globalThis.fetch

beforeEach(() => {
  stateDir = mkdtempSync(join(tmpdir(), 'tolgee-app-rotate-'))
})

afterEach(() => {
  globalThis.fetch = originalFetch
  rmSync(stateDir, { recursive: true, force: true })
})

/** Answers both calls the rotation makes: the token exchange and the issue. */
const stubTolgee = (issued: unknown): string[] => {
  const calls: string[] = []
  globalThis.fetch = (async (input: RequestInfo | URL) => {
    const url = String(input)
    calls.push(url)
    const body = url.endsWith('/v2/public/apps/token')
      ? { access_token: 'install-token', expires_in: 300 }
      : issued
    return new Response(JSON.stringify(body), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    })
  }) as typeof fetch
  return calls
}

const storeCredentials = (secretIssuedAt?: string): void => {
  saveAppInstall(
    {
      tolgeeUrl: TOLGEE_URL,
      installId: 42,
      clientId: 'issued-id',
      clientSecret: 'old-secret',
      secretIssuedAt,
    },
    { stateDir }
  )
}

describe('rotateAppClientSecret', () => {
  it('replaces the stored secret with the newly issued one', async () => {
    storeCredentials()
    const calls = stubTolgee({ id: 7, secret: 'new-secret', prefix: 'tgapps_ab' })

    const result = await rotateAppClientSecret({
      tolgeeUrl: TOLGEE_URL,
      stateDir,
    })

    assert.equal(result.secretId, 7)
    assert.ok(calls.some((url) => url.endsWith('/v2/apps/self/secrets')))

    const config = loadTolgeeAppConfig(
      { TOLGEE_URL } as NodeJS.ProcessEnv,
      { stateDir }
    )
    assert.equal(config.clientSecret, 'new-secret')
    assert.equal(config.clientId, 'issued-id')
    assert.equal(config.installId, 42)
  })

  it('records when the new secret was issued', async () => {
    storeCredentials('2020-01-01T00:00:00.000Z')
    stubTolgee({ id: 7, secret: 'new-secret' })

    await rotateAppClientSecret({ tolgeeUrl: TOLGEE_URL, stateDir })

    const issuedAt = readStoredAppInstall(TOLGEE_URL, { stateDir })
      ?.secretIssuedAt
    assert.ok(issuedAt !== null && issuedAt !== undefined)
    assert.ok(Date.parse(issuedAt) > Date.parse('2020-01-01T00:00:00.000Z'))
  })

  it('keeps the working secret when Tolgee returns nothing usable', async () => {
    storeCredentials()
    stubTolgee({ id: 7 })

    await assert.rejects(
      rotateAppClientSecret({ tolgeeUrl: TOLGEE_URL, stateDir })
    )
    assert.equal(
      readStoredAppInstall(TOLGEE_URL, { stateDir })?.clientSecret,
      'old-secret'
    )
  })

  it('refuses to rotate credentials the environment supplies', async () => {
    storeCredentials()
    stubTolgee({ id: 7, secret: 'new-secret' })
    process.env.TOLGEE_APP_CLIENT_ID = 'env-id'
    process.env.TOLGEE_APP_CLIENT_SECRET = 'env-secret'

    try {
      await assert.rejects(
        rotateAppClientSecret({ tolgeeUrl: TOLGEE_URL, stateDir }),
        /TOLGEE_APP_CLIENT_ID/
      )
    } finally {
      delete process.env.TOLGEE_APP_CLIENT_ID
      delete process.env.TOLGEE_APP_CLIENT_SECRET
    }
  })
})

describe('ensureAppCredentialsFresh', () => {
  it('rotates a secret older than the age limit', async () => {
    storeCredentials('2020-01-01T00:00:00.000Z')
    stubTolgee({ id: 7, secret: 'new-secret' })

    const result = await ensureAppCredentialsFresh({
      tolgeeUrl: TOLGEE_URL,
      stateDir,
    })

    assert.equal(result.rotated, true)
    assert.equal(
      readStoredAppInstall(TOLGEE_URL, { stateDir })?.clientSecret,
      'new-secret'
    )
  })

  it('leaves a recently issued secret alone', async () => {
    storeCredentials(new Date().toISOString())
    stubTolgee({ id: 7, secret: 'new-secret' })

    const result = await ensureAppCredentialsFresh({
      tolgeeUrl: TOLGEE_URL,
      stateDir,
    })

    assert.equal(result.rotated, false)
    assert.equal(result.reason, 'fresh')
    assert.equal(
      readStoredAppInstall(TOLGEE_URL, { stateDir })?.clientSecret,
      'old-secret'
    )
  })

  it('reports rather than throws when there is nothing stored', async () => {
    const result = await ensureAppCredentialsFresh({
      tolgeeUrl: TOLGEE_URL,
      stateDir,
    })

    assert.equal(result.rotated, false)
    assert.equal(result.reason, 'no-stored-credentials')
  })
})
