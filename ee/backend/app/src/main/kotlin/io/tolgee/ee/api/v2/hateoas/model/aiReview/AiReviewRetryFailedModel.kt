package io.tolgee.ee.api.v2.hateoas.model.aiReview

import io.swagger.v3.oas.annotations.media.Schema

data class AiReviewRetryFailedModel(
  @Schema(description = "Number of failed reviews queued to run again")
  val queued: Int,
)
