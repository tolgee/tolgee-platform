package io.tolgee.service.security

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.dtos.request.auth.SignUpDto
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.Invitation
import io.tolgee.model.UserAccount
import io.tolgee.security.authentication.JwtService
import io.tolgee.security.payload.JwtAuthenticationResponse
import io.tolgee.service.EmailVerificationService
import io.tolgee.service.InvitationService
import io.tolgee.service.QuickStartService
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
  private val quickStartService: QuickStartService,
  private val passwordEncoder: PasswordEncoder,
) {
  @Transactional
  fun signUp(dto: SignUpDto): JwtAuthenticationResponse? {
    userAccountService.findActive(dto.email)?.let {
      throw BadRequestException(Message.USERNAME_ALREADY_EXISTS)
    }

    val user = dtoToEntity(dto)
    signUp(user, dto.invitationCode, dto.organizationName, dto.userSource)

    if (!tolgeeProperties.authentication.needsEmailVerification) {
      return JwtAuthenticationResponse(jwtService.emitToken(user.id, true))
    }

    emailVerificationService.createForUser(user, dto.callbackUrl)
    return JwtAuthenticationResponse(jwtService.emitToken(user.id, true))
  }

  fun signUp(
    entity: UserAccount,
    invitationCode: String?,
    organizationName: String?,
    userSource: String? = null,
  ): UserAccount {
    val invitation = findAndCheckInvitationOnRegistration(invitationCode)
    val user = userAccountService.createUser(entity, userSource)
    if (invitation != null) {
      invitationService.accept(invitation.code, user)
    }

    val canCreateOrganization = tolgeeProperties.authentication.userCanCreateOrganizations
    if (canCreateOrganization && (invitation == null || !organizationName.isNullOrBlank())) {
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

  @Transactional
  fun findAndCheckInvitationOnRegistration(invitationCode: String?): Invitation? {
    if (invitationCode == null) {
      if (!tolgeeProperties.authentication.registrationsAllowed) {
        throw AuthenticationException(Message.REGISTRATIONS_NOT_ALLOWED)
      }
      return null
    }
    return invitationService.getInvitation(invitationCode)
  }
}
