package io.tolgee.service.aiReview

import io.tolgee.dtos.request.prompt.PromptDto
import org.springframework.stereotype.Service

@Service
class AiReviewPlaygroundReviewerOssStub : AiReviewPlaygroundReviewer {
  override fun review(
    projectId: Long,
    keyId: Long,
    languageId: Long,
    prompt: PromptDto,
  ): PlaygroundReviewResult? = null
}
