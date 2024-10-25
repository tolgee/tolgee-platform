package io.tolgee.security.service

import org.springframework.stereotype.Component

@Component
interface OAuthServiceEe {
  fun verifyUserIsStillEmployed(
    ssoDomain: String?,
    userId: Long,
    refreshToken: String?,
    thirdPartyAuth: String?,
  ): Boolean
}
