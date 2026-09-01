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
  @Schema(
    description =
      "Scopes the current manifest requests that are not granted yet. Non-empty after the app " +
        "widens its manifest, until the organization's owner approves them.",
  )
  val pendingScopes: List<String> = emptyList(),
) : RepresentationModel<AppSelfInstallationModel>()
