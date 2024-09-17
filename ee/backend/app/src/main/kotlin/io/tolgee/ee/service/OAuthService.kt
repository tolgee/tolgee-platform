package io.tolgee.ee.service

import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.jwk.source.RemoteJWKSet
import com.nimbusds.jose.proc.JWSAlgorithmFamilyJWSKeySelector
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor
import com.nimbusds.jwt.proc.DefaultJWTProcessor
import com.posthog.java.shaded.org.json.JSONObject
import io.tolgee.constants.Message
import io.tolgee.ee.exceptions.OAuthAuthorizationException
import io.tolgee.ee.repository.DynamicOAuth2ClientRegistrationRepository
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.model.UserAccount
import io.tolgee.security.authentication.JwtService
import io.tolgee.security.payload.JwtAuthenticationResponse
import io.tolgee.service.security.SignUpService
import io.tolgee.service.security.UserAccountService
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.http.*
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.net.URL
import java.util.*

@Service
class OAuthService(
  private val dynamicOAuth2ClientRegistrationRepository: DynamicOAuth2ClientRegistrationRepository,
  private val jwtService: JwtService,
  private val userAccountService: UserAccountService,
  private val signUpService: SignUpService,
  private val restTemplate: RestTemplate,
) : Logging {
  fun handleOAuthCallback(
    registrationId: String,
    code: String,
    redirectUrl: String,
    error: String,
    errorDescription: String,
    invitationCode: String?,
  ): JwtAuthenticationResponse? {
    if (error.isNotBlank()) {
      logger.info("Third party auth failed: $errorDescription $error")
      throw OAuthAuthorizationException(
        Message.THIRD_PARTY_AUTH_FAILED,
        "$errorDescription $error",
      )
    }

    val clientRegistration =
      dynamicOAuth2ClientRegistrationRepository
        .findByRegistrationId(registrationId)

    val tokenResponse =
      exchangeCodeForToken(clientRegistration, code, redirectUrl)
        ?: throw OAuthAuthorizationException(
          Message.TOKEN_EXCHANGE_FAILED,
          null,
        )

    val userInfo = verifyAndDecodeIdToken(tokenResponse.id_token, clientRegistration.providerDetails.jwkSetUri)
    return register(userInfo, clientRegistration.registrationId, invitationCode)
  }

  private fun exchangeCodeForToken(
    clientRegistration: ClientRegistration,
    code: String,
    redirectUrl: String,
  ): OAuth2TokenResponse? {
    val headers =
      HttpHeaders().apply {
        contentType = MediaType.APPLICATION_FORM_URLENCODED
      }

    val body: MultiValueMap<String, String> = LinkedMultiValueMap()
    body.add("grant_type", "authorization_code")
    body.add("code", code)
    body.add("redirect_uri", redirectUrl)
    body.add("client_id", clientRegistration.clientId)
    body.add("client_secret", clientRegistration.clientSecret)
    body.add("scope", "openid")

    val request = HttpEntity(body, headers)
    return try {
      val response: ResponseEntity<OAuth2TokenResponse> =
        restTemplate.exchange(
          clientRegistration.providerDetails.tokenUri,
          HttpMethod.POST,
          request,
          OAuth2TokenResponse::class.java,
        )
      response.body
    } catch (e: HttpClientErrorException) {
      logger.info("Failed to exchange code for token: ${e.message}")
      null
    }
  }

  fun verifyAndDecodeIdToken(
    idToken: String,
    jwkSetUri: String,
  ): GenericUserResponse {
    try {
      val signedJWT = SignedJWT.parse(idToken)

      val jwkSource: JWKSource<SecurityContext> = RemoteJWKSet(URL(jwkSetUri))

      val jwtProcessor: ConfigurableJWTProcessor<SecurityContext> = DefaultJWTProcessor()
      val keySelector = JWSAlgorithmFamilyJWSKeySelector.fromJWKSource(jwkSource)
      jwtProcessor.jwsKeySelector = keySelector

      val jwtClaimsSet: JWTClaimsSet = jwtProcessor.process(signedJWT, null)

      val expirationTime: Date = jwtClaimsSet.expirationTime
      if (expirationTime.before(Date())) {
        throw OAuthAuthorizationException(Message.ID_TOKEN_EXPIRED, null)
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
      throw OAuthAuthorizationException(Message.USER_INFO_RETRIEVAL_FAILED, null)
    }
  }

  fun decodeJwt(jwt: String): JSONObject {
    val parts = jwt.split(".")
    if (parts.size != 3) throw IllegalArgumentException("JWT does not have 3 parts") // todo change exception type

    val payload = parts[1]
    val decodedPayload = String(Base64.getUrlDecoder().decode(payload))

    return JSONObject(decodedPayload)
  }

  private fun register(
    userResponse: GenericUserResponse,
    registrationId: String,
    invitationCode: String?,
  ): JwtAuthenticationResponse? {
    val email =
      userResponse.email ?: let {
        logger.info("Third party user email is null. Missing scope email?")
        throw AuthenticationException(Message.THIRD_PARTY_AUTH_NO_EMAIL)
      }

    val userAccountOptional = userAccountService.findByThirdParty(registrationId, userResponse.sub!!)
    val user =
      userAccountOptional.orElseGet {
        userAccountService.findActive(email)?.let {
          throw AuthenticationException(Message.USERNAME_ALREADY_EXISTS)
        }

        val newUserAccount = UserAccount()
        newUserAccount.username =
          userResponse.email ?: throw AuthenticationException(Message.THIRD_PARTY_AUTH_NO_EMAIL)

        // build name for userAccount based on available fields by third party
        var name = userResponse.email!!.split("@")[0]
        if (userResponse.name != null) {
          name = userResponse.name!!
        } else if (userResponse.given_name != null && userResponse.family_name != null) {
          name = "${userResponse.given_name} ${userResponse.family_name}"
        }
        newUserAccount.name = name
        newUserAccount.thirdPartyAuthId = userResponse.sub
        newUserAccount.thirdPartyAuthType = registrationId
        newUserAccount.accountType = UserAccount.AccountType.THIRD_PARTY
        signUpService.signUp(newUserAccount, invitationCode, null)

        newUserAccount
      }
    val jwt = jwtService.emitToken(user.id)
    return JwtAuthenticationResponse(jwt)
  }

  class OAuth2TokenResponse(
    val id_token: String,
    val scope: String,
  )

  class GenericUserResponse {
    var sub: String? = null
    var name: String? = null
    var given_name: String? = null
    var family_name: String? = null
    var email: String? = null
  }
}
