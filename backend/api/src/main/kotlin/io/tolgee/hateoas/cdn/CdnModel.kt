package io.tolgee.hateoas.cdn

import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.io.Serializable

@Suppress("unused")
@Relation(collectionRelation = "activities", itemRelation = "activity")
class CdnModel(
  val id: Long,
  val name: String,
  val slug: String
) : RepresentationModel<CdnModel>(), Serializable
