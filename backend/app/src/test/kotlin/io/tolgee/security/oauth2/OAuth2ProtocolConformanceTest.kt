/**
 * Copyright (C) 2026 Tolgee s.r.o. and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.tolgee.security.oauth2

import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.UserAccount
import io.tolgee.security.authentication.JwtService
import io.tolgee.testing.AbstractControllerTest
import io.tolgee.testing.assert
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import tools.jackson.databind.JsonNode
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Duration

/**
 * The OAuth 2.1 contract of the authorization server as a client observes it over HTTP: `/oauth2/authorize`,
 * the consent submission, `/oauth2/token` and the discovery document.
 *
 * Registers no client of its own — it drives only the clients configured in the test `application.yaml` — and must
 * keep passing unchanged when the authorization server implementation is replaced.
 */
class OAuth2ProtocolConformanceTest : AbstractControllerTest() {
  @Autowired
  private lateinit var jwtService: JwtService

  @Autowired
  private lateinit var oauth2AuthorizationService: OAuth2AuthorizationService

  private lateinit var testData: BaseTestData
  private lateinit var otherUser: UserAccount
  private lateinit var driver: OAuth2FlowDriver

  @BeforeEach
  fun setup() {
    testData = BaseTestData()
    otherUser = testData.root.addUserAccount { username = "oauth_conformance_other" }.self
    testDataService.saveTestData(testData.root)
    driver = OAuth2FlowDriver(mvc)
  }

  @AfterEach
  fun cleanup() {
    currentDateProvider.forcedDate = null
    oauth2AuthorizationService.revokeAllForUser(testData.user.id)
    oauth2AuthorizationService.revokeAllForUser(otherUser.id)
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `authorize refuses an unregistered redirect_uri without redirecting anywhere`() {
    val response = driver.authorize(CLIENT_ID, "https://attacker.test/steal").andReturn().response
    response.status.assert.isEqualTo(400)
    response.getHeader("Location").assert.isNull()
  }

  @Test
  fun `authorize refuses a redirect_uri that only differs from the registered one by a suffix`() {
    val response = driver.authorize(CLIENT_ID, "$REDIRECT.attacker.test").andReturn().response
    response.status.assert.isEqualTo(400)
    response.getHeader("Location").assert.isNull()
  }

  @Test
  fun `authorize refuses an unknown client_id without redirecting anywhere`() {
    val response = driver.authorize("no-such-client", REDIRECT).andReturn().response
    response.status.assert.isEqualTo(400)
    response.getHeader("Location").assert.isNull()
  }

  @Test
  fun `opening an authorization for an unregistered client is refused`() {
    val result = driver.startAuthorization(jwt(), "no-such-client", REDIRECT, validParams()).andReturn()

    result.response.status.assert
      .isEqualTo(404)
    result.response.contentAsString.assert
      .contains(Message.OAUTH_UNKNOWN_CLIENT.code)
  }

  @Test
  fun `opening an authorization for a redirect_uri the client does not own is refused`() {
    // The SPA re-sends parameters that GET /oauth2/authorize already checked, so this endpoint has to check them
    // again — otherwise a crafted POST could open an authorization that redirects anywhere.
    val result =
      driver.startAuthorization(jwt(), CLIENT_ID, "https://attacker.test/steal", validParams()).andReturn()

    result.response.status.assert
      .isEqualTo(400)
    result.response.contentAsString.assert
      .contains(Message.OAUTH_REDIRECT_URI_NOT_REGISTERED.code)
  }

  @Test
  fun `a malformed authorize request is answered by the authorization endpoint itself, before any login`() {
    val location =
      driver
        .authorize(CLIENT_ID, REDIRECT, validParams() + mapOf("response_type" to "token", "state" to "s1"))
        .andReturn()
        .response
        .getHeader("Location")!!

    location.assert.startsWith(REDIRECT)
    location.assert.contains("error=unsupported_response_type")
    location.assert.contains("state=s1")
  }

  @Test
  fun `authorize hands a valid request to the consent screen without authenticating anybody`() {
    val response =
      driver
        .authorize(
          CLIENT_ID,
          REDIRECT,
          mapOf(
            "response_type" to "code",
            "scope" to "translations.view",
            "state" to "client-state",
            "code_challenge" to OAuth2FlowDriver.s256Challenge(OAuth2FlowDriver.randomVerifier()),
            "code_challenge_method" to "S256",
          ),
        ).andReturn()
        .response
    response.status.assert.isEqualTo(302)
    val location = response.getHeader("Location")!!
    location.assert.contains(OAuth2Constants.CONSENT_PAGE_PATH)
    // The client's own parameters have to survive the hand-off, or the screen cannot open the authorization.
    location.assert.contains("state=client-state")
    location.assert.contains("scope=translations.view")
  }

  @Test
  fun `the consent redirect is relative when no front-end url is configured`() {
    val original = tolgeeProperties.frontEndUrl
    tolgeeProperties.frontEndUrl = null
    try {
      authorizeRedirect().assert.startsWith(OAuth2Constants.CONSENT_PAGE_PATH)
    } finally {
      tolgeeProperties.frontEndUrl = original
    }
  }

  @Test
  fun `the consent redirect is absolute when a front-end url says where the SPA lives`() {
    val original = tolgeeProperties.frontEndUrl
    tolgeeProperties.frontEndUrl = "https://app.tolgee.example.com"
    try {
      val location = authorizeRedirect()
      location.assert.startsWith("https://app.tolgee.example.com${OAuth2Constants.CONSENT_PAGE_PATH}")
    } finally {
      tolgeeProperties.frontEndUrl = original
    }
  }

  @Test
  fun `opening an authorization with a scope the server does not support yields invalid_scope`() {
    errorRedirect(mapOf("scope" to "not.a.tolgee.scope")).assert.contains("error=invalid_scope")
  }

  @Test
  fun `omitting response_type is invalid_request, not unsupported_response_type`() {
    // OAuth 2.1 reserves unsupported_response_type for a value the server was given and cannot honour; a missing
    // required parameter is invalid_request, and a client needs to tell those apart.
    errorRedirect(mapOf("response_type" to null)).assert.contains("error=invalid_request")
  }

  @Test
  fun `opening an authorization with a response_type other than code is refused`() {
    errorRedirect(mapOf("response_type" to "token")).assert.contains("error=unsupported_response_type")
  }

  @Test
  fun `the plain PKCE method is refused`() {
    errorRedirect(mapOf("code_challenge_method" to "plain")).assert.contains("error=invalid_request")
  }

  @Test
  fun `an authorize request without a code_challenge is refused`() {
    errorRedirect(mapOf("code_challenge" to null)).assert.contains("error=invalid_request")
  }

  @Test
  fun `a code_challenge that is not a base64url S256 digest is refused`() {
    errorRedirect(mapOf("code_challenge" to "too-short")).assert.contains("error=invalid_request")
  }

  @Test
  fun `denying consent redirects to the client with access_denied and leaves nothing to approve later`() {
    val pending = driver.startPendingConsent(jwt(), CLIENT_ID, REDIRECT)
    val redirect = driver.consentRedirect(pending, approvedScopes = emptyList())
    redirect.assert.contains("error=access_denied")
    redirect.assert.contains("state=client-state")

    driver
      .submitConsent(pending)
      .andReturn()
      .response.status.assert
      .isEqualTo(404)
  }

  @Test
  fun `consent cannot approve a scope that the authorization request did not ask for`() {
    val pending = driver.startPendingConsent(jwt(), CLIENT_ID, REDIRECT, scope = "translations.view")
    driver
      .consentRedirect(pending, approvedScopes = listOf("translations.view", "keys.edit"))
      .assert
      .contains("error=invalid_scope")

    driver
      .submitConsent(pending)
      .andReturn()
      .response.status.assert
      .isEqualTo(404)
  }

  @Test
  fun `consent for a state that matches no pending authorization is refused`() {
    val pending = driver.startPendingConsent(jwt(), CLIENT_ID, REDIRECT).copy(state = "not-a-real-state")
    driver
      .submitConsent(pending)
      .andReturn()
      .response.status.assert
      .isEqualTo(404)
  }

  @Test
  fun `consent from another user cannot approve someone else's pending authorization`() {
    val pending = driver.startPendingConsent(jwt(), CLIENT_ID, REDIRECT)
    val theirs = pending.copy(jwt = jwtService.emitToken(otherUser.id))
    driver
      .submitConsent(theirs)
      .andReturn()
      .response.status.assert
      .isEqualTo(404)
  }

  @Test
  fun `approving SINGLE_PROJECT without naming a project is refused`() {
    val pending = driver.startPendingConsent(jwt(), CLIENT_ID, REDIRECT)
    val result =
      driver.submitConsentBody(
        pending,
        """{"state":"${pending.state}","scopes":["translations.view"],"projectScope":"SINGLE_PROJECT"}""",
      )

    result.response.status.assert
      .isEqualTo(400)
    result.response.contentAsString.assert
      .contains(Message.OAUTH_PROJECT_REQUIRED.code)
    driver.consentRedirect(pending, projectId = null).assert.contains("code=")
  }

  @Test
  fun `approving without saying which projects at all is refused`() {
    val pending = driver.startPendingConsent(jwt(), CLIENT_ID, REDIRECT)
    val result = driver.submitConsentBody(pending, """{"state":"${pending.state}","scopes":["translations.view"]}""")

    result.response.status.assert
      .isEqualTo(400)
    result.response.contentAsString.assert
      .contains(Message.OAUTH_PROJECT_SCOPE_REQUIRED.code)
  }

  @Test
  fun `denying needs no project scope at all`() {
    val pending = driver.startPendingConsent(jwt(), CLIENT_ID, REDIRECT)
    val result = driver.submitConsentBody(pending, """{"state":"${pending.state}","scopes":[]}""")

    result.response.status.assert
      .isEqualTo(200)
    result.response.contentAsString.assert
      .contains("access_denied")
  }

  @Test
  fun `a consent state cannot be reused once it has been granted`() {
    val pending = driver.startPendingConsent(jwt(), CLIENT_ID, REDIRECT)
    driver.consentRedirect(pending).assert.contains("code=")
    driver
      .submitConsent(pending)
      .andReturn()
      .response.status.assert
      .isEqualTo(404)
  }

  @Test
  fun `a successful code exchange answers with the RFC 6749 token response`() {
    val result = tokenResult()
    val response = result.response
    response.getHeader("Cache-Control").assert.contains("no-store")
    val body = json(result)
    body
      .get("token_type")
      .asString()
      .assert
      .isEqualTo("Bearer")
    body
      .get("access_token")
      .asString()
      .assert
      .isNotBlank()
    body
      .get("refresh_token")
      .asString()
      .assert
      .isNotBlank()
    body
      .get("scope")
      .asString()
      .assert
      .isEqualTo("translations.view")
    body
      .get("expires_in")
      .asLong()
      .assert
      .isEqualTo(oauth2.accessTokenValidityMinutes * 60)
  }

  @Test
  fun `an authorization code is single-use and replaying it revokes the tokens it already issued`() {
    val pending = driver.startPendingConsent(jwt(), CLIENT_ID, REDIRECT)
    val code = driver.queryParam(driver.consentRedirect(pending), "code")!!
    val first = json(driver.exchangeCode(code, CLIENT_ID, REDIRECT, pending.verifier).andReturn())
    val replay = driver.exchangeCode(code, CLIENT_ID, REDIRECT, pending.verifier).andReturn()

    json(replay)
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_grant")
    driver.refresh(first.get("refresh_token").asString(), CLIENT_ID).andReturn().let {
      json(it)
        .get("error")
        .asString()
        .assert
        .isEqualTo("invalid_grant")
    }
  }

  @Test
  fun `a code issued to one client cannot be exchanged by another client`() {
    val pending = driver.startPendingConsent(jwt(), CLIENT_ID, REDIRECT)
    val code = driver.queryParam(driver.consentRedirect(pending), "code")!!
    val result = driver.exchangeCode(code, OTHER_CLIENT_ID, OTHER_REDIRECT, pending.verifier).andReturn()
    json(result)
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_grant")

    // A code the wrong client has touched is treated as stolen, so the legitimate client cannot redeem it either.
    json(driver.exchangeCode(code, CLIENT_ID, REDIRECT, pending.verifier).andReturn())
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_grant")
  }

  @Test
  fun `a code cannot be exchanged with a redirect_uri other than the one it was issued for`() {
    val pending = driver.startPendingConsent(jwt(), CLIENT_ID, REDIRECT)
    val code = driver.queryParam(driver.consentRedirect(pending), "code")!!
    val result = driver.exchangeCode(code, CLIENT_ID, "https://extension.test/other", pending.verifier).andReturn()
    json(result)
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_grant")
  }

  @Test
  fun `a code exchange without a code_verifier is refused`() {
    val pending = driver.startPendingConsent(jwt(), CLIENT_ID, REDIRECT)
    val code = driver.queryParam(driver.consentRedirect(pending), "code")!!
    val result = driver.exchangeCode(code, CLIENT_ID, REDIRECT, "").andReturn()
    json(result)
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_request")
  }

  @Test
  fun `a code exchange with a malformed code_verifier is refused`() {
    val pending = driver.startPendingConsent(jwt(), CLIENT_ID, REDIRECT)
    val code = driver.queryParam(driver.consentRedirect(pending), "code")!!
    // RFC 7636 §4.1: shorter than the 43-character minimum.
    val result = driver.exchangeCode(code, CLIENT_ID, REDIRECT, "short").andReturn()
    json(result)
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_request")
  }

  @Test
  fun `a code exchange with a well-formed but wrong code_verifier is refused`() {
    val pending = driver.startPendingConsent(jwt(), CLIENT_ID, REDIRECT)
    val code = driver.queryParam(driver.consentRedirect(pending), "code")!!
    // Syntactically valid per RFC 7636 §4.1, so only the S256 comparison itself can reject it.
    val wrong = OAuth2FlowDriver.randomVerifier()
    wrong.assert.isNotEqualTo(pending.verifier)
    val result = driver.exchangeCode(code, CLIENT_ID, REDIRECT, wrong).andReturn()
    json(result)
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_grant")
  }

  @Test
  fun `a pending consent that went stale can no longer be approved`() {
    val pending = driver.startPendingConsent(jwt(), CLIENT_ID, REDIRECT)
    currentDateProvider.move(Duration.ofSeconds(oauth2.consentValiditySeconds + 60))
    driver
      .submitConsent(pending)
      .andReturn()
      .response.status.assert
      .isEqualTo(404)
  }

  @Test
  fun `an expired authorization code is refused`() {
    val pending = driver.startPendingConsent(jwt(), CLIENT_ID, REDIRECT)
    val code = driver.queryParam(driver.consentRedirect(pending), "code")!!
    currentDateProvider.move(Duration.ofSeconds(oauth2.authorizationCodeValiditySeconds + 60))
    val result = driver.exchangeCode(code, CLIENT_ID, REDIRECT, pending.verifier).andReturn()
    json(result)
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_grant")
  }

  @Test
  fun `the token endpoint refuses credentials sent in the query string`() {
    // OAuth 2.1 §3.2.2: the request is a form body. A code and its verifier in the request line end up in proxy
    // and access logs, and together they are a complete grant.
    val pending = driver.startPendingConsent(jwt(), CLIENT_ID, REDIRECT)
    val code = driver.queryParam(driver.consentRedirect(pending, projectId = null), "code")!!
    val result =
      mvc
        .perform(
          post("/oauth2/token?grant_type=authorization_code&client_id=$CLIENT_ID&code=$code")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED),
        ).andReturn()

    json(result)
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_request")
  }

  @Test
  fun `a parameter sent without a value counts as omitted`() {
    // OAuth 2.1 §3.2: "Parameters sent without a value MUST be treated as if they were omitted."
    val result =
      mvc
        .perform(
          post("/oauth2/token")
            .param("grant_type", "")
            .param("client_id", CLIENT_ID)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED),
        ).andReturn()

    json(result)
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_request")
  }

  @Test
  fun `an authorization error redirect carries the RFC 9207 iss`() {
    val redirect = errorRedirect(mapOf("response_type" to "token"))
    redirect.assert.contains("error=unsupported_response_type")
    redirect.assert.contains("iss=")
  }

  @Test
  fun `grant types the server does not offer to public clients are refused`() {
    listOf("client_credentials", "password", "implicit").forEach { grant ->
      val result =
        mvc
          .perform(
            post("/oauth2/token")
              .param("grant_type", grant)
              .param("client_id", CLIENT_ID)
              .contentType(MediaType.APPLICATION_FORM_URLENCODED),
          ).andReturn()
      json(result)
        .get("error")
        .asString()
        .assert
        .isEqualTo("unsupported_grant_type")
    }
  }

  @Test
  fun `a refresh answers with a fresh token response carrying the same scope`() {
    val issued = json(tokenResult())
    val refreshed = json(driver.refresh(issued.get("refresh_token").asString(), CLIENT_ID).andReturn())
    refreshed
      .get("access_token")
      .asString()
      .assert
      .isNotEqualTo(issued.get("access_token").asString())
    refreshed
      .get("refresh_token")
      .asString()
      .assert
      .isNotEqualTo(issued.get("refresh_token").asString())
    refreshed
      .get("scope")
      .asString()
      .assert
      .isEqualTo("translations.view")
  }

  @Test
  fun `a refresh cannot widen the scope beyond what was granted`() {
    val issued = json(tokenResult())
    val result = driver.refresh(issued.get("refresh_token").asString(), CLIENT_ID, scope = "keys.edit").andReturn()
    json(result)
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_scope")
  }

  @Test
  fun `a narrowing refresh issues the narrower token without shrinking the grant`() {
    val issued =
      driver.completeFlow(
        jwt(),
        CLIENT_ID,
        REDIRECT,
        scope = "translations.view keys.view",
        approvedScopes = listOf("translations.view", "keys.view"),
      )
    val narrowed =
      json(driver.refresh(issued.get("refresh_token").asString(), CLIENT_ID, scope = "keys.view").andReturn())
    narrowed
      .get("scope")
      .asString()
      .assert
      .isEqualTo("keys.view")

    // RFC 6749 §6 narrows the issued token, not the grant, so the full set is still available next time.
    val widened = json(driver.refresh(narrowed.get("refresh_token").asString(), CLIENT_ID).andReturn())
    widened
      .get("scope")
      .asString()
      .assert
      .isEqualTo("translations.view keys.view")
  }

  @Test
  fun `replaying the superseded refresh token revokes the grant`() {
    val issued = json(tokenResult())
    val superseded = issued.get("refresh_token").asString()
    val rotated = json(driver.refresh(superseded, CLIENT_ID).andReturn())

    json(driver.refresh(superseded, CLIENT_ID).andReturn())
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_grant")
    json(driver.refresh(rotated.get("refresh_token").asString(), CLIENT_ID).andReturn())
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_grant")
  }

  @Test
  fun `a token older than the last rotation fails without destroying the grant`() {
    // Detection reaches exactly one generation back: only the secret the grant most recently replaced revokes it.
    // Anything older is inert, so a secret an attacker guesses or replays late cannot be used as a kill switch.
    val issued = json(tokenResult())
    val oldest = issued.get("refresh_token").asString()
    val second = json(driver.refresh(oldest, CLIENT_ID).andReturn()).get("refresh_token").asString()
    val third = json(driver.refresh(second, CLIENT_ID).andReturn()).get("refresh_token").asString()

    json(driver.refresh(oldest, CLIENT_ID).andReturn())
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_grant")
    json(driver.refresh(third, CLIENT_ID).andReturn())
      .get("access_token")
      .asString()
      .assert
      .isNotBlank()
  }

  @Test
  fun `a refresh token this grant never issued is refused without revoking anything`() {
    val issued = json(tokenResult())
    json(driver.refresh("tgort_never-issued-secret", CLIENT_ID).andReturn())
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_grant")

    json(driver.refresh(issued.get("refresh_token").asString(), CLIENT_ID).andReturn())
      .get("access_token")
      .asString()
      .assert
      .isNotBlank()
  }

  @Test
  fun `an expired refresh token is refused`() {
    val issued = json(tokenResult())
    currentDateProvider.move(Duration.ofDays(oauth2.refreshTokenValidityDays + 1))
    json(driver.refresh(issued.get("refresh_token").asString(), CLIENT_ID).andReturn())
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_grant")
  }

  @Test
  fun `a refresh token cannot be used by a client other than the one it was issued to`() {
    val issued = json(tokenResult())
    json(driver.refresh(issued.get("refresh_token").asString(), OTHER_CLIENT_ID).andReturn())
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_grant")
  }

  @Test
  fun `a refresh without a client_id is refused`() {
    val issued = json(tokenResult())
    val result =
      mvc
        .perform(
          post("/oauth2/token")
            .param("grant_type", "refresh_token")
            .param("refresh_token", issued.get("refresh_token").asString())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED),
        ).andReturn()
    result.response.status.assert
      .isEqualTo(400)
    result.response
      .getHeader("WWW-Authenticate")
      .assert
      .isNull()
    json(result)
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_client")
  }

  @Test
  fun `a refresh token that was never issued is refused with invalid_grant`() {
    json(driver.refresh("tgort_nothing-like-this", CLIENT_ID).andReturn())
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_grant")
  }

  @Test
  fun `a token endpoint failure is a JSON error body, never a redirect`() {
    val result = driver.refresh("tgort_bogus", CLIENT_ID).andReturn()
    result.response
      .getHeader("Location")
      .assert
      .isNull()
    result.response.contentType.assert
      .contains("application/json")
  }

  @Test
  fun `the discovery document describes what the server actually supports`() {
    mvc
      .perform(get("/.well-known/oauth-authorization-server"))
      .andIsOk
      .andReturn()
      .let { json(it) }
      .let { doc ->
        assertThat(values(doc, "response_types_supported")).containsExactly("code")
        assertThat(values(doc, "grant_types_supported"))
          .containsExactlyInAnyOrder("authorization_code", "refresh_token")
        assertThat(values(doc, "code_challenge_methods_supported")).containsExactly("S256")
        assertThat(values(doc, "token_endpoint_auth_methods_supported")).containsExactly("none")
        // RFC 8414 names every member in snake_case; a camelCase key is invisible to a conforming client.
        assertThat(values(doc, "scopes_supported")).contains("translations.view")
        doc
          .get("authorization_response_iss_parameter_supported")
          .asBoolean()
          .assert
          .isTrue()
        doc
          .get("authorization_endpoint")
          .asString()
          .assert
          .endsWith("/oauth2/authorize")
        doc
          .get("token_endpoint")
          .asString()
          .assert
          .endsWith("/oauth2/token")
        doc
          .get("issuer")
          .asString()
          .assert
          .isNotBlank()
      }
  }

  private fun jwt(): String = jwtService.emitToken(testData.user.id)

  @Test
  fun `a state carrying reserved characters survives to the consent redirect`() {
    // RFC 6749 section 4.1.1 allows any printable ASCII in state, so a client that does not percent-encode its own
    // state must still be accepted rather than failing URI verification after the request was already validated.
    val state = "50%off [b] |c"
    val location =
      driver
        .authorize(CLIENT_ID, REDIRECT, validParams() + ("state" to state))
        .andReturn()
        .response
        .getHeader("Location")!!

    location.assert.contains(OAuth2Constants.CONSENT_PAGE_PATH)
    URLDecoder.decode(driver.queryParam(location, "state")!!, StandardCharsets.UTF_8).assert.isEqualTo(state)
  }

  @Test
  fun `the outbound encoder never emits a bare plus, so a space and a plus stay distinguishable`() {
    // RFC 6749 Appendix B: the authorize query is application/x-www-form-urlencoded, so a conforming client already
    // sends a space as `+` and a literal plus as `%2B`, and the container decodes both before Tolgee sees them.
    // Why the way out must not use a bare `+` is on OAuth2Redirects.encodeQueryValue.
    assertEncodedBothLegs(received = "abc+def", encoded = "abc%2Bdef")
    assertEncodedBothLegs(received = "abc def", encoded = "abc%20def")
  }

  /**
   * [received] is the value the handler gets after the request layer has decoded the query, which is where the two
   * legs meet: MockMvc percent-decodes and a servlet container form-decodes, so what a client put on the wire is not
   * reproducible here. What is asserted is the half Tolgee owns — how that value is written back out.
   */
  private fun assertEncodedBothLegs(
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

  @Test
  fun `a token request repeating a parameter is refused`() {
    val issued = json(tokenResult())
    val result =
      mvc
        .perform(
          post("/oauth2/token")
            .param("grant_type", "refresh_token")
            .param("client_id", CLIENT_ID)
            .param("refresh_token", issued.get("refresh_token").asString())
            .param("refresh_token", "a-second-value")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED),
        ).andReturn()

    result.response.status.assert
      .isEqualTo(400)
    json(result)
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_request")
  }

  @Test
  fun `a state too long to be stored is refused on the redirect rather than at consent`() {
    val location =
      driver
        .authorize(
          CLIENT_ID,
          REDIRECT,
          validParams() + ("state" to "x".repeat(OAuth2AuthorizationService.MAX_STATE_LENGTH + 1)),
        ).andReturn()
        .response
        .getHeader("Location")!!

    location.assert.startsWith(REDIRECT)
    location.assert.contains("error=invalid_request")
  }

  @Test
  fun `a parameter sent more than once is refused`() {
    val location =
      mvc
        .perform(
          get(
            "/oauth2/authorize?response_type=code&client_id=$CLIENT_ID" +
              "&redirect_uri=$REDIRECT" +
              "&scope=translations.view&scope=admin" +
              "&code_challenge=${OAuth2FlowDriver.s256Challenge(OAuth2FlowDriver.randomVerifier())}" +
              "&code_challenge_method=S256",
          ),
        ).andReturn()
        .response
        .getHeader("Location")!!

    location.assert.startsWith(REDIRECT)
    location.assert.contains("error=invalid_request")
  }

  private val oauth2 get() = tolgeeProperties.oauth2

  private fun validParams(): Map<String, String?> =
    mapOf(
      "response_type" to "code",
      "scope" to "translations.view",
      "code_challenge" to OAuth2FlowDriver.s256Challenge(OAuth2FlowDriver.randomVerifier()),
      "code_challenge_method" to "S256",
    )

  private fun authorizeRedirect(): String =
    driver
      .authorize(CLIENT_ID, REDIRECT, validParams())
      .andReturn()
      .response
      .getHeader("Location")!!

  private fun tokenResult(): MvcResult {
    val pending = driver.startPendingConsent(jwt(), CLIENT_ID, REDIRECT)
    val code = driver.queryParam(driver.consentRedirect(pending), "code")!!
    return driver.exchangeCode(code, CLIENT_ID, REDIRECT, pending.verifier).andReturn()
  }

  /** The redirect an authorize request produces when the parameter under test makes it invalid. */
  private fun errorRedirect(overrides: Map<String, String?>): String {
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

  private fun json(result: MvcResult): JsonNode = jacksonObjectMapper().readTree(result.response.contentAsString)

  private fun values(
    doc: JsonNode,
    field: String,
  ): List<String> =
    doc
      .get(field)
      .valueStream()
      .map { it.asString() }
      .toList()

  companion object {
    // Registered from tolgee.oauth2.* in the test application.yaml.
    private const val CLIENT_ID = OAuth2Constants.BROWSER_EXTENSION_CLIENT_ID
    private const val REDIRECT = "https://extension.test/callback"
    private const val OTHER_CLIENT_ID = OAuth2Constants.CLI_CLIENT_ID
    private const val OTHER_REDIRECT = "http://127.0.0.1:9999/callback"
  }
}
