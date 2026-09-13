package io.tolgee.security.oauth2

import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andIsUnauthorized
import io.tolgee.model.enums.Scope
import io.tolgee.security.OAUTH_ACCESS_TOKEN_PREFIX
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

/**
 * What an issued OAuth access token reaches on the REST API, and what stops reaching it once the grant is revoked.
 */
class OAuth2AccessTokenFlowTest : AbstractOAuth2FlowTest() {
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
    val initialScopes = initial.maxGrantedScopeValues
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
    refreshedStored.maxGrantedScopeValues.assert.isEqualTo(initialScopes)

    apiRequest(firstAccessToken).andIsUnauthorized
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
    grantsForUser().assert.isZero()
  }

  @Test
  fun `a stored scope value that no longer names a real scope is dropped rather than failing the request`() {
    val accessToken = accessToken(projectId = testData.project.id)
    val grant = stored(accessToken)
    // Written straight to the column: the property setter maps wire values to Scope names and would drop this first.
    grant.issuedTokenScopes = grant.issuedTokenScopes + " TRANSLATIONS_RETIRED_SCOPE"
    repository.save(grant)

    apiRequest(accessToken).andIsOk
    stored(accessToken).issuedTokenScopeValues.assert.containsExactly(Scope.TRANSLATIONS_VIEW.value)
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
}
