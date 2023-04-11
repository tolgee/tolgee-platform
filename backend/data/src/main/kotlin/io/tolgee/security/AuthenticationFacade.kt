package io.tolgee.security

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.ApiKey
import io.tolgee.model.Pat
import io.tolgee.model.UserAccount
import io.tolgee.security.apiKeyAuth.ApiKeyAuthenticationToken
import io.tolgee.security.patAuth.PatAuthenticationToken
import io.tolgee.service.security.UserAccountService
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
    get() = userAccountEntityOrNull ?: throw NotFoundException()

  val userAccountEntityOrNull: UserAccount?
    get() = userAccountService.findActive(userAccount.id)

  val isApiKeyAuthentication: Boolean
    get() = authentication is ApiKeyAuthenticationToken

  val apiKey: ApiKey
    get() {
      val authentication = authentication as ApiKeyAuthenticationToken
      return authentication.apiKey
    }

  val apiKeyOrNull: ApiKey?
    get() {
      val authentication = authentication as? ApiKeyAuthenticationToken
      return authentication?.apiKey
    }

  val isPatAuthentication: Boolean
    get() = authentication is PatAuthenticationToken

  val pat: Pat
    get() {
      val authentication = authentication as PatAuthenticationToken
      return authentication.pat
    }
}
