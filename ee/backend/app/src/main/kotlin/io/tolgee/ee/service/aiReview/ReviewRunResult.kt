package io.tolgee.ee.service.aiReview

import io.tolgee.dtos.PromptResult
import io.tolgee.model.aiReview.AiReviewIssue

data class ReviewRunResult(
  val issues: List<AiReviewIssue>,
  val score: Int,
  val rawResponse: String,
  val providerName: String?,
  val model: String?,
  val promptId: Long?,
  val promptName: String,
  val promptRevision: Int?,
  val renderedPrompt: String,
  val price: Int,
  val usage: PromptResult.Usage?,
)
