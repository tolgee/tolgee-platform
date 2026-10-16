package io.tolgee.ee.data.aiReview

import io.swagger.v3.oas.annotations.media.Schema

data class AiReviewRunRequest(
  @Schema(description = "Keys to review. Required unless ruleId is set.")
  val keyIds: List<Long>? = null,
  @Schema(description = "Languages to review. Null means all languages except the base language.")
  val languageIds: List<Long>? = null,
  @Schema(description = "Limits the run to translations matched by this rule. Without keyIds, all keys of the project.")
  val ruleId: Long? = null,
  @Schema(description = "Review again translations that already have an AI review result for their current text")
  val rereview: Boolean = false,
)
