package io.tolgee.security.thirdParty

import io.tolgee.configuration.tolgee.GoogleAuthenticationProperties
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.dtos.request.auth.AuthProviderChangeData
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ThirdPartyAuthType
import io.tolgee.security.authentication.JwtService
import io.tolgee.security.payload.JwtAuthenticationResponse
import io.tolgee.security.service.thirdParty.ThirdPartyAuthDelegate
import io.tolgee.service.TenantService
import io.tolgee.service.security.AuthProviderChangeService
import io.tolgee.service.security.SignUpService
import io.tolgee.service.security.UserAccountService
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate

@Component
class GoogleOAuthDelegate(
  private val jwtService: JwtService,
  private val userAccountService: UserAccountService,
  private val restTemplate: RestTemplate,
  properties: TolgeeProperties,
  private val signUpService: SignUpService,
  private val authProviderChangeService: AuthProviderChangeService,
  private val tenantService: TenantService,
) : ThirdPartyAuthDelegate {
  private val googleConfigurationProperties: GoogleAuthenticationProperties = properties.authentication.google

  override val name: String
    get() = "google"

  override fun getTokenResponse(
    receivedCode: String?,
    invitationCode: String?,
    redirectUri: String?,
    domain: String?,
  ): JwtAuthenticationResponse {
    try {
      val body = HashMap<String, String?>()
      body["client_id"] = googleConfigurationProperties.clientId
      body["client_secret"] = googleConfigurationProperties.clientSecret
      body["code"] = receivedCode
      body["grant_type"] = "authorization_code"
      body["redirect_uri"] = redirectUri

      // get token to authorize to google api
      val response: MutableMap<*, *>? =
        restTemplate
          .postForObject(googleConfigurationProperties.authorizationUrl, body, MutableMap::class.java)
      if (response != null && response.containsKey("access_token")) {
        val headers = HttpHeaders()
        headers["Authorization"] = "Bearer " + response["access_token"]
        val entity = HttpEntity<String?>(null, headers)

        // get google user data

        val exchange =
          restTemplate
            .exchange(googleConfigurationProperties.userUrl, HttpMethod.GET, entity, GoogleUserResponse::class.java)
        if (exchange.statusCode != HttpStatus.OK || exchange.body == null) {
          throw AuthenticationException(Message.THIRD_PARTY_UNAUTHORIZED)
        }
        val userResponse = exchange.body ?: throw AuthenticationException(Message.THIRD_PARTY_AUTH_UNKNOWN_ERROR)

        // check google email verified
        if (userResponse.email_verified == false) {
          throw AuthenticationException(Message.THIRD_PARTY_AUTH_NO_EMAIL)
        }

        // Split the comma-separated list of domains into a List
        val allowedDomains =
          googleConfigurationProperties.workspaceDomain?.split(",")?.map { it.trim() }
            ?: listOf()

        // ensure that only Google Workspace users from allowed domains can log in
        if (allowedDomains.isNotEmpty()) {
          if (!allowedDomains.contains(userResponse.hd)) {
            throw AuthenticationException(Message.THIRD_PARTY_GOOGLE_WORKSPACE_MISMATCH)
          }
        }

        val userAccount = findOrCreateAccount(userResponse, invitationCode)

        val jwt = jwtService.emitToken(userAccount.id)
        return JwtAuthenticationResponse(jwt)
      }
      if (response == null) {
        throw AuthenticationException(Message.THIRD_PARTY_AUTH_UNKNOWN_ERROR)
      }

      if (response.containsKey("error")) {
        throw AuthenticationException(Message.THIRD_PARTY_AUTH_ERROR_MESSAGE)
      }
      throw AuthenticationException(Message.THIRD_PARTY_AUTH_UNKNOWN_ERROR)
    } catch (e: HttpClientErrorException) {
      throw AuthenticationException(Message.THIRD_PARTY_AUTH_UNKNOWN_ERROR, null, e)
    }
  }

  private fun checkNotManagedByOrganization(domain: String?) {
    if (tenantService.getEnabledConfigByDomainOrNull(domain) != null) {
      // There is sso configured for the domain - don't allow sign up
      throw AuthenticationException(Message.USE_SSO_FOR_AUTHENTICATION_INSTEAD, listOf(domain))
    }
  }

  private fun findOrCreateAccount(
    userResponse: GoogleUserResponse,
    invitationCode: String?,
  ): UserAccount {
    val googleEmail = userResponse.email ?: throw AuthenticationException(Message.THIRD_PARTY_AUTH_NO_EMAIL)

    val userAccount = userAccountService.findByThirdParty(ThirdPartyAuthType.GOOGLE, userResponse.sub!!)

    authProviderChangeService.tryInitiate(
      userAccount,
      googleEmail,
      AuthProviderChangeData(
        UserAccount.AccountType.THIRD_PARTY,
        ThirdPartyAuthType.GOOGLE,
        userResponse.sub,
      ),
    )

    if (userAccount != null) {
      return userAccount
    }

    userAccountService.findActive(googleEmail)?.let {
      throw AuthenticationException(Message.USERNAME_ALREADY_EXISTS)
    }

    val newUserAccount = UserAccount()
    newUserAccount.username = userResponse.email
      ?: throw AuthenticationException(Message.THIRD_PARTY_AUTH_NO_EMAIL)
    newUserAccount.name = userResponse.name ?: (userResponse.given_name + " " + userResponse.family_name)
    newUserAccount.thirdPartyAuthId = userResponse.sub
    newUserAccount.thirdPartyAuthType = ThirdPartyAuthType.GOOGLE
    newUserAccount.accountType = UserAccount.AccountType.THIRD_PARTY

    checkNotManagedByOrganization(newUserAccount.domain)
    signUpService.signUp(newUserAccount, invitationCode, null)

    return newUserAccount
  }

  @Suppress("PropertyName")
  class GoogleUserResponse {
    var sub: String? = null
    var name: String? = null
    var given_name: String? = null
    var family_name: String? = null
    var picture: String? = null
    var email: String? = null
    var email_verified: Boolean? = false
    var locale: String? = null
    var hd: String? = null
  }
}
