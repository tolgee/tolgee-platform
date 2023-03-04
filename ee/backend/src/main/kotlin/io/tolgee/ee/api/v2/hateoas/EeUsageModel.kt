package io.tolgee.ee.api.v2.hateoas

import io.tolgee.constants.Feature
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.io.Serializable

@Suppress("unused")
@Relation(collectionRelation = "plans", itemRelation = "plan")
open class EeUsageModel(
  val enabledFeatures: Array<Feature>,
  val currentPeriodEnd: Long?,
  val cancelAtPeriodEnd: Boolean,
  val currentUserCount: Long,
  val userLimit: Long
) : RepresentationModel<EeUsageModel>(), Serializable
