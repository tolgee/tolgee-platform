package io.tolgee.service.security

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.dtos.request.auth.SignUpDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.Invitation
import io.tolgee.security.authentication.JwtService
import io.tolgee.security.payload.JwtAuthenticationResponse
import io.tolgee.service.EmailVerificationService
import io.tolgee.service.InvitationService
import io.tolgee.service.QuickStartService
import io.tolgee.service.organization.OrganizationService
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
  private val quickStartService: QuickStartService
) {
  @Transactional
  fun signUp(dto: SignUpDto): JwtAuthenticationResponse? {
    var invitation: Invitation? = null
    if (dto.invitationCode == null) {
      tolgeeProperties.authentication.checkAllowedRegistrations()
    } else {
      invitation = invitationService.getInvitation(dto.invitationCode) // it throws an exception
    }

    userAccountService.findActive(dto.email)?.let {
      throw BadRequestException(Message.USERNAME_ALREADY_EXISTS)
    }

    val user = userAccountService.createUser(dto)
    if (invitation != null) {
      invitationService.accept(invitation.code, user)
    }

    val canCreateOrganization = tolgeeProperties.authentication.userCanCreateOrganizations
    if (canCreateOrganization && (invitation == null || !dto.organizationName.isNullOrBlank())) {
      val name = if (dto.organizationName.isNullOrBlank()) user.name else dto.organizationName!!
      val organization = organizationService.createPreferred(user, name)
      quickStartService.create(user, organization)
    }

    if (!tolgeeProperties.authentication.needsEmailVerification) {
      return JwtAuthenticationResponse(jwtService.emitToken(user.id, true))
    }

    emailVerificationService.createForUser(user, dto.callbackUrl)

    return null
  }
}
