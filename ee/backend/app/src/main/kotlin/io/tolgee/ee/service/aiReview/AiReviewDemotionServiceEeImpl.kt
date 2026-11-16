package io.tolgee.ee.service.aiReview

import io.tolgee.constants.Feature
import io.tolgee.ee.service.aiReview.matching.RuleDecision
import io.tolgee.model.aiReview.AiReviewRule
import io.tolgee.model.aiReview.TranslationAiReview
import io.tolgee.model.enums.TranslationProtection
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.aiReview.AiReviewStatus
import io.tolgee.model.translation.Translation
import io.tolgee.repository.TranslationRepository
import io.tolgee.repository.aiReview.TranslationAiReviewRepository
import io.tolgee.service.aiReview.AiReviewDemotionService
import io.tolgee.service.project.ProjectFeatureGuard
import io.tolgee.service.project.ProjectService
import jakarta.persistence.EntityManager
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** See docs/ai-review/README.md 3.4 "Demotion uses the stored score and never promotes". */
@Primary
@Service
class AiReviewDemotionServiceEeImpl(
  private val evaluator: AiReviewRuleEvaluator,
  private val reviewRepository: TranslationAiReviewRepository,
  private val projectService: ProjectService,
  private val projectFeatureGuard: ProjectFeatureGuard,
  private val translationRepository: TranslationRepository,
  private val entityManager: EntityManager,
  private val websocketPublisher: AiReviewWebsocketPublisher,
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
    val projectRules = evaluator.forProject(projectId)
    val enabledRuleIds = projectRules.rules.map { it.id }.toSet()

    val updates = mutableListOf<AiReviewUpdate>()
    keyIds.chunked(IN_CLAUSE_CHUNK).forEach { chunk ->
      val translationIds = reviewRepository.findApprovedTranslationIdsByKeyIds(chunk)
      if (translationIds.isEmpty()) return@forEach
      // Locked before loading: Hibernate flushes full-column updates, so a snapshot fetched
      // before the lock could write a stale text back over a concurrent MT result.
      lockTranslations(translationIds)
      val rows = reviewRepository.findAllByTranslationIdIn(translationIds).associateBy { it.translation.id }
      translationRepository.findAllForAiReviewRuleEvaluation(translationIds).forEach { translation ->
        val row = rows[translation.id] ?: return@forEach
        if (row.status != AiReviewStatus.APPROVED) return@forEach
        reconcileApproval(row, translation, protectReviewed, projectRules, enabledRuleIds)?.let { updates += it }
      }
    }
    websocketPublisher.publishAfterCommit(projectId, updates)
  }

  private fun lockTranslations(translationIds: List<Long>) {
    entityManager
      .createNativeQuery("select id from translation where id in :ids order by id for update")
      .setParameter("ids", translationIds)
      .resultList
  }

  private fun reconcileApproval(
    row: TranslationAiReview,
    translation: Translation,
    protectReviewed: Boolean,
    projectRules: AiReviewRuleEvaluator.ProjectRules,
    enabledRuleIds: Set<Long>,
  ): AiReviewUpdate? {
    val protected = protectReviewed && translation.state == TranslationState.REVIEWED
    val decision = projectRules.evaluate(translation, activeOnly = true)
    if (decision == RuleDecision.NoRule && keepsWithoutActiveRule(row, translation, projectRules, enabledRuleIds)) {
      return null
    }
    if (decision !is RuleDecision.Review) {
      if (protected) return null
      translation.state = TranslationState.TRANSLATED
      reviewRepository.delete(row)
      return AiReviewUpdate.removed(translation)
    }

    val score = row.score ?: return null
    if (score >= decision.threshold) return null

    row.status = AiReviewStatus.BELOW_THRESHOLD
    row.rule = entityManager.getReference(AiReviewRule::class.java, decision.rule.id)
    row.ruleMode = decision.rule.mode
    row.threshold = decision.threshold
    if (!protected) {
      translation.state = TranslationState.TRANSLATED
    }
    return AiReviewUpdate.of(row, scoreOnlyRun = false)
  }

  // See docs/ai-review/README.md 3.4 "Deleting a rule keeps approval rows".
  private fun keepsWithoutActiveRule(
    row: TranslationAiReview,
    translation: Translation,
    projectRules: AiReviewRuleEvaluator.ProjectRules,
    enabledRuleIds: Set<Long>,
  ): Boolean {
    if (row.rule?.id !in enabledRuleIds) return true
    return projectRules.evaluate(translation) is RuleDecision.Review
  }
}
