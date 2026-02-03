package io.tolgee.service.security.thirdParty

import io.tolgee.constants.Message
import io.tolgee.exceptions.NotFoundException
import io.tolgee.security.payload.JwtAuthenticationResponse
import io.tolgee.security.thirdParty.ThirdPartyAuthDelegate
import org.springframework.stereotype.Service

@Service
class ThirdPartyAuthenticationServiceImpl(
  private val thirdPartyAuthDelegates: List<ThirdPartyAuthDelegate>,
) : ThirdPartyAuthenticationService {
  override fun authenticate(
    serviceType: String?,
    code: String?,
    redirectUri: String?,
    invitationCode: String?,
    domain: String?,
  ): JwtAuthenticationResponse {
    val delegate =
      thirdPartyAuthDelegates.find { it.name == serviceType }
        ?: throw NotFoundException(Message.SERVICE_NOT_FOUND)

    return delegate.getTokenResponse(code, invitationCode, redirectUri, domain)
  }
}
