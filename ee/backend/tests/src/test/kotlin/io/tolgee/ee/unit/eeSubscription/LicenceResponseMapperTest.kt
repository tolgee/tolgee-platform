package io.tolgee.ee.unit.eeSubscription

import io.tolgee.api.SubscriptionStatus
import io.tolgee.component.HttpClient
import io.tolgee.constants.BillingPeriod
import io.tolgee.hateoas.ee.SelfHostedEeSubscriptionModel
import io.tolgee.publicBilling.MetricType
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

/**
 * An instance can be older than the licensing server it talks to, so the licence response may
 * carry enum values this build has never heard of. One of them must not fail the whole response —
 * that would break the licence check entirely, not just the field it appeared in.
 */
class LicenceResponseMapperTest {
  @Test
  fun `an unknown plan metric falls back to the metric this instance can enforce`() {
    subscription(""""metricType": "HOSTED_CHARACTERS"""")
      .plan.metricType.assert
      .isEqualTo(MetricType.KEYS_SEATS)
  }

  @Test
  fun `an unknown subscription status falls back to UNKNOWN`() {
    subscription(""""status": "SOMETHING_NEW"""", onSubscription = true)
      .status.assert
      .isEqualTo(SubscriptionStatus.UNKNOWN)
  }

  @Test
  fun `an unknown billing period falls back to MONTHLY`() {
    subscription(""""currentBillingPeriod": "WEEKLY"""", onSubscription = true)
      .currentBillingPeriod.assert
      .isEqualTo(BillingPeriod.MONTHLY)
  }

  @Test
  fun `an unknown currency falls back to null, because the field allows it`() {
    subscription(""""currency": "GBP"""", onSubscription = true)
      .currency.assert
      .isNull()
  }

  @Test
  fun `a known value still deserializes`() {
    subscription(""""metricType": "HOSTED_WORDS"""")
      .plan.metricType.assert
      .isEqualTo(MetricType.HOSTED_WORDS)
  }

  private fun subscription(
    extraField: String,
    onSubscription: Boolean = false,
  ): SelfHostedEeSubscriptionModel {
    val planFields = if (onSubscription) "" else "$extraField,"
    val subscriptionFields = if (onSubscription) "$extraField," else ""
    val json =
      """
      {
        $subscriptionFields
        "plan": {
          $planFields
          "name": "Plan",
          "prices": {},
          "free": false,
          "nonCommercial": false,
          "enabledFeatures": []
        },
        "limits": {
          "keys": { "included": -1, "limit": -1 },
          "seats": { "included": -1, "limit": -1 },
          "mtCreditsInCents": { "included": -1, "limit": -1 }
        }
      }
      """.trimIndent()
    return HttpClient.LENIENT_ENUM_RESPONSE_MAPPER.readValue(json, SelfHostedEeSubscriptionModel::class.java)
  }
}
