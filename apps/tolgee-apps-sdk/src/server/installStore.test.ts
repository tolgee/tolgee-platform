import assert from 'node:assert/strict'
import {
  mkdirSync,
  mkdtempSync,
  readFileSync,
  rmSync,
  statSync,
  writeFileSync,
} from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, beforeEach, describe, it } from 'node:test'
import { loadTolgeeAppConfig } from './config'
import {
  appInstallStatePath,
  forgetAppInstall,
  hasStoredCredentials,
  readStoredApp,
  readStoredAppInstall,
  readStoredAppInstallById,
  readStoredAppInstalls,
  saveApp,
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
        organizationSlug: null,
      },
      { stateDir }
    )

    const stored = readStoredAppInstall(TOLGEE_URL, { stateDir })
    assert.equal(stored?.installId, 12)

    const mode = statSync(appInstallStatePath({ stateDir })).mode & 0o777
    assert.equal(mode, 0o600)
  })

  it('keeps stored install fields a later write does not carry', () => {
    saveAppInstall(
      { tolgeeUrl: TOLGEE_URL, installId: 12, organizationSlug: 'acme' },
      { stateDir }
    )
    saveAppInstall({ tolgeeUrl: TOLGEE_URL, installId: 12 }, { stateDir })

    assert.equal(
      readStoredAppInstall(TOLGEE_URL, { stateDir })?.organizationSlug,
      'acme'
    )
  })

  it('reads back a url that differs only in trailing slash or whitespace', () => {
    saveAppInstall(
      { tolgeeUrl: `${TOLGEE_URL}/`, installId: 3 },
      { stateDir }
    )

    assert.equal(
      readStoredAppInstall(` ${TOLGEE_URL}`, { stateDir })?.installId,
      3
    )
  })

  it('keeps the credentials of two instances apart', () => {
    saveApp(
      { tolgeeUrl: TOLGEE_URL, clientSecret: 'local' },
      { stateDir }
    )
    saveApp(
      { tolgeeUrl: 'https://app.tolgee.io', clientSecret: 'cloud' },
      { stateDir }
    )

    assert.equal(readStoredApp(TOLGEE_URL, { stateDir })?.clientSecret, 'local')
    assert.equal(
      readStoredApp('https://app.tolgee.io', { stateDir })?.clientSecret,
      'cloud'
    )
  })

  it('reads a corrupted state file as "nothing stored"', () => {
    saveAppInstall({ tolgeeUrl: TOLGEE_URL, installId: 1 }, { stateDir })
    const path = appInstallStatePath({ stateDir })
    rmSync(path)

    assert.equal(readStoredAppInstall(TOLGEE_URL, { stateDir }), null)
  })

  it('keeps the app-level secret when a later write carries none', () => {
    saveApp(
      {
        tolgeeUrl: TOLGEE_URL,
        clientId: 'tgpub_app',
        clientSecret: 'tgpubs_app',
        webhookSecret: 'signing-secret',
      },
      { stateDir }
    )
    saveApp({ tolgeeUrl: TOLGEE_URL, appId: 'named-later' }, { stateDir })

    const app = readStoredApp(TOLGEE_URL, { stateDir })
    assert.equal(app?.clientSecret, 'tgpubs_app')
    assert.equal(app?.webhookSecret, 'signing-secret')
    assert.equal(app?.appId, 'named-later')
  })

  it('holds one install per organization and resolves the current one', () => {
    saveAppInstall({ tolgeeUrl: TOLGEE_URL, installId: 7 }, { stateDir })
    saveAppInstall(
      {
        tolgeeUrl: TOLGEE_URL,
        installId: 9,
        organizationSlug: 'globex',
        makeCurrent: false,
      },
      { stateDir }
    )

    assert.equal(readStoredAppInstalls(TOLGEE_URL, { stateDir }).length, 2)
    assert.equal(readStoredAppInstall(TOLGEE_URL, { stateDir })?.installId, 7)
    assert.equal(
      readStoredAppInstallById(TOLGEE_URL, 9, { stateDir })?.organizationSlug,
      'globex'
    )

    assert.equal(forgetAppInstall(TOLGEE_URL, 9, { stateDir }), true)
    assert.equal(forgetAppInstall(TOLGEE_URL, 9, { stateDir }), false)
    assert.equal(readStoredAppInstalls(TOLGEE_URL, { stateDir }).length, 1)
  })

  it('reports whether anything credential-bearing is held', () => {
    assert.equal(hasStoredCredentials(TOLGEE_URL, { stateDir }), false)

    // Installs carry no credentials, so storing one changes nothing.
    saveAppInstall({ tolgeeUrl: TOLGEE_URL, installId: 7 }, { stateDir })
    assert.equal(hasStoredCredentials(TOLGEE_URL, { stateDir }), false)

    saveApp({ tolgeeUrl: TOLGEE_URL, clientSecret: 'tgpubs_x' }, { stateDir })
    assert.equal(hasStoredCredentials(TOLGEE_URL, { stateDir }), true)
  })

  /**
   * Apps in the wild already run on the one-install-per-URL file. The install
   * identity is read forward; the per-install credentials such a file carried
   * no longer exist as a concept and are dropped.
   */
  it('migrates a state file written before the app layer existed', () => {
    const path = appInstallStatePath({ stateDir })
    mkdirSync(stateDir, { recursive: true })
    writeFileSync(
      path,
      JSON.stringify({
        version: 1,
        installs: {
          [TOLGEE_URL]: {
            tolgeeUrl: TOLGEE_URL,
            installId: 12,
            clientId: 'tgapp_legacy',
            clientSecret: 'tgapps_legacy',
            organizationSlug: null,
            updatedAt: '2026-01-01T00:00:00.000Z',
          },
        },
      }),
      { mode: 0o600 }
    )

    const migrated = readStoredAppInstall(TOLGEE_URL, { stateDir })
    assert.equal(migrated?.installId, 12)
    assert.equal(readStoredApp(TOLGEE_URL, { stateDir }), null)
    assert.equal(hasStoredCredentials(TOLGEE_URL, { stateDir }), false)

    saveApp(
      { tolgeeUrl: TOLGEE_URL, webhookSecret: 'signing-secret' },
      { stateDir }
    )
    const rewritten = JSON.parse(readFileSync(path, 'utf8'))
    assert.equal(rewritten.version, 3)
    assert.equal(rewritten.instances[TOLGEE_URL].installs['12'].installId, 12)
    // The dead install credentials are not carried into the rewritten file.
    assert.equal(
      rewritten.instances[TOLGEE_URL].installs['12'].clientSecret,
      undefined
    )
    assert.equal(
      readStoredApp(TOLGEE_URL, { stateDir })?.webhookSecret,
      'signing-secret'
    )
  })
})

describe('selfRegisterApp persistence', () => {
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

  const CREATED_RESPONSE = {
    id: 42,
    created: true,
    app: {
      id: 3,
      appId: 'test-app',
      clientId: 'tgpub_issued',
      clientSecret: 'tgpubs_issued',
      webhookSecret: 'whsec',
    },
  }

  it('stores the one-time app credentials so the config picks them up', async () => {
    stubFetch(CREATED_RESPONSE)

    const result = await selfRegisterApp({
      tolgeeUrl: TOLGEE_URL,
      registrationToken: 'tgreg_test-token',
      manifestUrl: 'http://localhost:5181/manifest.json',
      stateDir,
    })

    assert.equal(result.created, true)
    assert.equal(result.credentialsPath, appInstallStatePath({ stateDir }))
    assert.ok(!readFileSync(result.credentialsPath!, 'utf8').includes('tgreg_test-token'))

    const config = loadTolgeeAppConfig(
      { TOLGEE_URL } as NodeJS.ProcessEnv,
      { stateDir }
    )
    assert.equal(config.clientId, 'tgpub_issued')
    assert.equal(config.clientSecret, 'tgpubs_issued')
    assert.equal(config.installId, 42)
    assert.equal(config.credentialsSource, 'stored')
  })

  it('leaves the stored credentials alone when the install already existed', async () => {
    stubFetch(CREATED_RESPONSE)
    await selfRegisterApp({
      tolgeeUrl: TOLGEE_URL,
      registrationToken: 'tgreg_test-token',
      manifestUrl: 'http://localhost:5181/manifest.json',
      stateDir,
    })

    stubFetch({ id: 42, created: false })
    const again = await selfRegisterApp({
      tolgeeUrl: TOLGEE_URL,
      registrationToken: 'tgreg_test-token',
      manifestUrl: 'https://tunnel.example.com/manifest.json',
      stateDir,
    })

    assert.equal(again.created, false)
    assert.equal(
      readStoredApp(TOLGEE_URL, { stateDir })?.clientSecret,
      'tgpubs_issued'
    )
  })

  it('writes nothing when persist is false', async () => {
    stubFetch(CREATED_RESPONSE)

    const result = await selfRegisterApp({
      tolgeeUrl: TOLGEE_URL,
      registrationToken: 'tgreg_test-token',
      manifestUrl: 'http://localhost:5181/manifest.json',
      persist: false,
      stateDir,
    })

    assert.equal(result.credentialsPath, null)
    assert.equal(readStoredAppInstall(TOLGEE_URL, { stateDir }), null)
    assert.equal(readStoredApp(TOLGEE_URL, { stateDir }), null)
  })

  it('lets the environment override the stored credentials', async () => {
    stubFetch(CREATED_RESPONSE)
    await selfRegisterApp({
      tolgeeUrl: TOLGEE_URL,
      registrationToken: 'tgreg_test-token',
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
