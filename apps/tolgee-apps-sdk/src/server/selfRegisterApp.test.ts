import assert from 'node:assert/strict'
import { mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, beforeEach, describe, it } from 'node:test'
import { readStoredApp, readStoredAppInstall, saveApp } from './installStore'
import { selfRegisterApp } from './selfRegisterApp'

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
    registrationSecret: 'reg-secret',
    manifestUrl: 'http://localhost:5181/manifest.json',
    stateDir,
  })

describe('selfRegisterApp', () => {
  it('stores the app-level credentials the registering call discloses', async () => {
    stubTolgee({
      id: 7,
      clientId: 'tgapp_install',
      clientSecret: 'tgapps_install',
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
    stubTolgee({ id: 8, clientId: 'tgapp_other', clientSecret: 'tgapps_other' })

    const result = await register()

    assert.equal(result.app, null)
    const stored = readStoredApp(TOLGEE_URL, { stateDir })
    assert.equal(stored?.clientSecret, 'tgpubs_app')
    assert.equal(stored?.webhookSecret, 'whsec')
  })

  it('ignores an app block that carries no client id', async () => {
    stubTolgee({
      id: 9,
      clientId: 'tgapp_install',
      clientSecret: 'tgapps_install',
      app: { id: 3, appId: 'keys-showcase' },
    })

    const result = await register()

    assert.equal(result.app, null)
    assert.equal(readStoredApp(TOLGEE_URL, { stateDir }), null)
  })
})
