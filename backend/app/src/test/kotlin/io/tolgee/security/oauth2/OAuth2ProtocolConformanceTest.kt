package io.tolgee.security.oauth2

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andIsUnauthorized
import io.tolgee.model.UserAccount
import io.tolgee.security.authentication.JwtService
import io.tolgee.testing.AbstractControllerTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpSession
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.web.util.UriComponentsBuilder
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * The OAuth 2.1 contract of the authorization server as a client observes it over HTTP: `/oauth2/authorize`,
 * the consent submission, `/oauth2/token` and the discovery document.
 *
 * Deliberately imports nothing from `org.springframework.security.oauth2.server.*` and registers no client of its
 * own: it drives only the clients configured in the test `application.yaml`. It has to keep passing unchanged when
 * the authorization server implementation is replaced.
 */
class OAuth2ProtocolConformanceTest : AbstractControllerTest() {
  @Autowired
  private lateinit var jwtService: JwtService

  @Autowired
  private lateinit var oauth2AuthorizationService: OAuth2AuthorizationService

  private lateinit var testData: BaseTestData
  private lateinit var otherUser: UserAccount

  @BeforeEach
  fun setup() {
    testData = BaseTestData()
    otherUser = testData.root.addUserAccount { username = "oauth_conformance_other" }.self
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun cleanup() {
    oauth2AuthorizationService.revokeAllForUser(testData.user.id)
    oauth2AuthorizationService.revokeAllForUser(otherUser.id)
    testDataService.cleanTestData(testData.root)
  }

  // ---------------------------------------------------------------------------------------------------------------
  // /oauth2/authorize: request validation
  // ---------------------------------------------------------------------------------------------------------------

  @Test
  fun `authorize refuses an unregistered redirect_uri without redirecting anywhere`() {
    val session = bootstrap(testData.user.id)
    val result =
      mvc
        .perform(
          authorizeRequest(redirect = "https://attacker.test/callback").session(session),
        ).andReturn()

    result.response.status.assert
      .isEqualTo(400)
    result.response
      .getHeader("Location")
      .assert
      .isNull()
  }

  @Test
  fun `authorize refuses a redirect_uri that only differs from the registered one by a suffix`() {
    val session = bootstrap(testData.user.id)
    val result =
      mvc
        .perform(
          authorizeRequest(redirect = "$EXTENSION_REDIRECT/../evil").session(session),
        ).andReturn()

    result.response.status.assert
      .isEqualTo(400)
    result.response
      .getHeader("Location")
      .assert
      .isNull()
  }

  @Test
  fun `authorize refuses an unknown client_id without redirecting anywhere`() {
    val session = bootstrap(testData.user.id)
    val result =
      mvc
        .perform(
          authorizeRequest(clientId = "no-such-client").session(session),
        ).andReturn()

    result.response.status.assert
      .isEqualTo(400)
    result.response
      .getHeader("Location")
      .assert
      .isNull()
  }

  @Test
  fun `authorize with a scope the client is not registered for redirects back with invalid_scope`() {
    val session = bootstrap(testData.user.id)
    val location =
      mvc
        .perform(
          authorizeRequest(scope = "translations.view not.a.scope", state = "scope-state").session(session),
        ).andReturn()
        .response
        .getHeader("Location")

    location.assert.startsWith(EXTENSION_REDIRECT)
    queryParam(location!!, "error").assert.isEqualTo("invalid_scope")
    queryParam(location, "state").assert.isEqualTo("scope-state")
    queryParam(location, "code").assert.isNull()
  }

  @Test
  fun `authorize refuses a response_type other than code`() {
    val session = bootstrap(testData.user.id)
    val result =
      mvc
        .perform(
          authorizeRequest(responseType = "token").session(session),
        ).andReturn()

    val location = result.response.getHeader("Location")
    location?.let { queryParam(it, "code").assert.isNull() }
    (
      result.response.status == 400 || (
        location != null && queryParam(
          location,
          "error",
        ) == "unsupported_response_type"
      )
    ).assert
      .withFailMessage("expected 400 or an unsupported_response_type redirect, got ${result.response.status} $location")
      .isTrue()
  }

  @Test
  fun `authorize refuses the plain PKCE method and issues no code`() {
    val session = bootstrap(testData.user.id)
    val verifier = randomVerifier()
    val location =
      mvc
        .perform(
          authorizeRequest(codeChallenge = verifier, codeChallengeMethod = "plain").session(session),
        ).andReturn()
        .response
        .getHeader("Location")

    location.assert.startsWith(EXTENSION_REDIRECT)
    queryParam(location!!, "error").assert.isEqualTo("invalid_request")
    queryParam(location, "code").assert.isNull()
  }

  // ---------------------------------------------------------------------------------------------------------------
  // Consent submission
  // ---------------------------------------------------------------------------------------------------------------

  @Test
  fun `denying consent redirects to the client with access_denied and leaves nothing to approve later`() {
    val pending = startConsent(testData.user.id, state = "deny-state")

    val location = submitConsent(pending, scopes = emptyList()).response.getHeader("Location")

    location.assert.startsWith(EXTENSION_REDIRECT)
    queryParam(location!!, "error").assert.isEqualTo("access_denied")
    queryParam(location, "state").assert.isEqualTo("deny-state")
    queryParam(location, "code").assert.isNull()

    // The denied authorization is gone: a later approval of the same pending state can't resurrect it.
    val retry = submitConsent(pending, scopes = listOf("translations.view"))
    retry.response.getHeader("Location")?.let { queryParam(it, "code").assert.isNull() }
    retry.response.status.assert
      .isNotEqualTo(200)
  }

  @Test
  fun `consent cannot approve a scope that the authorization request did not ask for`() {
    val pending = startConsent(testData.user.id, scope = "translations.view")

    val result = submitConsent(pending, scopes = listOf("translations.view", "translations.edit"))

    val location = result.response.getHeader("Location")
    location?.let { queryParam(it, "code").assert.isNull() }
    (result.response.status == 400 || (location != null && queryParam(location, "error") == "invalid_scope"))
      .assert
      .withFailMessage("expected 400 or an invalid_scope redirect, got ${result.response.status} $location")
      .isTrue()
  }

  @Test
  fun `consent for a state that matches no pending authorization is refused`() {
    val session = bootstrap(testData.user.id)
    val result =
      mvc
        .perform(
          post("/oauth2/authorize")
            .param("client_id", EXTENSION_CLIENT_ID)
            .param("state", "never-issued")
            .param("scope", "translations.view")
            .session(session),
        ).andReturn()

    result.response.status.assert
      .isEqualTo(400)
    result.response
      .getHeader("Location")
      .assert
      .isNull()
  }

  @Test
  fun `consent submitted from another user's session cannot approve someone else's pending authorization`() {
    val pending = startConsent(testData.user.id)
    val otherSession = bootstrap(otherUser.id)

    val result =
      mvc
        .perform(
          post("/oauth2/authorize")
            .param("client_id", EXTENSION_CLIENT_ID)
            .param("state", pending.state)
            .param("scope", "translations.view")
            .session(otherSession),
        ).andReturn()

    result.response.getHeader("Location")?.let { queryParam(it, "code").assert.isNull() }
    result.response.status.assert
      .isNotEqualTo(200)
  }

  // ---------------------------------------------------------------------------------------------------------------
  // /oauth2/token: authorization_code grant
  // ---------------------------------------------------------------------------------------------------------------

  @Test
  fun `a successful code exchange answers with the RFC 6749 token response`() {
    val pending = startConsent(testData.user.id, scope = "translations.view keys.view")
    val code = approve(pending, listOf("translations.view", "keys.view"))

    val result = exchange(code, pending.verifier)

    result.response.status.assert
      .isEqualTo(200)
    result.response.contentType.assert
      .startsWith(MediaType.APPLICATION_JSON_VALUE)
    result.response
      .getHeader("Cache-Control")
      .assert
      .contains("no-store")
    val body = json(result)
    body["access_token"].asText().assert.isNotBlank()
    body["token_type"].asText().assert.isEqualToIgnoringCase("Bearer")
    body["refresh_token"].asText().assert.isNotBlank()
    body["scope"]
      .asText()
      .split(" ")
      .toSet()
      .assert
      .isEqualTo(setOf("translations.view", "keys.view"))
    // 30 minutes from tolgee.oauth2.access-token-validity-minutes, minus whatever elapsed since issuance.
    body["expires_in"].asLong().assert.isBetween(ACCESS_TOKEN_VALIDITY_SECONDS - 10, ACCESS_TOKEN_VALIDITY_SECONDS)
  }

  @Test
  fun `an authorization code is single-use and replaying it revokes the tokens it already issued`() {
    val pending = startConsent(testData.user.id)
    val code = approve(pending, listOf("translations.view"))
    val accessToken = json(exchange(code, pending.verifier))["access_token"].asText()
    apiCall(accessToken).andIsOk

    val replay = exchange(code, pending.verifier)

    replay.response.status.assert
      .isEqualTo(400)
    json(replay)["error"].asText().assert.isEqualTo("invalid_grant")
    apiCall(accessToken).andIsUnauthorized
  }

  @Test
  fun `a code issued to one client cannot be exchanged by another client`() {
    val pending = startConsent(testData.user.id)
    val code = approve(pending, listOf("translations.view"))

    val result = exchange(code, pending.verifier, clientId = CLI_CLIENT_ID, redirect = CLI_REDIRECT)

    result.response.status.assert
      .isEqualTo(400)
    json(result)["error"].asText().assert.isEqualTo("invalid_grant")
  }

  @Test
  fun `a code cannot be exchanged with a redirect_uri other than the one it was issued for`() {
    // Both URIs are registered for the client; only the one used on /oauth2/authorize may redeem the code.
    val pending = startConsent(testData.user.id, redirect = EXTENSION_REDIRECT)
    val code = approve(pending, listOf("translations.view"))

    val result = exchange(code, pending.verifier, redirect = EXTENSION_ALTERNATE_REDIRECT)

    result.response.status.assert
      .isEqualTo(400)
    json(result)["error"].asText().assert.isEqualTo("invalid_grant")
  }

  @Test
  fun `a consent client's code cannot be redeemed when the consent never bound a project set`() {
    val pending = startConsent(testData.user.id)
    val code = approve(pending, listOf("translations.view"), bindProjects = false)

    val result = exchange(code, pending.verifier)

    assertNoTokenIssued(result)
  }

  @Test
  fun `a code exchange without a code_verifier is refused`() {
    val pending = startConsent(testData.user.id)
    val code = approve(pending, listOf("translations.view"))

    val result =
      mvc
        .perform(
          tokenRequest()
            .param("grant_type", "authorization_code")
            .param("code", code)
            .param("redirect_uri", EXTENSION_REDIRECT)
            .param("client_id", EXTENSION_CLIENT_ID),
        ).andReturn()

    assertNoTokenIssued(result)
  }

  @Test
  fun `a code exchange naming a client_id that did not authorize is refused even with the right verifier`() {
    val pending = startConsent(testData.user.id)
    val code = approve(pending, listOf("translations.view"))

    val result = exchange(code, pending.verifier, clientId = "no-such-client")

    assertNoTokenIssued(result)
  }

  @Test
  fun `grant types the server does not offer to public clients are refused`() {
    listOf("client_credentials", "password", "urn:ietf:params:oauth:grant-type:token-exchange").forEach { grant ->
      val result =
        mvc
          .perform(
            tokenRequest()
              .param("grant_type", grant)
              .param("client_id", EXTENSION_CLIENT_ID)
              .param("username", testData.user.username)
              .param("password", "irrelevant")
              .param("scope", "translations.view"),
          ).andReturn()

      assertNoTokenIssued(result)
    }
  }

  // ---------------------------------------------------------------------------------------------------------------
  // /oauth2/token: refresh_token grant
  // ---------------------------------------------------------------------------------------------------------------

  @Test
  fun `a refresh answers with a fresh token response carrying the same scope`() {
    val issued = issueTokens(scope = "translations.view keys.view")

    val result = refresh(issued["refresh_token"].asText())

    result.response.status.assert
      .isEqualTo(200)
    val body = json(result)
    body["access_token"]
      .asText()
      .assert
      .isNotBlank()
      .isNotEqualTo(issued["access_token"].asText())
    body["token_type"].asText().assert.isEqualToIgnoringCase("Bearer")
    body["scope"]
      .asText()
      .split(" ")
      .toSet()
      .assert
      .isEqualTo(setOf("translations.view", "keys.view"))
    body["expires_in"].asLong().assert.isBetween(ACCESS_TOKEN_VALIDITY_SECONDS - 10, ACCESS_TOKEN_VALIDITY_SECONDS)
    apiCall(body["access_token"].asText()).andIsOk
  }

  @Test
  fun `a refresh cannot widen the scope beyond what was granted`() {
    val issued = issueTokens(scope = "translations.view")

    val result = refresh(issued["refresh_token"].asText(), scope = "translations.view translations.edit")

    result.response.status.assert
      .isEqualTo(400)
    json(result)["error"].asText().assert.isEqualTo("invalid_scope")
  }

  @Test
  fun `a refresh token cannot be used by a client other than the one it was issued to`() {
    val issued = issueTokens()

    val result = refresh(issued["refresh_token"].asText(), clientId = CLI_CLIENT_ID)

    result.response.status.assert
      .isEqualTo(400)
    json(result)["error"].asText().assert.isEqualTo("invalid_grant")
  }

  @Test
  fun `a refresh without a client_id is refused`() {
    val issued = issueTokens()

    val result =
      mvc
        .perform(
          tokenRequest()
            .param("grant_type", "refresh_token")
            .param("refresh_token", issued["refresh_token"].asText()),
        ).andReturn()

    assertNoTokenIssued(result)
  }

  @Test
  fun `a refresh token that was never issued is refused with invalid_grant`() {
    val result = refresh("never-issued-refresh-token")

    result.response.status.assert
      .isEqualTo(400)
    json(result)["error"].asText().assert.isEqualTo("invalid_grant")
  }

  @Test
  fun `a token endpoint failure is a JSON error body, never a redirect`() {
    val result = refresh("never-issued-refresh-token")

    result.response
      .getHeader("Location")
      .assert
      .isNull()
    result.response.contentType.assert
      .startsWith(MediaType.APPLICATION_JSON_VALUE)
    json(result)["error"].asText().assert.isNotBlank()
  }

  // ---------------------------------------------------------------------------------------------------------------
  // Discovery
  // ---------------------------------------------------------------------------------------------------------------

  @Test
  fun `the discovery document describes what the server actually supports`() {
    val body =
      json(
        mvc
          .perform(get("/.well-known/oauth-authorization-server"))
          .andIsOk
          .andReturn(),
      )

    body["authorization_endpoint"].asText().assert.endsWith("/oauth2/authorize")
    body["token_endpoint"].asText().assert.endsWith("/oauth2/token")
    body["issuer"].asText().assert.isNotBlank()
    body["response_types_supported"].map { it.asText() }.assert.containsExactly("code")
    body["code_challenge_methods_supported"].map { it.asText() }.assert.containsExactly("S256")
    body["grant_types_supported"].map { it.asText() }.assert.contains("authorization_code", "refresh_token")
  }

  // ---------------------------------------------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------------------------------------------

  private data class PendingConsent(
    val session: MockHttpSession,
    val state: String,
    val verifier: String,
    val clientId: String,
    val userId: Long,
  )

  private fun bootstrap(userId: Long): MockHttpSession {
    val session = MockHttpSession()
    mvc
      .perform(
        post("/v2/oauth2/session-bootstrap")
          .header("Authorization", "Bearer ${jwtService.emitToken(userId)}")
          .session(session),
      ).andExpect {
        it.response.status.assert
          .isEqualTo(204)
      }
    return session
  }

  private fun authorizeRequest(
    clientId: String = EXTENSION_CLIENT_ID,
    redirect: String = EXTENSION_REDIRECT,
    scope: String = "translations.view",
    state: String = "client-state",
    responseType: String = "code",
    codeChallenge: String = s256Challenge(randomVerifier()),
    codeChallengeMethod: String = "S256",
  ): MockHttpServletRequestBuilder {
    val url =
      UriComponentsBuilder
        .fromPath("/oauth2/authorize")
        .queryParam("response_type", responseType)
        .queryParam("client_id", clientId)
        .queryParam("redirect_uri", redirect)
        .queryParam("scope", scope)
        .queryParam("state", state)
        .queryParam("code_challenge", codeChallenge)
        .queryParam("code_challenge_method", codeChallengeMethod)
        .build()
        .toUriString()
    return get(url).accept(MediaType.TEXT_HTML)
  }

  /** Bootstraps a session and drives `/oauth2/authorize` up to the consent page, whose `state` keys the pending grant. */
  private fun startConsent(
    userId: Long,
    clientId: String = EXTENSION_CLIENT_ID,
    redirect: String = EXTENSION_REDIRECT,
    scope: String = "translations.view",
    state: String = "client-state",
  ): PendingConsent {
    val session = bootstrap(userId)
    val verifier = randomVerifier()
    val location =
      mvc
        .perform(
          authorizeRequest(
            clientId = clientId,
            redirect = redirect,
            scope = scope,
            state = state,
            codeChallenge = s256Challenge(verifier),
          ).session(session),
        ).andReturn()
        .response
        .getHeader("Location")
    location.assert
      .withFailMessage(
        "expected a redirect to the consent page, got $location",
      ).contains("/oauth2/consent")
    val consentState = URLDecoder.decode(queryParam(location!!, "state")!!, StandardCharsets.UTF_8)
    return PendingConsent(session, consentState, verifier, clientId, userId)
  }

  private fun submitConsent(
    pending: PendingConsent,
    scopes: List<String>,
  ): MvcResult {
    val request =
      post("/oauth2/authorize")
        .param("client_id", pending.clientId)
        .param("state", pending.state)
        .session(pending.session)
    scopes.forEach { request.param("scope", it) }
    return mvc.perform(request).andReturn()
  }

  /** Binds the pending authorization to a project set, as the consent screen does before the form is submitted. */
  private fun selectProject(
    pending: PendingConsent,
    projectId: Long? = null,
  ) {
    val request =
      post("/v2/oauth2/select-project")
        .header("Authorization", "Bearer ${jwtService.emitToken(pending.userId)}")
        .param("state", pending.state)
    projectId?.let { request.param("projectId", it.toString()) }
    mvc.perform(request).andExpect {
      it.response.status.assert
        .isEqualTo(204)
    }
  }

  private fun approve(
    pending: PendingConsent,
    scopes: List<String>,
    bindProjects: Boolean = true,
  ): String {
    if (bindProjects) selectProject(pending)
    val location = submitConsent(pending, scopes).response.getHeader("Location")
    val code = location?.let { queryParam(it, "code") }
    code.assert.withFailMessage("consent did not deliver a code: $location").isNotNull()
    return code!!
  }

  private fun tokenRequest(): MockHttpServletRequestBuilder =
    post("/oauth2/token")
      .contentType(MediaType.APPLICATION_FORM_URLENCODED)
      .accept(MediaType.APPLICATION_JSON)

  private fun exchange(
    code: String,
    verifier: String,
    clientId: String = EXTENSION_CLIENT_ID,
    redirect: String = EXTENSION_REDIRECT,
  ): MvcResult =
    mvc
      .perform(
        tokenRequest()
          .param("grant_type", "authorization_code")
          .param("code", code)
          .param("redirect_uri", redirect)
          .param("client_id", clientId)
          .param("code_verifier", verifier),
      ).andReturn()

  private fun refresh(
    refreshToken: String,
    clientId: String = EXTENSION_CLIENT_ID,
    scope: String? = null,
  ): MvcResult {
    val request =
      tokenRequest()
        .param("grant_type", "refresh_token")
        .param("refresh_token", refreshToken)
        .param("client_id", clientId)
    scope?.let { request.param("scope", it) }
    return mvc.perform(request).andReturn()
  }

  private fun issueTokens(scope: String = "translations.view"): JsonNode {
    val pending = startConsent(testData.user.id, scope = scope)
    val code = approve(pending, scope.split(" "))
    val result = exchange(code, pending.verifier)
    result.response.status.assert
      .withFailMessage(result.response.contentAsString)
      .isEqualTo(200)
    return json(result)
  }

  private fun apiCall(accessToken: String) =
    mvc.perform(
      get("/v2/projects/${testData.project.id}/translations").header("Authorization", "Bearer $accessToken"),
    )

  private fun assertNoTokenIssued(result: MvcResult) {
    result.response.status.assert
      .withFailMessage("expected a 4xx, got ${result.response.status}: ${result.response.contentAsString}")
      .isBetween(400, 499)
    result.response
      .getHeader("Location")
      .assert
      .isNull()
    val body = result.response.contentAsString
    if (body.isNotBlank()) {
      jacksonObjectMapper().readTree(body)["access_token"].assert.isNull()
    }
  }

  private fun json(result: MvcResult): JsonNode = jacksonObjectMapper().readTree(result.response.contentAsString)

  private fun randomVerifier(): String {
    val bytes = ByteArray(32)
    SecureRandom().nextBytes(bytes)
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
  }

  private fun s256Challenge(verifier: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
    return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
  }

  private fun queryParam(
    url: String,
    name: String,
  ): String? =
    UriComponentsBuilder
      .fromUriString(url)
      .build()
      .queryParams
      .getFirst(name)

  companion object {
    // Registered from tolgee.oauth2.* in the test application.yaml.
    private const val EXTENSION_CLIENT_ID = OAuth2Constants.BROWSER_EXTENSION_CLIENT_ID
    private const val EXTENSION_REDIRECT = "https://extension.test/callback"
    private const val EXTENSION_ALTERNATE_REDIRECT = "https://extension.test/alternate"
    private const val CLI_CLIENT_ID = OAuth2Constants.CLI_CLIENT_ID
    private const val CLI_REDIRECT = "http://127.0.0.1:9999/callback"
    private const val ACCESS_TOKEN_VALIDITY_SECONDS = 30L * 60
  }
}
