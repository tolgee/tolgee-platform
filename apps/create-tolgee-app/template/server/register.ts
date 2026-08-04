import { selfRegisterApp } from '@tolgee/apps-sdk/server'
import { config } from './config'

/**
 * Registers this app in Tolgee on boot when TOLGEE_APP_REGISTRATION_SECRET and
 * TOLGEE_ORGANIZATION_SLUG are both set. Without them the app has to be added
 * by hand in Organization → Apps.
 *
 * Registering again with a different `manifestUrl` repoints the existing
 * install, which is how a dev tunnel's new hostname takes effect on restart.
 *
 * Never throws: `/manifest.json` must keep serving even when registration
 * fails, so manual registration stays possible.
 */
export const selfRegisterIfConfigured = async (
  manifestUrl: string
): Promise<void> => {
  const { organizationSlug, registrationSecret, tolgeeUrl } = config

  if (!registrationSecret || !organizationSlug) {
    console.log(
      '[register] self-registration off (TOLGEE_APP_REGISTRATION_SECRET / ' +
        'TOLGEE_ORGANIZATION_SLUG not set).'
    )
    console.log(`[register] register manually with manifest URL ${manifestUrl}`)
    return
  }

  try {
    const result = await selfRegisterApp({
      tolgeeUrl,
      registrationSecret,
      organizationSlug,
      manifestUrl,
    })

    if (!result.created) {
      console.log(
        `[register] already registered in "${organizationSlug}" ` +
          `(install ${result.installId}); it now points at ${manifestUrl}.`
      )
      return
    }

    console.log(
      `[register] registered in "${organizationSlug}" (install ${result.installId}).`
    )
    console.log('[register] Save these into .env.local — the secret is shown only once:')
    console.log(`  TOLGEE_APP_CLIENT_ID=${result.clientId}`)
    console.log(`  TOLGEE_APP_CLIENT_SECRET=${result.clientSecret}`)
  } catch (error) {
    console.error(
      `[register] self-registration against ${tolgeeUrl} failed. Check that ` +
        `Tolgee is reachable, that the registration secret matches the one ` +
        `configured on the server, and that organization "${organizationSlug}" exists.`
    )
    console.error('[register]', error instanceof Error ? error.message : error)
    console.error(`[register] register manually instead: ${manifestUrl}`)
  }
}
