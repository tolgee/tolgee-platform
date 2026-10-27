package io.tolgee.repository.aiReview

import io.tolgee.model.aiReview.TranslationAiReview
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
