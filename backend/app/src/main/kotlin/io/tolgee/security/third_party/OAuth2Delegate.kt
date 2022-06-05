package io.tolgee.security.third_party

import com.sun.istack.Nullable
import io.tolgee.configuration.tolgee.OAuth2AuthenticationProperties
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.model.Invitation
import io.tolgee.model.UserAccount
import io.tolgee.security.JwtTokenProviderImpl
import io.tolgee.security.payload.JwtAuthenticationResponse
import io.tolgee.service.InvitationService
import io.tolgee.service.UserAccountService
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate

@Component
class OAuth2Delegate(
  private val tokenProvider: JwtTokenProviderImpl,
  private val userAccountService: UserAccountService,
  private val restTemplate: RestTemplate,
  private val properties: TolgeeProperties,
  private val invitationService: InvitationService
) {
  private val oauth2ConfigurationProperties: OAuth2AuthenticationProperties = properties.authentication.oauth2

  fun getTokenResponse(
    receivedCode: String?,
    @Nullable invitationCode: String?,
    redirectUri: String?
  ): JwtAuthenticationResponse {
    try {
      val body = HashMap<String, String?>()
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

      val response: MutableMap<*, *>? = restTemplate
        .postForObject(oauth2ConfigurationProperties.tokenUrl!!, body, MutableMap::class.java)

      if (response != null && response.containsKey("access_token")) {
        val headers = HttpHeaders()
        headers["Authorization"] = "Bearer ${response["access_token"]}"
        val entity = HttpEntity<String?>(null, headers)

        val exchange = restTemplate
          .exchange(oauth2ConfigurationProperties.userUrl!!, HttpMethod.GET, entity, GenericUserResponse::class.java)
        if (exchange.statusCode != HttpStatus.OK || exchange.body == null) {
          throw AuthenticationException(Message.THIRD_PARTY_UNAUTHORIZED)
        }
        val userResponse = exchange.body
        val userAccountOptional = userAccountService.findByThirdParty("oauth2", userResponse!!.sub)
        val user = userAccountOptional.orElseGet {
          userAccountService.findOptional(userResponse.email).ifPresent {
            throw AuthenticationException(Message.USERNAME_ALREADY_EXISTS)
          }

          var invitation: Invitation? = null
          if (invitationCode == null) {
            if (!properties.authentication.registrationsAllowed) {
              throw AuthenticationException(Message.REGISTRATIONS_NOT_ALLOWED)
            }
          } else {
            invitation = invitationService.getInvitation(invitationCode)
          }

          val newUserAccount = UserAccount()
          newUserAccount.username = userResponse.email
            ?: throw AuthenticationException(Message.THIRD_PARTY_AUTH_NO_EMAIL)
          newUserAccount.name = userResponse.name ?: (userResponse.given_name + " " + userResponse.family_name)
          newUserAccount.thirdPartyAuthId = userResponse.sub
          newUserAccount.thirdPartyAuthType = "oauth2"
          userAccountService.createUser(newUserAccount)
          if (invitation != null) {
            invitationService.accept(invitation.code, newUserAccount)
          }
          newUserAccount
        }
        val jwt = tokenProvider.generateToken(user.id).toString()
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
      throw AuthenticationException(Message.THIRD_PARTY_AUTH_UNKNOWN_ERROR)
    }
  }

  class GenericUserResponse {
    var sub: String? = null
    var name: String? = null
    var given_name: String? = null
    var family_name: String? = null
    var email: String? = null
  }
}
