package io.tolgee

import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.constants.Message
import io.tolgee.controllers.PublicController
import io.tolgee.fixtures.generateUniqueString
import io.tolgee.fixtures.mapResponseTo
import io.tolgee.security.third_party.GithubOAuthDelegate.GithubEmailResponse
import io.tolgee.security.third_party.GithubOAuthDelegate.GithubUserResponse
import io.tolgee.testing.AbstractControllerTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestTemplate
import java.util.*

@Transactional
class AuthTest : AbstractControllerTest() {
  @Autowired
  private val publicController: PublicController? = null

  @MockBean
  @Autowired
  private val restTemplate: RestTemplate? = null
  private var authMvc: MockMvc? = null

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
    val fakeGithubUser = githubUserResponse
    val emailResponse = arrayOf<GithubEmailResponse>()
    val tokenResponse = tokenResponse
    val mvcResult = authorizeGithubUser(
      tokenResponse = tokenResponse,
      userResponse = ResponseEntity(fakeGithubUser, HttpStatus.OK),
      emailResponse = ResponseEntity(emailResponse, HttpStatus.OK)
    )
    val response = mvcResult.response
    assertThat(response.status).isEqualTo(401)
    assertThat(response.contentAsString).contains(Message.THIRD_PARTY_AUTH_NO_EMAIL.code)
  }

  @Test
  fun doesNotAuthorizeGithubUserWhenNoReceivedToken() {
    val fakeGithubUser = githubUserResponse
    val githubEmailResponse = githubEmailResponse
    val emailResponse = arrayOf(githubEmailResponse)
    var tokenResponse: MutableMap<String, String?>? = null
    var mvcResult = authorizeGithubUser(
      tokenResponse = tokenResponse,
      userResponse = ResponseEntity(fakeGithubUser, HttpStatus.OK),
      emailResponse = ResponseEntity(emailResponse, HttpStatus.OK)
    )
    var response = mvcResult.response
    assertThat(response.status).isEqualTo(401)
    assertThat(response.contentAsString).contains(Message.THIRD_PARTY_AUTH_UNKNOWN_ERROR.code)
    tokenResponse = HashMap()
    tokenResponse["error"] = null
    mvcResult = authorizeGithubUser(
      tokenResponse = tokenResponse,
      userResponse = ResponseEntity(fakeGithubUser, HttpStatus.OK),
      emailResponse = ResponseEntity(emailResponse, HttpStatus.OK)
    )
    response = mvcResult.response
    assertThat(response.status).isEqualTo(401)
    assertThat(response.contentAsString).contains(Message.THIRD_PARTY_AUTH_ERROR_MESSAGE.code)
  }

  @Test
  fun doesNotAuthorizeWhenRegistrationsNotAllowed() {
    val oldRegistrationsAllowed = tolgeeProperties.authentication.registrationsAllowed
    tolgeeProperties.authentication.registrationsAllowed = false
    val fakeGithubUser = githubUserResponse
    val githubEmailResponse = githubEmailResponse
    val emailResponse = arrayOf(githubEmailResponse)
    val tokenResponse = tokenResponse
    val response = authorizeGithubUser(
      tokenResponse = tokenResponse,
      userResponse = ResponseEntity(fakeGithubUser, HttpStatus.OK),
      emailResponse = ResponseEntity(emailResponse, HttpStatus.OK)
    )
    val result = response.mapResponseTo<Map<String, String>>()
    assertThat(result["code"]).isEqualTo("registrations_not_allowed")
    tolgeeProperties.authentication.registrationsAllowed = oldRegistrationsAllowed
  }

  private val githubEmailResponse: GithubEmailResponse
    get() {
      val githubEmailResponse = GithubEmailResponse()
      githubEmailResponse.email = "fake_email@email.com"
      githubEmailResponse.primary = true
      githubEmailResponse.verified = true
      return githubEmailResponse
    }
  private val githubUserResponse: GithubUserResponse
    get() {
      val fakeGithubUser = GithubUserResponse()
      fakeGithubUser.id = "fakeId"
      fakeGithubUser.name = "fakeName"
      return fakeGithubUser
    }

  @Test
  fun authorizesGithubUser() {
    val fakeGithubUser = githubUserResponse
    val githubEmailResponse = githubEmailResponse
    val emailResponse = arrayOf(githubEmailResponse)
    val tokenResponse = tokenResponse
    val response = authorizeGithubUser(
      tokenResponse = tokenResponse,
      userResponse = ResponseEntity(fakeGithubUser, HttpStatus.OK),
      emailResponse = ResponseEntity(emailResponse, HttpStatus.OK)
    ).response.contentAsString
    val result = ObjectMapper().readValue(response, HashMap::class.java)
    assertThat(result["accessToken"]).isNotNull
    assertThat(result["tokenType"]).isEqualTo("Bearer")
  }

  private val tokenResponse: Map<String, String?>
    get() {
      val accessToken = "fake_access_token"
      val tokenResponse: MutableMap<String, String?> = HashMap()
      tokenResponse["access_token"] = accessToken
      return tokenResponse
    }

  fun authorizeGithubUser(
    tokenResponse: Map<String, String?>?,
    userResponse: ResponseEntity<GithubUserResponse>,
    emailResponse: ResponseEntity<Array<GithubEmailResponse>>
  ): MvcResult {
    val receivedCode = "ThiS_Is_Fake_valid_COde"
    val githubConf = tolgeeProperties.authentication.github

    whenever(restTemplate!!.postForObject<Map<*, *>>(eq(githubConf.authorizationUrl), any(), any()))
      .thenReturn(tokenResponse)

    whenever(
      restTemplate.exchange(
        eq(githubConf.userUrl),
        eq(HttpMethod.GET),
        any(),
        eq(GithubUserResponse::class.java)
      )
    ).thenReturn(userResponse)

    whenever(
      restTemplate.exchange(
        eq(value = githubConf.userUrl + "/emails"),
        eq(HttpMethod.GET),
        any(),
        eq(Array<GithubEmailResponse>::class.java)
      )
    )
      .thenReturn(emailResponse)

    return authMvc!!.perform(
      MockMvcRequestBuilders.get("/api/public/authorize_oauth/github/$receivedCode")
        .accept(MediaType.ALL)
        .contentType(MediaType.APPLICATION_JSON)
    )
      .andReturn()
  }
}
