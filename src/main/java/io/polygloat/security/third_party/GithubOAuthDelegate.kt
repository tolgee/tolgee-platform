package io.polygloat.security.third_party

import com.sun.istack.Nullable
import io.polygloat.configuration.polygloat.GithubAuthenticationProperties
import io.polygloat.configuration.polygloat.PolygloatProperties
import io.polygloat.constants.Message
import io.polygloat.exceptions.AuthenticationException
import io.polygloat.model.Invitation
import io.polygloat.model.UserAccount
import io.polygloat.security.JwtTokenProvider
import io.polygloat.security.payload.JwtAuthenticationResponse
import io.polygloat.service.InvitationService
import io.polygloat.service.UserAccountService
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.util.*

@Component
class GithubOAuthDelegate(private val tokenProvider: JwtTokenProvider,
                          private val userAccountService: UserAccountService,
                          private val restTemplate: RestTemplate,
                          private val properties: PolygloatProperties,
                          private val invitationService: InvitationService) {
    private val githubConfigurationProperties: GithubAuthenticationProperties = properties.authentication.github

    fun getTokenResponse(receivedCode: String?, @Nullable invitationCode: String?): JwtAuthenticationResponse {
        val body = HashMap<String, String?>()
        body["client_id"] = githubConfigurationProperties.clientId
        body["client_secret"] = githubConfigurationProperties.clientSecret
        body["code"] = receivedCode

        //get token to authorize to github api
        val response: MutableMap<*, *>? = restTemplate.postForObject(githubConfigurationProperties.authorizationUrl, body, MutableMap::class.java)
        if (response != null && response.containsKey("access_token")) {
            val headers = HttpHeaders()
            headers["Authorization"] = "token " + response["access_token"]
            val entity = HttpEntity<String?>(null, headers)


            //get github user data
            val exchange = restTemplate.exchange(githubConfigurationProperties.userUrl, HttpMethod.GET, entity, GithubUserResponse::class.java)
            if (exchange.statusCode != HttpStatus.OK || exchange.body == null) {
                throw AuthenticationException(Message.THIRD_PARTY_UNAUTHORIZED)
            }
            val userResponse = exchange.body

            //get github user emails
            val emails = restTemplate.exchange(
                    githubConfigurationProperties.userUrl + "/emails", HttpMethod.GET, entity,
                    Array<GithubEmailResponse>::class.java).body
                    ?: throw AuthenticationException(Message.THIRD_PARTY_AUTH_NO_EMAIL)

            val githubEmail = Arrays.stream(emails).filter { obj: GithubEmailResponse -> obj.isPrimary }
                    .findFirst().orElse(null)
                    ?: throw AuthenticationException(Message.THIRD_PARTY_AUTH_NO_EMAIL)

            val userAccountOptional = userAccountService.findByThirdParty("github", userResponse!!.id)
            val user = userAccountOptional.orElseGet {
                userAccountService.getByUserName(githubEmail.email).ifPresent {
                    throw AuthenticationException(Message.USERNAME_ALREADY_EXISTS)
                }

                var invitation: Invitation? = null
                if (invitationCode == null) {
                    properties.authentication.enabled
                } else {
                    invitation = invitationService.getInvitation(invitationCode)
                }

                val newUserAccount = UserAccount()
                newUserAccount.username = githubEmail.email
                newUserAccount.name = userResponse.name
                newUserAccount.thirdPartyAuthId = userResponse.id
                newUserAccount.thirdPartyAuthType = "github"
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
    }

    class GithubEmailResponse {
        var email: String? = null
        var isPrimary = false
    }

    class GithubUserResponse {
        var name: String? = null
        var id: String? = null
    }

}