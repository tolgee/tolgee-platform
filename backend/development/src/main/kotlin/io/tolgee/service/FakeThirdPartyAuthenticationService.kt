package io.tolgee.service

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.dtos.sso.SsoTenantConfig
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.enums.ThirdPartyAuthType
import io.tolgee.security.authentication.JwtService
import io.tolgee.security.payload.JwtAuthenticationResponse
import io.tolgee.security.thirdParty.ThirdPartyAuthDelegate
import io.tolgee.security.thirdParty.ThirdPartyUserHandler
import io.tolgee.security.thirdParty.data.ThirdPartyUserDetails
import io.tolgee.service.security.thirdParty.ThirdPartyAuthenticationService
import io.tolgee.service.security.thirdParty.ThirdPartyAuthenticationServiceImpl
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Primary
@Service
@ConditionalOnProperty("tolgee.internal.fake-third-party-login", havingValue = "true")
class FakeThirdPartyAuthenticationService(
  private val tolgeeProperties: TolgeeProperties,
  private val thirdPartyAuthenticationService: ThirdPartyAuthenticationServiceImpl,
  private val thirdPartyAuthDelegates: List<ThirdPartyAuthDelegate>,
  private val tenantService: TenantService,
  private val thirdPartyUserHandler: ThirdPartyUserHandler,
  private val jwtService: JwtService,
) : ThirdPartyAuthenticationService {
  override fun authenticate(
    serviceType: String?,
    code: String?,
    redirectUri: String?,
    invitationCode: String?,
    domain: String?,
  ): JwtAuthenticationResponse {
    assert(tolgeeProperties.internal.fakeThirdPartyLogin)

    if (code?.startsWith(DUMMY_CODE_PREFIX) == true) {
      val delegate =
        thirdPartyAuthDelegates.find { it.name == serviceType }
          ?: throw NotFoundException(Message.SERVICE_NOT_FOUND)

      val username = code.removePrefix(DUMMY_CODE_PREFIX)
      val data =
        DummyThirdPartyUserDetails(
          username = username,
          name = username,
          thirdPartyAuthType = delegate.preferredAuthType,
        )
      return fakeThirdPartyLogin(data, invitationCode, domain)
    }

    return thirdPartyAuthenticationService.authenticate(
      serviceType = serviceType,
      code = code,
      redirectUri = redirectUri,
      invitationCode = invitationCode,
      domain = domain,
    )
  }

  private fun fakeThirdPartyLogin(
    data: DummyThirdPartyUserDetails,
    invitationCode: String?,
    domain: String?,
  ): JwtAuthenticationResponse {
    var tenant: SsoTenantConfig? = null
    if (data.thirdPartyAuthType == ThirdPartyAuthType.SSO) {
      tenant = tenantService.getEnabledConfigByDomain(domain)
      if (tenant.global) {
        // We have found global tenant - fix the auth type accordingly
        data.thirdPartyAuthType = ThirdPartyAuthType.SSO_GLOBAL
      }
    }
    val user =
      thirdPartyUserHandler.findOrCreateUser(
        ThirdPartyUserDetails(
          authId = "dummy_auth_id",
          username = data.username,
          name = data.name,
          thirdPartyAuthType = data.thirdPartyAuthType,
          invitationCode = invitationCode,
          refreshToken = null,
          tenant = tenant,
        ),
      )
    val jwt = jwtService.emitToken(user.id)
    return JwtAuthenticationResponse(jwt)
  }

  data class DummyThirdPartyUserDetails(
    var username: String,
    var name: String,
    var thirdPartyAuthType: ThirdPartyAuthType,
  )

  companion object {
    const val DUMMY_CODE_PREFIX = "this_is_dummy_code_"
  }
}
