package io.tolgee.security.thirdParty

import io.tolgee.configuration.tolgee.OAuth2AuthenticationProperties
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.model.enums.ThirdPartyAuthType
import io.tolgee.security.authentication.JwtService
import io.tolgee.security.payload.JwtAuthenticationResponse
import io.tolgee.security.thirdParty.data.OAuthUserDetails
import io.tolgee.security.thirdParty.data.ThirdPartyUserDetails
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate

@Component
class OAuth2Delegate(
  private val jwtService: JwtService,
  private val restTemplate: RestTemplate,
  properties: TolgeeProperties,
  private val thirdPartyUserHandler: ThirdPartyUserHandler,
) : ThirdPartyAuthDelegate {
  private val oauth2ConfigurationProperties: OAuth2AuthenticationProperties = properties.authentication.oauth2
  private val logger = LoggerFactory.getLogger(this::class.java)

  override val name: String
    get() = "oauth2"

  override val preferredAuthType: ThirdPartyAuthType
    get() = ThirdPartyAuthType.OAUTH2

  override fun getTokenResponse(
    receivedCode: String?,
    invitationCode: String?,
    redirectUri: String?,
    domain: String?,
  ): JwtAuthenticationResponse {
    try {
      val body: MultiValueMap<String, String> = LinkedMultiValueMap<String, String>()
      body["client_id"] = oauth2ConfigurationProperties.clientId
      body["client_secret"] = oauth2ConfigurationProperties.clientSecret
      body["code"] = receivedCode
      body["grant_type"] = "authorization_code"
      body["redirect_uri"] = redirectUri

      if (oauth2ConfigurationProperties.tokenUrl.isNullOrBlank()) {
        throw AuthenticationException(Message.OAUTH2_TOKEN_URL_NOT_SET)
      }
      if (oauth2ConfigurationProperties.userUrl.isNullOrBlank()) {
        throw AuthenticationException(Message.OAUTH2_USER_URL_NOT_SET)
      }

      val requestHeaders = HttpHeaders()
      requestHeaders.contentType = MediaType.APPLICATION_FORM_URLENCODED

      val response: MutableMap<*, *>? =
        restTemplate.postForObject(
          oauth2ConfigurationProperties.tokenUrl!!,
          HttpEntity(body, requestHeaders),
          MutableMap::class.java,
        )

      if (response != null && response.containsKey("access_token")) {
        val headers = HttpHeaders()
        headers["Authorization"] = "Bearer ${response["access_token"]}"
        val entity = HttpEntity<String?>(null, headers)

        val exchange =
          restTemplate.exchange(
            oauth2ConfigurationProperties.userUrl!!,
            HttpMethod.GET,
            entity,
            GenericUserResponse::class.java,
          )
        if (exchange.statusCode != HttpStatus.OK || exchange.body == null) {
          logger.error("Failed to get user info from OAuth2 provider")
          throw AuthenticationException(Message.THIRD_PARTY_UNAUTHORIZED)
        }
        val userResponse = exchange.body

        if (userResponse?.sub == null) {
          logger.info("Third party 'sub' field is null.")
          throw AuthenticationException(Message.THIRD_PARTY_AUTH_NO_SUB)
        }

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
          )
        val user =
          thirdPartyUserHandler.findOrCreateUser(
            ThirdPartyUserDetails.fromOAuth2(
              userData,
              ThirdPartyAuthType.OAUTH2,
              invitationCode,
            ),
          )

        val jwt = jwtService.emitToken(user.id)
        return JwtAuthenticationResponse(jwt)
      }
      if (response == null) {
        logger.error("Error getting token from third party server. Response is null.")
        throw AuthenticationException(Message.THIRD_PARTY_AUTH_UNKNOWN_ERROR)
      }
      if (response.containsKey("error")) {
        logger.error("Error while getting token from third party: ${response["error"]}")
        throw AuthenticationException(Message.THIRD_PARTY_AUTH_ERROR_MESSAGE)
      }
      throw AuthenticationException(Message.THIRD_PARTY_AUTH_UNKNOWN_ERROR)
    } catch (e: HttpClientErrorException) {
      logger.error("Error while getting token from third party: {}", e.message)
      throw AuthenticationException(Message.THIRD_PARTY_AUTH_UNKNOWN_ERROR)
    }
  }

  @Suppress("PropertyName")
  class GenericUserResponse {
    var sub: String? = null
    var name: String? = null
    var given_name: String? = null
    var family_name: String? = null
    var email: String? = null
  }
}
