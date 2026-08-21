package io.tolgee.hateoas.project.apps

import io.tolgee.dtos.apps.AppManifestModulesDto
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "projectApps", itemRelation = "projectApp")
open class ProjectAppModel(
  val id: Long,
  val appId: String,
  val name: String,
  val version: String,
  val baseUrl: String,
  val modules: AppManifestModulesDto,
  val enabled: Boolean,
) : RepresentationModel<ProjectAppModel>()
