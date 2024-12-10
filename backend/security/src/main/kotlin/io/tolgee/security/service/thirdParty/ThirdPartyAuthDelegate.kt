package io.tolgee.security.service.thirdParty

import io.tolgee.security.payload.JwtAuthenticationResponse

interface ThirdPartyAuthDelegate {
  val name: String

  fun getTokenResponse(
    receivedCode: String?,
    invitationCode: String?,
    redirectUri: String?,
    domain: String?,
  ): JwtAuthenticationResponse
}
