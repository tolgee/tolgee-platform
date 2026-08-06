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

import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.fixtures.andIsOk
import io.tolgee.security.authentication.JwtService
import io.tolgee.testing.AbstractControllerTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpSession
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.web.util.UriComponentsBuilder
import tools.jackson.databind.JsonNode
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * Drives the whole browser flow through MockMvc against the pre-registered CLI client (consent disabled, so no page is
 * needed): session bootstrap -> /oauth2/authorize -> code -> /oauth2/token -> use the access token on the REST API.
 * This is the end-to-end proof that a real authorization-code token works — including that `sub` is the numeric user id.
 */
class OAuth2AuthorizationCodeFlowTest : AbstractControllerTest() {
  @Autowired
  private lateinit var jwtService: JwtService

  @Autowired
  private lateinit var registeredClientRepository: RegisteredClientRepository

  private lateinit var testData: BaseTestData

  @BeforeEach
  fun setup() {
    testData = BaseTestData()
    testDataService.saveTestData(testData.root)
    // Tolgee's per-test DB reset wipes the startup-seeded clients, so register self-contained ones here.
    registeredClientRepository.save(flowClient(TEST_CLIENT_ID, CLI_REDIRECT))
    registeredClientRepository.save(flowClient(SECOND_CLIENT_ID, SECOND_REDIRECT))
    registeredClientRepository.save(flowClient(CONSENT_CLIENT_ID, CONSENT_REDIRECT, requireConsent = true))
  }

  private fun flowClient(
    clientId: String,
    redirect: String,
    requireConsent: Boolean = false,
  ): RegisteredClient =
    RegisteredClient
      .withId(clientId)
      .clientId(clientId)
      .clientName("Test Flow Client $clientId")
      .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
      .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
      .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
      .redirectUri(redirect)
      .scope("translations.view")
      .clientSettings(
        ClientSettings
          .builder()
          .requireProofKey(true)
          .requireAuthorizationConsent(requireConsent)
          .build(),
      ).tokenSettings(
        TokenSettings
          .builder()
          .accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED)
          .reuseRefreshTokens(false)
          .build(),
      ).build()

  @Test
  fun `connected-apps excludes a consent-required client the user reached but never approved`() {
    val jwt = jwtService.emitToken(testData.user.id)
    val session = MockHttpSession()
    mvc.perform(post("/v2/oauth2/session-bootstrap").header("Authorization", "Bearer $jwt").session(session))

    val verifier = randomVerifier()
    val authorizeUrl =
      UriComponentsBuilder
        .fromPath("/oauth2/authorize")
        .queryParam("response_type", "code")
        .queryParam("client_id", CONSENT_CLIENT_ID)
        .queryParam("redirect_uri", CONSENT_REDIRECT)
        .queryParam("scope", "translations.view")
        .queryParam("code_challenge", s256Challenge(verifier))
        .queryParam("code_challenge_method", "S256")
        .queryParam("state", "state-consent")
        .build()
        .toUriString()
    // Reaching /oauth2/authorize for a consent-required client persists a pending (token-less) authorization row.
    mvc.perform(get(authorizeUrl).session(session))

    assertThat(connectedApps(jwt)).doesNotContain("\"$CONSENT_CLIENT_ID\"")
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `authorization code + PKCE flow issues an access token that works on the REST API`() {
    val accessToken = runAuthorizationCodeFlow()

    assertThat(decodeClaims(accessToken).get("sub").asText()).isEqualTo(testData.user.id.toString())

    mvc
      .perform(
        get("/v2/projects/${testData.project.id}/translations")
          .header("Authorization", "Bearer $accessToken"),
      ).andIsOk
  }

  @Test
  fun `issues a refresh token to the public client and rotates it on refresh`() {
    // Bind to a single project so the refresh path can be checked for project-set widening.
    val firstResponse =
      authorizationCodeTokenResponse(mapOf(OAuth2Constants.PROJECT_PARAM to testData.project.id.toString()))
    val refreshToken = firstResponse.get("refresh_token")?.asString()
    assertThat(refreshToken).isNotNull()

    val refreshResponse =
      mvc
        .perform(
          post("/oauth2/token")
            .param("grant_type", "refresh_token")
            .param("refresh_token", refreshToken!!)
            .param("client_id", TEST_CLIENT_ID)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED),
        ).andReturn()
        .response.contentAsString

    val refreshed = jacksonObjectMapper().readTree(refreshResponse)
    val refreshedAccessToken = refreshed.get("access_token")?.asString()
    assertThat(refreshedAccessToken).isNotNull()
    assertThat(refreshed.get("refresh_token")?.asString()).isNotNull().isNotEqualTo(refreshToken)

    // The refreshed token must carry the same narrow binding as the original — refresh must never widen tg.prj to
    // ALL_PROJECTS, widen the scope, or drop the audience.
    val initialClaims = decodeClaims(firstResponse.get("access_token").asString())
    val refreshedClaims = decodeClaims(refreshedAccessToken!!)
    assertThat(refreshedClaims.get(OAuth2Constants.PROJECTS_CLAIM).isArray).isTrue()
    assertThat(refreshedClaims.get(OAuth2Constants.PROJECTS_CLAIM)[0].asLong()).isEqualTo(testData.project.id)
    assertThat(refreshedClaims.get("aud")).isEqualTo(initialClaims.get("aud"))
    assertThat(refreshedClaims.get("scope")).isEqualTo(initialClaims.get("scope"))

    // Reuse detection: replaying the consumed original refresh token must be rejected.
    val replayStatus =
      mvc
        .perform(
          post("/oauth2/token")
            .param("grant_type", "refresh_token")
            .param("refresh_token", refreshToken)
            .param("client_id", TEST_CLIENT_ID)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED),
        ).andReturn()
        .response.status
    assertThat(replayStatus).isEqualTo(400)
  }

  @Test
  fun `rejects the code exchange when the PKCE verifier is wrong`() {
    // Guards the public-client refresh path: it must never let a code be redeemed without a valid code_verifier.
    val jwt = jwtService.emitToken(testData.user.id)
    val session = MockHttpSession()
    mvc.perform(post("/v2/oauth2/session-bootstrap").header("Authorization", "Bearer $jwt").session(session))

    val verifier = randomVerifier()
    val authorizeUrl =
      UriComponentsBuilder
        .fromPath("/oauth2/authorize")
        .queryParam("response_type", "code")
        .queryParam("client_id", TEST_CLIENT_ID)
        .queryParam("redirect_uri", CLI_REDIRECT)
        .queryParam("scope", "translations.view")
        .queryParam("code_challenge", s256Challenge(verifier))
        .queryParam("code_challenge_method", "S256")
        .queryParam("state", "state-123")
        .build()
        .toUriString()
    val code =
      queryParam(
        mvc
          .perform(get(authorizeUrl).session(session))
          .andReturn()
          .response
          .getHeader("Location")!!,
        "code",
      )

    val status =
      mvc
        .perform(
          post("/oauth2/token")
            .param("grant_type", "authorization_code")
            .param("code", code!!)
            .param("redirect_uri", CLI_REDIRECT)
            .param("client_id", TEST_CLIENT_ID)
            .param("code_verifier", "not-the-real-verifier")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED),
        ).andReturn()
        .response.status
    assertThat(status).isEqualTo(400)
  }

  @Test
  fun `binds the token to the project hinted on the authorize request`() {
    val accessToken = runAuthorizationCodeFlow(mapOf(OAuth2Constants.PROJECT_PARAM to testData.project.id.toString()))

    val projectClaim = decodeClaims(accessToken).get(OAuth2Constants.PROJECTS_CLAIM)
    assertThat(projectClaim.isArray).isTrue()
    assertThat(projectClaim.size()).isEqualTo(1)
    assertThat(projectClaim[0].asLong()).isEqualTo(testData.project.id)
  }

  @Test
  fun `session-bootstrap rotates the session id (fixation defense)`() {
    val jwt = jwtService.emitToken(testData.user.id)
    val session = MockHttpSession()
    val originalId = session.id

    mvc
      .perform(
        post("/v2/oauth2/session-bootstrap")
          .header("Authorization", "Bearer $jwt")
          .session(session),
      ).andExpect { assertThat(it.response.status).isEqualTo(204) }

    assertThat(session.id).isNotEqualTo(originalId)
  }

  @Test
  fun `an API token cannot authenticate the authorize endpoint without a bootstrapped session`() {
    // AuthenticationFilter is intentionally NOT on the SAS chain: a raw webapp/PAK/PAT bearer must not establish a
    // principal at /oauth2/authorize (that would let a token mint another token). Unauthenticated -> bootstrap redirect.
    val jwt = jwtService.emitToken(testData.user.id)
    val verifier = randomVerifier()
    val authorizeUrl =
      UriComponentsBuilder
        .fromPath("/oauth2/authorize")
        .queryParam("response_type", "code")
        .queryParam("client_id", TEST_CLIENT_ID)
        .queryParam("redirect_uri", CLI_REDIRECT)
        .queryParam("scope", "translations.view")
        .queryParam("code_challenge", s256Challenge(verifier))
        .queryParam("code_challenge_method", "S256")
        .queryParam("state", "state-123")
        .build()
        .toUriString()

    val location =
      mvc
        .perform(get(authorizeUrl).header("Authorization", "Bearer $jwt"))
        .andReturn()
        .response
        .getHeader("Location")

    assertThat(location).contains("/oauth2/bootstrap")
    assertThat(location).doesNotContain(CLI_REDIRECT)
    assertThat(queryParam(location!!, "code")).isNull()
  }

  @Test
  fun `connected-apps lists authorized apps and revoking one leaves the others intact`() {
    runAuthorizationCodeFlow(clientId = TEST_CLIENT_ID, redirect = CLI_REDIRECT)
    runAuthorizationCodeFlow(clientId = SECOND_CLIENT_ID, redirect = SECOND_REDIRECT)
    val jwt = jwtService.emitToken(testData.user.id)

    assertThat(connectedApps(jwt)).contains("\"$TEST_CLIENT_ID\"").contains("\"$SECOND_CLIENT_ID\"")

    mvc
      .perform(delete("/v2/user/connected-apps/$TEST_CLIENT_ID").header("Authorization", "Bearer $jwt"))
      .andExpect { assertThat(it.response.status).isEqualTo(204) }

    assertThat(connectedApps(jwt)).doesNotContain("\"$TEST_CLIENT_ID\"").contains("\"$SECOND_CLIENT_ID\"")
  }

  private fun connectedApps(jwt: String): String =
    mvc
      .perform(get("/v2/user/connected-apps").header("Authorization", "Bearer $jwt"))
      .andIsOk
      .andReturn()
      .response.contentAsString

  private fun runAuthorizationCodeFlow(
    extraAuthorizeParams: Map<String, String> = emptyMap(),
    clientId: String = TEST_CLIENT_ID,
    redirect: String = CLI_REDIRECT,
  ): String {
    return authorizationCodeTokenResponse(extraAuthorizeParams, clientId, redirect).get("access_token").asString()
  }

  private fun authorizationCodeTokenResponse(
    extraAuthorizeParams: Map<String, String> = emptyMap(),
    clientId: String = TEST_CLIENT_ID,
    redirect: String = CLI_REDIRECT,
  ): JsonNode {
    val jwt = jwtService.emitToken(testData.user.id)
    val session = MockHttpSession()

    mvc
      .perform(
        post("/v2/oauth2/session-bootstrap")
          .header("Authorization", "Bearer $jwt")
          .session(session),
      ).andExpect { assertThat(it.response.status).isEqualTo(204) }

    val verifier = randomVerifier()
    val authorizeBuilder =
      UriComponentsBuilder
        .fromPath("/oauth2/authorize")
        .queryParam("response_type", "code")
        .queryParam("client_id", clientId)
        .queryParam("redirect_uri", redirect)
        .queryParam("scope", "translations.view")
        .queryParam("code_challenge", s256Challenge(verifier))
        .queryParam("code_challenge_method", "S256")
        .queryParam("state", "state-123")
    extraAuthorizeParams.forEach { (name, value) -> authorizeBuilder.queryParam(name, value) }

    val authorizeLocation =
      mvc
        .perform(get(authorizeBuilder.build().toUriString()).session(session))
        .andReturn()
        .response
        .getHeader("Location")

    val code = queryParam(authorizeLocation!!, "code")
    assertThat(code).isNotNull()

    val tokenResponse =
      mvc
        .perform(
          post("/oauth2/token")
            .param("grant_type", "authorization_code")
            .param("code", code!!)
            .param("redirect_uri", redirect)
            .param("client_id", clientId)
            .param("code_verifier", verifier)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED),
        ).andReturn()
        .response.contentAsString

    val tree = jacksonObjectMapper().readTree(tokenResponse)
    assertThat(tree.get("access_token")?.asString()).isNotNull()
    return tree
  }

  private fun decodeClaims(token: String): JsonNode {
    val part = token.split(".")[1]
    val padded = part + "=".repeat((4 - part.length % 4) % 4)
    val payload = String(Base64.getUrlDecoder().decode(padded), Charsets.UTF_8)
    return jacksonObjectMapper().readTree(payload)
  }

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
    private const val CLI_REDIRECT = "http://127.0.0.1:9876/callback"
    private const val TEST_CLIENT_ID = "test-flow-client"
    private const val SECOND_REDIRECT = "http://127.0.0.1:9877/callback"
    private const val SECOND_CLIENT_ID = "test-flow-client-2"
    private const val CONSENT_REDIRECT = "http://127.0.0.1:9878/callback"
    private const val CONSENT_CLIENT_ID = "test-flow-client-consent"
  }
}
