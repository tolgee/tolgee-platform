package io.tolgee.hateoas.ee

import io.tolgee.api.SubscriptionStatus
import io.tolgee.constants.BillingPeriod
import io.tolgee.hateoas.limits.SelfHostedUsageLimitsModel
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.io.Serializable
import java.math.BigDecimal

/**
 * This model is used by Tolgee Cloud to present data about
 * the Self-hosted subscription.
 */
@Suppress("unused")
@Relation(collectionRelation = "subscriptions", itemRelation = "subscription")
open class SelfHostedEeSubscriptionModel(
  val id: Long = 0,
  val currentPeriodStart: Long? = null,
  val currentPeriodEnd: Long? = null,
  val currentBillingPeriod: BillingPeriod = BillingPeriod.MONTHLY,
  val createdAt: Long = 0,
  open val plan: SelfHostedEePlanModel,
  val status: SubscriptionStatus = SubscriptionStatus.ACTIVE,
  val licenseKey: String? = null,
  val estimatedCosts: BigDecimal? = 0.toBigDecimal(),
  val limits: SelfHostedUsageLimitsModel,
) : RepresentationModel<SelfHostedEeSubscriptionModel>(), Serializable
