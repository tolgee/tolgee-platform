package io.tolgee.service.aiReview

import io.tolgee.dtos.request.prompt.PromptDto
import io.tolgee.model.aiReview.AiReviewIssue
import org.springframework.stereotype.Service

data class PlaygroundReviewResult(
  val score: Int,
  val issues: List<AiReviewIssue>,
)

interface AiReviewPlaygroundReviewer {
  /**
   * Reviews one translation with the given prompt, without a rule. Null means there is nothing to
   * review (missing or empty translation).
   */
  fun review(
    projectId: Long,
    keyId: Long,
    languageId: Long,
    prompt: PromptDto,
  ): PlaygroundReviewResult?
}

@Service
class AiReviewPlaygroundReviewerOssStub : AiReviewPlaygroundReviewer {
  override fun review(
    projectId: Long,
    keyId: Long,
    languageId: Long,
    prompt: PromptDto,
  ): PlaygroundReviewResult? = null
}
