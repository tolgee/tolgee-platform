package io.tolgee.security.oauth2

import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import java.time.Duration

/**
 * Refresh-token rotation: what a refresh may ask for, and which presentations kill the grant.
 */
class OAuth2RefreshConformanceTest : AbstractOAuth2ConformanceTest() {
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
  fun `a refresh token presented by the wrong client is refused, and the grant dies with it`() {
    val issued = json(tokenResult())
    val refreshToken = issued.get("refresh_token").asString()

    json(driver.refresh(refreshToken, OTHER_CLIENT_ID).andReturn())
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_grant")

    json(driver.refresh(refreshToken, CLIENT_ID).andReturn())
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_grant")
  }
}
