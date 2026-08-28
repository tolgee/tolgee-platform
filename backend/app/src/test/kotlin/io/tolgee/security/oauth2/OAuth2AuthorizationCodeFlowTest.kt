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

import io.tolgee.component.KeyGenerator
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andIsUnauthorized
import io.tolgee.model.UserAccount
import io.tolgee.model.oauth2.OAuth2Authorization
import io.tolgee.repository.oauth2.OAuth2AuthorizationRepository
import io.tolgee.security.authentication.JwtService
import io.tolgee.testing.AbstractControllerTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpSession
import org.springframework.test.web.servlet.ResultActions
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
 * Drives the whole browser flow through MockMvc against the clients configured in the test `application.yaml`:
 * session bootstrap → `/oauth2/authorize` → consent page → project selection → consent form → code → `/oauth2/token`,
 * and what the issued tokens then do on the REST API. Protocol edge cases live in [OAuth2ProtocolConformanceTest];
 * this file covers what is Tolgee's own: project binding, consent-screen data, and revocation.
 */
class OAuth2AuthorizationCodeFlowTest : AbstractControllerTest() {
  @Autowired
  private lateinit var jwtService: JwtService

  @Autowired
  private lateinit var oauth2AuthorizationService: OAuth2AuthorizationService

  @Autowired
  private lateinit var issuerResolver: OAuth2IssuerResolver

  @Autowired
  private lateinit var repository: OAuth2AuthorizationRepository

  @Autowired
  private lateinit var keyGenerator: KeyGenerator

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
  }

  @AfterEach
  fun cleanup() {
    oauth2AuthorizationService.revokeAllForUser(testData.user.id)
    oauth2AuthorizationService.revokeAllForUser(otherUser.id)
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `authorization code + PKCE flow issues an access token that works on the REST API`() {
    val accessToken = runAuthorizationCodeFlow()

    assertThat(stored(accessToken).userAccount.id).isEqualTo(testData.user.id)

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
    val initial = stored(firstAccessToken)
    val initialScopes = initial.grantedScopeValues
    val initialClientId = initial.clientId

    val refreshResponse =
      mvc
        .perform(
          post("/oauth2/token")
            .param("grant_type", "refresh_token")
            .param("refresh_token", refreshToken!!)
            .param("client_id", FLOW_CLIENT_ID)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED),
        ).andReturn()
        .response.contentAsString

    val refreshed = jacksonObjectMapper().readTree(refreshResponse)
    val refreshedAccessToken = refreshed.get("access_token")?.asString()
    assertThat(refreshedAccessToken).isNotNull()
    assertThat(refreshed.get("refresh_token")?.asString()).isNotNull().isNotEqualTo(refreshToken)

    val refreshedStored = stored(refreshedAccessToken!!)
    assertThat(refreshedStored.boundProjectIds()).containsExactly(testData.project.id)
    assertThat(refreshedStored.clientId).isEqualTo(initialClientId)
    assertThat(refreshedStored.grantedScopeValues).isEqualTo(initialScopes)

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
            .param("client_id", FLOW_CLIENT_ID)
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
            .param("client_id", FLOW_CLIENT_ID)
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
    val userId = testData.user.id

    userAccountService.delete(userId)

    val status =
      mvc
        .perform(
          post("/oauth2/token")
            .param("grant_type", "refresh_token")
            .param("refresh_token", refreshToken)
            .param("client_id", FLOW_CLIENT_ID)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED),
        ).andReturn()
        .response.status
    assertThat(status).isEqualTo(400)
    assertThat(repository.countByUserAccountId(userId)).isZero()
  }

  @Test
  fun `revoking the grant kills its already-issued access token on the next request`() {
    val accessToken = runAuthorizationCodeFlow()
    mvc
      .perform(get("/v2/projects/${testData.project.id}/translations").header("Authorization", "Bearer $accessToken"))
      .andIsOk

    oauth2AuthorizationService.revokeAllForUser(testData.user.id)

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

  private fun authorizationRowsForUser(): Long = repository.countByUserAccountId(testData.user.id)

  @Test
  fun `a scope deselected at consent is absent from the issued token`() {
    val jwt = jwtService.emitToken(testData.user.id)
    val pending = startPendingConsent(jwt, scope = "translations.view translations.edit")
    selectProject(jwt, pending.state, testData.project.id).andExpect { assertThat(it.response.status).isEqualTo(204) }

    val accessToken = completeConsent(pending, approvedScopes = listOf("translations.view"))

    assertThat(stored(accessToken).grantedScopeValues)
      .contains("translations.view")
      .doesNotContain("translations.edit")
  }

  @Test
  fun `the code-delivery redirect echoes the client's own state and the RFC 9207 iss`() {
    val jwt = jwtService.emitToken(testData.user.id)
    val pending = startPendingConsent(jwt, hintProjectId = testData.project.id)
    val codeLocation = submitConsent(pending, listOf("translations.view")).response.getHeader("Location")
    // The client's original `state` must round-trip for CSRF defense — NOT the server's internal consent state.
    val echoedState = URLDecoder.decode(queryParam(codeLocation!!, "state")!!, StandardCharsets.UTF_8)
    assertThat(echoedState).isEqualTo("client-state").isNotEqualTo(pending.state)
    // iss is present and is the issuer this server advertises (RFC 9207 AS mix-up defense). Read live rather than
    // hardcoded: another test in the same JVM can change the URL property it is derived from.
    assertThat(URLDecoder.decode(queryParam(codeLocation, "iss")!!, StandardCharsets.UTF_8))
      .isEqualTo(issuerResolver.issuerUrl)
  }

  @Test
  fun `a reconnect after a completed consent shows the consent screen again`() {
    // A token is bound to a project chosen on the consent screen, and that choice is per-authorization with nowhere to
    // be remembered. So consent is deliberately not remembered either: skipping the screen would leave the
    // authorization with no project selection and nothing able to mint a token for it.
    val jwt = jwtService.emitToken(testData.user.id)
    val first = startPendingConsent(jwt)
    selectProject(jwt, first.state, testData.project.id).andExpect { assertThat(it.response.status).isEqualTo(204) }
    completeConsent(first)

    val session = MockHttpSession()
    mvc
      .perform(post("/v2/oauth2/session-bootstrap").header("Authorization", "Bearer $jwt").session(session))
      .andExpect { assertThat(it.response.status).isEqualTo(204) }
    val location =
      mvc
        .perform(get(authorizeUrl(state = "client-state-2")).session(session))
        .andReturn()
        .response
        .getHeader("Location")

    // The consent page, not a code delivered straight to the client redirect.
    assertThat(location).isNotNull().contains(OAuth2Constants.CONSENT_PAGE_PATH)
    assertThat(queryParam(location!!, "code")).isNull()
  }

  @Test
  fun `a second consent can bind the token to a different project`() {
    // The corollary of re-prompting: the project is chosen afresh each time rather than inherited from the last grant.
    val jwt = jwtService.emitToken(testData.user.id)
    val first = startPendingConsent(jwt)
    selectProject(jwt, first.state, testData.project.id).andExpect { assertThat(it.response.status).isEqualTo(204) }
    completeConsent(first)

    val second = startPendingConsent(jwt)
    selectProject(jwt, second.state, publicProjectId).andExpect { assertThat(it.response.status).isEqualTo(204) }

    assertThat(stored(completeConsent(second)).boundProjectIds()).containsExactly(publicProjectId)
  }

  @Test
  fun `rejects an authorize request that omits the PKCE code_challenge for a public client`() {
    val jwt = jwtService.emitToken(testData.user.id)
    val session = MockHttpSession()
    mvc
      .perform(post("/v2/oauth2/session-bootstrap").header("Authorization", "Bearer $jwt").session(session))
      .andExpect { assertThat(it.response.status).isEqualTo(204) }

    // Deliberately omit code_challenge. Every client is public, so the server must refuse with an error redirect and
    // never issue a code — pins PKCE against an accidental downgrade that would re-open code interception.
    val authorizeUrl =
      UriComponentsBuilder
        .fromPath("/oauth2/authorize")
        .queryParam("response_type", "code")
        .queryParam("client_id", FLOW_CLIENT_ID)
        .queryParam("redirect_uri", FLOW_REDIRECT)
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
    val pending = startPendingConsent(jwt)
    selectProject(jwt, pending.state, null).andExpect { assertThat(it.response.status).isEqualTo(204) }
    val code = queryParam(submitConsent(pending, listOf("translations.view")).response.getHeader("Location")!!, "code")

    val status =
      mvc
        .perform(
          post("/oauth2/token")
            .param("grant_type", "authorization_code")
            .param("code", code!!)
            .param("redirect_uri", CONSENT_REDIRECT)
            .param("client_id", CONSENT_CLIENT_ID)
            .param("code_verifier", "not-the-real-verifier")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED),
        ).andReturn()
        .response.status
    assertThat(status).isEqualTo(400)
  }

  @Test
  fun `binds the token to the project hinted on the authorize request`() {
    val accessToken = runAuthorizationCodeFlow(mapOf(OAuth2Constants.PROJECT_PARAM to testData.project.id.toString()))

    assertThat(stored(accessToken).boundProjectIds()).containsExactly(testData.project.id)
  }

  @Test
  fun `a single project chosen on the consent screen binds the token to it`() {
    val accessToken = consentFlowAccessToken(testData.project.id)

    assertThat(stored(accessToken).boundProjectIds()).containsExactly(testData.project.id)
  }

  @Test
  fun `choosing all projects on the consent screen keeps the token unscoped`() {
    val accessToken = consentFlowAccessToken(projectId = null)

    assertThat(stored(accessToken).boundProjectIds()).isNull()
  }

  @Test
  fun `a consent-screen project choice overrides the client's authorize hint`() {
    val jwt = jwtService.emitToken(testData.user.id)
    val pending = startPendingConsent(jwt, hintProjectId = INACCESSIBLE_PROJECT_ID)
    selectProject(jwt, pending.state, testData.project.id)
      .andExpect { assertThat(it.response.status).isEqualTo(204) }

    assertThat(stored(completeConsent(pending)).boundProjectIds()).containsExactly(testData.project.id)
  }

  @Test
  fun `choosing all projects on the consent screen overrides a client's authorize hint`() {
    val jwt = jwtService.emitToken(testData.user.id)
    val pending = startPendingConsent(jwt, hintProjectId = testData.project.id)
    selectProject(jwt, pending.state, null).andExpect { assertThat(it.response.status).isEqualTo(204) }

    assertThat(stored(completeConsent(pending)).boundProjectIds()).isNull()
  }

  @Test
  fun `a consent-selected token keeps its project binding after a refresh`() {
    // A refresh must not silently widen the project set back to "all projects".
    val jwt = jwtService.emitToken(testData.user.id)
    val pending = startPendingConsent(jwt) // no authorize hint — binding comes only from the consent selection
    selectProject(jwt, pending.state, testData.project.id).andExpect { assertThat(it.response.status).isEqualTo(204) }
    val first = completeConsentTokenResponse(pending)
    val refreshToken = first.get("refresh_token")?.asString()
    assertThat(refreshToken).isNotNull()

    val initial = stored(first.get("access_token").asString())
    val initialScopes = initial.grantedScopeValues
    val initialClientId = initial.clientId

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

    val refreshedStored = stored(refreshed.get("access_token").asString())
    assertThat(refreshedStored.boundProjectIds()).containsExactly(testData.project.id)
    assertThat(refreshedStored.clientId).isEqualTo(initialClientId)
    assertThat(refreshedStored.grantedScopeValues).isEqualTo(initialScopes)
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
  fun `an unauthenticated authorize request bootstraps through the configured origin`() {
    // The browser has to be able to reach the continue URL, so it must be the externally configured origin and not the
    // one the container saw — behind a reverse proxy those differ, and X-Forwarded-* is not trusted.
    val location =
      mvc
        .perform(
          get("/oauth2/authorize")
            .queryParam("response_type", "code")
            .queryParam("client_id", FLOW_CLIENT_ID)
            .queryParam("redirect_uri", FLOW_REDIRECT)
            .queryParam("scope", "translations.view")
            .queryParam("code_challenge", s256Challenge(randomVerifier()))
            .queryParam("code_challenge_method", "S256")
            .accept(MediaType.TEXT_HTML),
        ).andReturn()
        .response
        .getHeader("Location")

    assertThat(location).isNotNull().startsWith(OAuth2Constants.BOOTSTRAP_PAGE_PATH)
    val continueUrl = URLDecoder.decode(queryParam(location!!, "continue")!!, StandardCharsets.UTF_8)
    // Not the host MockMvc served the request on, which is what a request-derived URL would give.
    assertThat(continueUrl).doesNotStartWith("http://localhost")
    assertThat(continueUrl).startsWith("${issuerResolver.issuerUrl}/oauth2/authorize?")
    assertThat(continueUrl).contains("client_id=$FLOW_CLIENT_ID")
  }

  @Test
  fun `consent-info describes the pending authorization, not an inflated scope parameter`() {
    // `scope` is a query parameter of the consent screen's own request; a screen that echoed it would ask the user to
    // approve scopes the pending authorization never requested, and the token would then be minted from that
    // authorization instead. The screen must describe what is actually being consented to.
    val jwt = jwtService.emitToken(testData.user.id)
    val pending = startPendingConsent(jwt)

    val info =
      jacksonObjectMapper().readTree(
        mvc
          .perform(
            get("/v2/oauth2/consent-info")
              .header("Authorization", "Bearer $jwt")
              .param("clientId", CONSENT_CLIENT_ID)
              .param("state", pending.state)
              .param("scope", "translations.view translations.edit admin"),
          ).andIsOk
          .andReturn()
          .response.contentAsString,
      )

    assertThat(info.get("scopes").toString())
      .contains("translations.view")
      .doesNotContain("translations.edit")
      .doesNotContain("admin")
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
    assertThat(stored(token).boundProjectIds()).containsExactly(publicProjectId)

    mvc
      .perform(get("/v2/projects/$publicProjectId/translations").header("Authorization", "Bearer $token"))
      .andIsOk
  }

  @Test
  fun `invalidates the http session once the authorization code is issued`() {
    // The session carries only the authorize round trip; killing it at code issuance stops a later connect from
    // silently reusing a stale principal (e.g. after the webapp user switched accounts). A fresh bootstrap must run.
    val jwt = jwtService.emitToken(testData.user.id)
    val pending = startPendingConsent(jwt)
    selectProject(jwt, pending.state, null).andExpect { assertThat(it.response.status).isEqualTo(204) }

    val location = submitConsent(pending, listOf("translations.view")).response.getHeader("Location")

    assertThat(queryParam(location!!, "code")).isNotNull()
    assertThat(pending.session.isInvalid).isTrue()
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
    // A raw webapp/PAK/PAT bearer must not establish a principal at /oauth2/authorize (that would let a token mint
    // another token). Unauthenticated -> bootstrap redirect.
    val jwt = jwtService.emitToken(testData.user.id)
    val verifier = randomVerifier()
    val authorizeUrl =
      UriComponentsBuilder
        .fromPath("/oauth2/authorize")
        .queryParam("response_type", "code")
        .queryParam("client_id", FLOW_CLIENT_ID)
        .queryParam("redirect_uri", FLOW_REDIRECT)
        .queryParam("scope", "translations.view")
        .queryParam("code_challenge", s256Challenge(verifier))
        .queryParam("code_challenge_method", "S256")
        .queryParam("state", "state-123")
        .build()
        .toUriString()

    val location =
      mvc
        .perform(get(authorizeUrl).header("Authorization", "Bearer $jwt").accept(MediaType.TEXT_HTML))
        .andReturn()
        .response
        .getHeader("Location")

    assertThat(location).contains(OAuth2Constants.BOOTSTRAP_PAGE_PATH)
    assertThat(location).doesNotContain(FLOW_REDIRECT)
    assertThat(queryParam(location!!, "code")).isNull()

    val continueUrl = URLDecoder.decode(queryParam(location, "continue")!!, StandardCharsets.UTF_8)
    assertThat(continueUrl).contains("/oauth2/authorize")
    assertThat(continueUrl).contains("client_id=$FLOW_CLIENT_ID")
    assertThat(continueUrl).contains("state=state-123")
    assertThat(continueUrl).contains("code_challenge=")
  }

  // -----------------------------------------------------------------------------------------------------------------
  // Helpers
  // -----------------------------------------------------------------------------------------------------------------

  private data class PendingConsent(
    val session: MockHttpSession,
    val state: String,
    val verifier: String,
    val clientId: String,
    val redirect: String,
  )

  private fun runAuthorizationCodeFlow(
    extraAuthorizeParams: Map<String, String> = emptyMap(),
    clientId: String = FLOW_CLIENT_ID,
    redirect: String = FLOW_REDIRECT,
  ): String = authorizationCodeTokenResponse(extraAuthorizeParams, clientId, redirect).get("access_token").asString()

  /**
   * The whole flow as the browser runs it: bootstrap, authorize, bind the hinted project (or all projects) on the
   * consent screen, approve, exchange the code.
   */
  private fun authorizationCodeTokenResponse(
    extraAuthorizeParams: Map<String, String> = emptyMap(),
    clientId: String = FLOW_CLIENT_ID,
    redirect: String = FLOW_REDIRECT,
  ): JsonNode {
    val jwt = jwtService.emitToken(testData.user.id)
    val pending =
      startPendingConsent(
        jwt,
        hintProjectId = extraAuthorizeParams[OAuth2Constants.PROJECT_PARAM]?.toLong(),
        clientId = clientId,
        redirect = redirect,
        clientState = "state-123",
      )
    selectProject(jwt, pending.state, extraAuthorizeParams[OAuth2Constants.PROJECT_PARAM]?.toLong())
      .andExpect { assertThat(it.response.status).isEqualTo(204) }
    return completeConsentTokenResponse(pending)
  }

  private fun authorizeUrl(state: String): String =
    UriComponentsBuilder
      .fromPath("/oauth2/authorize")
      .queryParam("response_type", "code")
      .queryParam("client_id", CONSENT_CLIENT_ID)
      .queryParam("redirect_uri", CONSENT_REDIRECT)
      .queryParam("scope", "translations.view")
      .queryParam("code_challenge", s256Challenge(randomVerifier()))
      .queryParam("code_challenge_method", "S256")
      .queryParam("state", state)
      .build()
      .toUriString()

  /** Bootstraps a session and reaches the consent page, leaving a pending authorization keyed by the returned state. */
  private fun startPendingConsent(
    jwt: String,
    hintProjectId: Long? = null,
    scope: String = "translations.view",
    clientId: String = CONSENT_CLIENT_ID,
    redirect: String = CONSENT_REDIRECT,
    clientState: String = "client-state",
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
        .queryParam("client_id", clientId)
        .queryParam("redirect_uri", redirect)
        .queryParam("scope", scope)
        .queryParam("code_challenge", s256Challenge(verifier))
        .queryParam("code_challenge_method", "S256")
        .queryParam("state", clientState)
    hintProjectId?.let { authorizeBuilder.queryParam(OAuth2Constants.PROJECT_PARAM, it.toString()) }
    // The consent page carries the server's own state (which keys the pending authorization); url-decoded, that is
    // what the SPA and select-project use, not the client's original state.
    val consentPageLocation =
      mvc
        .perform(get(authorizeBuilder.build().toUriString()).session(session))
        .andReturn()
        .response
        .getHeader("Location")
    assertThat(consentPageLocation)
      .withFailMessage("expected the consent page, got $consentPageLocation")
      .contains(OAuth2Constants.CONSENT_PAGE_PATH)
    val state = URLDecoder.decode(queryParam(consentPageLocation!!, "state")!!, StandardCharsets.UTF_8)
    return PendingConsent(session, state, verifier, clientId, redirect)
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

  /** The consent form as the SPA posts it: client_id, the consent state and one `scope` field per approved scope. */
  private fun submitConsent(
    pending: PendingConsent,
    approvedScopes: List<String>,
  ) = mvc
    .perform(
      post("/oauth2/authorize")
        .param("client_id", pending.clientId)
        .param("state", pending.state)
        .apply { approvedScopes.forEach { param("scope", it) } }
        .session(pending.session),
    ).andReturn()

  private fun completeConsent(
    pending: PendingConsent,
    approvedScopes: List<String> = listOf("translations.view"),
  ): String = completeConsentTokenResponse(pending, approvedScopes).get("access_token").asString()

  /** Submits the consent form for a pending authorization and exchanges the resulting code for the token response. */
  private fun completeConsentTokenResponse(
    pending: PendingConsent,
    approvedScopes: List<String> = listOf("translations.view"),
  ): JsonNode {
    val consentLocation = submitConsent(pending, approvedScopes).response.getHeader("Location")
    val code = queryParam(consentLocation!!, "code")
    assertThat(code).withFailMessage("consent did not deliver a code: $consentLocation").isNotNull()

    val tokenResponse =
      mvc
        .perform(
          post("/oauth2/token")
            .param("grant_type", "authorization_code")
            .param("code", code!!)
            .param("redirect_uri", pending.redirect)
            .param("client_id", pending.clientId)
            .param("code_verifier", pending.verifier)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED),
        ).andReturn()
        .response.contentAsString
    val tree = jacksonObjectMapper().readTree(tokenResponse)
    assertThat(tree.get("access_token")?.asString()).withFailMessage(tokenResponse).isNotNull()
    return tree
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

  private fun stored(accessToken: String): OAuth2Authorization =
    repository.findByAccessTokenHash(keyGenerator.hash(accessToken))
      ?: throw AssertionError("no authorization stored for the access token")

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
    private const val FLOW_CLIENT_ID = OAuth2Constants.CLI_CLIENT_ID
    private const val FLOW_REDIRECT = "http://127.0.0.1:9999/callback"
    private const val CONSENT_CLIENT_ID = OAuth2Constants.BROWSER_EXTENSION_CLIENT_ID
    private const val CONSENT_REDIRECT = "https://extension.test/callback"
    private const val INACCESSIBLE_PROJECT_ID = 9_999_999L
  }
}
