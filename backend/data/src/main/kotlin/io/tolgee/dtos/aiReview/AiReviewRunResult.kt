package io.tolgee.dtos.aiReview

import io.tolgee.dtos.PromptResult
import io.tolgee.model.aiReview.AiReviewIssue

data class AiReviewRunResult(
  val score: Int,
  val issues: List<AiReviewIssue>,
  val providerName: String,
  val prompt: String,
  val rawResponse: String,
  val price: Int,
  val usage: PromptResult.Usage?,
)
