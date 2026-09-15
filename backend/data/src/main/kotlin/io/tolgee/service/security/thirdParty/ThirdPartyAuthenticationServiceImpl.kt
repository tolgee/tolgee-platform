package io.tolgee.service.security.thirdParty

import io.tolgee.constants.Message
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.enums.AuthAuditEventType
import io.tolgee.security.payload.JwtAuthenticationResponse
import io.tolgee.security.thirdParty.ThirdPartyAuthDelegate
import io.tolgee.service.security.AuthAuditService
import org.springframework.stereotype.Service

@Service
class ThirdPartyAuthenticationServiceImpl(
  private val thirdPartyAuthDelegates: List<ThirdPartyAuthDelegate>,
  private val authAuditService: AuthAuditService,
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

    try {
      return delegate.getTokenResponse(code, invitationCode, redirectUri, domain)
    } catch (e: AuthenticationException) {
      authAuditService.recordIndependently(
        type = AuthAuditEventType.LOGIN_FAILED_THIRD_PARTY,
        data =
          mutableMapOf(
            "serviceType" to serviceType,
            "reason" to e.tolgeeMessage?.name,
          ),
      )
      throw e
    }
  }
}
