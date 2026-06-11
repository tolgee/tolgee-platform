package io.tolgee.hateoas.organization.apps

import io.tolgee.dtos.apps.AppManifestModules
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(itemRelation = "appManifestPreview")
open class AppManifestPreviewModel(
  val appId: String,
  val name: String,
  val version: String,
  val baseUrl: String,
  val modules: AppManifestModules,
  val requestedScopes: List<String>,
  val requestedWebhookEvents: List<String>,
) : RepresentationModel<AppManifestPreviewModel>()
