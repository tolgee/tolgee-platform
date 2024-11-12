package io.tolgee.security.service.thirdParty

import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.security.payload.JwtAuthenticationResponse
import org.springframework.stereotype.Component

@Component
interface SsoDelegate {
  fun getTokenResponse(
    receivedCode: String?,
    invitationCode: String?,
    redirectUri: String?,
    domain: String?,
  ): JwtAuthenticationResponse

  fun verifyUserSsoAccountAvailable(user: UserAccountDto): Boolean
}
