package io.tolgee.service.security

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ThirdPartyAuthType
import io.tolgee.service.QuickStartService
import io.tolgee.service.invitation.InvitationService
import io.tolgee.service.organization.OrganizationService
import org.springframework.context.ApplicationContext

class SignUpProcessor(
  private val applicationContext: ApplicationContext,
  private val entity: UserAccount,
  private val invitationCode: String?,
  private val organizationNameSuggestion: String?,
  /** The answer for the "Where did you hear about us? */
  private val userSource: String?,
) {
  private val tolgeeProperties by lazy {
    applicationContext.getBean(TolgeeProperties::class.java)
  }

  private val userAccountService by lazy {
    applicationContext.getBean(UserAccountService::class.java)
  }

  private val invitationService by lazy {
    applicationContext.getBean(InvitationService::class.java)
  }

  private val organizationService by lazy {
    applicationContext.getBean(OrganizationService::class.java)
  }

  private val quickStartService by lazy {
    applicationContext.getBean(QuickStartService::class.java)
  }

  val signUpAllowed by lazy {
    invitationCode != null ||
      entity.accountType == UserAccount.AccountType.MANAGED ||
      tolgeeProperties.authentication.registrationsAllowed
  }
  val shouldCreateOrganization by lazy {
    user.thirdPartyAuthType != ThirdPartyAuthType.SSO &&
      tolgeeProperties.authentication.userCanCreateOrganizations &&
      (invitation == null || !organizationNameSuggestion.isNullOrBlank())
  }
  val organizationName by lazy {
    if (organizationNameSuggestion.isNullOrBlank()) {
      user.name
    } else {
      organizationNameSuggestion
    }
  }

  val invitation by lazy {
    invitationCode?.let(invitationService::getInvitation)
  }
  val user by lazy {
    userAccountService.createUser(entity, userSource)
  }

  fun checkSignUpAllowed() {
    if (!signUpAllowed) {
      throw AuthenticationException(Message.REGISTRATIONS_NOT_ALLOWED)
    }
  }

  fun acceptInvitation() {
    invitation?.code?.let { invitationService.accept(it, user) }
  }

  fun createOrganization() {
    if (!shouldCreateOrganization) {
      return
    }

    val organization = organizationService.createPreferred(user, organizationName)
    quickStartService.create(user, organization)
  }

  fun process(): UserAccount {
    checkSignUpAllowed()
    acceptInvitation()
    createOrganization()
    return user
  }
}
