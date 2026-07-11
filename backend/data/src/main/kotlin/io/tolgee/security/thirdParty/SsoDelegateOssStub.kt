package io.tolgee.security.thirdParty

import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.exceptions.NotImplementedInOss
import io.tolgee.security.payload.JwtAuthenticationResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
class SsoDelegateOssStub : SsoDelegate {
  override fun verifyUserSsoAccountAvailable(user: UserAccountDto): Boolean {
    // no-op
    return true
  }

  override fun getTokenResponse(
    receivedCode: String?,
    invitationCode: String?,
    redirectUri: String?,
    domain: String?,
  ): JwtAuthenticationResponse {
    // no-op
    throw NotImplementedInOss()
  }
}
