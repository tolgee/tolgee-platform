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
  @Schema(description = "The manifest's version at the last read")
  val version: String,
  val manifestUrl: String,
  val baseUrl: String,
  @Schema(
    description =
      "The app's logo: an emoji, a native Tolgee icon name, or an absolute image URL served by " +
        "the app's own host. Null when the manifest declares none.",
  )
  val icon: String? = null,
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
) : RepresentationModel<OwnedAppModel>()
