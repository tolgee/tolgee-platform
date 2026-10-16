package io.tolgee.ee.data.aiReview

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.model.enums.aiReview.SourceChangeAction

data class AiReviewSettingsRequest(
  @Schema(description = "What happens to machine translations approved by AI when their source text changes")
  val onSourceChangeAiApproved: SourceChangeAction = SourceChangeAction.RETRANSLATE,
  @Schema(description = "What happens to translations edited by a human when their source text changes")
  val onSourceChangeHumanEdited: SourceChangeAction = SourceChangeAction.KEEP,
  @Schema(description = "What happens to translations reviewed by a human when their source text changes")
  val onSourceChangeHumanReviewed: SourceChangeAction = SourceChangeAction.KEEP,
)
