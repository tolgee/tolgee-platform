package io.tolgee.ee

import io.tolgee.constants.Feature
import io.tolgee.ee.api.v2.hateoas.PlanPricesModel
import io.tolgee.ee.api.v2.hateoas.PrepareSetEeLicenceKeyModel
import io.tolgee.ee.api.v2.hateoas.SelfHostedEePlanModel
import io.tolgee.ee.api.v2.hateoas.SelfHostedEeSubscriptionModel
import io.tolgee.ee.api.v2.hateoas.uasge.AverageProportionalUsageItemModel
import io.tolgee.ee.api.v2.hateoas.uasge.UsageModel
import io.tolgee.ee.data.SubscriptionStatus
import io.tolgee.fixtures.HttpClientMocker
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class EeLicensingMockRequestUtil {
  @Autowired
  lateinit var restTemplate: RestTemplate

  fun mock(mock: HttpClientMocker.() -> Unit) {
    val mocker = HttpClientMocker(restTemplate)
    mock(mocker)
  }

  final val mockedPlan = SelfHostedEePlanModel(
    id = 19919,
    name = "Tolgee",
    public = true,
    enabledFeatures = arrayOf(Feature.PREMIUM_SUPPORT),
    prices = PlanPricesModel(
      perSeat = 20.toBigDecimal(),
      subscriptionMonthly = 200.toBigDecimal(),
    ),
  )

  final val mockedSubscriptionResponse = SelfHostedEeSubscriptionModel(
    id = 19919,
    currentPeriodEnd = 1624313600000,
    createdAt = 1624313600000,
    plan = mockedPlan,
    status = SubscriptionStatus.ACTIVE,
    licenseKey = "mocked_license_key",
    estimatedCosts = 200.toBigDecimal(),
    currentPeriodStart = 1622313600000,
  )

  final val mockedPrepareResponse = PrepareSetEeLicenceKeyModel().apply {
    plan = mockedPlan
    usage = UsageModel(
      subscriptionPrice = 200.toBigDecimal(),
      seats =
      AverageProportionalUsageItemModel(
        total = 250.toBigDecimal(),
        usedQuantity = 2.toBigDecimal(),
        unusedQuantity = 10.toBigDecimal(),
        usedQuantityOverPlan = 0.toBigDecimal()
      ),
      total = 250.toBigDecimal(),
      translations = AverageProportionalUsageItemModel(
        total = 0.toBigDecimal(),
        unusedQuantity = 0.toBigDecimal(),
        usedQuantity = 0.toBigDecimal(),
        usedQuantityOverPlan = 0.toBigDecimal()
      ),
      credits = null
    )
  }
}
