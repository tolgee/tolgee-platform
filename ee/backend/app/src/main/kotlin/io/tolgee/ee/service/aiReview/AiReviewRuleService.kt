package io.tolgee.ee.service.aiReview

import io.tolgee.constants.Message
import io.tolgee.ee.data.aiReview.AiReviewRuleRequest
import io.tolgee.ee.data.aiReview.AiReviewRuleStats
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Project
import io.tolgee.model.Prompt
import io.tolgee.model.aiReview.AiReviewLanguageThreshold
import io.tolgee.model.aiReview.AiReviewRule
import io.tolgee.model.enums.PromptType
import io.tolgee.model.enums.aiReview.AiReviewOutcome
import io.tolgee.repository.aiReview.AiReviewRuleRepository
import io.tolgee.repository.aiReview.TranslationAiReviewRepository
import io.tolgee.service.PromptService
import io.tolgee.service.language.LanguageService
import jakarta.persistence.EntityManager
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AiReviewRuleService(
  private val aiReviewRuleRepository: AiReviewRuleRepository,
  private val translationAiReviewRepository: TranslationAiReviewRepository,
  private val promptService: PromptService,
  private val languageService: LanguageService,
  private val entityManager: EntityManager,
  private val applicationEventPublisher: ApplicationEventPublisher,
  private val triggerService: AiReviewTriggerService,
) {
  fun getAll(projectId: Long): List<AiReviewRule> = aiReviewRuleRepository.findAllByProjectId(projectId)

  fun get(
    projectId: Long,
    ruleId: Long,
  ): AiReviewRule =
    aiReviewRuleRepository.findByProjectIdAndId(projectId, ruleId)
      ?: throw NotFoundException(Message.AI_REVIEW_RULE_NOT_FOUND)

  @Transactional
  fun create(
    projectId: Long,
    request: AiReviewRuleRequest,
  ): AiReviewRule {
    val rule = AiReviewRule(project = entityManager.getReference(Project::class.java, projectId))
    applyRequest(projectId, rule, request)
    return aiReviewRuleRepository.save(rule).also {
      applicationEventPublisher.publishEvent(AiReviewRuleChangedEvent(projectId, it.id))
    }
  }

  @Transactional
  fun update(
    projectId: Long,
    ruleId: Long,
    request: AiReviewRuleRequest,
  ): AiReviewRule {
    val rule = get(projectId, ruleId)
    applyRequest(projectId, rule, request)
    return aiReviewRuleRepository.save(rule).also {
      applicationEventPublisher.publishEvent(AiReviewRuleChangedEvent(projectId, it.id))
    }
  }

  @Transactional
  fun delete(
    projectId: Long,
    ruleId: Long,
  ) {
    aiReviewRuleRepository.delete(get(projectId, ruleId))
  }

  @Transactional
  fun retryFailed(
    projectId: Long,
    ruleId: Long,
  ): Int = triggerService.retryFailed(projectId, get(projectId, ruleId).id)

  fun getStats(projectId: Long): Map<Long, AiReviewRuleStats> =
    translationAiReviewRepository
      .getRuleStats(projectId)
      .groupBy { it.ruleId }
      .mapValues { (_, rows) ->
        fun countDryRun(vararg outcomes: AiReviewOutcome) =
          rows.filter { it.dryRun && it.outcome in outcomes }.sumOf { it.count }

        fun countActive(vararg outcomes: AiReviewOutcome) =
          rows.filter { !it.dryRun && it.outcome in outcomes }.sumOf { it.count }

        AiReviewRuleStats(
          scored = countDryRun(AiReviewOutcome.WOULD_AUTO_REVIEW, AiReviewOutcome.BELOW_THRESHOLD),
          wouldAutoReview = countDryRun(AiReviewOutcome.WOULD_AUTO_REVIEW),
          autoReviewed =
            countActive(
              AiReviewOutcome.AUTO_REVIEWED,
              AiReviewOutcome.CONFIRMED,
              AiReviewOutcome.REJECTED,
            ),
          rejected = countActive(AiReviewOutcome.REJECTED),
          failed = rows.filter { it.outcome == AiReviewOutcome.FAILED }.sumOf { it.count },
        )
      }

  fun switchRulesUsingPromptToDryRun(promptId: Long) {
    aiReviewRuleRepository.setDryRunByPromptId(promptId)
  }

  fun detachPromptFromRulesAndSwitchToDryRun(promptId: Long) {
    aiReviewRuleRepository.detachPromptAndSetDryRun(promptId)
  }

  private fun applyRequest(
    projectId: Long,
    rule: AiReviewRule,
    request: AiReviewRuleRequest,
  ) {
    val projectLanguageIds = languageService.getProjectLanguages(projectId).map { it.id }.toSet()
    validateThreshold(request.threshold)
    validateLanguageIds(request.languageIds, projectLanguageIds)
    validateLanguageThresholds(request.languageThresholds, projectLanguageIds)

    rule.name = request.name?.trim()?.takeIf { it.isNotEmpty() }
    rule.enabled = request.enabled
    rule.mode = request.mode
    rule.threshold = request.threshold
    rule.languageThresholds = request.languageThresholds.toMutableList()
    rule.prompt = request.promptId?.let { findReviewPrompt(projectId, it) }
    rule.skipWithOpenQaIssues = request.skipWithOpenQaIssues
    rule.tagIn = request.tagIn.distinct().toMutableList()
    rule.tagNotIn = request.tagNotIn.distinct().toMutableList()
    rule.namespaceIn = request.namespaceIn.distinct().toMutableList()
    rule.namespaceNotIn = request.namespaceNotIn.distinct().toMutableList()
    rule.languageIds = request.languageIds.distinct().toMutableList()
  }

  private fun findReviewPrompt(
    projectId: Long,
    promptId: Long,
  ): Prompt {
    val prompt = promptService.findPrompt(projectId, promptId)
    if (prompt.type != PromptType.REVIEW) {
      throw BadRequestException(Message.PROMPT_TYPE_NOT_SUPPORTED)
    }
    return prompt
  }

  private fun validateThreshold(threshold: Int) {
    if (threshold !in THRESHOLD_RANGE) {
      throw BadRequestException(Message.AI_REVIEW_INVALID_THRESHOLD)
    }
  }

  private fun validateLanguageIds(
    languageIds: List<Long>,
    projectLanguageIds: Set<Long>,
  ) {
    if (languageIds.any { it !in projectLanguageIds }) {
      throw BadRequestException(Message.LANGUAGE_NOT_FROM_PROJECT)
    }
  }

  private fun validateLanguageThresholds(
    thresholds: List<AiReviewLanguageThreshold>,
    projectLanguageIds: Set<Long>,
  ) {
    validateLanguageIds(thresholds.map { it.languageId }, projectLanguageIds)
    if (thresholds.map { it.languageId }.distinct().size != thresholds.size) {
      throw BadRequestException(Message.AI_REVIEW_INVALID_THRESHOLD)
    }
    thresholds.forEach {
      val hasThreshold = it.threshold != null
      if (it.never == hasThreshold) {
        throw BadRequestException(Message.AI_REVIEW_INVALID_THRESHOLD)
      }
      if (hasThreshold && it.threshold !in THRESHOLD_RANGE) {
        throw BadRequestException(Message.AI_REVIEW_INVALID_THRESHOLD)
      }
    }
  }

  companion object {
    private val THRESHOLD_RANGE = 0..100
  }
}
