package io.tolgee.hateoas.organization.apps

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.dtos.apps.AppManifestModulesDto
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(itemRelation = "appManifestPreview")
open class AppManifestPreviewModel(
  val appId: String,
  val name: String,
  val version: String,
  val baseUrl: String,
  val icon: String? = null,
  val modules: AppManifestModulesDto,
  val requestedScopes: List<String>,
  @Schema(
    description =
      "SHA-256 hex of the manifest as fetched. Pass it back on register/install so the server can " +
        "reject a manifest whose bytes changed since this consent preview.",
  )
  val manifestHash: String,
) : RepresentationModel<AppManifestPreviewModel>()
