package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.OAuth2SupersededRefreshTokenBuilder
import io.tolgee.model.Project
import io.tolgee.model.oauth2.OAuth2Grant
import io.tolgee.model.oauth2.OAuth2SupersededRefreshToken
import java.time.Instant
import java.util.Date

/**
 * Shared by the token-flow suites. Beside the default user's project it holds two projects that user is not a member
 * of: [otherProject], which they cannot reach at all, and [publicProject], which they reach only through the
 * community floor — so a token can be shown to be narrowed by the user's own access rather than by its scopes alone.
 */
class OAuth2FlowTestData : BaseTestData() {
  val otherUserBuilder = root.addUserAccount { username = "oauth_other_user" }

  val otherUser = otherUserBuilder.self

  lateinit var otherProject: Project

  lateinit var publicProject: Project

  /**
   * Filler rotation history for a grant an HTTP flow created at runtime. That grant is not part of this graph, so
   * the rows come back unsaved and the caller persists them.
   */
  fun refreshHistoryFor(
    grant: OAuth2Grant,
    count: Int,
    from: Instant = Instant.now(),
  ): List<OAuth2SupersededRefreshToken> =
    (1..count).map { index ->
      OAuth2SupersededRefreshTokenBuilder(grant).self {
        tokenHash = "filler-$index"
        supersededAt = Date.from(from.minusSeconds(index.toLong()))
      }
    }

  init {
    otherProject =
      root
        .addProject {
          name = "foreign_project"
          organizationOwner = otherUserBuilder.defaultOrganizationBuilder.self
        }.self

    publicProject =
      root
        .addProject {
          name = "public_project"
          organizationOwner = otherUserBuilder.defaultOrganizationBuilder.self
          public = true
        }.build buildPublic@{
          addLanguage {
            name = "English"
            tag = "en"
            originalName = "English"
            this@buildPublic.self.baseLanguage = this
          }
        }.self
  }
}
