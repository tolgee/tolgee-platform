package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.model.oauth2.OAuth2Grant
import io.tolgee.model.oauth2.OAuth2SupersededRefreshToken

/**
 * Takes the grant entity rather than its builder, so a grant an HTTP flow created at runtime can be given history
 * too.
 */
class OAuth2SupersededRefreshTokenBuilder(
  grant: OAuth2Grant,
) : EntityDataBuilder<OAuth2SupersededRefreshToken, OAuth2SupersededRefreshTokenBuilder> {
  override var self: OAuth2SupersededRefreshToken =
    OAuth2SupersededRefreshToken().apply {
      this.grant = grant
    }
}
