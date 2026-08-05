import assert from 'node:assert/strict'
import { mkdtempSync, readFileSync, rmSync, statSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, beforeEach, describe, it } from 'node:test'
import { loadTolgeeAppConfig } from './config'
import {
  appInstallStatePath,
  readStoredAppInstall,
  saveAppInstall,
} from './installStore'
import { selfRegisterApp } from './selfRegisterApp'

const TOLGEE_URL = 'http://localhost:8718'

let stateDir: string

beforeEach(() => {
  stateDir = mkdtempSync(join(tmpdir(), 'tolgee-app-store-'))
})

afterEach(() => {
  rmSync(stateDir, { recursive: true, force: true })
})

describe('installStore', () => {
  it('round-trips an install and keeps the file owner-only', () => {
    saveAppInstall(
      {
        tolgeeUrl: TOLGEE_URL,
        installId: 12,
        clientId: 'client-id',
        clientSecret: 'client-secret',
        native: true,
      },
      { stateDir }
    )

    const stored = readStoredAppInstall(TOLGEE_URL, { stateDir })
    assert.equal(stored?.installId, 12)
    assert.equal(stored?.clientId, 'client-id')
    assert.equal(stored?.clientSecret, 'client-secret')
    assert.equal(stored?.native, true)

    const mode = statSync(appInstallStatePath({ stateDir })).mode & 0o777
    assert.equal(mode, 0o600)
  })

  it('keeps the stored secret when a re-registration returns none', () => {
    saveAppInstall(
      {
        tolgeeUrl: TOLGEE_URL,
        installId: 12,
        clientId: 'client-id',
        clientSecret: 'client-secret',
      },
      { stateDir }
    )
    saveAppInstall(
      { tolgeeUrl: TOLGEE_URL, installId: 12, clientSecret: null },
      { stateDir }
    )

    assert.equal(
      readStoredAppInstall(TOLGEE_URL, { stateDir })?.clientSecret,
      'client-secret'
    )
  })

  it('reads back a url that differs only in trailing slash or whitespace', () => {
    saveAppInstall(
      { tolgeeUrl: `${TOLGEE_URL}/`, installId: 3, clientSecret: 'secret' },
      { stateDir }
    )

    assert.equal(
      readStoredAppInstall(` ${TOLGEE_URL}`, { stateDir })?.clientSecret,
      'secret'
    )
  })

  it('keeps the credentials of two instances apart', () => {
    saveAppInstall(
      { tolgeeUrl: TOLGEE_URL, installId: 1, clientSecret: 'local' },
      { stateDir }
    )
    saveAppInstall(
      {
        tolgeeUrl: 'https://app.tolgee.io',
        installId: 2,
        clientSecret: 'cloud',
      },
      { stateDir }
    )

    assert.equal(
      readStoredAppInstall(TOLGEE_URL, { stateDir })?.clientSecret,
      'local'
    )
    assert.equal(
      readStoredAppInstall('https://app.tolgee.io', { stateDir })?.clientSecret,
      'cloud'
    )
  })

  it('reads a corrupted state file as "nothing stored"', () => {
    saveAppInstall({ tolgeeUrl: TOLGEE_URL, installId: 1 }, { stateDir })
    const path = appInstallStatePath({ stateDir })
    rmSync(path)

    assert.equal(readStoredAppInstall(TOLGEE_URL, { stateDir }), null)
  })
})

describe('selfRegisterApp', () => {
  const originalFetch = globalThis.fetch

  afterEach(() => {
    globalThis.fetch = originalFetch
  })

  /** No Tolgee server in the loop: only the persistence side is under test. */
  const stubFetch = (body: unknown): void => {
    globalThis.fetch = (async () =>
      new Response(JSON.stringify(body), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      })) as typeof fetch
  }

  it('stores the one-time credentials so the config picks them up', async () => {
    stubFetch({ id: 42, clientId: 'issued-id', clientSecret: 'issued-secret' })

    const result = await selfRegisterApp({
      tolgeeUrl: TOLGEE_URL,
      registrationSecret: 'reg-secret',
      manifestUrl: 'http://localhost:5181/manifest.json',
      stateDir,
    })

    assert.equal(result.created, true)
    assert.equal(result.native, true)
    assert.equal(result.credentialsPath, appInstallStatePath({ stateDir }))
    assert.ok(!readFileSync(result.credentialsPath!, 'utf8').includes('reg-secret'))

    const config = loadTolgeeAppConfig(
      { TOLGEE_URL } as NodeJS.ProcessEnv,
      { stateDir }
    )
    assert.equal(config.clientId, 'issued-id')
    assert.equal(config.clientSecret, 'issued-secret')
    assert.equal(config.installId, 42)
    assert.equal(config.credentialsSource, 'stored')
  })

  it('leaves the stored secret alone when the install already existed', async () => {
    stubFetch({ id: 42, clientId: 'issued-id', clientSecret: 'issued-secret' })
    await selfRegisterApp({
      tolgeeUrl: TOLGEE_URL,
      registrationSecret: 'reg-secret',
      manifestUrl: 'http://localhost:5181/manifest.json',
      stateDir,
    })

    stubFetch({ id: 42, clientId: 'issued-id', clientSecret: null })
    const again = await selfRegisterApp({
      tolgeeUrl: TOLGEE_URL,
      registrationSecret: 'reg-secret',
      manifestUrl: 'https://tunnel.example.com/manifest.json',
      stateDir,
    })

    assert.equal(again.created, false)
    assert.equal(
      readStoredAppInstall(TOLGEE_URL, { stateDir })?.clientSecret,
      'issued-secret'
    )
  })

  it('writes nothing when persist is false', async () => {
    stubFetch({ id: 42, clientId: 'issued-id', clientSecret: 'issued-secret' })

    const result = await selfRegisterApp({
      tolgeeUrl: TOLGEE_URL,
      registrationSecret: 'reg-secret',
      manifestUrl: 'http://localhost:5181/manifest.json',
      persist: false,
      stateDir,
    })

    assert.equal(result.credentialsPath, null)
    assert.equal(readStoredAppInstall(TOLGEE_URL, { stateDir }), null)
  })

  it('lets the environment override the stored credentials', async () => {
    stubFetch({ id: 42, clientId: 'issued-id', clientSecret: 'issued-secret' })
    await selfRegisterApp({
      tolgeeUrl: TOLGEE_URL,
      registrationSecret: 'reg-secret',
      manifestUrl: 'http://localhost:5181/manifest.json',
      stateDir,
    })

    const config = loadTolgeeAppConfig(
      {
        TOLGEE_URL,
        TOLGEE_APP_CLIENT_ID: 'env-id',
        TOLGEE_APP_CLIENT_SECRET: 'env-secret',
      } as NodeJS.ProcessEnv,
      { stateDir }
    )
    assert.equal(config.clientId, 'env-id')
    assert.equal(config.clientSecret, 'env-secret')
    assert.equal(config.credentialsSource, 'env')
  })
})
