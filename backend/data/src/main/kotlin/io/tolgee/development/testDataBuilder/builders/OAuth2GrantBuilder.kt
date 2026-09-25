package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.development.testDataBuilder.FT
import io.tolgee.model.enums.Scope
import io.tolgee.model.oauth2.OAuth2Grant
import io.tolgee.model.oauth2.OAuth2SupersededRefreshToken
import io.tolgee.security.oauth2.OAuth2Constants

class OAuth2GrantBuilder(
  val userAccountBuilder: UserAccountBuilder,
) : EntityDataBuilder<OAuth2Grant, OAuth2GrantBuilder> {
  override var self: OAuth2Grant =
    OAuth2Grant().apply {
      userAccount = userAccountBuilder.self
      clientId = OAuth2Constants.BROWSER_EXTENSION_CLIENT_ID
      redirectUri = "https://example.org/callback"
      codeChallenge = "test-challenge"
      requestedScopeValues = listOf(Scope.TRANSLATIONS_VIEW.value)
    }

  fun addSupersededRefreshToken(ft: FT<OAuth2SupersededRefreshToken>): OAuth2SupersededRefreshTokenBuilder {
    val builder = OAuth2SupersededRefreshTokenBuilder(self)
    ft(builder.self)
    self.supersededRefreshTokens.add(builder.self)
    return builder
  }
}
