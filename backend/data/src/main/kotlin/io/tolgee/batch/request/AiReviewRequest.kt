package io.tolgee.batch.request

import io.tolgee.batch.data.BatchTranslationTargetItem

data class AiReviewRequest(
  val target: List<BatchTranslationTargetItem>,
  val reevaluateOnly: Boolean = false,
  val handlingStuckItems: Boolean = false,
)
