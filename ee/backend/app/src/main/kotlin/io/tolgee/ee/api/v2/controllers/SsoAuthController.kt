package io.tolgee.ee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.tolgee.component.FrontendUrlProvider
import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.constants.Feature
import io.tolgee.controllers.AuthenticationTag
import io.tolgee.dtos.sso.SsoTenantConfig
import io.tolgee.ee.data.DomainRequest
import io.tolgee.ee.data.SsoUrlResponse
import io.tolgee.service.TenantService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@RestController
@RequestMapping("/api/public")
@AuthenticationTag
class SsoAuthController(
  private val tenantService: TenantService,
  private val frontendUrlProvider: FrontendUrlProvider,
  private val enabledFeaturesProvider: EnabledFeaturesProvider,
) {
  @PostMapping("/authorize_oauth/sso/authentication-url")
  @Operation(
    summary = "Generate authentication url (third-party, SSO)",
    description = "Returns URL which can be used to authenticate user using third party SSO service",
  )
  fun getAuthenticationUrl(
    @RequestBody request: DomainRequest,
  ): SsoUrlResponse {
    val registrationId = request.domain
    val tenant = tenantService.getEnabledConfigByDomain(registrationId)
    enabledFeaturesProvider.checkFeatureEnabled(
      organizationId = tenant.organizationId,
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
