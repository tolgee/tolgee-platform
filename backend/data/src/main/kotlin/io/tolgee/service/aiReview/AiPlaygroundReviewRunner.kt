package io.tolgee.service.aiReview

import io.tolgee.constants.Message
import io.tolgee.dtos.aiReview.AiReviewRunResult
import io.tolgee.dtos.request.prompt.PromptRunDto
import io.tolgee.exceptions.AiReviewInvalidOutputException
import io.tolgee.model.enums.LlmProviderPriority
import io.tolgee.service.AiPlaygroundResultService
import io.tolgee.service.PromptService
import org.springframework.stereotype.Component

@Component
class AiPlaygroundReviewRunner(
  private val promptService: PromptService,
  private val aiPlaygroundResultService: AiPlaygroundResultService,
) {
  fun reviewAndStoreResult(
    projectId: Long,
    userId: Long,
    data: PromptRunDto,
    priority: LlmProviderPriority,
  ): AiReviewRunResult {
    val result =
      try {
        promptService.review(projectId, data, priority)
      } catch (e: AiReviewInvalidOutputException) {
        storeInvalidOutput(projectId, userId, data)
        throw e
      }
    storeScoredResult(projectId, userId, data, result)
    return result
  }

  private fun storeScoredResult(
    projectId: Long,
    userId: Long,
    data: PromptRunDto,
    result: AiReviewRunResult,
  ) {
    aiPlaygroundResultService.setReviewResult(
      projectId = projectId,
      userId = userId,
      keyId = data.keyId,
      languageId = data.targetLanguageId,
      score = result.score,
      issues = result.issues,
      error = null,
    )
  }

  private fun storeInvalidOutput(
    projectId: Long,
    userId: Long,
    data: PromptRunDto,
  ) {
    aiPlaygroundResultService.setReviewResult(
      projectId = projectId,
      userId = userId,
      keyId = data.keyId,
      languageId = data.targetLanguageId,
      score = null,
      issues = null,
      error = Message.AI_REVIEW_INVALID_OUTPUT.code,
    )
  }
}
