package io.tolgee.repository.aiReview

import io.tolgee.dtos.queryResults.aiReview.AiReviewRuleStatsView
import io.tolgee.dtos.queryResults.aiReview.AiReviewTargetView
import io.tolgee.dtos.queryResults.aiReview.AiReviewTextView
import io.tolgee.dtos.queryResults.aiReview.TranslationAiReviewView
import io.tolgee.model.aiReview.TranslationAiReview
import io.tolgee.model.enums.aiReview.AiReviewOutcome
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Date

@Repository
@Lazy
interface TranslationAiReviewRepository : JpaRepository<TranslationAiReview, Long> {
  @Query(
    """
    from TranslationAiReview r
    left join fetch r.rule
    left join fetch r.prompt
    where r.translation.id = :translationId
    """,
  )
  fun findByTranslationId(translationId: Long): TranslationAiReview?

  @Modifying(flushAutomatically = false)
  @Query(
    nativeQuery = true,
    value = "DELETE FROM translation_ai_review WHERE translation_id IN (:translationIds)",
  )
  fun deleteAllByTranslationIdIn(translationIds: Collection<Long>)

  @Modifying(flushAutomatically = false)
  @Query(
    nativeQuery = true,
    value = """
    UPDATE translation_ai_review
    SET outcome = 'REJECTED', resolved_by_id = :userId, resolved_at = :now, updated_at = :now,
      previous_outcome = NULL, run_batch_job_id = NULL, claimed_at = NULL
    WHERE translation_id IN (:translationIds)
      AND (outcome = 'AUTO_REVIEWED' OR (outcome = 'PENDING' AND previous_outcome = 'AUTO_REVIEWED'))
    """,
  )
  fun markAutoReviewedRejected(
    translationIds: Collection<Long>,
    userId: Long?,
    now: Date,
  )

  @Query(
    """
    select new io.tolgee.dtos.queryResults.aiReview.AiReviewTargetView(t.id, t.key.id, t.language.id, t.qaChecksStale)
    from TranslationAiReview r
    join r.translation t
    where r.outcome = io.tolgee.model.enums.aiReview.AiReviewOutcome.PENDING
      and t.key.id in :keyIds and t.language.id in :languageIds
    """,
  )
  fun findPendingTargets(
    keyIds: Collection<Long>,
    languageIds: Collection<Long>,
  ): List<AiReviewTargetView>

  @Query(
    """
    select new io.tolgee.dtos.queryResults.aiReview.AiReviewTargetView(t.id, t.key.id, t.language.id, t.qaChecksStale)
    from TranslationAiReview r
    join r.translation t
    where r.outcome = io.tolgee.model.enums.aiReview.AiReviewOutcome.PENDING and t.key.id in :keyIds
    """,
  )
  fun findPendingTargetsByKeyIds(keyIds: Collection<Long>): List<AiReviewTargetView>

  @Query(
    """
    select new io.tolgee.dtos.queryResults.aiReview.AiReviewTargetView(t.id, t.key.id, t.language.id, t.qaChecksStale)
    from TranslationAiReview r
    join r.translation t
    where r.outcome = io.tolgee.model.enums.aiReview.AiReviewOutcome.PENDING and t.key.project.id = :projectId
    """,
  )
  fun findPendingTargetsByProjectId(projectId: Long): List<AiReviewTargetView>

  @Query(
    """
    select new io.tolgee.dtos.queryResults.aiReview.AiReviewTextView(t.id, t.key.id, t.language.id, t.text)
    from TranslationAiReview r
    join r.translation t
    where t.key.id in :keyIds and t.language.id <> :baseLanguageId
      and (
        r.outcome in :outcomes
        or (
          r.outcome = io.tolgee.model.enums.aiReview.AiReviewOutcome.AUTO_REVIEWED
          and t.state = io.tolgee.model.enums.TranslationState.TRANSLATED
        )
      )
    """,
  )
  fun findSiblingsToResetAfterSourceChange(
    keyIds: Collection<Long>,
    baseLanguageId: Long,
    outcomes: Collection<AiReviewOutcome>,
  ): List<AiReviewTextView>

  @Modifying(flushAutomatically = false)
  @Query(
    nativeQuery = true,
    value = """
    UPDATE translation_ai_review
    SET claimed_at = :now
    WHERE id = :id AND outcome = 'PENDING' AND text_hash = :textHash
      AND (claimed_at IS NULL OR claimed_at < :claimExpiredBefore)
    """,
  )
  fun claimPending(
    id: Long,
    textHash: String,
    now: Date,
    claimExpiredBefore: Date,
  ): Int

  @Modifying(flushAutomatically = false)
  @Query(
    nativeQuery = true,
    value = """
    UPDATE translation_ai_review
    SET claimed_at = NULL
    WHERE id = :id AND outcome = 'PENDING' AND text_hash = :textHash AND claimed_at = :claimedAt
    """,
  )
  fun releaseOwnClaim(
    id: Long,
    textHash: String,
    claimedAt: Date,
  )

  @Query(
    """
    select new io.tolgee.dtos.queryResults.aiReview.AiReviewTargetView(t.id, t.key.id, t.language.id, t.qaChecksStale)
    from TranslationAiReview r
    join r.translation t
    where r.outcome in :outcomes and t.key.id in :keyIds
    """,
  )
  fun findTargetsByKeyIdsAndOutcomes(
    keyIds: Collection<Long>,
    outcomes: Collection<AiReviewOutcome>,
  ): List<AiReviewTargetView>

  @Query(
    """
    select new io.tolgee.dtos.queryResults.aiReview.AiReviewTargetView(t.id, t.key.id, t.language.id, t.qaChecksStale)
    from TranslationAiReview r
    join r.translation t
    where r.outcome in :outcomes and t.key.project.id = :projectId
    """,
  )
  fun findTargetsByProjectIdAndOutcomes(
    projectId: Long,
    outcomes: Collection<AiReviewOutcome>,
  ): List<AiReviewTargetView>

  @Query(
    """
    select new io.tolgee.dtos.queryResults.aiReview.AiReviewTargetView(t.id, t.key.id, t.language.id, t.qaChecksStale)
    from TranslationAiReview r
    join r.translation t
    where r.outcome in :outcomes and r.rule.id = :ruleId
    """,
  )
  fun findTargetsByRuleIdAndOutcomes(
    ruleId: Long,
    outcomes: Collection<AiReviewOutcome>,
  ): List<AiReviewTargetView>

  @Query(
    """
    select new io.tolgee.dtos.queryResults.aiReview.AiReviewTextView(t.id, t.key.id, t.language.id, t.text)
    from TranslationAiReview r
    join r.translation t
    where r.rule.id = :ruleId
      and r.outcome = io.tolgee.model.enums.aiReview.AiReviewOutcome.FAILED
      and t.state = io.tolgee.model.enums.TranslationState.TRANSLATED
    """,
  )
  fun findRetryableFailedByRuleId(ruleId: Long): List<AiReviewTextView>

  @Query(
    """
    select new io.tolgee.dtos.queryResults.aiReview.AiReviewRuleStatsView(r.rule.id, r.dryRun, r.outcome, count(r))
    from TranslationAiReview r
    join r.translation t
    where r.rule.project.id = :projectId
      and (
        r.outcome <> io.tolgee.model.enums.aiReview.AiReviewOutcome.FAILED
        or t.state = io.tolgee.model.enums.TranslationState.TRANSLATED
      )
    group by r.rule.id, r.dryRun, r.outcome
    """,
  )
  fun getRuleStats(projectId: Long): List<AiReviewRuleStatsView>

  @Query(
    """
    select new io.tolgee.dtos.queryResults.aiReview.TranslationAiReviewView(
      r.translation.id, r.outcome, r.score, r.threshold, r.dryRun, r.textHash
    )
    from TranslationAiReview r
    where r.translation.id in :translationIds and r.outcome in :outcomes
    """,
  )
  fun findViewsByTranslationIds(
    translationIds: Collection<Long>,
    outcomes: Collection<AiReviewOutcome>,
  ): List<TranslationAiReviewView>
}
