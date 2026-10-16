package io.tolgee.ee.api.v2.hateoas.model.aiReview

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.ee.data.aiReview.AiReviewRunCounts

data class AiReviewRunPreviewModel(
  @Schema(
    description =
      "Selected translations counted by the run: non-base, not empty, translated or reviewed " +
        "(and matched by the rule when the run is scoped to a rule)",
  )
  val total: Long,
  @Schema(description = "Counts when translations that already have a result are skipped")
  val withoutRereview: AiReviewRunCounts,
  @Schema(description = "Counts when translations that already have a result are reviewed again")
  val withRereview: AiReviewRunCounts,
)
