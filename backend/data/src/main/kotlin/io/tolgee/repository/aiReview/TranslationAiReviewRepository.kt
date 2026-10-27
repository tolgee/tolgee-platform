package io.tolgee.repository.aiReview

import io.tolgee.model.aiReview.TranslationAiReview
import io.tolgee.model.enums.aiReview.AiReviewStatus
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface TranslationAiReviewRepository : JpaRepository<TranslationAiReview, Long> {
  fun findByTranslationId(translationId: Long): TranslationAiReview?

  /**
   * Assigns one queued row to a job before the LLM call (the "claim" step in
   * docs/ai-review/README.md). Runs under READ COMMITTED: a concurrent update waits on the row lock,
   * re-checks the WHERE and updates 0 rows. `batch_job_id = :jobId` in the guard lets a retried
   * chunk of the same job take its item again.
   */
  @Modifying
  @Query(
    nativeQuery = true,
    value = """
    update translation_ai_review
    set batch_job_id = :jobId, rule_id = :ruleId, rule_mode = :ruleMode,
        prompt_id = :promptId, prompt_name = :promptName, prompt_revision = :promptRevision,
        threshold = :threshold, updated_at = now()
    where translation_id = :translationId
      and text_hash = :textHash
      and (status = 'PENDING' or re_review_requested)
      and (batch_job_id is null or batch_job_id = :jobId)
    """,
  )
  fun assignToJob(
    jobId: Long,
    translationId: Long,
    textHash: String,
    ruleId: Long,
    ruleMode: String,
    promptId: Long?,
    promptName: String,
    promptRevision: Int?,
    threshold: Int,
  ): Int

  @Query(
    """
    select t.id from TranslationAiReview r
    join r.translation t
    where t.key.project.id = :projectId
      and t.id in :translationIds
      and (r.status = io.tolgee.model.enums.aiReview.AiReviewStatus.PENDING or r.reReviewRequested = true)
      and (t.language.id not in :qaLanguageIds or t.qaChecksStale = false)
    """,
  )
  fun findReadyTranslationIds(
    projectId: Long,
    translationIds: Collection<Long>,
    qaLanguageIds: Collection<Long>,
  ): List<Long>

  @Query(
    """
    select t.id from TranslationAiReview r
    join r.translation t
    where t.key.project.id = :projectId
      and t.id in :translationIds
      and (r.status = io.tolgee.model.enums.aiReview.AiReviewStatus.PENDING or r.reReviewRequested = true)
    """,
  )
  fun findClaimableTranslationIds(
    projectId: Long,
    translationIds: Collection<Long>,
  ): List<Long>

  /**
   * Clears the claim of rows whose job finished, failed, was cancelled or is missing — a pod can
   * crash between the claim and the save.
   */
  @Modifying
  @Query(
    nativeQuery = true,
    value = """
    update translation_ai_review r
    set batch_job_id = null
    from translation t
    join key k on k.id = t.key_id
    where t.id = r.translation_id
      and k.project_id = :projectId
      and r.batch_job_id is not null
      and (r.status = 'PENDING' or r.re_review_requested)
      and not exists (
        select 1 from tolgee_batch_job b
        where b.id = r.batch_job_id and b.status in ('PENDING', 'RUNNING')
      )
    """,
  )
  fun releaseStaleClaims(projectId: Long): Int

  @Query(
    """
    select t.id from TranslationAiReview r
    join r.translation t
    where t.key.project.id = :projectId
      and (r.status = io.tolgee.model.enums.aiReview.AiReviewStatus.PENDING or r.reReviewRequested = true)
      and (t.language.id not in :qaLanguageIds or t.qaChecksStale = false)
    """,
  )
  fun findReadyTranslationIds(
    projectId: Long,
    qaLanguageIds: Collection<Long>,
  ): List<Long>

  @Query(
    """
    select t.id from TranslationAiReview r
    join r.translation t
    where t.key.project.id = :projectId
      and (r.status = io.tolgee.model.enums.aiReview.AiReviewStatus.PENDING or r.reReviewRequested = true)
    """,
  )
  fun findClaimableTranslationIds(projectId: Long): List<Long>

  @Modifying
  @Query("delete from TranslationAiReview r where r.translation.id in :translationIds")
  fun deleteAllByTranslationIds(translationIds: Collection<Long>): Int

  @Modifying
  @Query(
    """
    delete from TranslationAiReview r
    where r.translation.id in (
      select t.id from Translation t
      where t.key.id in :keyIds and t.language.id <> :baseLanguageId
    )
    and r.status in :statuses
    """,
  )
  fun deleteSiblingRows(
    keyIds: Collection<Long>,
    baseLanguageId: Long,
    statuses: Collection<AiReviewStatus>,
  ): Int

  @Modifying
  @Query(
    """
    delete from TranslationAiReview r
    where r.translation.id in (
      select t.id from Translation t where t.language.id = :languageId
    )
    and r.status not in :keptStatuses
    """,
  )
  fun deleteByLanguage(
    languageId: Long,
    keptStatuses: Collection<AiReviewStatus>,
  ): Int

  @Query(
    nativeQuery = true,
    value = """
    select r.rule_id as ruleId, r.status as status, (r.last_failure_reason is not null) as lastFailed,
           count(*) as count
    from translation_ai_review r
    join translation t on t.id = r.translation_id
    join key k on k.id = t.key_id
    where k.project_id = :projectId
      and r.rule_id is not null
      and r.text_hash = encode(sha256(convert_to(coalesce(t.text, ''), 'UTF8')), 'hex')
    group by r.rule_id, r.status, (r.last_failure_reason is not null)
    """,
  )
  fun countByRuleAndStatus(projectId: Long): List<RuleStatusCountView>

  interface RuleStatusCountView {
    val ruleId: Long
    val status: String
    val lastFailed: Boolean
    val count: Long
  }
}
