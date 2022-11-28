package io.tolgee.ee.api.v2.hateoas

import io.tolgee.constants.BillingPeriod
import io.tolgee.ee.data.SubscriptionStatus
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.io.Serializable
import java.math.BigDecimal

@Suppress("unused")
@Relation(collectionRelation = "subscriptions", itemRelation = "subscription")
open class SelfHostedEeSubscriptionModel(
  val id: Long = 0,
  val currentPeriodStart: Long? = null,
  val currentPeriodEnd: Long? = null,
  val currentBillingPeriod: BillingPeriod = BillingPeriod.MONTHLY,
  val createdAt: Long = 0,
  val plan: SelfHostedEePlanModel = SelfHostedEePlanModel(prices = PlanPricesModel()),
  val status: SubscriptionStatus = SubscriptionStatus.ACTIVE,
  val licenseKey: String? = null,
  val estimatedCosts: BigDecimal? = 0.toBigDecimal(),
) : RepresentationModel<SelfHostedEeSubscriptionModel>(), Serializable
