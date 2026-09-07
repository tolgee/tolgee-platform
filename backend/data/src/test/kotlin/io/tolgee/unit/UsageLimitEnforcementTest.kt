package io.tolgee.unit

import io.tolgee.dtos.UsageLimits
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

/**
 * `isEnforced` decides whether a self-hosted instance blocks a write. It is deliberately narrower
 * than metering, which is a property of the plan's metric rather than of these numbers — see
 * [UsageLimits.metersWords].
 */
class UsageLimitEnforcementTest {
  @Test
  fun `an allowance with no ceiling is not enforced`() {
    limit(included = 100_000, limit = -1).isEnforced.assert.isFalse()
  }

  @Test
  fun `a zero ceiling is a real ceiling`() {
    limit(included = 0, limit = 0).isEnforced.assert.isTrue()
  }

  @Test
  fun `an allowance with a ceiling is enforced`() {
    limit(included = 100_000, limit = 100_000).isEnforced.assert.isTrue()
  }

  @Test
  fun `nothing is enforced when the metric is unlimited`() {
    limit(included = -1, limit = -1).isEnforced.assert.isFalse()
  }

  /**
   * A negotiated allowance has no number to block on. Enforcing it would compare every write
   * against -2 and refuse all of them.
   */
  @Test
  fun `a negotiated allowance is not enforced`() {
    limit(included = -2, limit = -2).isEnforced.assert.isFalse()
  }

  private fun limit(
    included: Long,
    limit: Long,
  ) = UsageLimits.Limit(included = included, limit = limit)
}
