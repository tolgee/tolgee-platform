package io.tolgee.security.authentication

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.service.security.UserAccountService
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

@Component
class DisabledAuthenticationResolver(
  @Lazy private val userAccountService: UserAccountService,
  private val tolgeeProperties: TolgeeProperties,
) {
  fun resolve(): TolgeeAuthentication? {
    if (tolgeeProperties.authentication.enabled) return null
    return superTokenAuthenticationOf(initialUser())
  }

  private fun initialUser(): UserAccountDto {
    val account =
      userAccountService.findInitialUser()
        ?: throw IllegalStateException("Authentication is disabled but there is no initial user")
    return UserAccountDto.fromEntity(account)
  }

  private fun superTokenAuthenticationOf(user: UserAccountDto): TolgeeAuthentication {
    return TolgeeAuthentication(
      credentials = null,
      deviceId = null,
      userAccount = user,
      actingAsUserAccount = null,
      isReadOnly = false,
      isSuperToken = true,
    )
  }
}
