package io.tolgee.security.service.thirdParty

import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ThirdPartyAuthType
import io.tolgee.security.payload.JwtAuthenticationResponse

interface ThirdPartyAuthDelegate {
  val name: String

  val preferredAccountType: UserAccount.AccountType

  val preferredThirdPartyAuthType: ThirdPartyAuthType

  fun getTokenResponse(
    receivedCode: String?,
    invitationCode: String?,
    redirectUri: String?,
    domain: String?,
  ): JwtAuthenticationResponse
}
