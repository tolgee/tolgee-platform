package io.tolgee.security.thirdParty

import io.tolgee.model.enums.ThirdPartyAuthType
import io.tolgee.security.payload.JwtAuthenticationResponse

interface ThirdPartyAuthDelegate {
  val name: String

  val preferredAuthType: ThirdPartyAuthType

  fun getTokenResponse(
    receivedCode: String?,
    invitationCode: String?,
    redirectUri: String?,
    domain: String?,
  ): JwtAuthenticationResponse
}
