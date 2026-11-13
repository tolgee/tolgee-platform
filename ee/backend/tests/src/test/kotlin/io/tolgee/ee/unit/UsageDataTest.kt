package io.tolgee.ee.unit

import io.tolgee.ee.data.UsageData
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class UsageDataTest {
  @Test
  fun `the upgrade proration counts into the total`() {
    val usage =
      UsageData(
        seatsUsage = emptyList(),
        translationsUsage = emptyList(),
        keysUsage = emptyList(),
        creditsUsage = null,
        subscriptionPrice = null,
        appliedStripeCredits = null,
        upgradeProrationTotal = BigDecimal("216.99"),
      )

    assertThat(usage.total).isEqualByComparingTo(BigDecimal("216.99"))
  }

  @Test
  fun `without an upgrade proration the total is unchanged`() {
    val usage =
      UsageData(
        seatsUsage = emptyList(),
        translationsUsage = emptyList(),
        keysUsage = emptyList(),
        creditsUsage = null,
        subscriptionPrice = BigDecimal("20.00"),
        appliedStripeCredits = null,
      )

    assertThat(usage.total).isEqualByComparingTo(BigDecimal("20.00"))
  }
}
