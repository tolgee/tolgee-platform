package io.tolgee.hateoas.apps

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "apps", itemRelation = "app")
open class AppModel(
  val id: Long,
  @Schema(description = "The `id` declared in the app's manifest, unique across the server")
  val appId: String,
  val name: String,
  @Schema(
    description =
      "App-level OAuth client id. Present only in the response to registering the app — an " +
        "organization that merely installed it never sees it.",
  )
  val clientId: String? = null,
  @Schema(
    description =
      "App-level OAuth client secret in plaintext — the app's only long-lived credential; the " +
        "token endpoint exchanges it for install-scoped access tokens. Present only in the " +
        "response to registering the app; Tolgee stores only a hash and cannot show it again.",
  )
  val clientSecret: String? = null,
  @Schema(
    description =
      "The secret Tolgee signs this app's lifecycle deliveries with. Present only in the response " +
        "to registering the app.",
  )
  val webhookSecret: String? = null,
) : RepresentationModel<AppModel>()
