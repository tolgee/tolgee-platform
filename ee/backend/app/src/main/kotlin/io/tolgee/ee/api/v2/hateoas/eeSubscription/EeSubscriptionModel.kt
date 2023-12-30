package io.tolgee.ee.api.v2.hateoas.eeSubscription

import io.tolgee.constants.Feature
import io.tolgee.ee.data.SubscriptionStatus
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
