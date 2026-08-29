package io.tolgee.hateoas.apps

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "installations", itemRelation = "installation")
open class AppSelfInstallationModel(
  @Schema(description = "Id of the install the calling token belongs to")
  val id: Long,
  val appId: String,
  val name: String,
  val version: String,
  @Schema(description = "Permission scopes granted to the install at consent time")
  val scopes: List<String>,
) : RepresentationModel<AppSelfInstallationModel>()
