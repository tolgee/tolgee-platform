package io.tolgee.ee.security.thirdParty

import io.jsonwebtoken.JwtParser
import io.jsonwebtoken.Jwts
import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Feature
import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.dtos.sso.SsoTenantConfig
import io.tolgee.ee.data.GenericUserResponse
import io.tolgee.ee.data.OAuth2TokenResponse
import io.tolgee.ee.exceptions.SsoAuthorizationException
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.enums.ThirdPartyAuthType
import io.tolgee.security.authentication.JwtService
import io.tolgee.security.payload.JwtAuthenticationResponse
import io.tolgee.security.thirdParty.SsoDelegate
import io.tolgee.security.thirdParty.ThirdPartyUserHandler
import io.tolgee.security.thirdParty.data.OAuthUserDetails
import io.tolgee.security.thirdParty.data.ThirdPartyUserDetails
import io.tolgee.service.TenantService
import io.tolgee.service.organization.OrganizationRoleService
import io.tolgee.service.security.UserAccountService
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.context.annotation.Primary
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import java.util.Date

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@Primary
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class SsoDelegateEe(
  private val jwtService: JwtService,
  private val restTemplate: RestTemplate,
  private val tolgeeProperties: TolgeeProperties,
  private val organizationRoleService: OrganizationRoleService,
  private val tenantService: TenantService,
  private val thirdPartyUserHandler: ThirdPartyUserHandler,
  private val userAccountService: UserAccountService,
  private val currentDateProvider: CurrentDateProvider,
  private val enabledFeaturesProvider: EnabledFeaturesProvider,
) : SsoDelegate,
  Logging {
  private val jwtParser: JwtParser =
    Jwts
      .parserBuilder()
      .setClock { currentDateProvider.date }
      .build()

  override fun getTokenResponse(
    code: String?,
    invitationCode: String?,
    redirectUri: String?,
    domain: String?,
  ): JwtAuthenticationResponse {
    if (domain.isNullOrEmpty()) {
      throw BadRequestException(Message.SSO_AUTH_MISSING_DOMAIN)
    }

    val tenant = tenantService.getEnabledConfigByDomain(domain)
    enabledFeaturesProvider.checkFeatureEnabled(
      organizationId = tenant.organizationId,
      Feature.SSO,
    )

    val token =
      fetchToken(tenant, code, redirectUri)
        ?: throw SsoAuthorizationException(Message.SSO_TOKEN_EXCHANGE_FAILED)

    val userInfo = decodeIdTokenUnsafe(token.id_token)
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

  private fun decodeIdTokenUnsafe(idToken: String): GenericUserResponse {
    // We assume the token was received directly from the SSO provider and is safe - no need to verify the signature.
    try {
      val jwt = idToken.substring(0, idToken.lastIndexOf('.') + 1)
      val claims = jwtParser.parseClaimsJwt(jwt).body

      val expirationTime: Date? = claims.expiration
      if (expirationTime?.before(currentDateProvider.date) == true) {
        throw SsoAuthorizationException(Message.SSO_ID_TOKEN_EXPIRED)
      }

      return GenericUserResponse().apply {
        sub = claims.subject
        name = claims.get("name", String::class.java)
        given_name = claims.get("given_name", String::class.java)
        family_name = claims.get("family_name", String::class.java)
        email = claims.get("email", String::class.java)
      }
    } catch (e: Exception) {
      logger.warn(e.stackTraceToString())
      throw SsoAuthorizationException(Message.SSO_USER_INFO_RETRIEVAL_FAILED, cause = e)
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
    val emailDomain = email.takeIf { it.count { c -> c == '@' } == 1 }?.split('@')?.getOrNull(1)
    if (emailDomain.isNullOrEmpty() || tenant.domain != emailDomain) {
      throw AuthenticationException(Message.THIRD_PARTY_AUTH_NON_MATCHING_EMAIL)
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
      thirdPartyUserHandler.findOrCreateUser(
        ThirdPartyUserDetails.fromOAuth2(
          userData,
          if (tenant.global) ThirdPartyAuthType.SSO_GLOBAL else ThirdPartyAuthType.SSO,
          invitationCode,
        ),
      )
    val jwt = jwtService.emitToken(user.id)
    return JwtAuthenticationResponse(jwt)
  }

  fun fetchOrganizationsSsoDomainFor(userId: Long): String? {
    val organization = organizationRoleService.getManagedBy(userId) ?: return null
    val tenant = tenantService.findTenant(organization.id)
    return tenant?.domain
  }

  override fun verifyUserSsoAccountAvailable(user: UserAccountDto): Boolean {
    val isSsoUser = user.thirdPartyAuth in arrayOf(ThirdPartyAuthType.SSO, ThirdPartyAuthType.SSO_GLOBAL)
    if (!isSsoUser) {
      return true
    }
    val isSessionExpired = user.ssoSessionExpiry?.after(currentDateProvider.date) != true
    if (!isSessionExpired) {
      return true
    }

    val domain =
      when (user.thirdPartyAuth) {
        ThirdPartyAuthType.SSO -> fetchOrganizationsSsoDomainFor(user.id)
        ThirdPartyAuthType.SSO_GLOBAL -> tolgeeProperties.authentication.ssoGlobal.domain
        else -> null
      }
    val refreshToken = user.ssoRefreshToken

    if (domain == null || refreshToken == null) {
      return false
    }

    return updateRefreshToken(user, domain, refreshToken)
  }

  private fun updateRefreshToken(
    user: UserAccountDto,
    domain: String,
    refreshToken: String,
  ): Boolean {
    val tenant = tenantService.getEnabledConfigByDomain(domain)

    val response = fetchRefreshToken(tenant, refreshToken)
    if (response?.refresh_token == null) {
      return false
    }

    val userAccount = userAccountService.get(user.id)
    userAccountService.updateSsoSession(userAccount, response.refresh_token)
    return true
  }

  private fun fetchRefreshToken(
    tenant: SsoTenantConfig,
    refreshToken: String,
  ): OAuth2TokenResponse? {
    val headers =
      HttpHeaders().apply {
        contentType = MediaType.APPLICATION_FORM_URLENCODED
      }

    val body: MultiValueMap<String, String> = LinkedMultiValueMap()
    body.apply {
      add("grant_type", "refresh_token")
      add("client_id", tenant.clientId)
      add("client_secret", tenant.clientSecret)
      add("scope", "offline_access openid")
      add("refresh_token", refreshToken)
    }

    val request = HttpEntity(body, headers)
    try {
      val response: ResponseEntity<OAuth2TokenResponse> =
        restTemplate.exchange(
          tenant.tokenUri,
          HttpMethod.POST,
          request,
          OAuth2TokenResponse::class.java,
        )
      return response.body
    } catch (e: RestClientException) {
      logger.info("Failed to refresh token: ${e.message}")
    }
    return null
  }
}
