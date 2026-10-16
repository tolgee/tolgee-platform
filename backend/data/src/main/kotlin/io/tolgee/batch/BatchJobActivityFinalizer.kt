package io.tolgee.batch

import io.sentry.Sentry
import io.tolgee.activity.ActivityHolder
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.events.OnBatchJobCancelled
import io.tolgee.batch.events.OnBatchJobFailed
import io.tolgee.batch.events.OnBatchJobFinalized
import io.tolgee.batch.events.OnBatchJobSucceeded
import io.tolgee.batch.state.BatchJobStateProvider
import io.tolgee.fixtures.WaitNotSatisfiedException
import io.tolgee.fixtures.waitFor
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.util.Logging
import io.tolgee.util.logger
import jakarta.persistence.EntityManager
import org.hibernate.query.TypedParameterValue
import org.hibernate.type.StandardBasicTypes
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class BatchJobActivityFinalizer(
  private val entityManager: EntityManager,
  private val activityHolder: ActivityHolder,
  private val batchJobStateProvider: BatchJobStateProvider,
  private val applicationEventPublisher: ApplicationEventPublisher,
) : Logging {
  @EventListener(OnBatchJobSucceeded::class)
  fun finalizeActivityWhenJobSucceeded(event: OnBatchJobSucceeded) {
    finalizeActivityWhenJobCompleted(event.job)
  }

  @EventListener(OnBatchJobFailed::class)
  fun finalizeActivityWhenJobFailed(event: OnBatchJobFailed) {
    finalizeActivityWhenJobCompleted(event.job)
  }

  @EventListener(OnBatchJobCancelled::class)
  fun finalizeActivityWhenJobCancelled(event: OnBatchJobCancelled) {
    finalizeActivityWhenJobCompleted(event.job)
  }

  fun finalizeActivityWhenJobCompleted(job: BatchJobDto) {
    activityHolder.afterActivityFlushed = afterFlush@{
      entityManager.clear()
      try {
        logger.debug("Finalizing activity for job ${job.id} (after flush)")
        waitForOtherChunksToComplete(job)
        val activityRevisionIdToMergeInto = getRevisionIdToMergeInto(job.id)
        if (activityRevisionIdToMergeInto != null) {
          removeDuplicityDescribingEntities(job.id)
          mergeDescribingEntities(activityRevisionIdToMergeInto, job.id)
          mergeModifiedEntities(activityRevisionIdToMergeInto, job.id)
          deleteUnusedRevisions(activityRevisionIdToMergeInto, job.id)
          setJobIdAndAuthorIdToRevision(activityRevisionIdToMergeInto, job)
        }
        applicationEventPublisher.publishEvent(OnBatchJobFinalized(job, activityRevisionIdToMergeInto))
      } catch (e: Exception) {
        Sentry.captureException(e)
        throw CannotFinalizeActivityException(e)
      }
    }
  }

  private fun waitForOtherChunksToComplete(job: BatchJobDto) {
    // how many executions are being committed in this transaction
    val committedInThisTransactions = activityHolder.activityRevision.cancelledBatchJobExecutionCount ?: 1
    logger.debug("Committed chunks executions in this transaction: $committedInThisTransactions")

    retryWaitingWithBatchJobResetting(job.id) {
      val committedChunks =
        batchJobStateProvider
          .get(job.id)
          .values
          .count { it.retry == false && it.transactionCommitted && it.status.completed }
      logger.debug(
        "Waiting for completed chunks and committed ($committedChunks) to be equal to all other chunks" +
          " count (${job.totalChunks - committedInThisTransactions})",
      )
      committedChunks == job.totalChunks - committedInThisTransactions
    }
  }

  private fun retryWaitingWithBatchJobResetting(
    jobId: Long,
    fn: () -> Boolean,
  ) {
    waitForNotThrowing(WaitNotSatisfiedException::class) {
      try {
        waitFor(2000) {
          fn()
        }
      } catch (e: WaitNotSatisfiedException) {
        batchJobStateProvider.removeJobState(jobId)
        throw e
      }
    }
  }

  private fun setJobIdAndAuthorIdToRevision(
    activityRevisionIdToMergeInto: Long,
    job: BatchJobDto,
  ) {
    entityManager
      .createNativeQuery(
        """
        update activity_revision
        set
         batch_job_chunk_execution_id = null,
         batch_job_id = :jobId,
         author_id = :authorId
        where id = :activityRevisionIdToMergeInto
        """,
      ).setParameter("activityRevisionIdToMergeInto", activityRevisionIdToMergeInto)
      .setParameter("jobId", job.id)
      .setParameter("authorId", TypedParameterValue(StandardBasicTypes.LONG, job.authorId))
      .executeUpdate()
  }

  private fun deleteUnusedRevisions(
    activityRevisionIdToMergeInto: Long,
    jobId: Long,
  ) {
    entityManager
      .createNativeQuery(
        """
        delete from activity_revision
        where id in ($JOB_REVISION_IDS_QUERY) and id <> :activityRevisionIdToMergeInto
        """,
      ).setParameter("activityRevisionIdToMergeInto", activityRevisionIdToMergeInto)
      .setParameter("jobId", jobId)
      .executeUpdate()
  }

  private fun mergeModifiedEntities(
    activityRevisionIdToMergeInto: Long,
    jobId: Long,
  ) {
    entityManager
      .createNativeQuery(
        """
        update activity_modified_entity set activity_revision_id = :activityRevisionIdToMergeInto
        where activity_revision_id in ($JOB_REVISION_IDS_QUERY)
          and activity_revision_id <> :activityRevisionIdToMergeInto
        """,
      ).setParameter("activityRevisionIdToMergeInto", activityRevisionIdToMergeInto)
      .setParameter("jobId", jobId)
      .executeUpdate()
  }

  private fun mergeDescribingEntities(
    activityRevisionIdToMergeInto: Long,
    jobId: Long,
  ) {
    entityManager
      .createNativeQuery(
        """
        update activity_describing_entity set activity_revision_id = :activityRevisionIdToMergeInto
        where activity_revision_id in ($JOB_REVISION_IDS_QUERY)
          and activity_revision_id <> :activityRevisionIdToMergeInto
        """,
      ).setParameter("activityRevisionIdToMergeInto", activityRevisionIdToMergeInto)
      .setParameter("jobId", jobId)
      .executeUpdate()
  }

  /**
   * Keeps one describing entity per (entity_class, entity_id) among the job's revisions - the one with the lowest
   * revision id - so the merge into a single revision cannot violate the primary key.
   */
  private fun removeDuplicityDescribingEntities(jobId: Long) {
    entityManager
      .createNativeQuery(
        """
        delete from activity_describing_entity ade
        using (
          select activity_revision_id, entity_class, entity_id,
            row_number() over (partition by entity_class, entity_id order by activity_revision_id) as rn
          from activity_describing_entity
          where activity_revision_id in ($JOB_REVISION_IDS_QUERY)
        ) duplicate
        where duplicate.rn > 1
          and ade.activity_revision_id = duplicate.activity_revision_id
          and ade.entity_class = duplicate.entity_class
          and ade.entity_id = duplicate.entity_id
        """.trimIndent(),
      ).setParameter("jobId", jobId)
      .executeUpdate()
  }

  private fun getRevisionIdToMergeInto(jobId: Long): Long? =
    entityManager
      .createNativeQuery("select min(id) from ($JOB_REVISION_IDS_QUERY) revision_ids(id)")
      .setParameter("jobId", jobId)
      .singleResult
      ?.let { (it as Number).toLong() }

  companion object {
    private const val JOB_REVISION_IDS_QUERY = """
      select ar.id
      from activity_revision ar
      join tolgee_batch_job_chunk_execution bjce on bjce.id = ar.batch_job_chunk_execution_id
      where bjce.batch_job_id = :jobId
    """
  }
}
