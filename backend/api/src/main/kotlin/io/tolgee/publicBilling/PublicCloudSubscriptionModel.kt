package io.tolgee.publicBilling

import io.tolgee.api.SubscriptionStatus
import io.tolgee.constants.BillingPeriod

interface PublicCloudSubscriptionModel {
  val currentBillingPeriod: BillingPeriod?
  val cancelAtPeriodEnd: Boolean
  val trialEnd: Long?
  val status: SubscriptionStatus
  val plan: PublicCloudPlanModel
}
