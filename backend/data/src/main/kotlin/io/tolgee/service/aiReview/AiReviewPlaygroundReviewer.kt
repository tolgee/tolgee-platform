package io.tolgee.service.aiReview

import io.tolgee.dtos.request.prompt.PromptDto
import io.tolgee.model.aiReview.AiReviewIssue

data class PlaygroundReviewResult(
  val score: Int,
  val issues: List<AiReviewIssue>,
)

interface AiReviewPlaygroundReviewer {
  /** Null means there is nothing to review (missing or empty translation). */
  fun review(
    projectId: Long,
    keyId: Long,
    languageId: Long,
    prompt: PromptDto,
  ): PlaygroundReviewResult?
}
