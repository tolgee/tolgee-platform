package io.tolgee.service.security

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.dtos.request.auth.SignUpDto
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.Organization
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.ThirdPartyAuthType
import io.tolgee.security.authentication.JwtService
import io.tolgee.security.payload.JwtAuthenticationResponse
import io.tolgee.service.EmailVerificationService
import io.tolgee.service.QuickStartService
import io.tolgee.service.TenantService
import io.tolgee.service.invitation.InvitationService
import io.tolgee.service.organization.OrganizationRoleService
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
  private val organizationRoleService: OrganizationRoleService,
  private val quickStartService: QuickStartService,
  private val passwordEncoder: PasswordEncoder,
  private val tenantService: TenantService,
) {
  @Transactional
  fun signUp(dto: SignUpDto): JwtAuthenticationResponse? {
    userAccountService.findActive(dto.email)?.let {
      throw BadRequestException(Message.USERNAME_ALREADY_EXISTS)
    }

    tenantService.checkSsoNotRequired(dto.email)

    val user = dtoToEntity(dto)
    signUp(user, dto.invitationCode, dto.organizationName, dto.userSource)

    if (!tolgeeProperties.authentication.needsEmailVerification) {
      return JwtAuthenticationResponse(jwtService.emitToken(user.id, true))
    }

    emailVerificationService.createForUser(user, dto.callbackUrl)
    return JwtAuthenticationResponse(jwtService.emitToken(user.id, true))
  }

  @Transactional
  fun signUp(
    entity: UserAccount,
    invitationCode: String?,
    organizationName: String?,
    userSource: String? = null,
    organizationForced: Organization? = null,
  ): UserAccount {
    if (invitationCode == null &&
      entity.accountType != UserAccount.AccountType.MANAGED &&
      !tolgeeProperties.authentication.registrationsAllowed
    ) {
      throw AuthenticationException(Message.REGISTRATIONS_NOT_ALLOWED)
    }

    val invitation = invitationCode?.let(invitationService::getInvitation)
    val user = userAccountService.createUser(entity, userSource)
    if (invitation != null) {
      if (organizationForced != null && invitation.organizationRole?.organization != organizationForced) {
        // Invitations are allowed only for specific organization
        throw BadRequestException(Message.INVITATION_ORGANIZATION_MISMATCH)
      }
      invitationService.accept(invitation.code, user)
    } else if (organizationForced != null) {
      organizationRoleService.grantRoleToUser(
        user,
        organizationForced,
        OrganizationRoleType.MEMBER,
      )
    }

    if (
      user.thirdPartyAuthType != ThirdPartyAuthType.SSO &&
      tolgeeProperties.authentication.userCanCreateOrganizations &&
      (invitation == null || !organizationName.isNullOrBlank())
    ) {
      val name = if (organizationName.isNullOrBlank()) user.name else organizationName
      val organization = organizationService.createPreferred(user, name)
      quickStartService.create(user, organization)
    }
    return user
  }

  fun dtoToEntity(request: SignUpDto): UserAccount {
    val encodedPassword = passwordEncoder.encode(request.password!!)
    return UserAccount(name = request.name, username = request.email, password = encodedPassword)
  }
}
