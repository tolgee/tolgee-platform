package io.tolgee.hateoas.project

import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Suppress("unused")
@Relation(collectionRelation = "transferOptions", itemRelation = "transferOption")
open class ProjectTransferOptionModel(
  val name: String,
  val slug: String,
  val id: Long,
) : RepresentationModel<ProjectTransferOptionModel>()
