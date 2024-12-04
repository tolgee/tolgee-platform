package io.tolgee.ee.api.v2.controllers

import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.component.FrontendUrlProvider
import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.constants.Feature
import io.tolgee.dtos.sso.SsoTenantConfig
import io.tolgee.ee.data.DomainRequest
import io.tolgee.ee.data.SsoUrlResponse
import io.tolgee.service.TenantService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/public")
@Tag(name = "Authentication")
class SsoAuthController(
  private val tenantService: TenantService,
  private val frontendUrlProvider: FrontendUrlProvider,
  private val enabledFeaturesProvider: EnabledFeaturesProvider,
) {
  @PostMapping("/authorize_oauth/sso/authentication-url")
  fun getAuthenticationUrl(
    @RequestBody request: DomainRequest,
  ): SsoUrlResponse {
    // FIXME: Maybe instead of this pretty specific endpoint, we could have an endpoint that returns tenant info and
    //  frontend can take care of building the URL like it already does for other login providers
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
      "redirect_uri=${frontendUrlProvider.url + "/login/auth_callback/sso"}&" +
      "response_type=code&" +
      "scope=openid profile email offline_access&" +
      "state=$state"
}
