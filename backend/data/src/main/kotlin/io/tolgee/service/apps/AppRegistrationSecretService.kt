package io.tolgee.service.apps

import io.tolgee.component.KeyGenerator
import io.tolgee.configuration.tolgee.AppsProperties
import io.tolgee.constants.Message
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.Organization
import io.tolgee.service.organization.OrganizationService
import io.tolgee.service.security.UserAccountService
import io.tolgee.util.constantTimeEquals
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Authenticates app self-registration against the server-configured secret. The configuration holds
 * only the SHA-256 hash of the secret ([AppsProperties.registrationSecretHash]) — the plaintext
 * lives solely with the app's deployment, so a leaked Tolgee config does not hand out a usable
 * credential. No hash configured means self-registration is disabled; apps are then registered by
 * hand in the UI.
 */
@Service
class AppRegistrationSecretService(
  private val appsProperties: AppsProperties,
  private val keyGenerator: KeyGenerator,
  private val organizationService: OrganizationService,
  private val userAccountService: UserAccountService,
) {
  fun authenticate(plaintext: String?) {
    val configuredHash =
      appsProperties.registrationSecretHash?.takeIf { it.isNotBlank() }
        ?: throw AuthenticationException(Message.INVALID_APP_REGISTRATION_SECRET)
    if (plaintext.isNullOrBlank()) throw AuthenticationException(Message.INVALID_APP_REGISTRATION_SECRET)
    if (!constantTimeEquals(keyGenerator.hash(plaintext), configuredHash)) {
      throw AuthenticationException(Message.INVALID_APP_REGISTRATION_SECRET)
    }
  }

  /**
   * The organization a self-registration targets: the one the app's configuration names, or the
   * server's initial organization when it names none.
   */
  @Transactional(readOnly = true)
  fun resolveOrganization(slug: String?): Organization {
    if (!slug.isNullOrBlank()) return organizationService.get(slug)
    return findInitialOrganization()
      ?: throw BadRequestException(Message.ORGANIZATION_NOT_FOUND)
  }

  private fun findInitialOrganization(): Organization? {
    val initialUser = userAccountService.findInitialUser() ?: return null
    return organizationService.getAllSingleOwnedByUser(initialUser).firstOrNull()
  }
}
