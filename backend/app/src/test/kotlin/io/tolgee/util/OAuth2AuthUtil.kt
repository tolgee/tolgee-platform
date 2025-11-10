package io.tolgee.util

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.fixtures.AuthorizedRequestPerformer
import io.tolgee.model.UserAccount
import io.tolgee.security.thirdParty.OAuth2Delegate
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

class OAuth2AuthUtil(
  private val tolgeeProperties: TolgeeProperties,
  private var authMvc: MockMvc? = null,
  private val restTemplate: RestTemplate? = null,
  private val authorizedRequestPerformer: AuthorizedRequestPerformer? = null,
) {
  private val defaultUserResponse: OAuth2Delegate.GenericUserResponse
    get() {
      val fakeGenericOAuth2User = OAuth2Delegate.GenericUserResponse()
      fakeGenericOAuth2User.sub = "fakeId"
      fakeGenericOAuth2User.given_name = "fakeGiveName"
      fakeGenericOAuth2User.family_name = "fakeGivenFamilyName"
      fakeGenericOAuth2User.email = "fakeEmail@domain.com"
      return fakeGenericOAuth2User
    }

  private val defaultTokenResponse: Map<String, String?>
    get() {
      val accessToken = "fake_access_token"
      val tokenResponse: MutableMap<String, String?> = HashMap()
      tokenResponse["access_token"] = accessToken
      return tokenResponse
    }

  fun authorizeOAuth2User(user: UserAccount): MvcResult {
    val response =
      OAuth2Delegate.GenericUserResponse().apply {
        email = user.username
        sub = "fakeId"
        given_name = "fakeGiveName"
        family_name = "fakeGivenFamilyName"
      }
    val entity = ResponseEntity(response, HttpStatus.OK)
    return authorizeOAuth2User(userResponse = entity)
  }

  fun authorizeOAuth2User(
    tokenResponse: Map<String, String?>? = this.defaultTokenResponse,
    userResponse: ResponseEntity<OAuth2Delegate.GenericUserResponse> =
      ResponseEntity(
        this.defaultUserResponse,
        HttpStatus.OK,
      ),
  ): MvcResult {
    val receivedCode = "ThiS_Is_Fake_valid_COde"
    val oauth2Conf = tolgeeProperties.authentication.oauth2

    if (oauth2Conf.tokenUrl != null) {
      whenever(restTemplate!!.postForObject<Map<*, *>>(eq(oauth2Conf.tokenUrl!!), any(), any()))
        .thenReturn(tokenResponse)
    }

    if (oauth2Conf.userUrl != null) {
      whenever(
        restTemplate?.exchange(
          eq(oauth2Conf.userUrl!!),
          eq(HttpMethod.GET),
          any(),
          eq(OAuth2Delegate.GenericUserResponse::class.java),
        ),
      ).thenReturn(userResponse)
    }

    val url = "/api/public/authorize_oauth/oauth2?code=$receivedCode"

    if (authorizedRequestPerformer != null) {
      return authorizedRequestPerformer.performAuthGet(url).andReturn()
    }

    return authMvc!!
      .perform(
        MockMvcRequestBuilders
          .get(url)
          .accept(MediaType.APPLICATION_JSON)
          .contentType(MediaType.APPLICATION_JSON),
      ).andReturn()
  }
}
