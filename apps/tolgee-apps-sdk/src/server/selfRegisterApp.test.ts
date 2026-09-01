import assert from 'node:assert/strict'
import { mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, beforeEach, describe, it } from 'node:test'
import { readStoredApp, readStoredAppInstall, saveApp } from './installStore'
import {
  SelfRegisterError,
  selfRegisterApp,
  selfRegisterAppWithRetry,
} from './selfRegisterApp'

const TOLGEE_URL = 'http://localhost:8718'

let stateDir: string
const originalFetch = globalThis.fetch

beforeEach(() => {
  stateDir = mkdtempSync(join(tmpdir(), 'tolgee-app-register-'))
})

afterEach(() => {
  globalThis.fetch = originalFetch
  rmSync(stateDir, { recursive: true, force: true })
})

const stubTolgee = (body: unknown): void => {
  globalThis.fetch = (async () =>
    new Response(JSON.stringify(body), {
      status: 200,
      headers: { 'content-type': 'application/json' },
    })) as typeof fetch
}

const register = () =>
  selfRegisterApp({
    tolgeeUrl: TOLGEE_URL,
    registrationToken: 'tgreg_test-token',
    manifestUrl: 'http://localhost:5181/manifest.json',
    stateDir,
  })

describe('selfRegisterApp', () => {
  it('stores the app-level credentials the registering call discloses', async () => {
    stubTolgee({
      id: 7,
      created: true,
      app: {
        id: 3,
        appId: 'keys-showcase',
        clientId: 'tgpub_app',
        clientSecret: 'tgpubs_app',
        webhookSecret: 'whsec',
      },
    })

    const result = await register()

    assert.equal(result.app?.clientId, 'tgpub_app')
    const stored = readStoredApp(TOLGEE_URL, { stateDir })
    assert.equal(stored?.clientId, 'tgpub_app')
    assert.equal(stored?.clientSecret, 'tgpubs_app')
    assert.equal(stored?.webhookSecret, 'whsec')
    assert.equal(stored?.appId, 'keys-showcase')
    assert.equal(readStoredAppInstall(TOLGEE_URL, { stateDir })?.installId, 7)
  })

  it('keeps the app credentials it already holds when a later call discloses none', async () => {
    saveApp(
      {
        tolgeeUrl: TOLGEE_URL,
        id: 3,
        appId: 'keys-showcase',
        clientId: 'tgpub_app',
        clientSecret: 'tgpubs_app',
        webhookSecret: 'whsec',
      },
      { stateDir }
    )
    // Installing an app somebody already registered returns no app block.
    stubTolgee({ id: 8, created: true })

    const result = await register()

    assert.equal(result.app, null)
    const stored = readStoredApp(TOLGEE_URL, { stateDir })
    assert.equal(stored?.clientSecret, 'tgpubs_app')
    assert.equal(stored?.webhookSecret, 'whsec')
  })

  it('ignores an app block that carries no client id', async () => {
    stubTolgee({
      id: 9,
      created: true,
      app: { id: 3, appId: 'keys-showcase' },
    })

    const result = await register()

    assert.equal(result.app, null)
    assert.equal(readStoredApp(TOLGEE_URL, { stateDir }), null)
  })

  it('surfaces an unreachable server as a retryable SelfRegisterError', async () => {
    globalThis.fetch = (async () => {
      throw new Error('ECONNREFUSED')
    }) as typeof fetch

    await assert.rejects(register(), (error: unknown) => {
      assert.ok(error instanceof SelfRegisterError)
      assert.equal(error.status, null)
      return true
    })
  })
})

describe('selfRegisterAppWithRetry', () => {
  it('retries with backoff until the server accepts, then succeeds', async () => {
    let calls = 0
    globalThis.fetch = (async () => {
      calls += 1
      if (calls < 3) throw new Error('ECONNREFUSED')
      return new Response(JSON.stringify({ id: 7, created: true }), {
        status: 200,
        headers: { 'content-type': 'application/json' },
      })
    }) as typeof fetch

    const delays: number[] = []
    const result = await selfRegisterAppWithRetry(
      {
        tolgeeUrl: TOLGEE_URL,
        registrationToken: 'tgreg_test-token',
        manifestUrl: 'http://localhost:5181/manifest.json',
        persist: false,
      },
      {
        initialDelayMs: 10,
        onRetry: (_error, _attempt, nextDelayMs) => delays.push(nextDelayMs),
        sleep: async () => {},
      }
    )

    assert.equal(result.installId, 7)
    assert.equal(calls, 3)
    assert.deepEqual(delays, [10, 20])
  })

  it('gives up after maxAttempts', async () => {
    globalThis.fetch = (async () => {
      throw new Error('ECONNREFUSED')
    }) as typeof fetch

    await assert.rejects(
      selfRegisterAppWithRetry(
        {
          tolgeeUrl: TOLGEE_URL,
          registrationToken: 'tgreg_test-token',
          manifestUrl: 'http://localhost:5181/manifest.json',
          persist: false,
        },
        { maxAttempts: 2, initialDelayMs: 1, sleep: async () => {} }
      ),
      SelfRegisterError
    )
  })
})
