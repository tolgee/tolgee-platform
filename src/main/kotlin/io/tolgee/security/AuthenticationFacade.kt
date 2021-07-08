package io.tolgee.security

import io.tolgee.configuration.tolgee.TolgeeProperties
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

    val userAccount: UserAccount
        get() = userAccountOrNull ?: throw IllegalStateException("No current user set!")

    val userAccountOrNull: UserAccount?
        get() = if (!configuration.authentication.enabled) {
            userAccountService.implicitUser
        } else authentication?.principal as? UserAccount

    val apiKey: ApiKey
        get() {
            val authentication = authentication as ApiKeyAuthenticationToken
            return authentication.apiKey
        }
}
