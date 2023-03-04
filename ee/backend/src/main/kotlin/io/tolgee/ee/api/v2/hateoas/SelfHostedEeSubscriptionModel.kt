package io.tolgee.ee.api.v2.hateoas

import io.tolgee.constants.Feature
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.io.Serializable

@Suppress("unused")
@Relation(collectionRelation = "plans", itemRelation = "plan")
open class SelfHostedEeSubscriptionModel(
  val enabledFeatures: Array<Feature> = arrayOf(),
  val currentPeriodEnd: Long? = null,
  val cancelAtPeriodEnd: Boolean = false,
  val plan: SelfHostedEePlanModel
) : RepresentationModel<SelfHostedEeSubscriptionModel>(), Serializable
