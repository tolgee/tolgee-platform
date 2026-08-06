package io.tolgee.ee.unit.eeSubscription

import io.tolgee.ee.model.EeSubscription
import io.tolgee.ee.service.eeSubscription.SubscriptionFromModelAssigner
import io.tolgee.hateoas.ee.PlanPricesModel
import io.tolgee.hateoas.ee.SelfHostedEePlanModel
import io.tolgee.hateoas.ee.SelfHostedEeSubscriptionModel
import io.tolgee.hateoas.limits.LimitModel
import io.tolgee.hateoas.limits.SelfHostedUsageLimitsModel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.Date

class SubscriptionFromModelAssignerTest {
  private fun model(
    words: LimitModel,
    autoUpgradeEnabled: Boolean? = null,
  ) = SelfHostedEeSubscriptionModel(
    plan =
      SelfHostedEePlanModel(
        prices = PlanPricesModel(),
        free = false,
        nonCommercial = false,
        isPayAsYouGo = false,
      ),
    limits =
      SelfHostedUsageLimitsModel(
        keys = LimitModel(included = -1, limit = -1),
        seats = LimitModel(included = -1, limit = -1),
        mtCreditsInCents = LimitModel(included = -1, limit = -1),
        words = words,
        autoUpgradeEnabled = autoUpgradeEnabled,
      ),
  )

  @Test
  fun `assigns included words and words limit from the model`() {
    val subscription = EeSubscription()

    SubscriptionFromModelAssigner(
      subscription,
      model(words = LimitModel(included = 100000, limit = 100000)),
      Date(),
    ).assign()

    assertThat(subscription.includedWords).isEqualTo(100000L)
    assertThat(subscription.wordsLimit).isEqualTo(100000L)
  }

  @Test
  fun `no word limit on the model - assigns -1 (behaviour preserving)`() {
    val subscription = EeSubscription()

    SubscriptionFromModelAssigner(
      subscription,
      model(words = LimitModel(included = -1, limit = -1)),
      Date(),
    ).assign()

    assertThat(subscription.includedWords).isEqualTo(-1L)
    assertThat(subscription.wordsLimit).isEqualTo(-1L)
  }

  @Test
  fun `assigns autoUpgradeEnabled from the model`() {
    val subscription = EeSubscription()

    SubscriptionFromModelAssigner(
      subscription,
      model(words = LimitModel(included = -1, limit = -1), autoUpgradeEnabled = true),
      Date(),
    ).assign()

    assertThat(subscription.autoUpgradeEnabled).isTrue()
  }

  @Test
  fun `no autoUpgradeEnabled on the model (old server) - assigns false (blocking, behaviour preserving)`() {
    val subscription = EeSubscription()

    SubscriptionFromModelAssigner(
      subscription,
      model(words = LimitModel(included = -1, limit = -1), autoUpgradeEnabled = null),
      Date(),
    ).assign()

    assertThat(subscription.autoUpgradeEnabled).isFalse()
  }
}
