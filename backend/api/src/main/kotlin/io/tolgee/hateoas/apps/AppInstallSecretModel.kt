package io.tolgee.hateoas.apps

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "appInstallSecrets", itemRelation = "appInstallSecret")
open class AppInstallSecretModel(
  val id: Long,
  @Schema(description = "First characters of the secret, enough to tell two of them apart")
  val prefix: String,
  val createdAt: Long,
  @Schema(
    description =
      "When this secret was last accepted at the token endpoint, or null if never. Recorded at a " +
        "granularity of about a minute — check it before revoking, to see whether anything still " +
        "uses the secret.",
  )
  val lastUsedAt: Long?,
  @Schema(description = "When the secret was revoked, or null while it still authenticates")
  val revokedAt: Long?,
  @Schema(
    description =
      "The secret in plaintext. Present only in the response to issuing it — Tolgee stores only a " +
        "hash and cannot show it again.",
  )
  val secret: String? = null,
) : RepresentationModel<AppInstallSecretModel>()
