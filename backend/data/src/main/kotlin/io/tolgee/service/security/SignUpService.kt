package io.tolgee.service.security

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.dtos.request.auth.SignUpDto
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.UserAccount
import io.tolgee.security.authentication.JwtService
import io.tolgee.security.payload.JwtAuthenticationResponse
import io.tolgee.service.EmailVerificationService
import io.tolgee.service.TenantService
import org.springframework.context.ApplicationContext
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SignUpService(
  private val applicationContext: ApplicationContext,
  private val userAccountService: UserAccountService,
  private val tolgeeProperties: TolgeeProperties,
  private val jwtService: JwtService,
  private val emailVerificationService: EmailVerificationService,
  private val tenantService: TenantService,
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

    return JwtAuthenticationResponse(jwtService.emitToken(user.id, isSuper = true))
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
    return SignUpProcessor(applicationContext, entity, invitationCode, organizationName, userSource).process()
  }

  fun dtoToEntity(request: SignUpDto): UserAccount {
    val encodedPassword = passwordEncoder.encode(request.password!!)
    return UserAccount(name = request.name, username = request.email, password = encodedPassword)
  }
}
