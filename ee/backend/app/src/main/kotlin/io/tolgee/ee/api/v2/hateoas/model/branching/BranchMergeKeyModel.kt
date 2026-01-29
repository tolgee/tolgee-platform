package io.tolgee.ee.api.v2.hateoas.model.branching

import io.swagger.v3.oas.annotations.media.Schema

data class BranchMergeKeyModel(
  @Schema(description = "Key id")
  val keyId: Long,
  @Schema(description = "Key name")
  val keyName: String,
  @Schema(description = "Whether key uses plural forms")
  val keyIsPlural: Boolean,
  @Schema(description = "Key description")
  val keyDescription: String?,
  @Schema(description = "Translations indexed by language tag")
  val translations: Map<String, BranchMergeTranslationModel>,
  @Schema(description = "Namespace of the key")
  val namespace: String?,
)
