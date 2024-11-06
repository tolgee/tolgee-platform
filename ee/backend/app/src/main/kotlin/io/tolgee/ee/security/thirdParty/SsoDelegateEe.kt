package io.tolgee.ee.security.thirdParty

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.jwk.source.RemoteJWKSet
import com.nimbusds.jose.proc.JWSAlgorithmFamilyJWSKeySelector
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor
import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.constants.Feature
import io.tolgee.constants.Message
import io.tolgee.ee.data.GenericUserResponse
import io.tolgee.ee.data.OAuth2TokenResponse
import io.tolgee.ee.exceptions.SsoAuthorizationException
import io.tolgee.ee.service.sso.TenantService
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ThirdPartyAuthType
import io.tolgee.security.authentication.JwtService
import io.tolgee.security.payload.JwtAuthenticationResponse
import io.tolgee.security.service.thirdParty.SsoDelegate
import io.tolgee.security.thirdParty.OAuthUserHandler
import io.tolgee.security.thirdParty.SsoTenantConfig
import io.tolgee.security.thirdParty.data.OAuthUserDetails
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.http.*
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import java.net.URL
import java.util.*

@Component
class SsoDelegateEe(
  private val jwtService: JwtService,
  private val restTemplate: RestTemplate,
  private val jwtProcessor: ConfigurableJWTProcessor<SecurityContext>,
  private val tenantService: TenantService,
  private val oAuthUserHandler: OAuthUserHandler,
  private val currentDateProvider: CurrentDateProvider,
  private val enabledFeaturesProvider: EnabledFeaturesProvider,
) : SsoDelegate, Logging {
  override fun getTokenResponse(
    code: String?,
    invitationCode: String?,
    redirectUri: String?,
    domain: String?,
  ): JwtAuthenticationResponse {
    val tenant = tenantService.getEnabledConfigByDomain(domain)
    enabledFeaturesProvider.checkFeatureEnabled(
      organizationId = tenant.organization?.id,
      Feature.SSO,
    )

    val token =
      fetchToken(tenant, code, redirectUri)
        ?: throw SsoAuthorizationException(Message.SSO_TOKEN_EXCHANGE_FAILED)

    val userInfo = decodeIdToken(token.id_token, tenant.jwkSetUri)
    return getTokenResponseForUser(userInfo, tenant, invitationCode, token.refresh_token)
  }

  private fun fetchToken(
    tenant: SsoTenantConfig,
    code: String?,
    redirectUrl: String?,
  ): OAuth2TokenResponse? {
    val headers =
      HttpHeaders().apply {
        contentType = MediaType.APPLICATION_FORM_URLENCODED
      }

    val body: MultiValueMap<String, String> = LinkedMultiValueMap()
    body.add("grant_type", "authorization_code")
    body.add("code", code)
    body.add("redirect_uri", redirectUrl)
    body.add("client_id", tenant.clientId)
    body.add("client_secret", tenant.clientSecret)
    body.add("scope", "openid")

    val request = HttpEntity(body, headers)
    return try {
      val response: ResponseEntity<OAuth2TokenResponse> =
        restTemplate.exchange(
          tenant.tokenUri,
          HttpMethod.POST,
          request,
          OAuth2TokenResponse::class.java,
        )
      response.body
    } catch (e: RestClientException) {
      logger.info("Failed to exchange code for token: ${e.message}")
      null
    }
  }

  private fun decodeIdToken(
    idToken: String,
    jwkSetUri: String,
  ): GenericUserResponse {
    try {
      val signedJWT = SignedJWT.parse(idToken)

      val jwkSource: JWKSource<SecurityContext> = RemoteJWKSet(URL(jwkSetUri))

      val keySelector = JWSAlgorithmFamilyJWSKeySelector(JWSAlgorithm.Family.RSA, jwkSource)
      jwtProcessor.jwsKeySelector = keySelector

      val jwtClaimsSet: JWTClaimsSet = jwtProcessor.process(signedJWT, null)

      val expirationTime: Date = jwtClaimsSet.expirationTime
      if (expirationTime.before(Date())) {
        throw SsoAuthorizationException(Message.SSO_ID_TOKEN_EXPIRED)
      }

      return GenericUserResponse().apply {
        sub = jwtClaimsSet.subject
        name = jwtClaimsSet.getStringClaim("name")
        given_name = jwtClaimsSet.getStringClaim("given_name")
        family_name = jwtClaimsSet.getStringClaim("family_name")
        email = jwtClaimsSet.getStringClaim("email")
      }
    } catch (e: Exception) {
      logger.info(e.stackTraceToString())
      throw SsoAuthorizationException(Message.SSO_USER_INFO_RETRIEVAL_FAILED)
    }
  }

  private fun getTokenResponseForUser(
    userResponse: GenericUserResponse,
    tenant: SsoTenantConfig,
    invitationCode: String?,
    refreshToken: String,
  ): JwtAuthenticationResponse {
    val email =
      userResponse.email ?: let {
        logger.info("Third party user email is null. Missing scope email?")
        throw AuthenticationException(Message.THIRD_PARTY_AUTH_NO_EMAIL)
      }
    val userData =
      OAuthUserDetails(
        sub = userResponse.sub!!,
        name = userResponse.name,
        givenName = userResponse.given_name,
        familyName = userResponse.family_name,
        email = email,
        refreshToken = refreshToken,
        tenant = tenant,
      )
    val user =
      oAuthUserHandler.findOrCreateUser(
        userData,
        invitationCode,
        ThirdPartyAuthType.SSO,
        UserAccount.AccountType.MANAGED,
      )
    val jwt = jwtService.emitToken(user.id)
    return JwtAuthenticationResponse(jwt)
  }

  override fun verifyUserSsoAccountAvailable(
    ssoDomain: String?,
    userId: Long,
    refreshToken: String?,
    thirdPartyAuth: ThirdPartyAuthType,
    ssoSessionExpiry: Date?,
  ): Boolean {
    if (thirdPartyAuth != ThirdPartyAuthType.SSO) {
      return true
    }

    if (ssoDomain == null || refreshToken == null) {
      throw AuthenticationException(Message.SSO_CANT_VERIFY_USER)
    }

    if (ssoSessionExpiry != null && isSsoUserValid(ssoSessionExpiry)) {
      return true
    }

    val tenant = tenantService.getEnabledConfigByDomain(ssoDomain)
    enabledFeaturesProvider.checkFeatureEnabled(
      organizationId = tenant.organization?.id,
      Feature.SSO,
    )
    val headers =
      HttpHeaders().apply {
        contentType = MediaType.APPLICATION_FORM_URLENCODED
      }

    val body: MultiValueMap<String, String> = LinkedMultiValueMap()
    body.add("grant_type", "refresh_token")
    body.add("client_id", tenant.clientId)
    body.add("client_secret", tenant.clientSecret)
    body.add("scope", "offline_access openid")
    body.add("refresh_token", refreshToken)

    val request = HttpEntity(body, headers)
    return try {
      val response: ResponseEntity<OAuth2TokenResponse> =
        restTemplate.exchange(
          tenant.tokenUri,
          HttpMethod.POST,
          request,
          OAuth2TokenResponse::class.java,
        )
      if (response.body?.refresh_token != null) {
        oAuthUserHandler.updateSsoSessionExpiry(userId)
        oAuthUserHandler.updateRefreshToken(userId, response.body?.refresh_token)
        return true
      }
      false
    } catch (e: RestClientException) {
      logger.info("Failed to refresh token: ${e.message}")
      false
    }
  }

  private fun isSsoUserValid(ssoSessionExpiry: Date): Boolean = ssoSessionExpiry.after(currentDateProvider.date)
}
