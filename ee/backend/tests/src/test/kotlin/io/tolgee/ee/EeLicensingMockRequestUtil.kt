package io.tolgee.ee

import io.tolgee.api.SubscriptionStatus
import io.tolgee.constants.Feature
import io.tolgee.fixtures.HttpClientMocker
import io.tolgee.hateoas.ee.PlanIncludedUsageModel
import io.tolgee.hateoas.ee.PlanPricesModel
import io.tolgee.hateoas.ee.PrepareSetEeLicenceKeyModel
import io.tolgee.hateoas.ee.SelfHostedEePlanModel
import io.tolgee.hateoas.ee.SelfHostedEeSubscriptionModel
import io.tolgee.hateoas.ee.uasge.proportional.AverageProportionalUsageItemModel
import io.tolgee.hateoas.ee.uasge.proportional.UsageModel
import io.tolgee.hateoas.limits.LimitModel
import io.tolgee.hateoas.limits.SelfHostedUsageLimitsModel
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class EeLicensingMockRequestUtil(
  private val restTemplate: RestTemplate,
) {
  fun mock(mock: HttpClientMocker.() -> Unit) {
    val mocker = HttpClientMocker(restTemplate)
    mock(mocker)
  }

  final val mockedPlan =
    SelfHostedEePlanModel(
      id = 19919,
      name = "Tolgee",
      public = true,
      enabledFeatures = arrayOf(Feature.PREMIUM_SUPPORT),
      prices =
        PlanPricesModel(
          perSeat = 20.toBigDecimal(),
          subscriptionMonthly = 200.toBigDecimal(),
        ),
      includedUsage =
        PlanIncludedUsageModel(
          seats = 10,
          keys = 10,
        ),
      free = false,
      nonCommercial = false,
      isPayAsYouGo = false,
    )

  final val mockedSubscriptionResponse =
    SelfHostedEeSubscriptionModel(
      id = 19919,
      currentPeriodEnd = 1624313600000,
      createdAt = 1624313600000,
      plan = mockedPlan,
      status = SubscriptionStatus.ACTIVE,
      licenseKey = "mocked_license_key",
      estimatedCosts = 200.toBigDecimal(),
      currentPeriodStart = 1622313600000,
      limits =
        SelfHostedUsageLimitsModel(
          keys =
            LimitModel(
              mockedPlan.includedUsage.keys,
              mockedPlan.includedUsage.keys,
            ),
          seats =
            LimitModel(
              mockedPlan.includedUsage.seats,
              mockedPlan.includedUsage.seats,
            ),
          mtCreditsInCents =
            LimitModel(
              mockedPlan.includedUsage.mtCredits,
              mockedPlan.includedUsage.mtCredits,
            ),
        ),
    )

  final val mockedPrepareResponse =
    PrepareSetEeLicenceKeyModel().apply {
      plan = mockedPlan
      usage =
        UsageModel(
          subscriptionPrice = 200.toBigDecimal(),
          seats =
            AverageProportionalUsageItemModel(
              total = 250.toBigDecimal(),
              usedQuantity = 2.toBigDecimal(),
              unusedQuantity = 10.toBigDecimal(),
              usedQuantityOverPlan = 0.toBigDecimal(),
            ),
          total = 250.toBigDecimal(),
          translations =
            AverageProportionalUsageItemModel(
              total = 0.toBigDecimal(),
              unusedQuantity = 0.toBigDecimal(),
              usedQuantity = 0.toBigDecimal(),
              usedQuantityOverPlan = 0.toBigDecimal(),
            ),
          credits = null,
        )
    }
}
