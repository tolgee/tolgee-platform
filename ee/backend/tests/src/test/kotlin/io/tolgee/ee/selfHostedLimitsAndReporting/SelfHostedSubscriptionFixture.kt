package io.tolgee.ee.selfHostedLimitsAndReporting

import io.tolgee.api.SubscriptionStatus
import io.tolgee.constants.Feature
import io.tolgee.ee.model.EeSubscription
import io.tolgee.publicBilling.MetricType
import java.util.Date

object SelfHostedSubscriptionFixture {
  fun activeSubscription(build: EeSubscription.() -> Unit = {}): EeSubscription =
    EeSubscription().apply {
      licenseKey = "mock"
      name = "Plaaan"
      status = SubscriptionStatus.ACTIVE
      currentPeriodEnd = Date()
      enabledFeatures = Feature.entries.toTypedArray()
      lastValidCheck = Date()
      includedKeys = 1
      includedSeats = 1
      keysLimit = 1
      seatsLimit = 1
      isPayAsYouGo = false
      build(this)
    }

  /** Word metering is gated on the plan's metric, not on the allowance being a positive number. */
  fun wordPlan(build: EeSubscription.() -> Unit = {}): EeSubscription =
    activeSubscription {
      metricType = MetricType.HOSTED_WORDS
      build(this)
    }
}
