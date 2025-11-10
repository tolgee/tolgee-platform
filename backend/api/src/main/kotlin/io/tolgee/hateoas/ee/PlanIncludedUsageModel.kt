package io.tolgee.hateoas.ee

import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.io.Serializable

@Suppress("unused")
@Relation(collectionRelation = "prices", itemRelation = "prices")
open class PlanIncludedUsageModel(
  val seats: Long = -1L,
  var translations: Long = -1L,
  var mtCredits: Long = -1L,
  var keys: Long = -1L,
) : RepresentationModel<PlanIncludedUsageModel>(),
  Serializable
