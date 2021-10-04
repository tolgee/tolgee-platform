package io.tolgee.security

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.ApiKey
import io.tolgee.model.UserAccount
import io.tolgee.security.api_key_auth.ApiKeyAuthenticationToken
import io.tolgee.service.UserAccountService
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class AuthenticationFacade(
  private val configuration: TolgeeProperties,
  private val userAccountService: UserAccountService
) {
  val authentication: Authentication?
    get() = SecurityContextHolder.getContext().authentication

  val userAccount: UserAccountDto
    get() = userAccountOrNull ?: throw IllegalStateException("No current user set!")

  val userAccountOrNull: UserAccountDto?
    get() = if (!configuration.authentication.enabled) {
      UserAccountDto.fromEntity(userAccountService.implicitUser)
    } else authentication?.principal as? UserAccountDto

  val userAccountEntity: UserAccount
    get() = userAccountService[userAccount.id]
      .orElseThrow { throw NotFoundException() }!!

  val isApiKeyAuthentication: Boolean
    get() = authentication is ApiKeyAuthenticationToken

  val apiKey: ApiKey
    get() {
      val authentication = authentication as ApiKeyAuthenticationToken
      return authentication.apiKey
    }
}
