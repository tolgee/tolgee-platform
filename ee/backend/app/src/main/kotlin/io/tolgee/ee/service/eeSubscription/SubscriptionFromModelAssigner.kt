package io.tolgee.ee.service.eeSubscription

import io.tolgee.ee.model.EeSubscription
import io.tolgee.hateoas.ee.SelfHostedEeSubscriptionModel
import java.util.Date

class SubscriptionFromModelAssigner(
  private val subscription: EeSubscription,
  private val model: SelfHostedEeSubscriptionModel,
  private val currentDate: Date,
) {
  fun assign() {
    subscription.name = model.plan.name
    subscription.currentPeriodEnd = model.currentPeriodEnd?.let { Date(it) }
    subscription.enabledFeatures = model.plan.enabledFeatures
    subscription.nonCommercial = model.plan.nonCommercial
    subscription.includedKeys = model.plan.includedUsage.keys
    subscription.includedSeats = model.plan.includedUsage.seats
    subscription.isPayAsYouGo = model.plan.isPayAsYouGo
    subscription.keysLimit = model.limits.keys.limit
    subscription.seatsLimit = model.limits.seats.limit
    subscription.status = model.status
    subscription.lastValidCheck = currentDate
  }
}
