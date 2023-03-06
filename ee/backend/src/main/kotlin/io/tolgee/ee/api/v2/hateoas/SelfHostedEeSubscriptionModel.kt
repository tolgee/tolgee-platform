package io.tolgee.ee.api.v2.hateoas

import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.io.Serializable
import java.util.*

@Suppress("unused")
@Relation(collectionRelation = "subscriptions", itemRelation = "subscription")
open class SelfHostedEeSubscriptionModel(
  val id: Long = 0,
  val currentPeriodEnd: Long? = null,
  val cancelAtPeriodEnd: Boolean = false,
  val createdAt: Date = Date(),
  val plan: SelfHostedEePlanModel = SelfHostedEePlanModel()
) : RepresentationModel<SelfHostedEeSubscriptionModel>(), Serializable
