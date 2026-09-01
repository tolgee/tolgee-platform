package io.tolgee.hateoas.apps

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "appSecrets", itemRelation = "appSecret")
open class AppSecretModel(
  val id: Long,
  @Schema(description = "How the secret is identified wherever it is shown: its start and end, e.g. `tgpubs_ab…yz`")
  val name: String,
  val createdAt: Long,
  @Schema(
    description =
      "When this secret last authenticated a request — token minting, install discovery or " +
        "self-administration — or null if it never has.",
  )
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
) : RepresentationModel<AppSecretModel>()
