package io.tolgee.development.testDataBuilder

import io.tolgee.model.UserAccount
import io.tolgee.model.enums.Scope
import io.tolgee.model.oauth2.OAuth2Grant
import io.tolgee.security.oauth2.OAuth2Constants

fun newOAuth2Grant(
  user: UserAccount,
  clientId: String = OAuth2Constants.BROWSER_EXTENSION_CLIENT_ID,
  scopes: List<String> = listOf(Scope.TRANSLATIONS_VIEW.value),
): OAuth2Grant =
  OAuth2Grant().apply {
    userAccount = user
    this.clientId = clientId
    redirectUri = "https://example.org/callback"
    codeChallenge = "test-challenge"
    requestedScopeValues = scopes
  }
