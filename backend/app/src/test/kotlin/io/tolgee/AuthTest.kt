package io.tolgee

import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.constants.Message
import io.tolgee.controllers.PublicController
import io.tolgee.fixtures.generateUniqueString
import io.tolgee.fixtures.mapResponseTo
import io.tolgee.security.third_party.GithubOAuthDelegate.GithubEmailResponse
import io.tolgee.testing.AbstractControllerTest
import io.tolgee.util.GitHubAuthUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestTemplate

@Transactional
class AuthTest : AbstractControllerTest() {
  @Autowired
  private val publicController: PublicController? = null

  @MockBean
  @Autowired
  private val restTemplate: RestTemplate? = null

  @Autowired
  private var authMvc: MockMvc? = null

  private val gitHubAuthUtil: GitHubAuthUtil by lazy { GitHubAuthUtil(tolgeeProperties, authMvc, restTemplate) }

  @BeforeEach
  fun setup() {
    dbPopulator.createBase(generateUniqueString())
    authMvc = MockMvcBuilders.standaloneSetup(publicController).setControllerAdvice(ExceptionHandlers()).build()
  }

  @Test
  fun generatesTokenForValidUser() {
    val response = doAuthentication(initialUsername, initialPassword)
    val result: HashMap<String, Any> = response.mapResponseTo()
    assertThat(result["accessToken"]).isNotNull
    assertThat(result["tokenType"]).isEqualTo("Bearer")
  }

  @Test
  fun doesNotGenerateTokenForInvalidUser() {
    val mvcResult = doAuthentication("bena", "benaspassword")
    assertThat(mvcResult.response.status).isEqualTo(401)
  }

  @Test
  fun userWithTokenHasAccess() {
    val response = doAuthentication(initialUsername, initialPassword)
      .response.contentAsString
    val token = mapper.readValue(response, HashMap::class.java)["accessToken"] as String?
    val mvcResult = mvc.perform(
      MockMvcRequestBuilders.get("/api/projects")
        .accept(MediaType.ALL)
        .header("Authorization", String.format("Bearer %s", token))
        .contentType(MediaType.APPLICATION_JSON)
    )
      .andReturn()
    assertThat(mvcResult.response.status).isEqualTo(200)
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
  fun doesNotAuthorizeWhenRegistrationsNotAllowed() {
    val oldRegistrationsAllowed = tolgeeProperties.authentication.registrationsAllowed
    tolgeeProperties.authentication.registrationsAllowed = false
    val response = gitHubAuthUtil.authorizeGithubUser()
    val result = response.mapResponseTo<Map<String, String>>()
    assertThat(result["code"]).isEqualTo("registrations_not_allowed")
    tolgeeProperties.authentication.registrationsAllowed = oldRegistrationsAllowed
  }

  @Test
  fun authorizesGithubUser() {
    val response = gitHubAuthUtil.authorizeGithubUser().response.contentAsString
    val result = ObjectMapper().readValue(response, HashMap::class.java)
    assertThat(result["accessToken"]).isNotNull
    assertThat(result["tokenType"]).isEqualTo("Bearer")
  }
}
