package io.tolgee.security.oauth2

import io.tolgee.component.KeyGenerator
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.OAuth2FlowTestData
import io.tolgee.dtos.request.pat.CreatePatDto
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.enums.Scope
import io.tolgee.model.oauth2.OAuth2Grant
import io.tolgee.repository.oauth2.OAuth2GrantRepository
import io.tolgee.security.OAUTH_ACCESS_TOKEN_PREFIX
import io.tolgee.security.authentication.JwtService
import io.tolgee.testing.AbstractControllerTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import tools.jackson.databind.JsonNode
import tools.jackson.module.kotlin.jacksonObjectMapper

/**
 * What is Tolgee's own on top of the protocol: project binding, the consent-screen API, and revocation. Protocol
 * edge cases live in the `*ConformanceTest` classes.
 */
abstract class AbstractOAuth2FlowTest : AbstractControllerTest() {
  @Autowired
  protected lateinit var jwtService: JwtService

  @Autowired
  protected lateinit var oauth2AuthorizationService: OAuth2AuthorizationService

  @Autowired
  protected lateinit var issuerResolver: OAuth2IssuerResolver

  @Autowired
  protected lateinit var repository: OAuth2GrantRepository

  @Autowired
  protected lateinit var keyGenerator: KeyGenerator

  protected lateinit var testData: OAuth2FlowTestData
  protected lateinit var driver: OAuth2FlowDriver
  protected var otherProjectId: Long = 0
  protected var publicProjectId: Long = 0

  @BeforeEach
  fun setup() {
    testData = OAuth2FlowTestData()
    testDataService.saveTestData(testData.root)
    otherProjectId = testData.otherProject.id
    publicProjectId = testData.publicProject.id
    driver = OAuth2FlowDriver(mvc)
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  protected fun jwt(): String = jwtService.emitToken(testData.user.id, isSuper = true)

  protected fun pak(): String =
    "tgpak_" + apiKeyService.create(testData.user, setOf(Scope.TRANSLATIONS_VIEW), testData.project).encodedKey

  protected fun pat(): String = "tgpat_" + patService.create(CreatePatDto("oauth-guard"), testData.user).token

  protected fun startAuthorizationWith(
    header: String,
    value: String,
  ) = mvc.perform(
    post("/v2/oauth2/authorize")
      .header(header, value)
      .contentType(MediaType.APPLICATION_JSON)
      .content(
        """{"clientId":"$CLIENT_ID","redirectUri":"$REDIRECT","responseType":"code",""" +
          """"scope":"translations.view","codeChallengeMethod":"S256",""" +
          """"codeChallenge":"${OAuth2FlowDriver.s256Challenge(OAuth2FlowDriver.randomVerifier())}"}""",
      ),
  )

  protected fun apiAccessForbidden(actions: ResultActions) {
    val response = actions.andReturn().response
    response.status.assert.isEqualTo(403)
    response.contentAsString.assert.contains(Message.API_ACCESS_FORBIDDEN.code)
  }

  protected fun apiRequest(accessToken: String) =
    mvc.perform(
      get("/v2/projects/${testData.project.id}/translations").header("Authorization", "Bearer $accessToken"),
    )

  protected fun completeFlow(projectId: Long?): JsonNode =
    driver.completeFlow(jwt(), CLIENT_ID, REDIRECT, projectId = projectId)

  protected fun accessToken(projectId: Long?): String = completeFlow(projectId).get("access_token").asString()

  protected fun tokenFrom(
    pending: OAuth2FlowDriver.PendingConsent,
    approvedScopes: List<String> = listOf("translations.view"),
    projectId: Long? = testData.project.id,
  ): JsonNode {
    val code = driver.queryParam(driver.consentRedirect(pending, approvedScopes, projectId), "code")!!
    return json(driver.exchangeCode(code, pending.clientId, pending.redirect, pending.verifier))
  }

  protected fun consentInfo(
    jwt: String,
    state: String,
  ): JsonNode = json(driver.consentInfo(jwt, state).andIsOk)

  protected fun json(actions: ResultActions): JsonNode =
    jacksonObjectMapper().readTree(actions.andReturn().response.contentAsString)

  protected fun stored(accessToken: String): OAuth2Grant =
    repository.findByAccessTokenHash(keyGenerator.hash(accessToken.removePrefix(OAUTH_ACCESS_TOKEN_PREFIX)))
      ?: throw AssertionError("no authorization stored for the access token")

  companion object {
    internal const val INACCESSIBLE_PROJECT_ID = 9_999_999L
  }

  protected fun grantsForUser(userId: Long = testData.user.id): Int =
    repository.findAll().count {
      it.userAccount.id ==
        userId
    }
}
