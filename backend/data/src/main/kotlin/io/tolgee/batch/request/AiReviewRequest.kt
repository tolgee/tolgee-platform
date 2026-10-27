package io.tolgee.batch.request

data class AiReviewRequest(
  val translationIds: List<Long>,
  val handlingStuckItems: Boolean = false,
)
