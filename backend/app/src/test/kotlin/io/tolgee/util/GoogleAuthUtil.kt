package io.tolgee.util

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.security.thirdParty.GoogleOAuthDelegate
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.web.client.RestTemplate

class GoogleAuthUtil(
  private val tolgeeProperties: TolgeeProperties,
  private var authMvc: MockMvc? = null,
  private val restTemplate: RestTemplate? = null,
) {
  private val defaultUserResponse: GoogleOAuthDelegate.GoogleUserResponse
    get() {
      val fakeGoogleUser = GoogleOAuthDelegate.GoogleUserResponse()
      fakeGoogleUser.sub = "fakeId"
      fakeGoogleUser.name = "fakeName"
      fakeGoogleUser.given_name = "fakeGiveName"
      fakeGoogleUser.family_name = "fakeGivenFamilyName"
      fakeGoogleUser.email = "fakeEmail@domain.com"
      fakeGoogleUser.email_verified = true
      return fakeGoogleUser
    }

  private val defaultTokenResponse: Map<String, String?>
    get() {
      val accessToken = "fake_access_token"
      val tokenResponse: MutableMap<String, String?> = HashMap()
      tokenResponse["access_token"] = accessToken
      return tokenResponse
    }

  fun authorizeGoogleUser(
    tokenResponse: Map<String, String?>? = this.defaultTokenResponse,
    userResponse: ResponseEntity<GoogleOAuthDelegate.GoogleUserResponse> =
      ResponseEntity(
        this.defaultUserResponse,
        HttpStatus.OK,
      ),
  ): MvcResult {
    val receivedCode = "ThiS_Is_Fake_valid_COde"
    val googleConf = tolgeeProperties.authentication.google

    whenever(restTemplate!!.postForObject<Map<*, *>>(eq(googleConf.authorizationUrl), any(), any()))
      .thenReturn(tokenResponse)

    whenever(
      restTemplate.exchange(
        eq(googleConf.userUrl),
        eq(HttpMethod.GET),
        any(),
        eq(GoogleOAuthDelegate.GoogleUserResponse::class.java),
      ),
    ).thenReturn(userResponse)

    return authMvc!!.perform(
      MockMvcRequestBuilders.get("/api/public/authorize_oauth/google?code=$receivedCode")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON),
    )
      .andReturn()
  }
}
