package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.oauth2.OAuth2Grant
import java.time.Instant
import java.util.Date

/**
 * Grants in the states the scheduled cleanup sorts between. Each test adds only the ones it asserts on, so the
 * expiries stay next to the assertion that depends on them.
 */
class OAuth2GrantCleanupTestData : BaseTestData() {
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
  ): OAuth2Grant =
    userAccountBuilder
      .addOAuth2Grant {
        clientId = "cleanup-test-client"
        this.codeHash = codeHash
        refreshTokenExpiresAt = refreshExpiresAt?.let { Date.from(it) }
        accessTokenExpiresAt = accessExpiresAt?.let { Date.from(it) }
        this.codeExpiresAt = codeExpiresAt?.let { Date.from(it) }
        this.consentExpiresAt = consentExpiresAt?.let { Date.from(it) }
      }.self
}
