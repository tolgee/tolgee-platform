package io.tolgee.ee.service.aiReview

import io.tolgee.constants.Feature
import io.tolgee.ee.service.aiReview.matching.RuleDecision
import io.tolgee.model.aiReview.AiReviewRule
import io.tolgee.model.aiReview.TranslationAiReview
import io.tolgee.model.enums.TranslationProtection
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.aiReview.AiReviewRuleMode
import io.tolgee.model.enums.aiReview.AiReviewStatus
import io.tolgee.model.translation.Translation
import io.tolgee.repository.aiReview.TranslationAiReviewRepository
import io.tolgee.service.aiReview.AiReviewDemotionService
import io.tolgee.service.project.ProjectFeatureGuard
import io.tolgee.service.project.ProjectService
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Re-checks AI approvals against the current rules using the stored score — no LLM call. It never
 * promotes: a stored score is good enough to remove an approval, not to give one.
 */
@Primary
@Service
class AiReviewDemotionServiceImpl(
  private val evaluator: AiReviewRuleEvaluator,
  private val reviewRepository: TranslationAiReviewRepository,
  private val projectService: ProjectService,
  private val projectFeatureGuard: ProjectFeatureGuard,
  private val entityManager: EntityManager,
) : AiReviewDemotionService {
  @Transactional
  override fun demoteKeys(
    projectId: Long,
    keyIds: Collection<Long>,
  ) {
    if (keyIds.isEmpty()) return
    val project = projectService.getDto(projectId)
    if (!projectFeatureGuard.isFeatureEnabled(Feature.AI_REVIEW, project)) return
    val protectReviewed = project.translationProtection == TranslationProtection.PROTECT_REVIEWED

    keyIds.chunked(IN_CLAUSE_CHUNK).forEach { chunk ->
      reviewRepository.findApprovedTranslationIdsByKeyIds(chunk).forEach { translationId ->
        // Loaded fresh under the lock: Hibernate flushes full-column updates, so a snapshot
        // fetched before the lock could write a stale text back over a concurrent MT result.
        val translation =
          entityManager.find(Translation::class.java, translationId, LockModeType.PESSIMISTIC_WRITE)
            ?: return@forEach
        val row = reviewRepository.findByTranslationId(translationId) ?: return@forEach
        if (row.status != AiReviewStatus.APPROVED) return@forEach
        reconcileApproval(row, translation, protectReviewed)
      }
    }
  }

  private fun reconcileApproval(
    row: TranslationAiReview,
    translation: Translation,
    protectReviewed: Boolean,
  ) {
    val protected = protectReviewed && translation.state == TranslationState.REVIEWED
    val decision = evaluator.evaluate(translation)
    if (decision !is RuleDecision.Review) {
      if (protected) return
      translation.state = TranslationState.TRANSLATED
      reviewRepository.delete(row)
      return
    }
    if (decision.rule.mode != AiReviewRuleMode.ACTIVE) return

    row.rule = entityManager.getReference(AiReviewRule::class.java, decision.rule.id)
    row.ruleMode = decision.rule.mode
    row.threshold = decision.threshold
    val score = row.score ?: return
    if (score >= decision.threshold) return

    row.status = AiReviewStatus.BELOW_THRESHOLD
    if (!protected) {
      translation.state = TranslationState.TRANSLATED
    }
  }
}
