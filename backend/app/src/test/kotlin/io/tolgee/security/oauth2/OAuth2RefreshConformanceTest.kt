package io.tolgee.security.oauth2

import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import tools.jackson.databind.JsonNode
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
  fun `a refresh cannot widen the scope to one this server knows but never granted`() {
    val issued = json(tokenResult())
    val result = driver.refresh(issued.get("refresh_token").asString(), CLIENT_ID, scope = "keys.edit").andReturn()
    json(result)
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_scope")
  }

  @Test
  fun `a refresh naming a scope the server does not know drops it and narrows to the rest`() {
    val issued = twoScopeGrant()

    json(
      driver
        .refresh(issued.get("refresh_token").asString(), CLIENT_ID, scope = "translations.view not.a.tolgee.scope")
        .andReturn(),
    ).get("scope")
      .asString()
      .assert
      .isEqualTo("translations.view")
  }

  @Test
  fun `a refresh mixing a granted scope with a known but ungranted one is refused, not quietly narrowed`() {
    val issued = json(tokenResult())

    json(
      driver
        .refresh(issued.get("refresh_token").asString(), CLIENT_ID, scope = "translations.view keys.edit")
        .andReturn(),
    ).get("error")
      .asString()
      .assert
      .isEqualTo("invalid_scope")
  }

  @Test
  fun `a refresh left with no scope this server knows is refused rather than silently widened`() {
    val issued = json(tokenResult())
    val result =
      driver.refresh(issued.get("refresh_token").asString(), CLIENT_ID, scope = "not.a.tolgee.scope").andReturn()

    json(result)
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_scope")
  }

  @Test
  fun `a narrowing refresh issues the narrower token without shrinking the grant`() {
    val issued = twoScopeGrant()
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

  /** A grant holding two scopes, so a narrowing assertion can tell "the rest" from "the whole grant". */
  private fun twoScopeGrant(): JsonNode =
    driver.completeFlow(
      jwt(),
      CLIENT_ID,
      REDIRECT,
      scope = "translations.view keys.view",
      approvedScopes = listOf("translations.view", "keys.view"),
    )
}
