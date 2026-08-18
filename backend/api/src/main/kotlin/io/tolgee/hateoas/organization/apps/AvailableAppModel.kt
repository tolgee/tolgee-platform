package io.tolgee.hateoas.organization.apps

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

/**
 * An app a server admin has offered to every organization, as a non-owner organization sees it in
 * the "Available on this server" list — enough to install it, no owner-only fields.
 */
@Relation(collectionRelation = "availableApps", itemRelation = "availableApp")
open class AvailableAppModel(
  @Schema(description = "Id of the registered app")
  val id: Long,
  @Schema(description = "The `id` declared in the app's manifest, unique across the server")
  val appId: String,
  val name: String,
  val baseUrl: String,
  @Schema(description = "Manifest URL to install the app from")
  val manifestUrl: String,
) : RepresentationModel<AvailableAppModel>()
