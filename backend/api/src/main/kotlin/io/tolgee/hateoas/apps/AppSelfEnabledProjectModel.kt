package io.tolgee.hateoas.apps

import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "enabledProjects", itemRelation = "enabledProject")
open class AppSelfEnabledProjectModel(
  val id: Long,
  val name: String,
  val organization: AppSelfProjectOrganizationModel,
) : RepresentationModel<AppSelfEnabledProjectModel>()
