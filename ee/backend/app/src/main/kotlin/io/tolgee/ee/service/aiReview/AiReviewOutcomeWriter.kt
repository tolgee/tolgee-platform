package io.tolgee.ee.service.aiReview

import io.tolgee.component.CurrentDateProvider
import io.tolgee.ee.service.aiReview.matching.RuleDecision
import io.tolgee.ee.service.aiReview.scoring.AiReviewScoring
import io.tolgee.model.aiReview.AiReviewIssue
import io.tolgee.model.aiReview.AiReviewRule
import io.tolgee.model.aiReview.TranslationAiReview
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.aiReview.AiReviewFailureReason
import io.tolgee.model.enums.aiReview.AiReviewStatus
import io.tolgee.model.translation.Translation
import io.tolgee.repository.aiReview.AiReviewRuleRepository
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component

/**
 * Writes an LLM outcome to the review row: status by mode and threshold, issues, score,
 * attribution, and the translation state change an approval or a demotion brings.
 */
@Component
class AiReviewOutcomeWriter(
  private val ruleRepository: AiReviewRuleRepository,
  private val currentDateProvider: CurrentDateProvider,
  private val entityManager: EntityManager,
) {
  sealed interface LlmOutcome {
    data class Success(
      val issues: List<AiReviewIssue>,
      val score: Int,
      val providerName: String?,
      val model: String?,
    ) : LlmOutcome

    data class Failure(
      val reason: AiReviewFailureReason,
    ) : LlmOutcome
  }

  data class PromptAttribution(
    val promptId: Long?,
    val promptName: String,
    val promptRevision: Int?,
  )

  fun record(
    translation: Translation,
    row: TranslationAiReview,
    outcome: LlmOutcome,
    decision: RuleDecision.Review,
    prompt: PromptAttribution,
  ) {
    val keepsPreviousResult = outcome is LlmOutcome.Failure && row.score != null
    when (outcome) {
      is LlmOutcome.Success -> applySuccess(translation, row, outcome, decision)
      is LlmOutcome.Failure -> applyFailure(row, outcome)
    }
    if (!keepsPreviousResult) {
      attributeResult(row, decision, prompt)
      row.resolvedBy = null
      row.resolvedAt = null
    }
    row.batchJobId = null
    row.reReviewRequested = false
    row.reviewedAt = currentDateProvider.date
  }

  fun clearFailure(row: TranslationAiReview) {
    row.failureReason = null
    row.lastFailureReason = null
    row.lastFailedAt = null
  }

  private fun applySuccess(
    translation: Translation,
    row: TranslationAiReview,
    outcome: LlmOutcome.Success,
    decision: RuleDecision.Review,
  ) {
    val status = AiReviewScoring.outcome(decision.rule.mode, outcome.score, decision.threshold)
    val wasAiManagedReview = AiReviewEligibility.isAiManagedReview(translation, row)
    if (status == AiReviewStatus.APPROVED && !isApprovalOf(row, decision.rule.id)) {
      ruleRepository.addToAutoReviewedCount(decision.rule.id, 1)
    }
    row.status = status
    row.score = outcome.score
    row.issues = outcome.issues.toMutableList()
    row.providerName = outcome.providerName
    row.model = outcome.model
    clearFailure(row)

    if (status == AiReviewStatus.APPROVED) {
      translation.state = TranslationState.REVIEWED
      return
    }
    if (status == AiReviewStatus.BELOW_THRESHOLD && wasAiManagedReview &&
      !AiReviewEligibility.isProtectedReviewed(translation)
    ) {
      translation.state = TranslationState.TRANSLATED
    }
  }

  private fun isApprovalOf(
    row: TranslationAiReview,
    ruleId: Long,
  ): Boolean = row.status == AiReviewStatus.APPROVED && row.rule?.id == ruleId

  private fun attributeResult(
    row: TranslationAiReview,
    decision: RuleDecision.Review,
    prompt: PromptAttribution,
  ) {
    row.rule = entityManager.getReference(AiReviewRule::class.java, decision.rule.id)
    row.ruleMode = decision.rule.mode
    row.threshold = decision.threshold
    row.promptId = prompt.promptId
    row.promptName = prompt.promptName
    row.promptRevision = prompt.promptRevision
  }

  private fun applyFailure(
    row: TranslationAiReview,
    outcome: LlmOutcome.Failure,
  ) {
    if (row.score != null) {
      row.lastFailureReason = outcome.reason
      row.lastFailedAt = currentDateProvider.date
      return
    }
    row.status = AiReviewStatus.FAILED
    row.failureReason = outcome.reason
  }
}
