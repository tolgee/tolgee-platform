package io.tolgee.hateoas.organization.apps

import io.tolgee.dtos.apps.AppManifestModules
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "appInstalls", itemRelation = "appInstall")
open class AppInstallModel(
  val id: Long,
  val manifestUrl: String,
  val appId: String,
  val name: String,
  val version: String,
  val baseUrl: String,
  val modules: AppManifestModules,
) : RepresentationModel<AppInstallModel>()
