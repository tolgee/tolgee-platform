package io.tolgee.repository.aiReview

import io.tolgee.model.aiReview.TranslationAiReview
import io.tolgee.model.enums.aiReview.AiReviewStatus
import io.tolgee.service.aiReview.AiReviewManaged
import io.tolgee.service.aiReview.AiReviewQueued
import io.tolgee.service.aiReview.AiReviewTextHash
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface TranslationAiReviewRepository : JpaRepository<TranslationAiReview, Long> {
  fun findByTranslationId(translationId: Long): TranslationAiReview?

  fun findAllByTranslationIdIn(translationIds: Collection<Long>): List<TranslationAiReview>

  @Query(
    """
    select r from TranslationAiReview r
    join fetch r.translation t
    join fetch t.key
    join fetch t.language
    where t.id in :translationIds
    """,
  )
  fun findAllWithTranslationByTranslationIdIn(translationIds: Collection<Long>): List<TranslationAiReview>

  // `batch_job_id = :jobId` in the guard lets a retried chunk of the same job take its item again.
  @Modifying
  @Query(
    nativeQuery = true,
    value = """
    update translation_ai_review r
    set batch_job_id = :jobId, updated_at = now()
    where r.translation_id = :translationId
      and r.text_hash = :textHash
      and ${AiReviewQueued.SQL}
      and (r.batch_job_id is null or r.batch_job_id = :jobId)
    """,
  )
  fun claimForJob(
    jobId: Long,
    translationId: Long,
    textHash: String,
  ): Int

  @Query(
    """
    select t.id from TranslationAiReview r
    join r.translation t
    join t.key k
    left join k.branch b
    where k.project.id = :projectId
      and k.deletedAt is null and (b is null or b.deletedAt is null)
      and t.id in :translationIds
      and ${AiReviewQueued.JPQL}
      and (t.language.id not in :qaLanguageIds or t.qaChecksStale = false)
    """,
  )
  fun findQueuedTranslationIdsWithFreshQa(
    projectId: Long,
    translationIds: Collection<Long>,
    qaLanguageIds: Collection<Long>,
  ): List<Long>

  @Query(
    """
    select t.id from TranslationAiReview r
    join r.translation t
    join t.key k
    left join k.branch b
    where k.project.id = :projectId
      and k.deletedAt is null and (b is null or b.deletedAt is null)
      and t.id in :translationIds
      and ${AiReviewQueued.JPQL}
    """,
  )
  fun findQueuedTranslationIds(
    projectId: Long,
    translationIds: Collection<Long>,
  ): List<Long>

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
      and ${AiReviewQueued.SQL}
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
    join t.key k
    left join k.branch b
    where k.project.id = :projectId
      and k.deletedAt is null and (b is null or b.deletedAt is null)
      and ${AiReviewQueued.JPQL}
      and (t.language.id not in :qaLanguageIds or t.qaChecksStale = false)
    """,
  )
  fun findQueuedTranslationIdsWithFreshQa(
    projectId: Long,
    qaLanguageIds: Collection<Long>,
  ): List<Long>

  @Query(
    """
    select t.id from TranslationAiReview r
    join r.translation t
    join t.key k
    left join k.branch b
    where k.project.id = :projectId
      and k.deletedAt is null and (b is null or b.deletedAt is null)
      and ${AiReviewQueued.JPQL}
    """,
  )
  fun findQueuedTranslationIds(projectId: Long): List<Long>

  @Query(
    """
    select distinct t.key.id from TranslationAiReview r
    join r.translation t
    where t.key.project.id = :projectId
      and r.status = io.tolgee.model.enums.aiReview.AiReviewStatus.APPROVED
    """,
  )
  fun findApprovedKeyIds(projectId: Long): List<Long>

  @Query(
    """
    select t.id from TranslationAiReview r
    join r.translation t
    where t.key.id in :keyIds
      and r.status = io.tolgee.model.enums.aiReview.AiReviewStatus.APPROVED
    """,
  )
  fun findApprovedTranslationIdsByKeyIds(keyIds: Collection<Long>): List<Long>

  @Query(
    """
    select t.id from TranslationAiReview r
    join r.translation t
    where r.rule.id = :ruleId
      and (
        (r.status = io.tolgee.model.enums.aiReview.AiReviewStatus.FAILED and r.failureReason is not null)
        or r.lastFailureReason is not null
      )
    """,
  )
  fun findFailedTranslationIdsByRule(ruleId: Long): List<Long>

  @Modifying
  @Query(
    """
    delete from TranslationAiReview r
    where r.manualRunBatchJobId = :jobId and r.status = io.tolgee.model.enums.aiReview.AiReviewStatus.PENDING
    """,
  )
  fun deletePendingOfManualRun(jobId: Long): Int

  @Modifying
  @Query(
    """
    update TranslationAiReview r
    set r.reReviewRequested = false, r.manualRunBatchJobId = null
    where r.manualRunBatchJobId = :jobId and r.reReviewRequested = true
    """,
  )
  fun clearReReviewOfManualRun(jobId: Long): Int

  @Modifying
  @Query(
    """
    delete from TranslationAiReview r
    where r.rule.id = :ruleId and r.status in :statuses
      and not exists (
        select 1 from Translation t where t.id = r.translation.id and ${AiReviewManaged.JPQL}
      )
    """,
  )
  fun deleteByRuleAndStatusesUnlessAiManaged(
    ruleId: Long,
    statuses: Collection<AiReviewStatus>,
  ): Int

  @Query(
    """
    select distinct k.project.id from TranslationAiReview r
    join r.translation t
    join t.key k
    left join k.branch b
    where k.deletedAt is null and (b is null or b.deletedAt is null)
      and ${AiReviewQueued.JPQL}
      and k.project.organizationOwner.id = :organizationId
    """,
  )
  fun findProjectIdsWithClaimableRows(organizationId: Long): List<Long>

  @Query(
    """
    select distinct k.project.id from TranslationAiReview r
    join r.translation t
    join t.key k
    left join k.branch b
    where k.deletedAt is null and (b is null or b.deletedAt is null)
      and ${AiReviewQueued.JPQL}
    """,
  )
  fun findProjectIdsWithClaimableRows(): List<Long>

  @Query(
    nativeQuery = true,
    value = """
    select exists (
      select 1 from translation t
      join translation sibling on sibling.key_id = t.key_id
      join translation_ai_review r on r.translation_id = sibling.id
      where t.id in :translationIds
    )
    """,
  )
  fun existsOnKeysOfTranslations(translationIds: Collection<Long>): Boolean

  @Query(
    nativeQuery = true,
    value = """
    select exists (
      select 1 from activity_modified_entity me
      join translation t on t.id = me.entity_id
      join translation sibling on sibling.key_id = t.key_id
      join translation_ai_review r on r.translation_id = sibling.id
      where me.activity_revision_id = :activityRevisionId and me.entity_class = 'Translation'
    )
    """,
  )
  fun existsOnKeysOfRevisionTranslations(activityRevisionId: Long): Boolean

  @Query(
    """
    select r from TranslationAiReview r
    join fetch r.translation t
    join fetch t.key
    join fetch t.language
    where t.key.id in :keyIds and t.language.id <> :baseLanguageId
    and r.status in :statuses
    """,
  )
  fun findSiblingRows(
    keyIds: Collection<Long>,
    baseLanguageId: Long,
    statuses: Collection<AiReviewStatus>,
  ): List<TranslationAiReview>

  @Modifying
  @Query(
    """
    delete from TranslationAiReview r
    where r.translation.id in (
      select t.id from Translation t where t.key.project.id = :projectId
    )
    and r.status not in :keptStatuses
    """,
  )
  fun deleteByProject(
    projectId: Long,
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
      and r.text_hash = ${AiReviewTextHash.CURRENT_TEXT_SQL}
      and (r.status <> 'FAILED' or r.failure_reason is not null)
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
