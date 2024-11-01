package io.tolgee.security.service

import io.tolgee.model.enums.ThirdPartyAuthType
import org.springframework.stereotype.Component

@Component
interface OAuthServiceEe {
  fun verifyUserSsoAccountAvailable(
    ssoDomain: String?,
    userId: Long,
    refreshToken: String?,
    thirdPartyAuth: ThirdPartyAuthType,
  ): Boolean
}
