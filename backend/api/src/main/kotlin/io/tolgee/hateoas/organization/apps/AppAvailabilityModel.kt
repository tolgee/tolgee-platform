package io.tolgee.hateoas.organization.apps

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

/** An app's availability set as a server admin manages it. */
@Relation(itemRelation = "appAvailability")
open class AppAvailabilityModel(
  @Schema(description = "Whether the app is offered to every organization on the server")
  val availableToAll: Boolean,
  @Schema(description = "The organizations the app is specifically offered to, besides the owner")
  val organizations: List<InstallingOrganizationModel>,
) : RepresentationModel<AppAvailabilityModel>()
