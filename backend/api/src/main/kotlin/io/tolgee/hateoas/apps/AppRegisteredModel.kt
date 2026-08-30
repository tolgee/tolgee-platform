package io.tolgee.hateoas.apps

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

/**
 * The response to registering an app: the only place the app-level credentials are ever disclosed.
 * When the manifest describes an app somebody already registered, this only installs it and the
 * credential fields are null - an organization that merely installs an app never sees them.
 */
@Relation(itemRelation = "app")
open class AppRegisteredModel(
  val id: Long,
  @Schema(description = "The `id` declared in the app's manifest, unique across the server")
  val appId: String,
  val name: String,
  @Schema(
    description =
      "App-level OAuth client id. Present only when this call registered the app.",
  )
  val clientId: String? = null,
  @Schema(
    description =
      "App-level OAuth client secret in plaintext - the app's only long-lived credential; the " +
        "token endpoint exchanges it for install-scoped access tokens. Present only when this call " +
        "registered the app; Tolgee stores only a hash and cannot show it again.",
  )
  val clientSecret: String? = null,
  @Schema(
    description =
      "The secret Tolgee signs this app's lifecycle deliveries with. Present only when this call " +
        "registered the app.",
  )
  val webhookSecret: String? = null,
  @Schema(description = "Id of the install created for this organization, or null when install was skipped")
  val installId: Long? = null,
  @Schema(
    description =
      "Whether the app-level credentials reached the app over the lifecycle channel. Present only " +
        "when this call registered the app; null when nothing was disclosed to deliver.",
  )
  val delivery: AppDeliveryOutcomeModel? = null,
) : RepresentationModel<AppRegisteredModel>()
