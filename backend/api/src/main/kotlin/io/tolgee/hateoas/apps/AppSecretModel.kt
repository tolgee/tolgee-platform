package io.tolgee.hateoas.apps

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "appSecrets", itemRelation = "appSecret")
open class AppSecretModel(
  val id: Long,
  @Schema(description = "First characters of the secret, enough to tell two of them apart")
  val prefix: String,
  @Schema(description = "Last characters of the secret; the prefix is always the same, so the suffix distinguishes")
  val suffix: String,
  val createdAt: Long,
  @Schema(description = "When this secret was last used to administer the app, or null if never")
  val lastUsedAt: Long?,
  @Schema(
    description =
      "When this secret stops authenticating, or null while it has no scheduled end. Set on the " +
        "outgoing secret during a rotation's grace window.",
  )
  val expiresAt: Long?,
  @Schema(description = "When the secret was revoked, or null while it still authenticates")
  val revokedAt: Long?,
  @Schema(
    description =
      "The secret in plaintext. Present only in the response to issuing it — Tolgee stores only a " +
        "hash and cannot show it again. Everything the app does starts from it — the token " +
        "endpoint exchanges it for the short-lived tokens that reach translation data.",
  )
  val secret: String? = null,
  @Schema(
    description =
      "Whether the new secret reached the app over the lifecycle channel. Present only in the " +
        "response to an owner issuing it; null on the app-initiated path and when listing.",
  )
  val delivery: AppDeliveryOutcomeModel? = null,
) : RepresentationModel<AppSecretModel>()
