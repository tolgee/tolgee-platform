package io.tolgee.security.oauth2

import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andIsUnauthorized
import io.tolgee.fixtures.bearerHeaders
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

/**
 * RFC 7009 revocation, and the request-shape rules the token and revocation endpoints share.
 */
class OAuth2RevocationConformanceTest : AbstractOAuth2ConformanceTest() {
  @Test
  fun `a client can revoke its own refresh token, and the access token dies with the grant`() {
    val issued = json(tokenResult())
    val accessToken = issued.get("access_token").asString()

    driver.revoke(issued.get("refresh_token").asString(), CLIENT_ID).andIsOk

    json(driver.refresh(issued.get("refresh_token").asString(), CLIENT_ID).andReturn())
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_grant")
    performGet("/v2/projects/${testData.project.id}/translations", bearerHeaders(accessToken)).andIsUnauthorized
  }

  @Test
  fun `a client can revoke by presenting its access token, not only the refresh token`() {
    val issued = json(tokenResult())
    val accessToken = issued.get("access_token").asString()

    driver.revoke(accessToken, CLIENT_ID).andIsOk

    performGet("/v2/projects/${testData.project.id}/translations", bearerHeaders(accessToken)).andIsUnauthorized
    json(driver.refresh(issued.get("refresh_token").asString(), CLIENT_ID).andReturn())
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_grant")
  }

  @Test
  fun `a token sent in the query string is refused, and the grant stays live`() {
    val issued = json(tokenResult())
    val refreshToken = issued.get("refresh_token").asString()

    val refused =
      mvc
        .perform(
          post("${OAuth2Constants.REVOKE_PATH}?token=$refreshToken&client_id=$CLIENT_ID")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED),
        ).andReturn()
    refused.response.status.assert
      .isEqualTo(400)
    json(refused)
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_request")

    json(driver.refresh(refreshToken, CLIENT_ID).andReturn())
      .get("access_token")
      .asString()
      .assert
      .isNotBlank()
  }

  @Test
  fun `a client cannot revoke another client's grant`() {
    val issued = json(tokenResult())
    val refreshToken = issued.get("refresh_token").asString()

    assertOAuthError(driver.revoke(refreshToken, OTHER_CLIENT_ID).andReturn(), "invalid_grant")

    json(driver.refresh(refreshToken, CLIENT_ID).andReturn())
      .get("access_token")
      .asString()
      .assert
      .isNotBlank()
  }

  @Test
  fun `revoking an unknown token answers 200 all the same`() {
    driver.revoke("tgoat_never-issued", CLIENT_ID).andIsOk
  }

  @Test
  fun `the token endpoint refuses a parameter sent more than once`() {
    assertOAuthError(
      mvc
        .perform(
          post(OAuth2Constants.TOKEN_PATH)
            .param("grant_type", "authorization_code")
            .param("grant_type", "refresh_token")
            .param("client_id", CLIENT_ID)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED),
        ).andReturn(),
      "invalid_request",
    )
  }

  @Test
  fun `the revocation endpoint refuses a parameter sent more than once`() {
    assertOAuthError(
      mvc
        .perform(
          post(OAuth2Constants.REVOKE_PATH)
            .param("token", "tgoat_one")
            .param("token", "tgoat_two")
            .param("client_id", CLIENT_ID)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED),
        ).andReturn(),
      "invalid_request",
    )
  }

  @Test
  fun `a revocation carrying no token is refused rather than silently doing nothing`() {
    val issued = json(tokenResult())

    listOf(null, "").forEach { token ->
      val refused = driver.revoke(token, CLIENT_ID).andReturn()
      refused.response.status.assert
        .isEqualTo(400)
      json(refused)
        .get("error")
        .asString()
        .assert
        .isEqualTo("invalid_request")
    }

    json(driver.refresh(issued.get("refresh_token").asString(), CLIENT_ID).andReturn())
      .get("access_token")
      .asString()
      .assert
      .isNotBlank()
  }

  @Test
  fun `revoking the refresh token the current one replaced still ends the grant`() {
    val issued = json(tokenResult())
    val superseded = issued.get("refresh_token").asString()
    val current = json(driver.refresh(superseded, CLIENT_ID).andReturn()).get("refresh_token").asString()

    driver.revoke(superseded, CLIENT_ID).andIsOk

    json(driver.refresh(current, CLIENT_ID).andReturn())
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_grant")
  }

  @Test
  fun `a revocation naming no registered client is refused rather than silently doing nothing`() {
    val issued = json(tokenResult())
    val refreshToken = issued.get("refresh_token").asString()

    listOf(null, "not-a-registered-client").forEach { clientId ->
      val refused = driver.revoke(refreshToken, clientId).andReturn()
      refused.response.status.assert
        .isEqualTo(400)
      json(refused)
        .get("error")
        .asString()
        .assert
        .isEqualTo("invalid_client")
    }

    json(driver.refresh(refreshToken, CLIENT_ID).andReturn())
      .get("access_token")
      .asString()
      .assert
      .isNotBlank()
  }

  @Test
  fun `a refresh without a client_id is refused`() {
    val issued = json(tokenResult())
    val result =
      mvc
        .perform(
          post("/oauth2/token")
            .param("grant_type", "refresh_token")
            .param("refresh_token", issued.get("refresh_token").asString())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED),
        ).andReturn()
    result.response.status.assert
      .isEqualTo(400)
    result.response
      .getHeader("WWW-Authenticate")
      .assert
      .isNull()
    json(result)
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_client")
  }

  @Test
  fun `a refresh token that was never issued is refused with invalid_grant`() {
    json(driver.refresh("tgort_nothing-like-this", CLIENT_ID).andReturn())
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_grant")
  }

  @Test
  fun `a token endpoint failure is a JSON error body, never a redirect`() {
    val result = driver.refresh("tgort_bogus", CLIENT_ID).andReturn()
    result.response
      .getHeader("Location")
      .assert
      .isNull()
    result.response.contentType.assert
      .contains("application/json")
  }
}
