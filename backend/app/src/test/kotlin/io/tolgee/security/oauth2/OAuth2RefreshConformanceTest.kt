package io.tolgee.security.oauth2

import io.tolgee.Metrics
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration

/**
 * Refresh-token rotation: what a refresh may ask for, and which presentations kill the grant.
 */
class OAuth2RefreshConformanceTest : AbstractOAuth2ConformanceTest() {
  @Autowired
  private lateinit var metrics: Metrics

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

    assertOAuthError(
      driver.refresh(issued.get("refresh_token").asString(), CLIENT_ID, scope = "keys.edit").andReturn(),
      "invalid_scope",
    )
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
  fun `a replay inside the grace window is counted, which is the only trace the kept grant leaves`() {
    val issued = json(tokenResult())
    val superseded = issued.get("refresh_token").asString()
    json(driver.refresh(superseded, CLIENT_ID).andReturn())
    val before = metrics.oauth2RefreshGraceHitsCounter.count()

    assertOAuthError(driver.refresh(superseded, CLIENT_ID).andReturn(), "invalid_grant")

    metrics.oauth2RefreshGraceHitsCounter
      .count()
      .assert
      .isEqualTo(before + 1)
  }

  @Test
  fun `replaying the just-rotated token within the grace window fails but keeps the grant`() {
    val issued = json(tokenResult())
    val superseded = issued.get("refresh_token").asString()
    val rotated = json(driver.refresh(superseded, CLIENT_ID).andReturn())

    assertOAuthError(driver.refresh(superseded, CLIENT_ID).andReturn(), "invalid_grant")
    assertRefreshSucceeds(rotated.get("refresh_token").asString())
  }

  @Test
  fun `replaying the just-rotated token after the grace window revokes the grant`() {
    val issued = json(tokenResult())
    val superseded = issued.get("refresh_token").asString()
    val rotated = json(driver.refresh(superseded, CLIENT_ID).andReturn())

    currentDateProvider.move(Duration.ofSeconds(oauth2.refreshTokenGraceSeconds + 5))

    assertOAuthError(driver.refresh(superseded, CLIENT_ID).andReturn(), "invalid_grant")
    assertOAuthError(driver.refresh(rotated.get("refresh_token").asString(), CLIENT_ID).andReturn(), "invalid_grant")
  }

  @Test
  fun `forgiveness follows the replayed token's own age, not the age of the rotation that demoted it`() {
    val issued = json(tokenResult())
    val oldest = issued.get("refresh_token").asString()
    val second = rotate(oldest)

    currentDateProvider.move(Duration.ofSeconds(oauth2.refreshTokenGraceSeconds + 5))
    val third = rotate(second)

    assertOAuthError(driver.refresh(oldest, CLIENT_ID).andReturn(), "invalid_grant")
    assertOAuthError(driver.refresh(third, CLIENT_ID).andReturn(), "invalid_grant")
  }

  @Test
  fun `a replay from two rotations back inside the grace window fails without killing the grant`() {
    val issued = json(tokenResult())
    val oldest = issued.get("refresh_token").asString()
    val second = rotate(oldest)
    val third = rotate(second)

    assertOAuthError(driver.refresh(oldest, CLIENT_ID).andReturn(), "invalid_grant")
    assertRefreshSucceeds(third)
  }

  @Test
  fun `a replay from two rotations back is treated as theft and kills the grant`() {
    val issued = json(tokenResult())
    val oldest = issued.get("refresh_token").asString()
    val second = rotate(oldest)
    val third = rotate(second)

    currentDateProvider.move(Duration.ofSeconds(oauth2.refreshTokenGraceSeconds + 5))

    assertOAuthError(driver.refresh(oldest, CLIENT_ID).andReturn(), "invalid_grant")
    assertOAuthError(driver.refresh(third, CLIENT_ID).andReturn(), "invalid_grant")
  }

  @Test
  fun `a refresh token this grant never issued is refused without revoking anything`() {
    val issued = json(tokenResult())
    assertOAuthError(driver.refresh("tgort_never-issued-secret", CLIENT_ID).andReturn(), "invalid_grant")

    assertRefreshSucceeds(issued.get("refresh_token").asString())
  }

  @Test
  fun `an expired refresh token is refused`() {
    val issued = json(tokenResult())
    currentDateProvider.move(Duration.ofDays(oauth2.refreshTokenValidityDays + 1))
    assertOAuthError(driver.refresh(issued.get("refresh_token").asString(), CLIENT_ID).andReturn(), "invalid_grant")
  }

  @Test
  fun `a refresh token presented by the wrong client is refused, and the grant dies with it`() {
    val issued = json(tokenResult())
    val refreshToken = issued.get("refresh_token").asString()

    assertOAuthError(driver.refresh(refreshToken, OTHER_CLIENT_ID).andReturn(), "invalid_grant")
    assertOAuthError(driver.refresh(refreshToken, CLIENT_ID).andReturn(), "invalid_grant")
  }

  private fun rotate(token: String): String =
    json(driver.refresh(token, CLIENT_ID).andReturn()).get("refresh_token").asString()

  private fun assertRefreshSucceeds(token: String) =
    json(driver.refresh(token, CLIENT_ID).andReturn())
      .get("access_token")
      .asString()
      .assert
      .isNotBlank()
}
