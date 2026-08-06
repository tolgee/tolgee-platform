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
    // Tolgee's per-test DB reset wipes the startup-seeded clients, so register a self-contained one here.
    registeredClientRepository.save(
      RegisteredClient
        .withId(TEST_CLIENT_ID)
        .clientId(TEST_CLIENT_ID)
        .clientName("Test Flow Client")
        .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
        .redirectUri(CLI_REDIRECT)
        .scope("translations.view")
        .clientSettings(
          ClientSettings
            .builder()
            .requireProofKey(true)
            .requireAuthorizationConsent(false)
            .build(),
        ).tokenSettings(TokenSettings.builder().accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED).build())
        .build(),
    )
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `authorization code + PKCE flow issues an access token that works on the REST API`() {
    val accessToken = runAuthorizationCodeFlow()

    mvc
      .perform(
        get("/v2/projects/${testData.project.id}/translations")
          .header("Authorization", "Bearer $accessToken"),
      ).andIsOk
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

  private fun runAuthorizationCodeFlow(extraAuthorizeParams: Map<String, String> = emptyMap()): String {
    return authorizationCodeTokenResponse(extraAuthorizeParams).get("access_token").asString()
  }

  private fun authorizationCodeTokenResponse(extraAuthorizeParams: Map<String, String> = emptyMap()): JsonNode {
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
        .queryParam("client_id", TEST_CLIENT_ID)
        .queryParam("redirect_uri", CLI_REDIRECT)
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
            .param("redirect_uri", CLI_REDIRECT)
            .param("client_id", TEST_CLIENT_ID)
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
  }
}
