package io.tolgee.service.security

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.dtos.request.auth.SignUpDto
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ThirdPartyAuthType
import io.tolgee.security.authentication.JwtService
import io.tolgee.security.payload.JwtAuthenticationResponse
import io.tolgee.service.EmailVerificationService
import io.tolgee.service.QuickStartService
import io.tolgee.service.TenantService
import io.tolgee.service.invitation.InvitationService
import io.tolgee.service.organization.OrganizationService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SignUpService(
  private val invitationService: InvitationService,
  private val userAccountService: UserAccountService,
  private val tolgeeProperties: TolgeeProperties,
  private val jwtService: JwtService,
  private val emailVerificationService: EmailVerificationService,
  private val organizationService: OrganizationService,
  private val tenantService: TenantService,
  private val quickStartService: QuickStartService,
  private val passwordEncoder: PasswordEncoder,
) {
  @Transactional
  fun signUp(dto: SignUpDto): JwtAuthenticationResponse? {
    userAccountService.findActive(dto.email)?.let {
      throw BadRequestException(Message.USERNAME_ALREADY_EXISTS)
    }

    val user = dtoToEntity(dto)
    checkNotManagedByOrganization(user.domain)
    signUp(user, dto.invitationCode, dto.organizationName, dto.userSource)

    if (tolgeeProperties.authentication.needsEmailVerification) {
      emailVerificationService.createForUser(user, dto.callbackUrl)
    }

    return JwtAuthenticationResponse(jwtService.emitToken(user.id, true))
  }

  private fun checkNotManagedByOrganization(domain: String?) {
    if (tenantService.getEnabledConfigByDomainOrNull(domain) != null) {
      // There is sso configured for the domain - don't allow sign up
      throw AuthenticationException(Message.USE_SSO_FOR_AUTHENTICATION_INSTEAD, listOf(domain))
    }
  }

  @Transactional
  fun signUp(
    entity: UserAccount,
    invitationCode: String?,
    organizationName: String?,
    userSource: String? = null,
  ): UserAccount {
    return SignUpProcessor(entity, invitationCode, organizationName, userSource).process()
  }

  fun dtoToEntity(request: SignUpDto): UserAccount {
    val encodedPassword = passwordEncoder.encode(request.password!!)
    return UserAccount(name = request.name, username = request.email, password = encodedPassword)
  }

  inner class SignUpProcessor(
    private val entity: UserAccount,
    private val invitationCode: String?,
    private val organizationNameSuggestion: String?,
    private val userSource: String?,
  ) {
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
}
