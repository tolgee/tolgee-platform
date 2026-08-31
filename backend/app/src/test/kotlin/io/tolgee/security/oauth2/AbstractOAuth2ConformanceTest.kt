package io.tolgee.security.oauth2

import io.tolgee.development.testDataBuilder.data.OAuth2ConformanceTestData
import io.tolgee.security.authentication.JwtService
import io.tolgee.testing.AbstractControllerTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import tools.jackson.databind.JsonNode
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * The OAuth 2.1 contract of the authorization server as a client observes it over HTTP. Registers no client of its
 * own — it drives only the clients configured in the test `application.yaml` — and must keep passing unchanged when
 * the authorization server implementation is replaced.
 */
abstract class AbstractOAuth2ConformanceTest : AbstractControllerTest() {
  @Autowired
  protected lateinit var jwtService: JwtService

  @Autowired
  protected lateinit var oauth2AuthorizationService: OAuth2AuthorizationService

  protected lateinit var testData: OAuth2ConformanceTestData
  protected lateinit var driver: OAuth2FlowDriver

  @BeforeEach
  fun setup() {
    testData = OAuth2ConformanceTestData()
    testDataService.saveTestData(testData.root)
    driver = OAuth2FlowDriver(mvc)
  }

  @AfterEach
  fun cleanup() {
    currentDateProvider.forcedDate = null
    testDataService.cleanTestData(testData.root)
  }

  protected val oauth2 get() = tolgeeProperties.oauth2

  protected fun jwt(): String = jwtService.emitToken(testData.user.id, isSuper = true)

  /**
   * [received] is the value the handler gets after the request layer has decoded the query, which is where the two
   * legs meet: MockMvc percent-decodes and a servlet container form-decodes, so what a client put on the wire is not
   * reproducible here. What is asserted is the half Tolgee owns — how that value is written back out.
   */
  protected fun assertEncodedBothLegs(
    received: String,
    encoded: String,
  ) {
    val consentRedirect =
      driver
        .authorize(CLIENT_ID, REDIRECT, validParams() + ("state" to received))
        .andReturn()
        .response
        .getHeader("Location")!!
    driver.queryParam(consentRedirect, "state").assert.isEqualTo(encoded)

    val pending = driver.startPendingConsent(jwt(), CLIENT_ID, REDIRECT, clientState = received)
    val codeRedirect = driver.consentRedirect(pending, projectId = null)
    driver.queryParam(codeRedirect, "state").assert.isEqualTo(encoded)
    URLDecoder.decode(driver.queryParam(codeRedirect, "state")!!, StandardCharsets.UTF_8).assert.isEqualTo(received)
  }

  protected fun validParams(): Map<String, String?> =
    mapOf(
      "response_type" to "code",
      "scope" to "translations.view",
      "code_challenge" to OAuth2FlowDriver.s256Challenge(OAuth2FlowDriver.randomVerifier()),
      "code_challenge_method" to "S256",
    )

  protected fun authorizeRedirect(): String =
    driver
      .authorize(CLIENT_ID, REDIRECT, validParams())
      .andReturn()
      .response
      .getHeader("Location")!!

  protected fun tokenResult(): MvcResult {
    val pending = driver.startPendingConsent(jwt(), CLIENT_ID, REDIRECT)
    val code = driver.queryParam(driver.consentRedirect(pending), "code")!!
    return driver.exchangeCode(code, CLIENT_ID, REDIRECT, pending.verifier).andReturn()
  }

  /** The redirect an authorize request produces when the parameter under test makes it invalid. */
  protected fun errorRedirect(overrides: Map<String, String?>): String {
    val params = (validParams() + mapOf("state" to "client-state")).toMutableMap()
    params.putAll(overrides)
    val body =
      driver
        .startAuthorization(jwt(), CLIENT_ID, REDIRECT, params)
        .andReturn()
        .response.contentAsString
    val url = jacksonObjectMapper().readTree(body).get("redirectUrl")?.asString()
    url.assert.withFailMessage("expected an error redirect, got $body").isNotNull()
    return url!!
  }

  protected fun assertOAuthError(
    result: MvcResult,
    error: String,
  ) {
    result.response.status.assert
      .isEqualTo(400)
    json(result)
      .get("error")
      .asString()
      .assert
      .isEqualTo(error)
  }

  protected fun json(result: MvcResult): JsonNode = jacksonObjectMapper().readTree(result.response.contentAsString)

  protected fun values(
    doc: JsonNode,
    field: String,
  ): List<String> =
    doc
      .get(field)
      .valueStream()
      .map { it.asString() }
      .toList()
}
