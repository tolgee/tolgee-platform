package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.OAuth2GrantBuilder
import io.tolgee.model.oauth2.OAuth2Grant
import io.tolgee.model.oauth2.OAuth2SupersededRefreshToken
import java.time.Instant
import java.util.Date
import java.util.IdentityHashMap

/**
 * Grants in the states the scheduled cleanup sorts between. Each test adds only the ones it asserts on, so the
 * expiries stay next to the assertion that depends on them.
 */
class OAuth2GrantCleanupTestData : BaseTestData() {
  // Identity, not equals: a saved entity's hashCode changes with the id it is given, which would lose the entry.
  private val grantBuilders = IdentityHashMap<OAuth2Grant, OAuth2GrantBuilder>()

  /**
   * A grant carrying whichever expiries the caller sets. [codeHash] decides which reaper owns it: a grant holding a
   * code is past the pending-consent stage, so only the retention window can remove it.
   */
  fun addGrant(
    refreshExpiresAt: Instant? = null,
    accessExpiresAt: Instant? = null,
    codeExpiresAt: Instant? = null,
    consentExpiresAt: Instant? = null,
    codeHash: String? = null,
    clientId: String = DEFAULT_CLIENT_ID,
  ): OAuth2Grant {
    val builder =
      userAccountBuilder.addOAuth2Grant {
        this.clientId = clientId
        this.codeHash = codeHash
        refreshTokenExpiresAt = refreshExpiresAt?.let { Date.from(it) }
        accessTokenExpiresAt = accessExpiresAt?.let { Date.from(it) }
        this.codeExpiresAt = codeExpiresAt?.let { Date.from(it) }
        this.consentExpiresAt = consentExpiresAt?.let { Date.from(it) }
      }
    grantBuilders[builder.self] = builder
    return builder.self
  }

  fun addSupersededRefreshToken(
    grant: OAuth2Grant,
    tokenHash: String,
    supersededAt: Instant,
  ): OAuth2SupersededRefreshToken {
    val grantBuilder = grantBuilders[grant] ?: throw IllegalArgumentException("the grant was not built here")
    return grantBuilder
      .addSupersededRefreshToken {
        this.tokenHash = tokenHash
        this.supersededAt = Date.from(supersededAt)
      }.self
  }

  companion object {
    const val DEFAULT_CLIENT_ID = "cleanup-test-client"
  }
}
