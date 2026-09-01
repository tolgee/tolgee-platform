package io.tolgee.hateoas.organization.apps

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

/** An app the organization registered, as its owner sees it. */
@Relation(collectionRelation = "ownedApps", itemRelation = "ownedApp")
open class OwnedAppModel(
  val id: Long,
  @Schema(description = "The `id` declared in the app's manifest, unique across the server")
  val appId: String,
  val name: String,
  val manifestUrl: String,
  val baseUrl: String,
  @Schema(description = "App-level OAuth client id, or null for an app that predates the app layer")
  val clientId: String?,
  @Schema(description = "How many organizations, this one included, currently have the app installed")
  val installCount: Long,
  val manifestLastCheckedAt: Long?,
  @Schema(description = "Consecutive failed manifest checks; zero while the manifest is answering")
  val manifestFailureCount: Int,
  val manifestFirstFailedAt: Long?,
  val manifestLastError: String?,
  @Schema(description = "`UNREACHABLE` or `INVALID`, or null while the manifest is answering")
  val manifestLastFailureKind: String?,
  @Schema(
    description =
      "When the app was marked unhealthy, or null while it is healthy. An unhealthy app keeps " +
        "working; it is only a warning that its manifest has stopped answering.",
  )
  val unhealthySince: Long?,
  @Schema(
    description =
      "Whether a server admin has offered this app to every organization on the server, not just " +
        "the owner. Toggled from the server-admin controls on the owner's Apps page.",
  )
  val availableToAllOrganizations: Boolean,
) : RepresentationModel<OwnedAppModel>()
