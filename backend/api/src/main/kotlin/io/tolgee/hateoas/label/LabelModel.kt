package io.tolgee.hateoas.label

import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "labels", itemRelation = "label")
open class LabelModel(
  val id: Long,
  val name: String,
  val color: String,
  val description: String?,
) : RepresentationModel<LabelModel>()
