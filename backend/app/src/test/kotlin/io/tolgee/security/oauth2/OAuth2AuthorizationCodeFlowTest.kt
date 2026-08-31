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
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.dtos.request.pat.CreatePatDto
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andIsUnauthorized
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.Scope
import io.tolgee.model.oauth2.OAuth2Grant
import io.tolgee.repository.oauth2.OAuth2GrantRepository
import io.tolgee.security.OAUTH_ACCESS_TOKEN_PREFIX
import io.tolgee.security.authentication.JwtService
import io.tolgee.testing.AbstractControllerTest
import io.tolgee.testing.assert
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import tools.jackson.databind.JsonNode
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Date

/**
 * What is Tolgee's own on top of the protocol: project binding, the consent-screen API, and revocation. Protocol edge
 * cases live in [OAuth2ProtocolConformanceTest].
 */
class OAuth2AuthorizationCodeFlowTest : AbstractControllerTest() {
  @Autowired
  private lateinit var jwtService: JwtService

  @Autowired
  private lateinit var oauth2AuthorizationService: OAuth2AuthorizationService

  @Autowired
  private lateinit var issuerResolver: OAuth2IssuerResolver

  @Autowired
  private lateinit var repository: OAuth2GrantRepository

  @Autowired
  private lateinit var keyGenerator: KeyGenerator

  private lateinit var testData: BaseTestData
  private lateinit var otherUser: UserAccount
  private lateinit var driver: OAuth2FlowDriver
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
    driver = OAuth2FlowDriver(mvc)
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `authorization code + PKCE flow issues an access token that works on the REST API`() {
    val accessToken = accessToken(projectId = testData.project.id)

    accessToken.assert.startsWith(OAUTH_ACCESS_TOKEN_PREFIX)
    stored(accessToken)
      .userAccount.id.assert
      .isEqualTo(testData.user.id)
    apiRequest(accessToken).andIsOk
  }

  @Test
  fun `issues a refresh token to the public client and rotates it on refresh`() {
    val first = completeFlow(projectId = testData.project.id)
    val refreshToken = first.get("refresh_token").asString()
    val firstAccessToken = first.get("access_token").asString()
    val initial = stored(firstAccessToken)
    val initialScopes = initial.grantedScopeValues
    val initialClientId = initial.clientId

    val refreshed = json(driver.refresh(refreshToken, CLIENT_ID))
    val refreshedAccessToken = refreshed.get("access_token").asString()
    refreshed
      .get("refresh_token")
      .asString()
      .assert
      .isNotEqualTo(refreshToken)

    val refreshedStored = stored(refreshedAccessToken)
    refreshedStored.boundProjectIds().assert.containsExactly(testData.project.id)
    refreshedStored.clientId.assert.isEqualTo(initialClientId)
    refreshedStored.grantedScopeValues.assert.isEqualTo(initialScopes)

    apiRequest(firstAccessToken).andIsUnauthorized
  }

  @Test
  fun `refresh grant is rejected after the user invalidates their tokens`() {
    val refreshToken = completeFlow(projectId = testData.project.id).get("refresh_token").asString()

    val user = userAccountService.get(testData.user.id)
    user.tokensValidNotBefore = Date(System.currentTimeMillis() + 3_600_000)
    userAccountService.save(user)

    driver
      .refresh(refreshToken, CLIENT_ID)
      .andReturn()
      .response.status.assert
      .isEqualTo(400)
    authorizationRowsForUser().assert.isZero()
  }

  @Test
  fun `changing the password revokes the user's OAuth grants`() {
    completeFlow(projectId = testData.project.id)
    authorizationRowsForUser().assert.isNotZero()

    userAccountService.setUserPassword(userAccountService.get(testData.user.id), "new-password-123")

    authorizationRowsForUser().assert.isZero()
  }

  @Test
  fun `refresh grant is rejected and revoked after the subject user is deleted`() {
    val refreshToken = completeFlow(projectId = testData.project.id).get("refresh_token").asString()
    val userId = testData.user.id

    userAccountService.delete(userId)

    driver
      .refresh(refreshToken, CLIENT_ID)
      .andReturn()
      .response.status.assert
      .isEqualTo(400)
    grantsForUser(userId).assert.isZero()
  }

  @Test
  fun `revoking the grant kills its already-issued access token on the next request`() {
    val accessToken = accessToken(projectId = testData.project.id)
    apiRequest(accessToken).andIsOk

    oauth2AuthorizationService.revokeAllForUser(testData.user.id)

    apiRequest(accessToken).andIsUnauthorized
  }

  @Test
  fun `invalidating all tokens kills already-issued OAuth access tokens and grants`() {
    val accessToken = accessToken(projectId = testData.project.id)
    apiRequest(accessToken).andIsOk

    userAccountService.invalidateTokens(userAccountService.get(testData.user.id))

    apiRequest(accessToken).andIsUnauthorized
    authorizationRowsForUser().assert.isZero()
  }

  @Test
  fun `a scope deselected at consent is absent from the issued token`() {
    val pending = driver.startPendingConsent(jwt(), CLIENT_ID, REDIRECT, scope = "translations.view translations.edit")
    val accessToken = tokenFrom(pending, approvedScopes = listOf("translations.view")).get("access_token").asString()

    stored(accessToken)
      .grantedScopeValues.assert
      .contains("translations.view")
      .doesNotContain("translations.edit")
  }

  @Test
  fun `a grant stores Scope names, not the wire values it was requested with`() {
    val accessToken = accessToken(projectId = testData.project.id)
    val grant = stored(accessToken)

    grant.grantedScopes.assert.isEqualTo(Scope.TRANSLATIONS_VIEW.name)
    grant.grantedScopeValues.assert.containsExactly(Scope.TRANSLATIONS_VIEW.value)
  }

  @Test
  fun `a stored scope value that no longer names a real scope is dropped rather than failing the request`() {
    val accessToken = accessToken(projectId = testData.project.id)
    val grant = stored(accessToken)
    // Written straight to the column: the property setter maps wire values to Scope names and would drop this first.
    grant.activeScopes = grant.activeScopes + " TRANSLATIONS_RETIRED_SCOPE"
    repository.save(grant)

    apiRequest(accessToken).andIsOk
    stored(accessToken).activeScopeValues.assert.containsExactly(Scope.TRANSLATIONS_VIEW.value)
  }

  @Test
  fun `the code-delivery redirect echoes the client's own state and the RFC 9207 iss`() {
    val pending = driver.startPendingConsent(jwt(), CLIENT_ID, REDIRECT)
    val codeRedirect = driver.consentRedirect(pending, projectId = testData.project.id)

    val echoedState = URLDecoder.decode(driver.queryParam(codeRedirect, "state")!!, StandardCharsets.UTF_8)
    echoedState.assert.isEqualTo("client-state").isNotEqualTo(pending.state)
    // Read live rather than hardcoded: another test in the same JVM can change the URL property it derives from.
    assertThat(URLDecoder.decode(driver.queryParam(codeRedirect, "iss")!!, StandardCharsets.UTF_8))
      .isEqualTo(issuerResolver.issuerUrl)
  }

  @Test
  fun `a reconnect after a completed consent shows the consent screen again`() {
    completeFlow(projectId = testData.project.id)

    val location =
      driver
        .authorize(
          CLIENT_ID,
          REDIRECT,
          mapOf(
            "response_type" to "code",
            "scope" to "translations.view",
            "code_challenge" to OAuth2FlowDriver.s256Challenge(OAuth2FlowDriver.randomVerifier()),
            "code_challenge_method" to "S256",
          ),
        ).andReturn()
        .response
        .getHeader("Location")

    location.assert.isNotNull().contains(OAuth2Constants.CONSENT_PAGE_PATH)
    driver.queryParam(location!!, "code").assert.isNull()
  }

  @Test
  fun `a second consent can bind the token to a different project`() {
    completeFlow(projectId = testData.project.id)

    val second = accessToken(projectId = publicProjectId)

    stored(second).boundProjectIds().assert.containsExactly(publicProjectId)
  }

  @Test
  fun `a single project chosen on the consent screen binds the token to it`() {
    assertThat(stored(accessToken(projectId = testData.project.id)).boundProjectIds())
      .containsExactly(testData.project.id)
  }

  @Test
  fun `choosing all projects on the consent screen keeps the token unscoped`() {
    assertThat(stored(accessToken(projectId = null)).boundProjectIds()).isNull()
  }

  @Test
  fun `the client's authorize hint binds nothing on its own`() {
    val pending =
      driver.startPendingConsent(jwt(), CLIENT_ID, REDIRECT, hintProjectId = testData.project.id)
    val accessToken = tokenFrom(pending, projectId = null).get("access_token").asString()

    stored(accessToken).boundProjectIds().assert.isNull()
  }

  @Test
  fun `a consent-screen project choice overrides the client's authorize hint`() {
    val pending =
      driver.startPendingConsent(jwt(), CLIENT_ID, REDIRECT, hintProjectId = INACCESSIBLE_PROJECT_ID)
    val accessToken = tokenFrom(pending, projectId = testData.project.id).get("access_token").asString()

    stored(accessToken).boundProjectIds().assert.containsExactly(testData.project.id)
  }

  @Test
  fun `a consent-selected token keeps its project binding after a refresh`() {
    val first = completeFlow(projectId = testData.project.id)
    val refreshed = json(driver.refresh(first.get("refresh_token").asString(), CLIENT_ID))

    assertThat(stored(refreshed.get("access_token").asString()).boundProjectIds())
      .containsExactly(testData.project.id)
  }

  @Test
  fun `consent-info names no project when the client sent no hint`() {
    val jwt = jwt()

    val info = consentInfo(jwt, driver.startPendingConsent(jwt, CLIENT_ID, REDIRECT).state)

    info
      .get("project")
      .isNull.assert
      .isTrue()
    info
      .get("requestedProjectId")
      .isNull.assert
      .isTrue()
  }

  @Test
  fun `consent-info reports the requested id but no project when the hint is one the user cannot reach`() {
    val jwt = jwt()

    listOf(otherProjectId, INACCESSIBLE_PROJECT_ID).forEach { hinted ->
      val pending = driver.startPendingConsent(jwt, CLIENT_ID, REDIRECT, hintProjectId = hinted)
      val info = consentInfo(jwt, pending.state)

      info
        .get("project")
        .isNull.assert
        .isTrue()
      info
        .get("requestedProjectId")
        .asLong()
        .assert
        .isEqualTo(hinted)
    }
  }

  @Test
  fun `consent-info resolves a hinted project the user can reach`() {
    val jwt = jwt()

    val pending = driver.startPendingConsent(jwt, CLIENT_ID, REDIRECT, hintProjectId = testData.project.id)
    val hinted = consentInfo(jwt, pending.state).get("project")

    hinted
      .get("id")
      .asLong()
      .assert
      .isEqualTo(testData.project.id)
    hinted
      .get("name")
      .asString()
      .assert
      .isEqualTo(testData.project.name)
  }

  @Test
  fun `consent-info marks only the client's required scopes as required`() {
    val jwt = jwt()
    val pending =
      driver.startPendingConsent(jwt, CLIENT_ID, REDIRECT, scope = "translations.view translations.edit")
    val info = consentInfo(jwt, pending.state)

    info
      .get("scopes")
      .toString()
      .assert
      .contains("translations.view")
      .contains("translations.edit")
    info
      .get("requiredScopes")
      .toString()
      .assert
      .contains("translations.view")
      .doesNotContain("translations.edit")
  }

  @Test
  fun `consent-info describes the pending authorization it is keyed by`() {
    val jwt = jwt()
    val pending = driver.startPendingConsent(jwt, CLIENT_ID, REDIRECT, scope = "translations.view")

    consentInfo(jwt, pending.state)
      .get("scopes")
      .toString()
      .assert
      .contains("translations.view")
      .doesNotContain("translations.edit")
  }

  @Test
  fun `consent-info for another user's pending authorization is not found`() {
    val pending = driver.startPendingConsent(jwt(), CLIENT_ID, REDIRECT)
    driver
      .consentInfo(jwtService.emitToken(otherUser.id), pending.state)
      .andReturn()
      .response.status
      .let { it.assert.isEqualTo(404) }
  }

  @Test
  fun `consent rejects a project the user has no access to`() {
    val pending = driver.startPendingConsent(jwt(), CLIENT_ID, REDIRECT)
    driver
      .submitConsent(pending, projectId = otherProjectId)
      .andReturn()
      .response.status.assert
      .isEqualTo(403)
    driver
      .submitConsent(pending, projectId = INACCESSIBLE_PROJECT_ID)
      .andReturn()
      .response.status.assert
      .isEqualTo(403)
  }

  @Test
  fun `a public project the user is not a member of is selectable via the community floor`() {
    val jwt = jwt()
    val pending = driver.startPendingConsent(jwt, CLIENT_ID, REDIRECT, hintProjectId = publicProjectId)

    consentInfo(jwt, pending.state)
      .get("project")
      .get("id")
      .asLong()
      .assert
      .isEqualTo(publicProjectId)

    val token = tokenFrom(pending, projectId = publicProjectId).get("access_token").asString()
    stored(token).boundProjectIds().assert.containsExactly(publicProjectId)

    mvc
      .perform(get("/v2/projects/$publicProjectId/translations").header("Authorization", "Bearer $token"))
      .andIsOk
  }

  @Test
  fun `no API credential can open an authorization`() {
    val oauthToken = accessToken(projectId = testData.project.id)
    apiAccessForbidden(startAuthorizationWith("Authorization", "Bearer $oauthToken"))
    apiAccessForbidden(startAuthorizationWith("X-API-Key", pak()))
    apiAccessForbidden(startAuthorizationWith("X-API-Key", pat()))
  }

  private fun jwt(): String = jwtService.emitToken(testData.user.id)

  private fun pak(): String =
    "tgpak_" + apiKeyService.create(testData.user, setOf(Scope.TRANSLATIONS_VIEW), testData.project).encodedKey

  private fun pat(): String = "tgpat_" + patService.create(CreatePatDto("oauth-guard"), testData.user).token

  private fun startAuthorizationWith(
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

  private fun apiAccessForbidden(actions: ResultActions) {
    val response = actions.andReturn().response
    response.status.assert.isEqualTo(403)
    response.contentAsString.assert.contains(Message.API_ACCESS_FORBIDDEN.code)
  }

  private fun authorizationRowsForUser(): Int = grantsForUser(testData.user.id)

  private fun apiRequest(accessToken: String) =
    mvc.perform(
      get("/v2/projects/${testData.project.id}/translations").header("Authorization", "Bearer $accessToken"),
    )

  private fun completeFlow(projectId: Long?): JsonNode =
    driver.completeFlow(jwt(), CLIENT_ID, REDIRECT, projectId = projectId)

  private fun accessToken(projectId: Long?): String = completeFlow(projectId).get("access_token").asString()

  private fun tokenFrom(
    pending: OAuth2FlowDriver.PendingConsent,
    approvedScopes: List<String> = listOf("translations.view"),
    projectId: Long? = testData.project.id,
  ): JsonNode {
    val code = driver.queryParam(driver.consentRedirect(pending, approvedScopes, projectId), "code")!!
    return json(driver.exchangeCode(code, pending.clientId, pending.redirect, pending.verifier))
  }

  private fun consentInfo(
    jwt: String,
    state: String,
  ): JsonNode = json(driver.consentInfo(jwt, state).andIsOk)

  private fun json(actions: ResultActions): JsonNode =
    jacksonObjectMapper().readTree(actions.andReturn().response.contentAsString)

  private fun stored(accessToken: String): OAuth2Grant =
    repository.findByAccessTokenHash(keyGenerator.hash(accessToken.removePrefix(OAUTH_ACCESS_TOKEN_PREFIX)))
      ?: throw AssertionError("no authorization stored for the access token")

  companion object {
    // Registered from tolgee.oauth2.* in the test application.yaml.
    private const val CLIENT_ID = OAuth2Constants.BROWSER_EXTENSION_CLIENT_ID
    private const val REDIRECT = "https://extension.test/callback"
    private const val INACCESSIBLE_PROJECT_ID = 9_999_999L
  }

  private fun grantsForUser(userId: Long): Int = repository.findAll().count { it.userAccount.id == userId }
}
