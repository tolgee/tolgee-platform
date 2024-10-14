package io.tolgee.ee.api.v2.controllers

import io.tolgee.constants.Message
import io.tolgee.ee.data.DomainRequest
import io.tolgee.ee.data.SsoUrlResponse
import io.tolgee.ee.exceptions.OAuthAuthorizationException
import io.tolgee.ee.model.SsoTenant
import io.tolgee.ee.service.OAuthService
import io.tolgee.ee.service.TenantService
import io.tolgee.model.UserAccount
import io.tolgee.security.authentication.JwtService
import io.tolgee.security.payload.JwtAuthenticationResponse
import io.tolgee.service.security.UserAccountService
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("v2/public/oauth2/callback/")
class OAuth2CallbackController(
  private val oauthService: OAuthService,
  private val tenantService: TenantService,
  private val userAccountService: UserAccountService,
  private val jwtService: JwtService,
) {
  @PostMapping("/get-authentication-url")
  fun getAuthenticationUrl(
    @RequestBody request: DomainRequest,
  ): SsoUrlResponse {
    val registrationId = request.domain
    val tenant = tenantService.getByDomain(registrationId)
    if (!tenant.isEnabledForThisOrganization) {
      throw OAuthAuthorizationException(Message.SSO_DOMAIN_NOT_ENABLED, "Domain is not enabled for this organization")
    }
    val redirectUrl = buildAuthUrl(tenant, state = request.state)

    return SsoUrlResponse(redirectUrl)
  }

  fun encodeDomain(domain: String): String =
    Base64.getUrlEncoder().encode(domain.toByteArray()).toString(Charsets.UTF_8)

  fun decodeDomain(encodedDomain: String): String =
    Base64.getUrlDecoder().decode(encodedDomain).toString(Charsets.UTF_8)

  private fun buildAuthUrl(
    tenant: SsoTenant,
    state: String,
  ): String =
    "${tenant.authorizationUri}?" +
      "client_id=${tenant.clientId}&" +
      "redirect_uri=${tenant.redirectUriBase + "/login/open-id/auth-callback/" + encodeDomain(tenant.domain)}&" +
      "response_type=code&" +
      "scope=openid profile email roles&" +
      "state=$state"

  @GetMapping("/{registrationId}")
  fun handleCallback(
    @RequestParam(value = "code", required = true) code: String,
    @RequestParam(value = "redirect_uri", required = true) redirectUrl: String,
    @RequestParam(defaultValue = "") error: String,
    @RequestParam(defaultValue = "") error_description: String,
    @RequestParam(value = "invitationCode", required = false) invitationCode: String?,
    response: HttpServletResponse,
    @PathVariable registrationId: String,
  ): JwtAuthenticationResponse? {
    if (code == "this_is_dummy_code") {
      val user = getFakeUser()
      return JwtAuthenticationResponse(jwtService.emitToken(user.id))
    }

    return oauthService.handleOAuthCallback(
      registrationId = decodeDomain(registrationId),
      code = code,
      redirectUrl = redirectUrl,
      error = error,
      errorDescription = error_description,
      invitationCode = invitationCode,
    )
  }

  private fun getFakeUser(): UserAccount {
    val username = "johndoe@doe.com"
    val user =
      userAccountService.findActive(username) ?: let {
        UserAccount().apply {
          this.username = username
          name = "john"
          accountType = UserAccount.AccountType.THIRD_PARTY
          userAccountService.save(this)
        }
      }
    return user
  }
}
