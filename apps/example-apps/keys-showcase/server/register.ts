import { selfRegisterAppWithRetry } from '@tolgee/apps-sdk/server'
import { config } from './config'

/**
 * Registers this app in Tolgee on boot when TOLGEE_APP_REGISTRATION_TOKEN is
 * set. Without it the app has to be added by hand in Organization → Apps.
 *
 * The secret is server-wide and comes from the Tolgee administrator — Tolgee's
 * configuration holds only its hash. The app registers into, and is owned by,
 * the organization TOLGEE_APP_ORGANIZATION names, or the server's initial
 * organization when unset. A server admin can later offer the app to every
 * organization from the owner's Apps page.
 *
 * Registering again with a different `manifestUrl` repoints the existing
 * install, which is how a dev tunnel's new hostname takes effect on restart.
 *
 * The SDK stores the credentials Tolgee issues in `.tolgee-dev/install.json`
 * (gitignored) and reads them back through `loadTolgeeAppConfig()`, so nothing
 * has to be copied by hand and the secret is never printed.
 *
 * Retries with backoff, so an app that boots before Tolgee — or before its
 * token exists — still connects once Tolgee is ready, without a restart.
 */
export const selfRegisterIfConfigured = async (
  manifestUrl: string
): Promise<void> => {
  const { registrationToken, organizationSlug, tolgeeUrl } = config

  if (!registrationToken) {
    console.log(
      '[register] self-registration off (TOLGEE_APP_REGISTRATION_TOKEN not set).'
    )
    console.log(`[register] register manually with manifest URL ${manifestUrl}`)
    return
  }

  const result = await selfRegisterAppWithRetry(
    { tolgeeUrl, registrationToken, organizationSlug, manifestUrl },
    {
      onRetry: (error, attempt) =>
        console.warn(
          `[register] attempt ${attempt} against ${tolgeeUrl} failed ` +
            `(${error.message}); retrying — still serving the manifest.`
        ),
    }
  )

  if (!result.created) {
    console.log(
      `[register] already registered (install ${result.installId}); ` +
        `it now points at ${manifestUrl}.`
    )
    return
  }

  console.log(`[register] registered (install ${result.installId}).`)
  console.log(
    `[register] credentials stored in ${result.credentialsPath} (gitignored); ` +
      'the SDK reads them from there — nothing to copy.'
  )
}
