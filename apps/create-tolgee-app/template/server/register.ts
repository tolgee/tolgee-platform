import { selfRegisterApp } from '@tolgee/apps-sdk/server'
import { config } from './config'

/**
 * Registers this app in Tolgee on boot when TOLGEE_APP_REGISTRATION_SECRET is
 * set. Without it the app has to be added by hand in Organization → Apps.
 *
 * By default this registers a **native** app: one owned by no organization. A
 * server admin then decides which organizations may use it, under
 * Administration → Apps. Set TOLGEE_ORGANIZATION_SLUG only if you instead want
 * the app installed into that one organization.
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

  if (!registrationSecret) {
    console.log(
      '[register] self-registration off (TOLGEE_APP_REGISTRATION_SECRET not set).'
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

    const where = result.native
      ? 'as a native (server-wide) app'
      : `in "${organizationSlug}"`

    if (!result.created) {
      console.log(
        `[register] already registered ${where} ` +
          `(install ${result.installId}); it now points at ${manifestUrl}.`
      )
      return
    }

    console.log(`[register] registered ${where} (install ${result.installId}).`)
    console.log('[register] Save these into .env.local — the secret is shown only once:')
    console.log(`  TOLGEE_APP_CLIENT_ID=${result.clientId}`)
    console.log(`  TOLGEE_APP_CLIENT_SECRET=${result.clientSecret}`)
    if (result.native) {
      console.log(
        '[register] Next: grant it to an organization under Administration → Apps, ' +
          'then enable it for a project.'
      )
    }
  } catch (error) {
    console.error(
      `[register] self-registration against ${tolgeeUrl} failed. Check that ` +
        `Tolgee is reachable and that the registration secret matches the one ` +
        `configured on the server.`
    )
    console.error('[register]', error instanceof Error ? error.message : error)
    console.error(`[register] register manually instead: ${manifestUrl}`)
  }
}
