package io.tolgee.service.security

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.UserAccount
import io.tolgee.repository.UserAccountRepository
import io.tolgee.util.EmailNormalizer
import org.springframework.stereotype.Service

/**
 * Validates the email of a **new native registration**. Login and existing accounts are never
 * affected — duplicate detection here only decides whether a fresh sign-up is allowed.
 */
@Service
class RegistrationEmailValidator(
  private val tolgeeProperties: TolgeeProperties,
  private val emailDomainBlocklistService: EmailDomainBlocklistService,
  private val userAccountRepository: UserAccountRepository,
) {
  fun validate(email: String) {
    validateDomainAllowed(email)
    validateNotDuplicate(email)
  }

  private fun validateDomainAllowed(email: String) {
    val domain = EmailNormalizer.domainOf(email) ?: return
    if (emailDomainBlocklistService.isBlocked(domain)) {
      throw BadRequestException(Message.EMAIL_DOMAIN_NOT_ALLOWED)
    }
  }

  private fun validateNotDuplicate(email: String) {
    val existing = findExistingAccounts(email)
    if (existing.isEmpty()) {
      return
    }
    if (existing.any { it.disabledAt == null }) {
      throw BadRequestException(Message.USERNAME_ALREADY_EXISTS)
    }
    throw BadRequestException(Message.USER_ACCOUNT_DISABLED)
  }

  private fun findExistingAccounts(email: String): List<UserAccount> {
    if (!tolgeeProperties.authentication.blockEmailAliases) {
      return listOfNotNull(userAccountRepository.findActiveOrDisabled(email))
    }
    return userAccountRepository.findNonDeletedByNormalizedEmail(EmailNormalizer.normalize(email))
  }
}
