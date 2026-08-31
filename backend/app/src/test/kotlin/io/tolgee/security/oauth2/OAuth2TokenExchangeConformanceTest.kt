package io.tolgee.security.oauth2

import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import java.time.Duration

/**
 * Redeeming an authorization code at `/oauth2/token`, including every way the exchange is refused.
 */
class OAuth2TokenExchangeConformanceTest : AbstractOAuth2ConformanceTest() {
  @Test
  fun `a successful code exchange answers with the RFC 6749 token response`() {
    val result = tokenResult()
    val response = result.response
    response.getHeader("Cache-Control").assert.contains("no-store")
    val body = json(result)
    body
      .get("token_type")
      .asString()
      .assert
      .isEqualTo("Bearer")
    body
      .get("access_token")
      .asString()
      .assert
      .isNotBlank()
    body
      .get("refresh_token")
      .asString()
      .assert
      .isNotBlank()
    body
      .get("scope")
      .asString()
      .assert
      .isEqualTo("translations.view")
    body
      .get("expires_in")
      .asLong()
      .assert
      .isEqualTo(oauth2.accessTokenValidityMinutes * 60)
  }

  @Test
  fun `an authorization code is single-use and replaying it revokes the tokens it already issued`() {
    val pending = driver.startPendingConsent(jwt(), CLIENT_ID, REDIRECT)
    val code = driver.queryParam(driver.consentRedirect(pending), "code")!!
    val first = json(driver.exchangeCode(code, CLIENT_ID, REDIRECT, pending.verifier).andReturn())
    val replay = driver.exchangeCode(code, CLIENT_ID, REDIRECT, pending.verifier).andReturn()

    json(replay)
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_grant")
    driver.refresh(first.get("refresh_token").asString(), CLIENT_ID).andReturn().let {
      json(it)
        .get("error")
        .asString()
        .assert
        .isEqualTo("invalid_grant")
    }
  }

  @Test
  fun `a code issued to one client cannot be exchanged by another client`() {
    val pending = driver.startPendingConsent(jwt(), CLIENT_ID, REDIRECT)
    val code = driver.queryParam(driver.consentRedirect(pending), "code")!!
    val result = driver.exchangeCode(code, OTHER_CLIENT_ID, OTHER_REDIRECT, pending.verifier).andReturn()
    json(result)
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_grant")

    // A code the wrong client has touched is treated as stolen, so the legitimate client cannot redeem it either.
    json(driver.exchangeCode(code, CLIENT_ID, REDIRECT, pending.verifier).andReturn())
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_grant")
  }

  @Test
  fun `a code cannot be exchanged with a redirect_uri other than the one it was issued for`() {
    val pending = driver.startPendingConsent(jwt(), CLIENT_ID, REDIRECT)
    val code = driver.queryParam(driver.consentRedirect(pending), "code")!!
    val result = driver.exchangeCode(code, CLIENT_ID, "https://extension.test/other", pending.verifier).andReturn()
    json(result)
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_grant")
  }

  @Test
  fun `a code exchange without a code_verifier is refused`() {
    val pending = driver.startPendingConsent(jwt(), CLIENT_ID, REDIRECT)
    val code = driver.queryParam(driver.consentRedirect(pending), "code")!!
    val result = driver.exchangeCode(code, CLIENT_ID, REDIRECT, "").andReturn()
    json(result)
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_request")
  }

  @Test
  fun `a code exchange with a malformed code_verifier is refused`() {
    val pending = driver.startPendingConsent(jwt(), CLIENT_ID, REDIRECT)
    val code = driver.queryParam(driver.consentRedirect(pending), "code")!!
    val tooShortForRfc7636 = "short"
    val result = driver.exchangeCode(code, CLIENT_ID, REDIRECT, tooShortForRfc7636).andReturn()
    json(result)
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_request")
  }

  @Test
  fun `a code exchange with a well-formed but wrong code_verifier is refused`() {
    val pending = driver.startPendingConsent(jwt(), CLIENT_ID, REDIRECT)
    val code = driver.queryParam(driver.consentRedirect(pending), "code")!!
    val wrong = OAuth2FlowDriver.randomVerifier()
    wrong.assert.isNotEqualTo(pending.verifier)
    val result = driver.exchangeCode(code, CLIENT_ID, REDIRECT, wrong).andReturn()
    json(result)
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_grant")
  }

  @Test
  fun `a pending consent that went stale can no longer be approved`() {
    val pending = driver.startPendingConsent(jwt(), CLIENT_ID, REDIRECT)
    currentDateProvider.move(Duration.ofSeconds(oauth2.consentValiditySeconds + 60))
    driver
      .submitConsent(pending)
      .andReturn()
      .response.status.assert
      .isEqualTo(404)
  }

  @Test
  fun `an expired authorization code is refused`() {
    val pending = driver.startPendingConsent(jwt(), CLIENT_ID, REDIRECT)
    val code = driver.queryParam(driver.consentRedirect(pending), "code")!!
    currentDateProvider.move(Duration.ofSeconds(oauth2.authorizationCodeValiditySeconds + 60))
    val result = driver.exchangeCode(code, CLIENT_ID, REDIRECT, pending.verifier).andReturn()
    json(result)
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_grant")
  }

  @Test
  fun `the token endpoint refuses credentials sent in the query string`() {
    // OAuth 2.1 §3.2.2: the request is a form body. A code and its verifier in the request line end up in proxy
    // and access logs, and together they are a complete grant.
    val pending = driver.startPendingConsent(jwt(), CLIENT_ID, REDIRECT)
    val code = driver.queryParam(driver.consentRedirect(pending, projectId = null), "code")!!
    val result =
      mvc
        .perform(
          post("/oauth2/token?grant_type=authorization_code&client_id=$CLIENT_ID&code=$code")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED),
        ).andReturn()

    json(result)
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_request")
  }

  @Test
  fun `a parameter sent without a value counts as omitted`() {
    // OAuth 2.1 §3.2: "Parameters sent without a value MUST be treated as if they were omitted."
    val result =
      mvc
        .perform(
          post("/oauth2/token")
            .param("grant_type", "")
            .param("client_id", CLIENT_ID)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED),
        ).andReturn()

    json(result)
      .get("error")
      .asString()
      .assert
      .isEqualTo("invalid_request")
  }

  @Test
  fun `an authorization error redirect carries the RFC 9207 iss`() {
    val redirect = errorRedirect(mapOf("response_type" to "token"))
    redirect.assert.contains("error=unsupported_response_type")
    redirect.assert.contains("iss=")
  }

  @Test
  fun `grant types the server does not offer to public clients are refused`() {
    listOf("client_credentials", "password", "implicit").forEach { grant ->
      val result =
        mvc
          .perform(
            post("/oauth2/token")
              .param("grant_type", grant)
              .param("client_id", CLIENT_ID)
              .contentType(MediaType.APPLICATION_FORM_URLENCODED),
          ).andReturn()
      json(result)
        .get("error")
        .asString()
        .assert
        .isEqualTo("unsupported_grant_type")
    }
  }
}
