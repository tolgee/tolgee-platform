package io.tolgee.security.oauth2

import io.tolgee.model.enums.Scope
import io.tolgee.testing.assert
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant
import java.util.Date

/**
 * The consent-screen API and the project binding it produces: what the screen may offer, what a decision stores on the
 * grant, and what a reconnect sees. Protocol edge cases live in the `*ConformanceTest` classes; what an issued token
 * then reaches lives in [OAuth2AccessTokenFlowTest].
 */
class OAuth2AuthorizationCodeFlowTest : AbstractOAuth2FlowTest() {
  @org.springframework.beans.factory.annotation.Autowired
  private lateinit var supersededRepository: io.tolgee.repository.oauth2.OAuth2SupersededRefreshTokenRepository

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
    grantsForUser().assert.isZero()
  }

  @Test
  fun `changing the password revokes the user's OAuth grants`() {
    completeFlow(projectId = testData.project.id)
    grantsForUser().assert.isNotZero()

    userAccountService.setUserPassword(userAccountService.get(testData.user.id), "new-password-123")

    grantsForUser().assert.isZero()
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

  /**
   * The ceiling binds where rows are written, because a thief holding a stolen token can rotate in a loop and any
   * eviction that loop can drive would take the victim's row with it. A grant that reaches the ceiling on its own —
   * several processes sharing one do — still has to record, so the slot comes from a row the prune would have taken.
   */
  @Test
  fun `a grant at its ceiling makes room from rows past the floor rather than stopping`() {
    // The second rotation is the first with a predecessor to demote; the first has none.
    val (grant, refreshToken) = rotatedOnce()
    val ceiling = OAuth2AuthorizationService.MAX_HISTORY_ROWS_PER_GRANT
    val ancient = Instant.now().minus(Duration.ofDays(400))
    val oldest = supersededRepository.saveAll(historyRows(grant, ceiling, ancient)).last()

    json(driver.refresh(refreshToken, CLIENT_ID))

    supersededRepository.countByGrantId(grant.id).assert.isEqualTo(ceiling.toLong())
    supersededRepository.existsById(oldest.id).assert.isFalse()
  }

  @Test
  fun `a grant whose whole history is younger than the floor stops recording rather than evicting`() {
    val (grant, refreshToken) = rotatedOnce()
    val ceiling = OAuth2AuthorizationService.MAX_HISTORY_ROWS_PER_GRANT
    val young = supersededRepository.saveAll(historyRows(grant, ceiling)).toList()

    val refreshed = json(driver.refresh(refreshToken, CLIENT_ID))

    refreshed
      .get("access_token")
      .asString()
      .assert
      .isNotBlank()
    young.forEach { supersededRepository.existsById(it.id).assert.isTrue() }
    supersededRepository.countByGrantId(grant.id).assert.isEqualTo(ceiling.toLong())
  }

  /** A grant that has rotated once, so the next rotation has a predecessor to demote into the history. */
  private fun rotatedOnce(): Pair<io.tolgee.model.oauth2.OAuth2Grant, String> {
    val first = completeFlow(projectId = testData.project.id)
    val second = json(driver.refresh(first.get("refresh_token").asString(), CLIENT_ID))
    return stored(second.get("access_token").asString()) to second.get("refresh_token").asString()
  }

  private fun historyRows(
    grant: io.tolgee.model.oauth2.OAuth2Grant,
    count: Int,
    from: Instant = Instant.now(),
  ) = (1..count).map { index ->
    io.tolgee.model.oauth2.OAuth2SupersededRefreshToken().apply {
      this.grant = grant
      tokenHash = "filler-$index"
      supersededAt = Date.from(from.minusSeconds(index.toLong()))
    }
  }

  @Test
  fun `a scope deselected at consent is absent from the issued token`() {
    val pending = driver.startPendingConsent(jwt(), CLIENT_ID, REDIRECT, scope = "translations.view translations.edit")
    val accessToken = tokenFrom(pending, approvedScopes = listOf("translations.view")).get("access_token").asString()

    stored(accessToken)
      .maxGrantedScopeValues.assert
      .contains("translations.view")
      .doesNotContain("translations.edit")
  }

  @Test
  fun `a grant stores Scope names, not the wire values it was requested with`() {
    val accessToken = accessToken(projectId = testData.project.id)
    val grant = stored(accessToken)

    grant.maxGrantedScopes.assert.isEqualTo(Scope.TRANSLATIONS_VIEW.name)
    grant.maxGrantedScopeValues.assert.containsExactly(Scope.TRANSLATIONS_VIEW.value)
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
            "code_challenge" to OAuth2FlowDriver.randomChallenge(),
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
      .consentInfo(jwtService.emitToken(testData.otherUser.id), pending.state)
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
}
