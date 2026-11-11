package io.tolgee.ee.service.aiReview

import io.tolgee.dtos.request.prompt.PromptDto
import io.tolgee.model.enums.LlmProviderPriority
import io.tolgee.repository.TranslationRepository
import io.tolgee.service.aiReview.AiReviewEligibleText
import io.tolgee.service.aiReview.AiReviewPlaygroundReviewer
import io.tolgee.service.aiReview.PlaygroundReviewResult
import io.tolgee.service.project.ProjectService
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Primary
@Service
class AiReviewPlaygroundReviewerEeImpl(
  private val aiReviewPromptService: AiReviewPromptService,
  private val translationRepository: TranslationRepository,
  private val projectService: ProjectService,
) : AiReviewPlaygroundReviewer {
  @Transactional
  override fun review(
    projectId: Long,
    keyId: Long,
    languageId: Long,
    prompt: PromptDto,
  ): PlaygroundReviewResult? {
    val translation =
      translationRepository.findOneByProjectIdAndKeyIdAndLanguageId(projectId, keyId, languageId)
        ?: return null
    if (!AiReviewEligibleText.hasReviewableText(translation)) return null
    val organizationId = projectService.getDto(projectId).organizationOwnerId
    val result =
      aiReviewPromptService.reviewWithPrompt(
        organizationId = organizationId,
        projectId = projectId,
        translation = translation,
        prompt = prompt,
        priority = LlmProviderPriority.LOW,
        savedPrompt = null,
      )
    return PlaygroundReviewResult(score = result.score, issues = result.issues)
  }
}
