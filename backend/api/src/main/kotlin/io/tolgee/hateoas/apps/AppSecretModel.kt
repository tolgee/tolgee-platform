package io.tolgee.hateoas.apps

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "appSecrets", itemRelation = "appSecret")
open class AppSecretModel(
  val id: Long,
  @Schema(description = "First characters of the secret, enough to tell two of them apart")
  val prefix: String,
  val createdAt: Long,
  @Schema(description = "When this secret was last used to administer the app, or null if never")
  val lastUsedAt: Long?,
  @Schema(description = "When the secret was revoked, or null while it still authenticates")
  val revokedAt: Long?,
  @Schema(
    description =
      "The secret in plaintext. Present only in the response to issuing it — Tolgee stores only a " +
        "hash and cannot show it again. It administers the app and grants access to no data.",
  )
  val secret: String? = null,
) : RepresentationModel<AppSecretModel>()
