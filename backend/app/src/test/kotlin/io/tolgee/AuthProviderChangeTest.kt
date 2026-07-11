package io.tolgee

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.AuthProviderChangeTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ThirdPartyAuthType
import io.tolgee.service.security.AuthProviderChangeService
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import io.tolgee.util.GitHubAuthUtil
import io.tolgee.util.GoogleAuthUtil
import io.tolgee.util.OAuth2AuthUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.ResultActions
import org.springframework.web.client.RestTemplate

class AuthProviderChangeTest : AuthorizedControllerTest() {
  @Autowired
  private lateinit var authProviderChangeService: AuthProviderChangeService

  @MockitoBean
  @Autowired
  private val restTemplate: RestTemplate? = null

  private lateinit var testData: AuthProviderChangeTestData

  private val gitHubAuthorizedAuthUtil: GitHubAuthUtil by lazy {
    GitHubAuthUtil(tolgeeProperties, null, restTemplate, authorizedRequestPerformer)
  }
  private val googleAuthorizedAuthUtil: GoogleAuthUtil by lazy {
    GoogleAuthUtil(tolgeeProperties, null, restTemplate, authorizedRequestPerformer)
  }
  private val oAuth2AuthorizedAuthUtil: OAuth2AuthUtil by lazy {
    OAuth2AuthUtil(tolgeeProperties, null, restTemplate, authorizedRequestPerformer)
  }
  private val gitHubAuthUtil: GitHubAuthUtil by lazy {
    GitHubAuthUtil(tolgeeProperties, authorizedRequestPerformer.mvc, restTemplate)
  }
  private val googleAuthUtil: GoogleAuthUtil by lazy {
    GoogleAuthUtil(tolgeeProperties, authorizedRequestPerformer.mvc, restTemplate)
  }
  private val oAuth2AuthUtil: OAuth2AuthUtil by lazy {
    OAuth2AuthUtil(tolgeeProperties, authorizedRequestPerformer.mvc, restTemplate)
  }

  @BeforeEach
  fun setup() {
    setForcedDate()
    testData = AuthProviderChangeTestData(currentDateProvider.date)
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  @AfterEach
  fun clean() {
    testDataService.cleanTestData(testData.root)
    clearForcedDate()
  }

  fun ResultActions.andIsProvider(provider: ThirdPartyAuthType?): ResultActions {
    if (provider == null) {
      return andIsNotFound
    }
    return andIsOk.andAssertThatJson {
      node("authType").isString.isEqualTo(provider.name)
    }
  }

  fun `performs auth provider change`(
    user: UserAccount,
    targetProvider: ThirdPartyAuthType?,
    initiate: (UserAccount) -> MvcResult,
    auth: (
      (UserAccount) -> MvcResult
    )?,
  ) {
    userAccount = user
    val sourceProvider = user.thirdPartyAuthType

    performAuthGet("/v2/auth-provider").andIsProvider(sourceProvider)
    performAuthGet("/v2/auth-provider/change").andIsNotFound

    val tokenResponse = initiate(user).response
    if (targetProvider != null) {
      tokenResponse.status.assert.isEqualTo(HttpStatus.UNAUTHORIZED.value())
      val tokenResponseResult = ObjectMapper().readValue(tokenResponse.contentAsString, HashMap::class.java)
      assertThat(tokenResponseResult["code"]).isEqualTo(Message.THIRD_PARTY_SWITCH_INITIATED.name.lowercase())
    } else {
      tokenResponse.status.assert.isEqualTo(HttpStatus.OK.value())
    }

    val req = authProviderChangeService.getRequestedChange(user)
    performAuthPost("/v2/auth-provider/change", mapOf("id" to req!!.id)).andIsOk

    performAuthGet("/v2/auth-provider").andIsProvider(targetProvider)

    if (auth != null) {
      val authTokenResponse = auth(user).response
      authTokenResponse.status.assert.isEqualTo(HttpStatus.OK.value())
      val authResult = jacksonObjectMapper().readValue(authTokenResponse.contentAsString, HashMap::class.java)
      assertThat(authResult["accessToken"]).isNotNull
      assertThat(authResult["tokenType"]).isEqualTo("Bearer")
    }
  }

  @Test
  fun `performs auth provider change from none to github`() {
    `performs auth provider change`(
      testData.userNoProvider,
      ThirdPartyAuthType.GITHUB,
      gitHubAuthorizedAuthUtil::authorizeGithubUser,
      gitHubAuthUtil::authorizeGithubUser,
    )
  }

  @Test
  fun `performs auth provider change from none to google`() {
    `performs auth provider change`(
      testData.userNoProvider,
      ThirdPartyAuthType.GOOGLE,
      googleAuthorizedAuthUtil::authorizeGoogleUser,
      googleAuthUtil::authorizeGoogleUser,
    )
  }

  @Test
  fun `performs auth provider change from none to oauth2`() {
    `performs auth provider change`(
      testData.userNoProvider,
      ThirdPartyAuthType.OAUTH2,
      oAuth2AuthorizedAuthUtil::authorizeOAuth2User,
      oAuth2AuthUtil::authorizeOAuth2User,
    )
  }

  @Test
  fun `performs auth provider change from github to google`() {
    `performs auth provider change`(
      testData.userGithub,
      ThirdPartyAuthType.GOOGLE,
      googleAuthorizedAuthUtil::authorizeGoogleUser,
      googleAuthUtil::authorizeGoogleUser,
    )
  }

  @Test
  fun `performs auth provider change from github to oauth2`() {
    `performs auth provider change`(
      testData.userGithub,
      ThirdPartyAuthType.OAUTH2,
      oAuth2AuthorizedAuthUtil::authorizeOAuth2User,
      oAuth2AuthUtil::authorizeOAuth2User,
    )
  }

  @Test
  fun `performs auth provider change from github to none`() {
    `performs auth provider change`(
      testData.userGithub,
      null,
      {
        executeInNewTransaction {
          performAuthDelete("/v2/auth-provider").andReturn()
        }
      },
      null,
    )
  }

  @Test
  fun `doesn't initiate auth provider change when email not matching`() {
    userAccount = testData.userNoProvider

    performAuthGet("/v2/auth-provider/change").andIsNotFound

    val tokenResponse = gitHubAuthorizedAuthUtil.authorizeGithubUser().response
    tokenResponse.status.assert.isEqualTo(HttpStatus.UNAUTHORIZED.value())
    val tokenResponseResult = ObjectMapper().readValue(tokenResponse.contentAsString, HashMap::class.java)
    assertThat(tokenResponseResult["code"]).isEqualTo(Message.THIRD_PARTY_SWITCH_CONFLICT.name.lowercase())

    performAuthGet("/v2/auth-provider/change").andIsNotFound
  }
}
