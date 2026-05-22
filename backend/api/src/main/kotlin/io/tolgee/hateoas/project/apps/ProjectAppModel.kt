package io.tolgee.hateoas.project.apps

import io.tolgee.dtos.apps.AppManifestModules
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "projectApps", itemRelation = "projectApp")
open class ProjectAppModel(
  val id: Long,
  val manifestUrl: String,
  val appId: String,
  val name: String,
  val version: String,
  val baseUrl: String,
  val modules: AppManifestModules,
  val enabled: Boolean,
  val decoratorsUrl: String? = null,
) : RepresentationModel<ProjectAppModel>()
