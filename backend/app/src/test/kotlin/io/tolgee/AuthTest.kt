package io.tolgee

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.constants.Message
import io.tolgee.controllers.PublicController
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsUnauthorized
import io.tolgee.fixtures.mapResponseTo
import io.tolgee.model.Project
import io.tolgee.security.authentication.JwtService
import io.tolgee.security.thirdParty.GithubOAuthDelegate.GithubEmailResponse
import io.tolgee.testing.AbstractControllerTest
import io.tolgee.util.GitHubAuthUtil
import io.tolgee.util.GoogleAuthUtil
import io.tolgee.util.OAuth2AuthUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestTemplate
import java.util.Date

@Transactional
class AuthTest : AbstractControllerTest() {
  @Autowired
  private val publicController: PublicController? = null

  @MockitoBean
  @Autowired
  private val restTemplate: RestTemplate? = null

  @Autowired
  private var authMvc: MockMvc? = null

  @Autowired
  private lateinit var jwtService: JwtService

  private val gitHubAuthUtil: GitHubAuthUtil by lazy { GitHubAuthUtil(tolgeeProperties, authMvc, restTemplate) }
  private val googleAuthUtil: GoogleAuthUtil by lazy { GoogleAuthUtil(tolgeeProperties, authMvc, restTemplate) }
  private val oAuth2AuthUtil: OAuth2AuthUtil by lazy { OAuth2AuthUtil(tolgeeProperties, authMvc, restTemplate) }

  private lateinit var project: Project

  @BeforeEach
  fun setup() {
    project = dbPopulator.createBase().project
    authMvc = MockMvcBuilders.standaloneSetup(publicController).setControllerAdvice(ExceptionHandlers()).build()
  }

  @AfterEach
  fun clean() {
    clearForcedDate()
  }

  @Test
  fun generatesTokenForValidUser() {
    val response = doAuthentication(initialUsername, initialPassword)
    println(response.andReturn().response.contentAsString)
    val result: HashMap<String, Any> = response.andReturn().mapResponseTo()
    println(result)
    assertThat(result["accessToken"]).isNotNull
    assertThat(result["tokenType"]).isEqualTo("Bearer")
  }

  @Test
  fun doesNotGenerateTokenForInvalidUser() {
    val mvcResult = doAuthentication("bena", "benaspassword")
    assertThat(mvcResult.andReturn().response.status).isEqualTo(401)
  }

  @Test
  fun userWithTokenHasAccess() {
    val response =
      doAuthentication(initialUsername, initialPassword)
        .andReturn()
        .response.contentAsString
    val token = mapper.readValue(response, HashMap::class.java)["accessToken"] as String?
    val mvcResult =
      mvc
        .perform(
          MockMvcRequestBuilders
            .get("/api/projects")
            .accept(MediaType.ALL)
            .header("Authorization", String.format("Bearer %s", token))
            .contentType(MediaType.APPLICATION_JSON),
        ).andReturn()
    assertThat(mvcResult.response.status).isEqualTo(200)
  }

  @Test
  fun `expired tokens do not have access`() {
    val baseline = Date()

    currentDateProvider.forcedDate = Date(baseline.time - tolgeeProperties.authentication.jwtExpiration - 10_000)

    val user = userAccountService[initialUsername].id
    val token = jwtService.emitToken(user)

    currentDateProvider.forcedDate = baseline

    val mvcResult =
      mvc
        .perform(
          MockMvcRequestBuilders
            .get("/api/projects")
            .accept(MediaType.ALL)
            .header("Authorization", String.format("Bearer %s", token))
            .contentType(MediaType.APPLICATION_JSON),
        ).andReturn()

    assertThat(mvcResult.response.status).isEqualTo(401)
    assertThat(mvcResult.response.contentAsString).contains(Message.EXPIRED_JWT_TOKEN.code)
  }

  @Test
  fun doesNotAuthorizeGithubUserWhenNoEmail() {
    val emailResponse = arrayOf<GithubEmailResponse>()
    val mvcResult = gitHubAuthUtil.authorizeGithubUser(emailResponse = ResponseEntity(emailResponse, HttpStatus.OK))
    val response = mvcResult.response
    assertThat(response.status).isEqualTo(401)
    assertThat(response.contentAsString).contains(Message.THIRD_PARTY_AUTH_NO_EMAIL.code)
  }

  @Test
  fun doesNotAuthorizeGithubUserWhenNoReceivedToken() {
    var tokenResponse: MutableMap<String, String?>? = null
    var mvcResult = gitHubAuthUtil.authorizeGithubUser(tokenResponse = tokenResponse)
    var response = mvcResult.response
    assertThat(response.status).isEqualTo(401)
    assertThat(response.contentAsString).contains(Message.THIRD_PARTY_AUTH_UNKNOWN_ERROR.code)
    tokenResponse = HashMap()
    tokenResponse["error"] = null
    mvcResult = gitHubAuthUtil.authorizeGithubUser(tokenResponse = tokenResponse)
    response = mvcResult.response
    assertThat(response.status).isEqualTo(401)
    assertThat(response.contentAsString).contains(Message.THIRD_PARTY_AUTH_ERROR_MESSAGE.code)
  }

  @Test
  fun doesNotAuthorizeGoogleUserWhenNoReceivedToken() {
    var tokenResponse: MutableMap<String, String?>? = null
    var mvcResult = googleAuthUtil.authorizeGoogleUser(tokenResponse = tokenResponse)
    var response = mvcResult.response
    assertThat(response.status).isEqualTo(401)
    assertThat(response.contentAsString).contains(Message.THIRD_PARTY_AUTH_UNKNOWN_ERROR.code)
    tokenResponse = HashMap()
    tokenResponse["error"] = null
    mvcResult = googleAuthUtil.authorizeGoogleUser(tokenResponse = tokenResponse)
    response = mvcResult.response
    assertThat(response.status).isEqualTo(401)
    assertThat(response.contentAsString).contains(Message.THIRD_PARTY_AUTH_ERROR_MESSAGE.code)
  }

  @Test
  fun doesNotAuthorizeOAuth2UserWhenNoReceivedToken() {
    var tokenResponse: MutableMap<String, String?>? = null
    var mvcResult = oAuth2AuthUtil.authorizeOAuth2User(tokenResponse = tokenResponse)
    var response = mvcResult.response
    assertThat(response.status).isEqualTo(401)
    assertThat(response.contentAsString).contains(Message.THIRD_PARTY_AUTH_UNKNOWN_ERROR.code)
    tokenResponse = HashMap()
    tokenResponse["error"] = null
    mvcResult = oAuth2AuthUtil.authorizeOAuth2User(tokenResponse = tokenResponse)
    response = mvcResult.response
    assertThat(response.status).isEqualTo(401)
    assertThat(response.contentAsString).contains(Message.THIRD_PARTY_AUTH_ERROR_MESSAGE.code)
  }

  @Test
  fun doesNotAuthorizeGitHubUserWhenRegistrationsNotAllowed() {
    val oldRegistrationsAllowed = tolgeeProperties.authentication.registrationsAllowed
    tolgeeProperties.authentication.registrationsAllowed = false
    val response = gitHubAuthUtil.authorizeGithubUser()
    val result = response.mapResponseTo<Map<String, String>>()
    assertThat(result["code"]).isEqualTo("registrations_not_allowed")
    tolgeeProperties.authentication.registrationsAllowed = oldRegistrationsAllowed
  }

  @Test
  fun doesNotAuthorizeGoogleUserWhenRegistrationsNotAllowed() {
    val oldRegistrationsAllowed = tolgeeProperties.authentication.registrationsAllowed
    tolgeeProperties.authentication.registrationsAllowed = false
    val response = googleAuthUtil.authorizeGoogleUser()
    val result = response.mapResponseTo<Map<String, String>>()
    assertThat(result["code"]).isEqualTo("registrations_not_allowed")
    tolgeeProperties.authentication.registrationsAllowed = oldRegistrationsAllowed
  }

  @Test
  fun doesNotAuthorizeOAuth2UserWhenRegistrationsNotAllowed() {
    val oldRegistrationsAllowed = tolgeeProperties.authentication.registrationsAllowed
    tolgeeProperties.authentication.registrationsAllowed = false
    val response = oAuth2AuthUtil.authorizeOAuth2User()
    val result = response.mapResponseTo<Map<String, String>>()
    assertThat(result["code"]).isEqualTo("registrations_not_allowed")
    tolgeeProperties.authentication.registrationsAllowed = oldRegistrationsAllowed
  }

  @Test
  fun doesNotAuthorizeOAuth2UserWhenUrlsMissingInConfiguration() {
    // tokenUrl
    val oldTokenUrl = tolgeeProperties.authentication.oauth2.tokenUrl
    tolgeeProperties.authentication.oauth2.tokenUrl = null
    var mvcResult = oAuth2AuthUtil.authorizeOAuth2User()
    var response = mvcResult.response
    assertThat(response.status).isEqualTo(401)
    assertThat(response.contentAsString).contains(Message.OAUTH2_TOKEN_URL_NOT_SET.code)
    tolgeeProperties.authentication.oauth2.tokenUrl = oldTokenUrl
    // userUrl
    val oldUserUrl = tolgeeProperties.authentication.oauth2.userUrl
    tolgeeProperties.authentication.oauth2.userUrl = null
    mvcResult = oAuth2AuthUtil.authorizeOAuth2User()
    response = mvcResult.response
    assertThat(response.status).isEqualTo(401)
    assertThat(response.contentAsString).contains(Message.OAUTH2_USER_URL_NOT_SET.code)
    tolgeeProperties.authentication.oauth2.userUrl = oldUserUrl
  }

  @Test
  fun authorizesGithubUser() {
    val response = gitHubAuthUtil.authorizeGithubUser().response.contentAsString
    val result = jacksonObjectMapper().readValue(response, HashMap::class.java)
    assertThat(result["accessToken"]).isNotNull
    assertThat(result["tokenType"]).isEqualTo("Bearer")
  }

  @Test
  fun authorizesGoogleUser() {
    val response = googleAuthUtil.authorizeGoogleUser().response.contentAsString
    val result = jacksonObjectMapper().readValue(response, HashMap::class.java)
    assertThat(result["accessToken"]).isNotNull
    assertThat(result["tokenType"]).isEqualTo("Bearer")
  }

  @Test
  fun authorizesOAuth2User() {
    val response = oAuth2AuthUtil.authorizeOAuth2User().response.contentAsString
    val result = ObjectMapper().readValue(response, HashMap::class.java)
    assertThat(result["accessToken"]).isNotNull
    assertThat(result["tokenType"]).isEqualTo("Bearer")
  }

  @Test
  fun `doesn't auth deleted user`() {
    val admin = userAccountService.get(initialUsername)
    userAccountService.delete(admin)
    doAuthentication(initialUsername, initialPassword).andIsUnauthorized
  }

  @Test
  fun `super token endpoints require super token`() {
    val admin = userAccountService[initialUsername]
    var token = jwtService.emitToken(admin.id, isSuper = false)
    assertExpired(token)

    val baseline = Date()
    val newDate = baseline.time - tolgeeProperties.authentication.jwtSuperExpiration - 10_000

    setForcedDate(Date(newDate))
    token = jwtService.emitToken(admin.id, isSuper = true)
    setForcedDate(baseline)

    assertExpired(token)
  }

  private fun assertExpired(token: String) {
    mvc
      .perform(
        MockMvcRequestBuilders
          .put("/v2/projects/${project.id}/users/${project.id}/revoke-access")
          .accept(MediaType.ALL)
          .header("Authorization", String.format("Bearer %s", token))
          .contentType(MediaType.APPLICATION_JSON),
      ).andIsForbidden
      .andAssertThatJson {
        node("code").isEqualTo(Message.EXPIRED_SUPER_JWT_TOKEN.code)
      }
  }
}
