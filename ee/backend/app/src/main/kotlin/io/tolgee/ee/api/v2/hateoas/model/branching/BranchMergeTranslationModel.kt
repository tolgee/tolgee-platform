package io.tolgee.ee.api.v2.hateoas.model.branching

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.model.enums.TranslationState

data class BranchMergeTranslationModel(
  @Schema(description = "Translation id")
  val id: Long?,
  @Schema(description = "Language tag")
  val language: String,
  @Schema(description = "Translation text")
  val text: String?,
  @Schema(description = "Translation state")
  val state: TranslationState,
  @Schema(description = "Whether translation is outdated")
  val outdated: Boolean,
)
