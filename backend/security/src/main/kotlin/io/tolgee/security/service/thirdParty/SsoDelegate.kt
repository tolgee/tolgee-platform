package io.tolgee.security.service.thirdParty

import io.tolgee.model.enums.ThirdPartyAuthType
import io.tolgee.security.payload.JwtAuthenticationResponse
import org.springframework.stereotype.Component
import java.util.Date

@Component
interface SsoDelegate {
  fun getTokenResponse(
    receivedCode: String?,
    invitationCode: String?,
    redirectUri: String?,
    domain: String?,
  ): JwtAuthenticationResponse

  fun verifyUserSsoAccountAvailable(
    ssoDomain: String?,
    userId: Long,
    refreshToken: String?,
    thirdPartyAuth: ThirdPartyAuthType,
    ssoSessionExpiry: Date?,
  ): Boolean
}
