package io.tolgee.security.oauth2

import io.tolgee.constants.Message
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

/**
 * The consent submission: who may resolve a pending authorization, what they may approve, and what a denial does.
 */
class OAuth2ConsentConformanceTest : AbstractOAuth2ConformanceTest() {
  @Test
  fun `denying consent redirects to the client with access_denied and leaves nothing to approve later`() {
    val pending = driver.startPendingConsent(jwt(), CLIENT_ID, REDIRECT)
    val redirect = driver.consentRedirect(pending, approvedScopes = emptyList())
    redirect.assert.contains("error=access_denied")
    redirect.assert.contains("state=client-state")

    driver
      .submitConsent(pending)
      .andReturn()
      .response.status.assert
      .isEqualTo(404)
  }

  @Test
  fun `consent cannot approve a scope that the authorization request did not ask for`() {
    val pending = driver.startPendingConsent(jwt(), CLIENT_ID, REDIRECT, scope = "translations.view")
    driver
      .consentRedirect(pending, approvedScopes = listOf("translations.view", "keys.edit"))
      .assert
      .contains("error=invalid_scope")

    driver
      .submitConsent(pending)
      .andReturn()
      .response.status.assert
      .isEqualTo(404)
  }

  @Test
  fun `consent for a state that matches no pending authorization is refused`() {
    val pending = driver.startPendingConsent(jwt(), CLIENT_ID, REDIRECT).copy(state = "not-a-real-state")
    driver
      .submitConsent(pending)
      .andReturn()
      .response.status.assert
      .isEqualTo(404)
  }

  @Test
  fun `approving needs a super token, so a lifted session cannot mint a credential on its own`() {
    val pending = driver.startPendingConsent(jwt(), CLIENT_ID, REDIRECT)
    val ordinary = pending.copy(jwt = jwtService.emitToken(testData.user.id))

    driver
      .submitConsent(ordinary)
      .andReturn()
      .response.status.assert
      .isEqualTo(403)
  }

  @Test
  fun `consent from another user cannot approve someone else's pending authorization`() {
    val pending = driver.startPendingConsent(jwt(), CLIENT_ID, REDIRECT)
    val theirs = pending.copy(jwt = jwtService.emitToken(testData.otherUser.id, isSuper = true))
    driver
      .submitConsent(theirs)
      .andReturn()
      .response.status.assert
      .isEqualTo(404)
  }

  @Test
  fun `approving SINGLE_PROJECT without naming a project is refused`() {
    val pending = driver.startPendingConsent(jwt(), CLIENT_ID, REDIRECT)
    val result =
      driver.submitConsentBody(
        pending,
        """{"state":"${pending.state}","scopes":["translations.view"],"projectScope":"SINGLE_PROJECT"}""",
      )

    result.response.status.assert
      .isEqualTo(400)
    result.response.contentAsString.assert
      .contains(Message.OAUTH_PROJECT_REQUIRED.code)
    driver.consentRedirect(pending, projectId = null).assert.contains("code=")
  }

  @Test
  fun `approving without saying which projects at all is refused`() {
    val pending = driver.startPendingConsent(jwt(), CLIENT_ID, REDIRECT)
    val result = driver.submitConsentBody(pending, """{"state":"${pending.state}","scopes":["translations.view"]}""")

    result.response.status.assert
      .isEqualTo(400)
    result.response.contentAsString.assert
      .contains(Message.OAUTH_PROJECT_SCOPE_REQUIRED.code)
  }

  @Test
  fun `denying needs no project scope at all`() {
    val pending = driver.startPendingConsent(jwt(), CLIENT_ID, REDIRECT)
    val result = driver.submitConsentBody(pending, """{"state":"${pending.state}","scopes":[]}""")

    result.response.status.assert
      .isEqualTo(200)
    result.response.contentAsString.assert
      .contains("access_denied")
  }

  @Test
  fun `a consent state cannot be reused once it has been granted`() {
    val pending = driver.startPendingConsent(jwt(), CLIENT_ID, REDIRECT)
    driver.consentRedirect(pending).assert.contains("code=")
    driver
      .submitConsent(pending)
      .andReturn()
      .response.status.assert
      .isEqualTo(404)
  }
}
