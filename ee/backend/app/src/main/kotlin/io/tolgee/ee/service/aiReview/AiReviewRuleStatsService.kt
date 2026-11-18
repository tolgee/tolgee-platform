package io.tolgee.ee.service.aiReview

import io.tolgee.ee.service.aiReview.matching.RuleDecision
import io.tolgee.model.enums.aiReview.AiReviewRuleMode
import io.tolgee.model.enums.aiReview.AiReviewStatus
import io.tolgee.repository.aiReview.AiReviewRuleRepository
import io.tolgee.repository.aiReview.TranslationAiReviewRepository
import io.tolgee.service.language.LanguageService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AiReviewRuleStatsService(
  private val translationAiReviewRepository: TranslationAiReviewRepository,
  private val aiReviewRuleRepository: AiReviewRuleRepository,
  private val backlogQuery: AiReviewBacklogQuery,
  private val evaluator: AiReviewRuleEvaluator,
  private val languageService: LanguageService,
) {
  data class AiReviewRuleCounts(
    val ruleId: Long,
    val autoReviewed: Long = 0,
    val failed: Long = 0,
    val scored: Long = 0,
    val wouldBeAutoReviewed: Long = 0,
    val backlog: Long = 0,
  )

  @Transactional(readOnly = true)
  fun getCounts(projectId: Long): Map<Long, AiReviewRuleCounts> {
    val counts =
      aiReviewRuleRepository
        .findDecisionCounts(projectId)
        .associate {
          it.ruleId to
            AiReviewRuleCounts(
              ruleId = it.ruleId,
              autoReviewed = it.autoReviewedCount,
            )
        }.toMutableMap()

    translationAiReviewRepository.countByRuleAndStatus(projectId).forEach { row ->
      val current = counts[row.ruleId] ?: return@forEach
      counts[row.ruleId] = current.add(AiReviewStatus.valueOf(row.status), row.lastFailed, row.count)
    }

    val decide = evaluator.decider(projectId)
    backlogQuery
      .groupCandidates(projectId, baseLanguageId(projectId), evaluator.defaultBranchName(projectId))
      .forEach { group ->
        val decision = decide(group.scope, group.languageId) as? RuleDecision.Review ?: return@forEach
        if (group.hasScoreOnlyRow && decision.rule.mode != AiReviewRuleMode.ACTIVE) return@forEach
        val current = counts[decision.rule.id] ?: return@forEach
        counts[decision.rule.id] = current.copy(backlog = current.backlog + group.translationCount)
      }
    return counts
  }

  @Transactional(readOnly = true)
  fun forEachBacklogItem(
    projectId: Long,
    ruleId: Long,
    fn: (translationId: Long) -> Unit,
  ) {
    val decide = evaluator.decider(projectId)
    backlogQuery.forEachCandidate(projectId, baseLanguageId(projectId), evaluator.defaultBranchName(projectId)) {
      val decision = decide(it.scope, it.languageId) as? RuleDecision.Review ?: return@forEachCandidate
      if (it.hasScoreOnlyRow && decision.rule.mode != AiReviewRuleMode.ACTIVE) return@forEachCandidate
      if (decision.rule.id == ruleId) fn(it.translationId)
    }
  }

  private fun baseLanguageId(projectId: Long): Long = languageService.getProjectBaseLanguage(projectId).id

  private fun AiReviewRuleCounts.add(
    status: AiReviewStatus,
    lastFailed: Boolean,
    count: Long,
  ): AiReviewRuleCounts {
    val retryableFailure = lastFailed && status != AiReviewStatus.OVERRIDDEN
    val withFailure =
      when {
        retryableFailure -> copy(failed = failed + count)
        else -> this
      }
    return when (status) {
      AiReviewStatus.FAILED -> withFailure.copy(failed = withFailure.failed + count)
      AiReviewStatus.SCORE_ONLY_PASS ->
        withFailure.copy(scored = scored + count, wouldBeAutoReviewed = wouldBeAutoReviewed + count)
      AiReviewStatus.SCORE_ONLY_FAIL -> withFailure.copy(scored = scored + count)
      AiReviewStatus.PENDING,
      AiReviewStatus.APPROVED,
      AiReviewStatus.BELOW_THRESHOLD,
      AiReviewStatus.SKIPPED_QA_ISSUES,
      AiReviewStatus.OVERRIDDEN,
      -> withFailure
    }
  }
}
