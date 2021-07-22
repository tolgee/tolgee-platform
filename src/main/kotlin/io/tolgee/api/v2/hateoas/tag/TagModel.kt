package io.tolgee.api.v2.hateoas.invitation

import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "tags", itemRelation = "tag")
open class TagModel(
  val id: Long,
  val name: String
) : RepresentationModel<TagModel>()
