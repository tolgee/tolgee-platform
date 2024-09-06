package io.tolgee.ee.api.v2.controllers

import io.tolgee.ee.repository.DynamicOAuth2ClientRegistrationRepository
import io.tolgee.ee.service.OAuthService
import io.tolgee.security.payload.JwtAuthenticationResponse
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("v2/oauth2/callback/")
class OAuth2CallbackController(
  private val dynamicOAuth2ClientRegistrationRepository: DynamicOAuth2ClientRegistrationRepository,
  private val oauthService: OAuthService,
) {
  @PostMapping("/get-authentication-url")
  fun getAuthenticationUrl(
    @RequestBody request: DomainRequest,
  ): SsoUrlResponse {
    val registrationId = request.domain
    val clientRegistration = dynamicOAuth2ClientRegistrationRepository.findByRegistrationId(registrationId)
    val redirectUrl = buildAuthUrl(clientRegistration, state = request.state)

    return SsoUrlResponse(redirectUrl)
  }

  private fun buildAuthUrl(
    clientRegistration: ClientRegistration,
    state: String,
  ): String {
    return "${clientRegistration.providerDetails.authorizationUri}?" +
      "client_id=${clientRegistration.clientId}&" +
      "redirect_uri=${clientRegistration.redirectUri}&" +
      "response_type=code&" +
      "scope=${clientRegistration.scopes.joinToString(" ")}&" +
      "state=$state"
  }

  @GetMapping("/{registrationId}")
  fun handleCallback(
    @RequestParam(value = "code", required = true) code: String,
    @RequestParam(value = "redirect_uri", required = true) redirectUrl: String,
    @RequestParam(defaultValue = "") error: String,
    @RequestParam(defaultValue = "") error_description: String,
    response: HttpServletResponse,
    @PathVariable registrationId: String,
  ): JwtAuthenticationResponse? {
    return oauthService.handleOAuthCallback(
      registrationId = registrationId,
      code = code,
      redirectUrl = redirectUrl,
      error = error,
      errorDescription = error_description,
    )
  }

  data class DomainRequest(val domain: String, val state: String)

  data class SsoUrlResponse(val redirectUrl: String)
}
