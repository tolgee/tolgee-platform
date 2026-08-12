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
import org.springframework.mock.web.MockHttpSession
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
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
import java.util.Base64
import java.util.Date

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

  @Autowired
  private lateinit var oauth2AuthorizationQueryService: OAuth2AuthorizationQueryService

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
    // Tolgee's per-test DB reset wipes the startup-seeded clients, so register self-contained ones here.
    registeredClientRepository.save(flowClient(TEST_CLIENT_ID, CLI_REDIRECT))
    registeredClientRepository.save(flowClient(SECOND_CLIENT_ID, SECOND_REDIRECT))
    registeredClientRepository.save(
      flowClient(
        CONSENT_CLIENT_ID,
        CONSENT_REDIRECT,
        requireConsent = true,
        scopes = listOf("translations.view", "translations.edit"),
      ),
    )
  }

  private fun flowClient(
    clientId: String,
    redirect: String,
    requireConsent: Boolean = false,
    scopes: Collection<String> = listOf("translations.view"),
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
  fun `refresh grant is rejected after the user invalidates their tokens`() {
    val refreshToken =
      authorizationCodeTokenResponse(mapOf(OAuth2Constants.PROJECT_PARAM to testData.project.id.toString()))
        .get("refresh_token")
        .asString()

    // Password change / forced sign-out: the already-issued refresh token must no longer mint access tokens.
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

    // Detecting the invalidated grant on refresh also revokes it — the authorization row is gone. (Checked directly
    // rather than via the connected-apps endpoint, whose JWT would itself be rejected by the future cutoff above.)
    assertThat(oauth2AuthorizationQueryService.findAuthorizedClients(testData.user.id.toString())).isEmpty()
  }

  @Test
  fun `disconnecting the app kills its already-issued access token on the next request`() {
    val accessToken = runAuthorizationCodeFlow()
    mvc
      .perform(get("/v2/projects/${testData.project.id}/translations").header("Authorization", "Bearer $accessToken"))
      .andIsOk

    // Disconnect deletes the authorization row; the resolver's liveness check must reject the still-unexpired token.
    val superJwt = jwtService.emitToken(testData.user.id, isSuper = true)
    mvc
      .perform(delete("/v2/user/connected-apps/$TEST_CLIENT_ID").header("Authorization", "Bearer $superJwt"))
      .andIsOk

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
    // The grant itself is gone (not merely time-cut): connected-apps no longer lists it.
    assertThat(connectedApps(jwtService.emitToken(testData.user.id))).doesNotContain("\"$TEST_CLIENT_ID\"")
  }

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

    // Approve only translations.view — translations.edit is deselected even though the user holds it live. The issued
    // token must reflect the consent, not the full request or the user's live scopes.
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
    // Hint an unrelated project on the authorize request, then consent-select the real one.
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
    // The client hints a concrete project, but the user widens to "all projects" (frontend omits projectId).
    val pending = startPendingConsent(jwt, hintProjectId = testData.project.id)
    selectProject(jwt, pending.state, null).andExpect { assertThat(it.response.status).isEqualTo(204) }

    assertThat(decodeClaims(completeConsent(pending)).get(OAuth2Constants.PROJECTS_CLAIM).asString())
      .isEqualTo(OAuth2Constants.ALL_PROJECTS)
  }

  @Test
  fun `connected-apps surfaces the granted scopes and last-authorized time`() {
    val jwt = jwtService.emitToken(testData.user.id)
    // Drive the consent client so a real consent row (with granted scopes) is recorded for the app.
    val pending = startPendingConsent(jwt)
    selectProject(jwt, pending.state, testData.project.id).andExpect { assertThat(it.response.status).isEqualTo(204) }
    completeConsent(pending)

    val entry =
      jacksonObjectMapper()
        .readTree(connectedApps(jwt))
        .first { it.get("clientId").asString() == CONSENT_CLIENT_ID }
    assertThat(entry.get("scopes").toString()).contains("translations.view")
    assertThat(entry.get("lastAuthorizedAt").isNull).isFalse()
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

    val initialClaims = decodeClaims(first.get("access_token").asString())
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

    // A hint pointing at a project the user cannot access must not leak the project's name, but the raw id is still
    // reported (as requestedProjectId) so the consent screen can say "you can't edit the requested project".
    val inaccessible = startPendingConsent(jwt, hintProjectId = otherProjectId)
    val inaccessibleInfo = consentInfo(jwt, state = inaccessible.state)
    assertThat(inaccessibleInfo.get("project").isNull).isTrue()
    assertThat(inaccessibleInfo.get("requestedProjectId").asLong()).isEqualTo(otherProjectId)

    // An accessible hint is surfaced (id + name) so the SPA can pre-select it.
    val accessible = startPendingConsent(jwt, hintProjectId = testData.project.id)
    val accessibleInfo = consentInfo(jwt, state = accessible.state)
    val hinted = accessibleInfo.get("project")
    assertThat(hinted.get("id").asLong()).isEqualTo(testData.project.id)
    assertThat(hinted.get("name").asText()).isEqualTo(testData.project.name)
    assertThat(accessibleInfo.get("requestedProjectId").asLong()).isEqualTo(testData.project.id)

    // A stale/nonexistent hint (project deleted mid-flow) resolves to a null project but still reports the raw id, so
    // the SPA's inaccessible-warning fires — distinct from the no-hint (null id) case.
    val nonexistent = startPendingConsent(jwt, hintProjectId = INACCESSIBLE_PROJECT_ID)
    val nonexistentInfo = consentInfo(jwt, state = nonexistent.state)
    assertThat(nonexistentInfo.get("project").isNull).isTrue()
    assertThat(nonexistentInfo.get("requestedProjectId").asLong()).isEqualTo(INACCESSIBLE_PROJECT_ID)
  }

  @Test
  fun `select-project rejects binding another user's pending authorization`() {
    val pending = startPendingConsent(jwtService.emitToken(testData.user.id))
    // otherUser tries to retarget user A's pending authorization by its state value.
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
    // The headline community-contributor case: a non-member of a public project narrows the token to it via the hint.
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

    // and the minted token works on a community-permitted read endpoint, bounded by the contributor's live permissions.
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

    // The bootstrap redirect must preserve the original authorize URL (with its query) so the flow can resume after
    // the session is established.
    val continueUrl = URLDecoder.decode(queryParam(location, "continue")!!, StandardCharsets.UTF_8)
    assertThat(continueUrl).contains("/oauth2/authorize")
    assertThat(continueUrl).contains("client_id=$TEST_CLIENT_ID")
    assertThat(continueUrl).contains("state=state-123")
    assertThat(continueUrl).contains("code_challenge=")
  }

  @Test
  fun `connected-apps lists authorized apps and revoking one leaves the others intact`() {
    runAuthorizationCodeFlow(clientId = TEST_CLIENT_ID, redirect = CLI_REDIRECT)
    runAuthorizationCodeFlow(clientId = SECOND_CLIENT_ID, redirect = SECOND_REDIRECT)
    val jwt = jwtService.emitToken(testData.user.id)

    assertThat(connectedApps(jwt)).contains("\"$TEST_CLIENT_ID\"").contains("\"$SECOND_CLIENT_ID\"")

    // Revoke is a credential-management mutation: a plain token is rejected; only a super-authenticated one works.
    val plainDeleteStatus =
      mvc
        .perform(delete("/v2/user/connected-apps/$TEST_CLIENT_ID").header("Authorization", "Bearer $jwt"))
        .andReturn()
        .response.status
    assertThat(plainDeleteStatus).isGreaterThanOrEqualTo(400)
    assertThat(connectedApps(jwt)).contains("\"$TEST_CLIENT_ID\"")

    val superJwt = jwtService.emitToken(testData.user.id, isSuper = true)
    mvc
      .perform(delete("/v2/user/connected-apps/$TEST_CLIENT_ID").header("Authorization", "Bearer $superJwt"))
      .andIsOk

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
    private const val INACCESSIBLE_PROJECT_ID = 9_999_999L
  }
}
