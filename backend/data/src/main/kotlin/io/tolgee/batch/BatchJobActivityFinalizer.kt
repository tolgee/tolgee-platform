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
        val revisionIds = getRevisionIds(job.id)
        logger.debug("Merging revisions (${revisionIds.size})")

        val activityRevisionIdToMergeInto = revisionIds.firstOrNull() ?: return@afterFlush
        revisionIds.remove(activityRevisionIdToMergeInto)

        mergeDescribingEntities(activityRevisionIdToMergeInto, revisionIds)
        mergeModifiedEntities(activityRevisionIdToMergeInto, revisionIds)
        deleteUnusedRevisions(revisionIds)
        setJobIdAndAuthorIdToRevision(activityRevisionIdToMergeInto, job)
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

  private fun deleteUnusedRevisions(revisionIds: MutableList<Long>) {
    entityManager
      .createNativeQuery(
        """
          delete from activity_revision where id in (:revisionIds)
        """,
      ).setParameter("revisionIds", revisionIds)
      .executeUpdate()
  }

  private fun mergeModifiedEntities(
    activityRevisionIdToMergeInto: Long,
    revisionIds: MutableList<Long>,
  ) {
    entityManager
      .createNativeQuery(
        """
        update activity_modified_entity set activity_revision_id = :activityRevisionIdToMergeInto
        where activity_revision_id in (:revisionIds)
        """,
      ).setParameter("activityRevisionIdToMergeInto", activityRevisionIdToMergeInto)
      .setParameter("revisionIds", revisionIds)
      .executeUpdate()
  }

  private fun mergeDescribingEntities(
    activityRevisionIdToMergeInto: Long,
    revisionIds: MutableList<Long>,
  ) {
    removeDuplicityDescribingEntities(activityRevisionIdToMergeInto, revisionIds)

    entityManager
      .createNativeQuery(
        """
        update activity_describing_entity set activity_revision_id = :activityRevisionIdToMergeInto
        where activity_revision_id in (:revisionIds)
        """,
      ).setParameter("activityRevisionIdToMergeInto", activityRevisionIdToMergeInto)
      .setParameter("revisionIds", revisionIds)
      .executeUpdate()
  }

  private fun removeDuplicityDescribingEntities(
    activityRevisionIdToMergeInto: Long,
    revisionIds: MutableList<Long>,
  ) {
    entityManager
      .createNativeQuery(
        """
        delete from activity_describing_entity
        where (entity_class, entity_id) in
              (select entity_class, entity_id
               from activity_describing_entity
               where activity_revision_id in (:revisionIds)
                  or activity_revision_id = :activityRevisionIdToMergeInto
               group by entity_class, entity_id
               having count(*) > 1)
        and
            (activity_revision_id, entity_class, entity_id) not in (
            select min(activity_revision_id), entity_class, entity_id
                from activity_describing_entity
                where activity_revision_id in (:revisionIds)
                    or activity_revision_id = :activityRevisionIdToMergeInto
                group by entity_class, entity_id
                having count(*) > 1)
        """.trimIndent(),
      ).setParameter("activityRevisionIdToMergeInto", activityRevisionIdToMergeInto)
      .setParameter("revisionIds", revisionIds)
      .executeUpdate()
  }

  private fun getRevisionIds(jobId: Long): MutableList<Long> =
    entityManager
      .createQuery(
        """
        select ar.id
        from ActivityRevision ar
        join ar.batchJobChunkExecution b
        where b.batchJob.id = :jobId
      """,
        Long::class.javaObjectType,
      ).setParameter("jobId", jobId)
      .resultList
}
