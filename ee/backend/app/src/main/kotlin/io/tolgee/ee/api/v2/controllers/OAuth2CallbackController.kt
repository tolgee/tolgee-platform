package io.tolgee.ee.api.v2.controllers

import com.posthog.java.shaded.org.json.JSONObject
import io.tolgee.constants.Message
import io.tolgee.ee.repository.DynamicOAuth2ClientRegistrationRepository
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.model.UserAccount
import io.tolgee.security.authentication.JwtService
import io.tolgee.security.payload.JwtAuthenticationResponse
import io.tolgee.service.security.SignUpService
import io.tolgee.service.security.UserAccountService
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.ApplicationContext
import org.springframework.http.*
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.util.*

@RestController
@RequestMapping("v2/oauth2/callback/")
class OAuth2CallbackController(
  private val jwtService: JwtService,
  private val restTemplate: RestTemplate,
  private val userAccountService: UserAccountService,
  private val signUpService: SignUpService,
  private val applicationContext: ApplicationContext,
) {
  @PostMapping("/get-authentication-url")
  fun getAuthenticationUrl(
    @RequestBody request: DomainRequest,
  ): SsoUrlResponse {
    val dynamicOAuth2ClientRegistrationRepository = DynamicOAuth2ClientRegistrationRepository(applicationContext)

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
    @RequestParam state: String,
    @RequestParam(value = "redirect_uri", required = true) redirectUrl: String,
    @RequestParam(defaultValue = "") error: String,
    @RequestParam(defaultValue = "") error_description: String,
    response: HttpServletResponse,
    @PathVariable registrationId: String,
  ): JwtAuthenticationResponse? {
    if (error.isNotBlank()) {
      println(error)
      println(error_description)
      throw Exception()
    }
    val dynamicOAuth2ClientRegistrationRepository = DynamicOAuth2ClientRegistrationRepository(applicationContext)

    val clientRegistration = dynamicOAuth2ClientRegistrationRepository.findByRegistrationId(registrationId)

    val tokenResponse = exchangeCodeForToken(clientRegistration, code, redirectUrl)

    if (tokenResponse == null) {
      println("Failed to obtain access token")
      throw Exception()
    }

    val userInfo = getUserInfo(tokenResponse.id_token)

    if (userInfo == null) {
      println("Failed to get user info from OAuth2 provider")
      throw Exception()
    }

    return registration(userInfo, clientRegistration.registrationId)
  }

  private fun registration(
    userResponse: GenericUserResponse,
    registrationId: String,
  ): JwtAuthenticationResponse? {
    val email =
      userResponse.email ?: let {
        println("Third party user email is null. Missing scope email?")
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
        signUpService.signUp(newUserAccount, null, null)

        newUserAccount
      }
    val jwt = jwtService.emitToken(user.id)
    return JwtAuthenticationResponse(jwt)
  }

  private fun getUserInfo(idToken: String): GenericUserResponse {
    val jwt = decodeJwt(idToken)
    val response =
      GenericUserResponse().apply {
        sub = jwt.optString("sub")
        name = jwt.optString("name")
        given_name = jwt.optString("given_name")
        family_name = jwt.optString("family_name")
        email = jwt.optString("email")
      }

    return response
  }

  fun decodeJwt(jwt: String): JSONObject {
    val parts = jwt.split(".")
    if (parts.size != 3) throw IllegalArgumentException("JWT does not have 3 parts")

    val payload = parts[1]
    val decodedPayload = String(Base64.getUrlDecoder().decode(payload))

    return JSONObject(decodedPayload)
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
      println(e)
      null
    }
  }

  class GenericUserResponse {
    var sub: String? = null
    var name: String? = null
    var given_name: String? = null
    var family_name: String? = null
    var email: String? = null
  }

  class OAuth2TokenResponse(
    val id_token: String,
    val scope: String,
  )

  data class DomainRequest(val domain: String, val state: String)

  data class SsoUrlResponse(val redirectUrl: String)
}
