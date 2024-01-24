package io.tolgee.hateoas.ee.eeSubscription

import io.tolgee.api.SubscriptionStatus
import io.tolgee.constants.Feature
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.io.Serializable
import java.util.*

@Suppress("unused")
@Relation(collectionRelation = "plans", itemRelation = "plan")
open class EeSubscriptionModel(
  val name: String,
  val licenseKey: String,
  val enabledFeatures: Array<Feature>,
  val currentPeriodEnd: Long?,
  val cancelAtPeriodEnd: Boolean,
  val currentUserCount: Long,
  val status: SubscriptionStatus,
  var lastValidCheck: Date?,
) : RepresentationModel<EeSubscriptionModel>(), Serializable
