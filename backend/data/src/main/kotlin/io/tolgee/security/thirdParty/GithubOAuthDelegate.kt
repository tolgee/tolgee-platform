package io.tolgee.security.thirdParty

import io.tolgee.configuration.tolgee.GithubAuthenticationProperties
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.model.enums.ThirdPartyAuthType
import io.tolgee.security.authentication.JwtService
import io.tolgee.security.payload.JwtAuthenticationResponse
import io.tolgee.security.thirdParty.data.ThirdPartyUserDetails
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.util.Arrays
import java.util.stream.Collectors

@Component
class GithubOAuthDelegate(
  private val jwtService: JwtService,
  private val restTemplate: RestTemplate,
  properties: TolgeeProperties,
  private val thirdPartyUserHandler: ThirdPartyUserHandler,
) : ThirdPartyAuthDelegate {
  private val githubConfigurationProperties: GithubAuthenticationProperties = properties.authentication.github

  override val name: String
    get() = "github"

  override val preferredAuthType: ThirdPartyAuthType
    get() = ThirdPartyAuthType.GITHUB

  override fun getTokenResponse(
    receivedCode: String?,
    invitationCode: String?,
    redirectUri: String?,
    domain: String?,
  ): JwtAuthenticationResponse {
    val body = HashMap<String, String?>()
    body["client_id"] = githubConfigurationProperties.clientId
    body["client_secret"] = githubConfigurationProperties.clientSecret
    body["code"] = receivedCode

    // get token to authorize to github api
    val response: MutableMap<*, *>? =
      restTemplate
        .postForObject(githubConfigurationProperties.authorizationUrl, body, MutableMap::class.java)
    if (response != null && response.containsKey("access_token")) {
      val headers = HttpHeaders()
      headers["Authorization"] = "token " + response["access_token"]
      val entity = HttpEntity<String?>(null, headers)

      // get github user data
      val exchange =
        restTemplate
          .exchange(githubConfigurationProperties.userUrl, HttpMethod.GET, entity, GithubUserResponse::class.java)
      if (exchange.statusCode != HttpStatus.OK || exchange.body == null) {
        throw AuthenticationException(Message.THIRD_PARTY_UNAUTHORIZED)
      }
      val userResponse = exchange.body

      // get github user emails
      val emails =
        restTemplate
          .exchange(
            githubConfigurationProperties.userUrl + "/emails",
            HttpMethod.GET,
            entity,
            Array<GithubEmailResponse>::class.java,
          ).body
          ?: throw AuthenticationException(Message.THIRD_PARTY_AUTH_NO_EMAIL)

      val verifiedEmails = Arrays.stream(emails).filter { it.verified }.collect(Collectors.toList())

      val githubEmail =
        (
          verifiedEmails.firstOrNull { it.primary }
            ?: verifiedEmails.firstOrNull()
        )?.email
          ?: throw AuthenticationException(Message.THIRD_PARTY_AUTH_NO_EMAIL)

      val userAccount =
        thirdPartyUserHandler.findOrCreateUser(
          ThirdPartyUserDetails.fromGithub(
            userResponse!!,
            githubEmail,
            invitationCode,
          ),
        )

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
  }

  class GithubEmailResponse {
    var email: String? = null
    var primary = false
    var verified = false
  }

  class GithubUserResponse {
    var name: String? = null
    var id: String? = null
    val login: String = ""
  }
}
