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
import io.tolgee.fixtures.andIsUnauthorized
import io.tolgee.model.UserAccount
import io.tolgee.security.authentication.JwtService
import io.tolgee.testing.AbstractControllerTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.mock.web.MockHttpSession
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.web.util.UriComponentsBuilder
import tools.jackson.databind.JsonNode
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import java.util.Date

/**
 * Drives the whole browser flow through MockMvc against a test client registered below (consent disabled, so no page
 * is needed): session bootstrap -> /oauth2/authorize -> code -> /oauth2/token -> use the access token on the REST API.
 * This is the end-to-end proof that a real authorization-code token works — including that `sub` is the numeric user id.
 */
class OAuth2AuthorizationCodeFlowTest : AbstractControllerTest() {
  @Autowired
  private lateinit var jwtService: JwtService

  @Autowired
  private lateinit var registeredClientRepository: RegisteredClientRepository

  @Autowired
  private lateinit var oauth2AuthorizationQueryService: OAuth2AuthorizationQueryService

  @Autowired
  private lateinit var authorizationServerSettings: AuthorizationServerSettings

  @Autowired
  private lateinit var jdbcTemplate: JdbcTemplate

  @Autowired
  private lateinit var authorizationService: OAuth2AuthorizationService

  private lateinit var testData: BaseTestData
  private lateinit var otherUser: UserAccount
  private var otherProjectId: Long = 0
  private var publicProjectId: Long = 0

  @BeforeEach
  fun setup() {
    testData = BaseTestData()
    // A second user, plus a private project only they can access, for the cross-user / permission guards.
    val otherUserBuilder = testData.root.addUserAccount { username = "oauth_other_user" }
    otherUser = otherUserBuilder.self
    val otherProjectBuilder =
      testData.root.addProject {
        name = "foreign_project"
        organizationOwner = otherUserBuilder.defaultOrganizationBuilder.self
      }
    // A public project testData.user is NOT a member of — reachable only via the community floor.
    val publicProjectBuilder =
      testData.root
        .addProject {
          name = "public_project"
          organizationOwner = otherUserBuilder.defaultOrganizationBuilder.self
          public = true
        }.build buildPublic@{
          addLanguage {
            name = "English"
            tag = "en"
            originalName = "English"
            this@buildPublic.self.baseLanguage = this
          }
        }
    testDataService.saveTestData(testData.root)
    otherProjectId = otherProjectBuilder.self.id
    publicProjectId = publicProjectBuilder.self.id
    // Self-contained clients, so the test does not depend on which clients the running configuration seeds.
    registeredClientRepository.save(flowClient(TEST_CLIENT_ID, CLI_REDIRECT))
    registeredClientRepository.save(flowClient(SECOND_CLIENT_ID, SECOND_REDIRECT))
    registeredClientRepository.save(
      flowClient(
        CONSENT_CLIENT_ID,
        CONSENT_REDIRECT,
        requireConsent = true,
        scopes = listOf("translations.view", "translations.edit"),
        requiredScopes = listOf("translations.view"),
      ),
    )
  }

  private fun flowClient(
    clientId: String,
    redirect: String,
    requireConsent: Boolean = false,
    scopes: Collection<String> = listOf("translations.view"),
    requiredScopes: List<String> = emptyList(),
  ): RegisteredClient =
    RegisteredClient
      .withId(clientId)
      .clientId(clientId)
      .clientName("Test Flow Client $clientId")
      .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
      .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
      .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
      .redirectUri(redirect)
      .scopes { it.addAll(scopes) }
      .clientSettings(
        ClientSettings
          .builder()
          .requireProofKey(true)
          .requireAuthorizationConsent(requireConsent)
          .apply {
            if (requiredScopes.isNotEmpty()) {
              setting(OAuth2Constants.REQUIRED_SCOPES_SETTING, requiredScopes.joinToString(" "))
            }
          }.build(),
      ).tokenSettings(
        TokenSettings
          .builder()
          .accessTokenFormat(OAuth2TokenFormat.REFERENCE)
          .reuseRefreshTokens(false)
          .build(),
      ).build()

  fun cleanup() {
    // Consent is remembered per client+user; without clearing it a later test whose user id repeats would have SAS skip
    // the consent screen (making startPendingConsent capture the client state, not the pending-authorization state).
    jdbcTemplate.update("DELETE FROM oauth2_authorization_consent")
    jdbcTemplate.update("DELETE FROM oauth2_authorization")
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

    val firstAccessToken = firstResponse.get("access_token").asString()
    // Read before refreshing: rotation replaces the authorization's single access token, so the superseded one no
    // longer resolves.
    val initialClaims = decodeClaims(firstAccessToken)

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

    val refreshedClaims = decodeClaims(refreshedAccessToken!!)
    assertThat(refreshedClaims.get(OAuth2Constants.PROJECTS_CLAIM).isArray).isTrue()
    assertThat(refreshedClaims.get(OAuth2Constants.PROJECTS_CLAIM)[0].asLong()).isEqualTo(testData.project.id)
    assertThat(refreshedClaims.get("aud")).isEqualTo(initialClaims.get("aud"))
    assertThat(refreshedClaims.get("scope")).isEqualTo(initialClaims.get("scope"))

    // Rotation supersedes the previous access token as well as the refresh token: opaque tokens are read from the
    // grant on every request, so the old value stops working at once instead of living out its TTL.
    mvc
      .perform(
        get("/v2/projects/${testData.project.id}/translations")
          .header("Authorization", "Bearer $firstAccessToken"),
      ).andIsUnauthorized

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
  fun `refresh grant is rejected after the user invalidates their tokens`() {
    val refreshToken =
      authorizationCodeTokenResponse(mapOf(OAuth2Constants.PROJECT_PARAM to testData.project.id.toString()))
        .get("refresh_token")
        .asString()

    val user = userAccountService.get(testData.user.id)
    user.tokensValidNotBefore = Date(System.currentTimeMillis() + 3_600_000)
    userAccountService.save(user)

    val status =
      mvc
        .perform(
          post("/oauth2/token")
            .param("grant_type", "refresh_token")
            .param("refresh_token", refreshToken)
            .param("client_id", TEST_CLIENT_ID)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED),
        ).andReturn()
        .response.status
    assertThat(status).isEqualTo(400)

    // Detecting the invalidated grant on refresh also revokes it — the authorization row is gone.
    assertThat(authorizationRowsForUser()).isZero()
  }

  @Test
  fun `changing the password revokes the user's OAuth grants`() {
    authorizationCodeTokenResponse(mapOf(OAuth2Constants.PROJECT_PARAM to testData.project.id.toString()))
    assertThat(authorizationRowsForUser()).isNotZero()

    // A password change must delete the grants, not only bump tokensValidNotBefore (which the refresh gate reads from a
    // per-node-cached DTO that lags without Redis), so a stolen refresh token can't keep minting on a stale replica.
    userAccountService.setUserPassword(userAccountService.get(testData.user.id), "new-password-123")

    assertThat(authorizationRowsForUser()).isZero()
  }

  @Test
  fun `refresh grant is rejected and revoked after the subject user is deleted`() {
    val refreshToken =
      authorizationCodeTokenResponse(mapOf(OAuth2Constants.PROJECT_PARAM to testData.project.id.toString()))
        .get("refresh_token")
        .asString()

    // Deleting the account does not revoke its grants, so the refresh grant must reject the now-missing user itself
    // and revoke the dead grant, not mint a fresh access token for a user who no longer exists.
    userAccountService.delete(testData.user.id)

    val status =
      mvc
        .perform(
          post("/oauth2/token")
            .param("grant_type", "refresh_token")
            .param("refresh_token", refreshToken)
            .param("client_id", TEST_CLIENT_ID)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED),
        ).andReturn()
        .response.status
    assertThat(status).isEqualTo(400)
    assertThat(authorizationRowsForUser()).isZero()
  }

  @Test
  fun `revoking the grant kills its already-issued access token on the next request`() {
    val accessToken = runAuthorizationCodeFlow()
    mvc
      .perform(get("/v2/projects/${testData.project.id}/translations").header("Authorization", "Bearer $accessToken"))
      .andIsOk

    oauth2AuthorizationQueryService.revokeAllForPrincipal(testData.user.id.toString())

    mvc
      .perform(get("/v2/projects/${testData.project.id}/translations").header("Authorization", "Bearer $accessToken"))
      .andIsUnauthorized
  }

  @Test
  fun `invalidating all tokens kills already-issued OAuth access tokens and grants`() {
    val accessToken = runAuthorizationCodeFlow()
    mvc
      .perform(get("/v2/projects/${testData.project.id}/translations").header("Authorization", "Bearer $accessToken"))
      .andIsOk

    userAccountService.invalidateTokens(userAccountService.get(testData.user.id))

    mvc
      .perform(get("/v2/projects/${testData.project.id}/translations").header("Authorization", "Bearer $accessToken"))
      .andIsUnauthorized
    assertThat(authorizationRowsForUser()).isZero()
  }

  private fun authorizationRowsForUser(): Int =
    jdbcTemplate.queryForObject(
      "SELECT COUNT(*) FROM oauth2_authorization WHERE principal_name = ?",
      Int::class.java,
      testData.user.id.toString(),
    ) ?: 0

  @Test
  fun `a scope deselected at consent is absent from the issued token`() {
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
        .queryParam("scope", "translations.view translations.edit")
        .queryParam("code_challenge", s256Challenge(verifier))
        .queryParam("code_challenge_method", "S256")
        .queryParam("state", "client-state")
        .build()
        .toUriString()
    val consentLocation =
      mvc
        .perform(get(authorizeUrl).session(session))
        .andReturn()
        .response
        .getHeader("Location")
    val state = URLDecoder.decode(queryParam(consentLocation!!, "state")!!, StandardCharsets.UTF_8)
    selectProject(jwt, state, testData.project.id).andExpect { assertThat(it.response.status).isEqualTo(204) }

    val codeLocation =
      mvc
        .perform(
          post("/oauth2/authorize")
            .param("client_id", CONSENT_CLIENT_ID)
            .param("state", state)
            .param("scope", "translations.view")
            .session(session),
        ).andReturn()
        .response
        .getHeader("Location")
    val code = queryParam(codeLocation!!, "code")

    val tokenResponse =
      mvc
        .perform(
          post("/oauth2/token")
            .param("grant_type", "authorization_code")
            .param("code", code!!)
            .param("redirect_uri", CONSENT_REDIRECT)
            .param("client_id", CONSENT_CLIENT_ID)
            .param("code_verifier", verifier)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED),
        ).andReturn()
        .response.contentAsString
    val accessToken = jacksonObjectMapper().readTree(tokenResponse).get("access_token").asString()

    val scopeClaim = decodeClaims(accessToken).get("scope").toString()
    assertThat(scopeClaim).contains("translations.view").doesNotContain("translations.edit")
  }

  @Test
  fun `the code-delivery redirect echoes the client's own state and the RFC 9207 iss`() {
    val jwt = jwtService.emitToken(testData.user.id)
    val pending = startPendingConsent(jwt, hintProjectId = testData.project.id)
    val codeLocation =
      mvc
        .perform(
          post("/oauth2/authorize")
            .param("client_id", CONSENT_CLIENT_ID)
            .param("state", pending.state)
            .param("scope", "translations.view")
            .session(pending.session),
        ).andReturn()
        .response
        .getHeader("Location")
    // The client's original `state` must round-trip for CSRF defense — NOT SAS's internal pending-authorization state.
    val echoedState = URLDecoder.decode(queryParam(codeLocation!!, "state")!!, StandardCharsets.UTF_8)
    assertThat(echoedState).isEqualTo("client-state").isNotEqualTo(pending.state)
    // iss is present and equals the issuer SAS was configured with (RFC 9207 AS mix-up defense). Compared against the
    // AuthorizationServerSettings bean, not audienceResolver.serverBaseUrl (a live property another test can mutate).
    assertThat(URLDecoder.decode(queryParam(codeLocation, "iss")!!, StandardCharsets.UTF_8))
      .isEqualTo(authorizationServerSettings.issuer)
  }

  @Test
  fun `a remembered-consent reconnect without a project selection fails closed instead of widening to all projects`() {
    val jwt = jwtService.emitToken(testData.user.id)
    // First flow: consent the client and bind it to a single project. This records a remembered consent for client+user.
    val pending = startPendingConsent(jwt)
    selectProject(jwt, pending.state, testData.project.id).andExpect { assertThat(it.response.status).isEqualTo(204) }
    completeConsent(pending)

    // Second authorize, same client+scope: SAS skips the consent screen (consent remembered) and issues a code directly,
    // so select-project never runs. Without a project hint the token must not silently widen from the consented project.
    val session = MockHttpSession()
    mvc
      .perform(post("/v2/oauth2/session-bootstrap").header("Authorization", "Bearer $jwt").session(session))
      .andExpect { assertThat(it.response.status).isEqualTo(204) }
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
        .queryParam("state", "client-state-2")
        .build()
        .toUriString()
    val codeLocation =
      mvc
        .perform(get(authorizeUrl).session(session))
        .andReturn()
        .response
        .getHeader("Location")
    val code = queryParam(codeLocation!!, "code")
    assertThat(code).isNotNull() // consent skipped -> code issued straight to the client redirect

    val status =
      mvc
        .perform(
          post("/oauth2/token")
            .param("grant_type", "authorization_code")
            .param("code", code!!)
            .param("redirect_uri", CONSENT_REDIRECT)
            .param("client_id", CONSENT_CLIENT_ID)
            .param("code_verifier", verifier)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED),
        ).andReturn()
        .response.status
    assertThat(status).isEqualTo(400)
  }

  @Test
  fun `rejects an authorize request that omits the PKCE code_challenge for a public client`() {
    val jwt = jwtService.emitToken(testData.user.id)
    val session = MockHttpSession()
    mvc
      .perform(post("/v2/oauth2/session-bootstrap").header("Authorization", "Bearer $jwt").session(session))
      .andExpect { assertThat(it.response.status).isEqualTo(204) }

    // Deliberately omit code_challenge. Every public client sets requireProofKey, so SAS must refuse with an error
    // redirect and never issue a code — pins PKCE against an accidental downgrade that would re-open code interception.
    val authorizeUrl =
      UriComponentsBuilder
        .fromPath("/oauth2/authorize")
        .queryParam("response_type", "code")
        .queryParam("client_id", TEST_CLIENT_ID)
        .queryParam("redirect_uri", CLI_REDIRECT)
        .queryParam("scope", "translations.view")
        .queryParam("state", "no-pkce")
        .build()
        .toUriString()
    val location =
      mvc
        .perform(get(authorizeUrl).session(session))
        .andReturn()
        .response
        .getHeader("Location")
    assertThat(location).contains("error=invalid_request")
    assertThat(queryParam(location!!, "code")).isNull()
  }

  @Test
  fun `rejects the code exchange when the PKCE verifier is wrong`() {
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
  fun `a single project chosen on the consent screen binds the token to it`() {
    val accessToken = consentFlowAccessToken(testData.project.id)

    val projectClaim = decodeClaims(accessToken).get(OAuth2Constants.PROJECTS_CLAIM)
    assertThat(projectClaim.isArray).isTrue()
    assertThat(projectClaim.size()).isEqualTo(1)
    assertThat(projectClaim[0].asLong()).isEqualTo(testData.project.id)
  }

  @Test
  fun `choosing all projects on the consent screen keeps the token unscoped`() {
    val accessToken = consentFlowAccessToken(projectId = null)

    assertThat(decodeClaims(accessToken).get(OAuth2Constants.PROJECTS_CLAIM).asString())
      .isEqualTo(OAuth2Constants.ALL_PROJECTS)
  }

  @Test
  fun `a consent-screen project choice overrides the client's authorize hint`() {
    val jwt = jwtService.emitToken(testData.user.id)
    val pending = startPendingConsent(jwt, hintProjectId = INACCESSIBLE_PROJECT_ID)
    selectProject(jwt, pending.state, testData.project.id)
      .andExpect { assertThat(it.response.status).isEqualTo(204) }

    val projectClaim = decodeClaims(completeConsent(pending)).get(OAuth2Constants.PROJECTS_CLAIM)
    assertThat(projectClaim.isArray).isTrue()
    assertThat(projectClaim[0].asLong()).isEqualTo(testData.project.id)
  }

  @Test
  fun `choosing all projects on the consent screen overrides a client's authorize hint`() {
    val jwt = jwtService.emitToken(testData.user.id)
    val pending = startPendingConsent(jwt, hintProjectId = testData.project.id)
    selectProject(jwt, pending.state, null).andExpect { assertThat(it.response.status).isEqualTo(204) }

    assertThat(decodeClaims(completeConsent(pending)).get(OAuth2Constants.PROJECTS_CLAIM).asString())
      .isEqualTo(OAuth2Constants.ALL_PROJECTS)
  }

  @Test
  fun `a consent-selected token keeps its project binding after a refresh`() {
    // The consent selection is stored as an OAuth2Authorization attribute (not the authorize-time hint), so this proves
    // that attribute survives the refresh grant — a refresh must not silently widen tg.prj back to ALL_PROJECTS.
    val jwt = jwtService.emitToken(testData.user.id)
    val pending = startPendingConsent(jwt) // no authorize hint — binding comes only from the consent attribute
    selectProject(jwt, pending.state, testData.project.id).andExpect { assertThat(it.response.status).isEqualTo(204) }
    val first = completeConsentTokenResponse(pending)
    val refreshToken = first.get("refresh_token")?.asString()
    assertThat(refreshToken).isNotNull()

    // Read before refreshing: rotation supersedes the previous access token, which then no longer resolves.
    val initialClaims = decodeClaims(first.get("access_token").asString())

    val refreshed =
      jacksonObjectMapper().readTree(
        mvc
          .perform(
            post("/oauth2/token")
              .param("grant_type", "refresh_token")
              .param("refresh_token", refreshToken!!)
              .param("client_id", CONSENT_CLIENT_ID)
              .contentType(MediaType.APPLICATION_FORM_URLENCODED),
          ).andReturn()
          .response.contentAsString,
      )

    val refreshedClaims = decodeClaims(refreshed.get("access_token").asString())
    assertThat(refreshedClaims.get(OAuth2Constants.PROJECTS_CLAIM).isArray).isTrue()
    assertThat(refreshedClaims.get(OAuth2Constants.PROJECTS_CLAIM)[0].asLong()).isEqualTo(testData.project.id)
    assertThat(refreshedClaims.get("aud")).isEqualTo(initialClaims.get("aud"))
    assertThat(refreshedClaims.get("scope")).isEqualTo(initialClaims.get("scope"))
  }

  @Test
  fun `consent-info surfaces an accessible hinted project and hides an inaccessible one`() {
    val jwt = jwtService.emitToken(testData.user.id)

    val noHint = consentInfo(jwt, state = null)
    assertThat(noHint.get("project").isNull).isTrue()
    assertThat(noHint.get("requestedProjectId").isNull).isTrue()
    assertThat(noHint.get("requiredScopes").toString()).contains("translations.view")

    val inaccessible = startPendingConsent(jwt, hintProjectId = otherProjectId)
    val inaccessibleInfo = consentInfo(jwt, state = inaccessible.state)
    assertThat(inaccessibleInfo.get("project").isNull).isTrue()
    assertThat(inaccessibleInfo.get("requestedProjectId").asLong()).isEqualTo(otherProjectId)

    val accessible = startPendingConsent(jwt, hintProjectId = testData.project.id)
    val accessibleInfo = consentInfo(jwt, state = accessible.state)
    val hinted = accessibleInfo.get("project")
    assertThat(hinted.get("id").asLong()).isEqualTo(testData.project.id)
    assertThat(hinted.get("name").asText()).isEqualTo(testData.project.name)
    assertThat(accessibleInfo.get("requestedProjectId").asLong()).isEqualTo(testData.project.id)

    val nonexistent = startPendingConsent(jwt, hintProjectId = INACCESSIBLE_PROJECT_ID)
    val nonexistentInfo = consentInfo(jwt, state = nonexistent.state)
    assertThat(nonexistentInfo.get("project").isNull).isTrue()
    assertThat(nonexistentInfo.get("requestedProjectId").asLong()).isEqualTo(INACCESSIBLE_PROJECT_ID)
  }

  @Test
  fun `consent-info marks only the client's required scopes as required`() {
    val jwt = jwtService.emitToken(testData.user.id)
    val info =
      jacksonObjectMapper().readTree(
        mvc
          .perform(
            get("/v2/oauth2/consent-info")
              .header("Authorization", "Bearer $jwt")
              .param("clientId", CONSENT_CLIENT_ID)
              .param("scope", "translations.view translations.edit"),
          ).andIsOk
          .andReturn()
          .response.contentAsString,
      )
    assertThat(info.get("scopes").toString())
      .contains("translations.view")
      .contains("translations.edit")
    assertThat(info.get("requiredScopes").toString())
      .contains("translations.view")
      .doesNotContain("translations.edit")
  }

  @Test
  fun `select-project rejects binding another user's pending authorization`() {
    val pending = startPendingConsent(jwtService.emitToken(testData.user.id))
    selectProject(jwtService.emitToken(otherUser.id), pending.state, testData.project.id)
      .andExpect { assertThat(it.response.status).isEqualTo(404) }
  }

  @Test
  fun `select-project rejects a project the user has no access to`() {
    val jwt = jwtService.emitToken(testData.user.id)
    val pending = startPendingConsent(jwt)
    // an existing project the user isn't a member of
    selectProject(jwt, pending.state, otherProjectId)
      .andExpect { assertThat(it.response.status).isEqualTo(403) }
    // a nonexistent project id (e.g. deleted mid-flow) is denied with 403, not a leaked 404
    selectProject(jwt, pending.state, INACCESSIBLE_PROJECT_ID)
      .andExpect { assertThat(it.response.status).isEqualTo(403) }
  }

  @Test
  fun `select-project returns 404 for an unknown state`() {
    selectProject(jwtService.emitToken(testData.user.id), "no-such-state", testData.project.id)
      .andExpect { assertThat(it.response.status).isEqualTo(404) }
  }

  @Test
  fun `a public project the user is not a member of is selectable via the community floor`() {
    val jwt = jwtService.emitToken(testData.user.id)
    val pending = startPendingConsent(jwt, hintProjectId = publicProjectId)

    // findAllPermitted excludes it (not a member), but the community floor resolves the hint, so it is surfaced.
    val hinted = consentInfo(jwt, state = pending.state).get("project")
    assertThat(hinted.get("id").asLong()).isEqualTo(publicProjectId)

    // select-project takes the community-floor allow-path (204, not the private-foreign-project 403), binding the token.
    selectProject(jwt, pending.state, publicProjectId).andExpect { assertThat(it.response.status).isEqualTo(204) }

    val token = completeConsent(pending)
    val claim = decodeClaims(token).get(OAuth2Constants.PROJECTS_CLAIM)
    assertThat(claim.isArray).isTrue()
    assertThat(claim[0].asLong()).isEqualTo(publicProjectId)

    mvc
      .perform(get("/v2/projects/$publicProjectId/translations").header("Authorization", "Bearer $token"))
      .andIsOk
  }

  private data class PendingConsent(
    val session: MockHttpSession,
    val state: String,
    val verifier: String,
  )

  /** Bootstraps a session and reaches the consent page for the consent-required client, leaving a pending authorization. */
  private fun startPendingConsent(
    jwt: String,
    hintProjectId: Long? = null,
  ): PendingConsent {
    val session = MockHttpSession()
    mvc
      .perform(post("/v2/oauth2/session-bootstrap").header("Authorization", "Bearer $jwt").session(session))
      .andExpect { assertThat(it.response.status).isEqualTo(204) }

    val verifier = randomVerifier()
    val authorizeBuilder =
      UriComponentsBuilder
        .fromPath("/oauth2/authorize")
        .queryParam("response_type", "code")
        .queryParam("client_id", CONSENT_CLIENT_ID)
        .queryParam("redirect_uri", CONSENT_REDIRECT)
        .queryParam("scope", "translations.view")
        .queryParam("code_challenge", s256Challenge(verifier))
        .queryParam("code_challenge_method", "S256")
        .queryParam("state", "client-state")
    hintProjectId?.let { authorizeBuilder.queryParam(OAuth2Constants.PROJECT_PARAM, it.toString()) }
    // The consent page carries SAS's own state (which keys the pending authorization); url-decoded, that is what the
    // SPA and select-project use, not the client's original state.
    val consentPageLocation =
      mvc
        .perform(
          get(authorizeBuilder.build().toUriString()).session(session),
        ).andReturn()
        .response
        .getHeader("Location")
    val state = URLDecoder.decode(queryParam(consentPageLocation!!, "state")!!, StandardCharsets.UTF_8)
    return PendingConsent(session, state, verifier)
  }

  private fun selectProject(
    jwt: String,
    state: String,
    projectId: Long?,
  ): ResultActions {
    val request = post("/v2/oauth2/select-project").header("Authorization", "Bearer $jwt").param("state", state)
    projectId?.let { request.param("projectId", it.toString()) }
    return mvc.perform(request)
  }

  private fun completeConsent(pending: PendingConsent): String =
    completeConsentTokenResponse(pending).get("access_token").asString()

  /** Submits the SAS consent form for a pending authorization and exchanges the resulting code for the token response. */
  private fun completeConsentTokenResponse(pending: PendingConsent): JsonNode {
    val consentLocation =
      mvc
        .perform(
          post("/oauth2/authorize")
            .param("client_id", CONSENT_CLIENT_ID)
            .param("state", pending.state)
            .param("scope", "translations.view")
            .session(pending.session),
        ).andReturn()
        .response
        .getHeader("Location")
    val code = queryParam(consentLocation!!, "code")
    assertThat(code).isNotNull()

    val tokenResponse =
      mvc
        .perform(
          post("/oauth2/token")
            .param("grant_type", "authorization_code")
            .param("code", code!!)
            .param("redirect_uri", CONSENT_REDIRECT)
            .param("client_id", CONSENT_CLIENT_ID)
            .param("code_verifier", pending.verifier)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED),
        ).andReturn()
        .response.contentAsString
    return jacksonObjectMapper().readTree(tokenResponse)
  }

  private fun consentFlowAccessToken(projectId: Long?): String {
    val jwt = jwtService.emitToken(testData.user.id)
    val pending = startPendingConsent(jwt)
    selectProject(jwt, pending.state, projectId).andExpect { assertThat(it.response.status).isEqualTo(204) }
    return completeConsent(pending)
  }

  private fun consentInfo(
    jwt: String,
    state: String?,
  ): JsonNode {
    val request =
      get("/v2/oauth2/consent-info")
        .header("Authorization", "Bearer $jwt")
        .param("clientId", CONSENT_CLIENT_ID)
        .param("scope", "translations.view")
    state?.let { request.param("state", it) }
    return jacksonObjectMapper().readTree(
      mvc
        .perform(request)
        .andIsOk
        .andReturn()
        .response.contentAsString,
    )
  }

  @Test
  fun `invalidates the http session once the authorization code is issued`() {
    // The session carries only the authorize round trip; killing it at code issuance stops a later connect from
    // silently reusing a stale principal (e.g. after the webapp user switched accounts). A fresh bootstrap must run.
    val jwt = jwtService.emitToken(testData.user.id)
    val session = MockHttpSession()
    mvc
      .perform(post("/v2/oauth2/session-bootstrap").header("Authorization", "Bearer $jwt").session(session))
      .andExpect { assertThat(it.response.status).isEqualTo(204) }

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
        .perform(get(authorizeUrl).session(session))
        .andReturn()
        .response
        .getHeader("Location")

    assertThat(queryParam(location!!, "code")).isNotNull()
    assertThat(session.isInvalid).isTrue()
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

    val continueUrl = URLDecoder.decode(queryParam(location, "continue")!!, StandardCharsets.UTF_8)
    assertThat(continueUrl).contains("/oauth2/authorize")
    assertThat(continueUrl).contains("client_id=$TEST_CLIENT_ID")
    assertThat(continueUrl).contains("state=state-123")
    assertThat(continueUrl).contains("code_challenge=")
  }

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

  // Access tokens are opaque, so the claims live on the stored authorization rather than inside the token value.
  // Rotation replaces the row's single access token, so a superseded token no longer resolves — read claims before
  // refreshing.
  private fun decodeClaims(token: String): JsonNode {
    val authorization =
      authorizationService.findByToken(token, OAuth2TokenType.ACCESS_TOKEN)
        ?: throw AssertionError("no authorization stored for the access token")
    val claims = authorization.accessToken?.claims ?: throw AssertionError("access token carries no claims")
    // Instants have no natural JSON mapping without the time module; the timestamps are not what any assertion reads.
    val normalized = claims.mapValues { (_, value) -> if (value is Instant) value.epochSecond else value }
    return jacksonObjectMapper().valueToTree(normalized)
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
    private const val INACCESSIBLE_PROJECT_ID = 9_999_999L
  }
}
