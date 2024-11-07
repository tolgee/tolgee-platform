package io.tolgee.ee.api.v2.controllers

import io.tolgee.component.FrontendUrlProvider
import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.constants.Feature
import io.tolgee.ee.data.DomainRequest
import io.tolgee.ee.data.SsoUrlResponse
import io.tolgee.ee.service.sso.TenantService
import io.tolgee.security.thirdParty.SsoTenantConfig
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("v2/public/oauth2/callback/")
class OAuth2CallbackController(
  private val tenantService: TenantService,
  private val frontendUrlProvider: FrontendUrlProvider,
  private val enabledFeaturesProvider: EnabledFeaturesProvider,
) {
  // TODO: Move to PublicController?
  @PostMapping("/get-authentication-url")
  fun getAuthenticationUrl(
    @RequestBody request: DomainRequest,
  ): SsoUrlResponse {
    val registrationId = request.domain
    val tenant = tenantService.getEnabledConfigByDomain(registrationId)
    enabledFeaturesProvider.checkFeatureEnabled(
      organizationId = tenant.organization?.id,
      Feature.SSO,
    )
    val redirectUrl = buildAuthUrl(tenant, state = request.state)

    return SsoUrlResponse(redirectUrl)
  }

  private fun buildAuthUrl(
    tenant: SsoTenantConfig,
    state: String,
  ): String =
    "${tenant.authorizationUri}?" +
      "client_id=${tenant.clientId}&" +
      "redirect_uri=${frontendUrlProvider.url + "/login/open-id/auth-callback"}&" +
      "response_type=code&" +
      "scope=openid profile email offline_access&" +
      "state=$state"
}
