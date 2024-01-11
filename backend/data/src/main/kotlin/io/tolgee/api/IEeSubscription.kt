package io.tolgee.api

import io.tolgee.constants.Feature
import java.util.*

interface IEeSubscription {
  val id: Int
  var currentPeriodEnd: Date?
  var cancelAtPeriodEnd: Boolean
  var enabledFeatures: Array<Feature>
  var status: SubscriptionStatus
  var lastValidCheck: Date?
}
