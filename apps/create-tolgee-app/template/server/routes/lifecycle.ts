import { mountTolgeeLifecycle } from '@tolgee/apps-sdk/server'
import type { Express } from 'express'
import { config } from '../config'

/**
 * Receives Tolgee's signed lifecycle deliveries: the app-level credentials when
 * the app is registered, the per-install credentials when an organization
 * installs it, and every later rotation. The SDK verifies the signature and
 * stores what arrives, so nothing below ever touches a secret.
 *
 * Mount this **before** `express.json()`: the signature covers the exact bytes
 * Tolgee sent, and a body parser that drains the stream first leaves nothing to
 * verify.
 */
export const registerLifecycleRoute = (app: Express): void => {
  mountTolgeeLifecycle(app, {
    tolgeeUrl: config.tolgeeUrl,
    on: {
      registered: (event) => {
        console.log(
          `[lifecycle] registered as "${event.app?.appId ?? 'this app'}" — ` +
            'app-level credentials stored.'
        )
      },
      installed: (event) => {
        console.log(
          `[lifecycle] installed by ${event.organization?.slug ?? 'no organization'} ` +
            `(install ${event.install?.installId}) — recorded; the app credentials mint its tokens.`
        )
      },
      uninstalled: (event) => {
        console.log(
          `[lifecycle] uninstalled (install ${event.install?.installId ?? 'all'}) — ` +
            'credentials dropped.'
        )
      },
      secretRotated: (event) => {
        console.log(
          `[lifecycle] Tolgee rotated the ${event.rotatedLayer ?? 'app'}-level secret — ` +
            'the new one is stored.'
        )
      },
    },
    onRejected: (rejected) => {
      console.warn(`[lifecycle] refused a delivery: ${rejected.message}`)
    },
  })
}
