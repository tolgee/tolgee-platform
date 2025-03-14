package io.tolgee.service.security.thirdParty

import io.tolgee.security.payload.JwtAuthenticationResponse

interface ThirdPartyAuthenticationService {
  fun authenticate(
    serviceType: String?,
    code: String?,
    redirectUri: String?,
    invitationCode: String?,
    domain: String?,
  ): JwtAuthenticationResponse
}
