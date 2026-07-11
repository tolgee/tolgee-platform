package io.tolgee.batch.cleaning

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import io.tolgee.batch.BatchJobProjectLockingManager
import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.LockingProvider
import io.tolgee.component.SchedulingManager
import io.tolgee.configuration.tolgee.BatchProperties
import io.tolgee.util.Logging
import io.tolgee.util.addDays
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.logger
import io.tolgee.util.runSentryCatching
import jakarta.persistence.EntityManager
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Lazy
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import java.time.Duration
import java.util.Date
import java.util.concurrent.atomic.AtomicLong

@Component
class OldBatchJobCleaner(
  private val entityManager: EntityManager,
  private val currentDateProvider: CurrentDateProvider,
  private val batchProperties: BatchProperties,
  private val meterRegistry: MeterRegistry,
  private val lockingProvider: LockingProvider,
  private val transactionManager: PlatformTransactionManager,
  private val schedulingManager: SchedulingManager,
  @Lazy
  private val lockingManager: BatchJobProjectLockingManager,
) : Logging {
  @EventListener(ApplicationReadyEvent::class)
  fun scheduleCleanup() {
    if (!batchProperties.oldJobCleanupEnabled) {
      logger.info("Old batch job cleanup is disabled, skipping scheduling")
      return
    }
    val period = Duration.ofMillis(batchProperties.oldJobCleanupDelayInMs)
    schedulingManager.scheduleWithFixedDelay(::cleanup, period)
    logger.debug("Scheduled old batch job cleanup task with period: {}", period)
  }

  fun cleanup() {
    val leaseTime = Duration.ofMillis(batchProperties.jobCleanupLockLeaseTimeMs)
    lockingProvider.withLockingIfFree(CLEANUP_LOCK_NAME, leaseTime) {
      runSentryCatching {
        cleanupTimer.record(
          Runnable {
            cleanupCompletedJobs()
            cleanupFailedJobs()
            lastCleanupTimestamp.set(System.currentTimeMillis() / 1000)
          },
        )
      }
    }
  }

  fun cleanupCompletedJobs() {
    val cutoffDate = currentDateProvider.date.addDays(-batchProperties.completedJobRetentionDays)
    deleteJobsOlderThan(listOf("SUCCESS", "CANCELLED"), cutoffDate, "completed")
  }

  fun cleanupFailedJobs() {
    val cutoffDate = currentDateProvider.date.addDays(-batchProperties.failedJobRetentionDays)
    deleteJobsOlderThan(listOf("FAILED"), cutoffDate, "failed")
  }

  private fun deleteJobsOlderThan(
    statuses: List<String>,
    cutoffDate: Date,
    jobType: String,
  ) {
    var totalDeleted = 0
    var totalChunksDeleted = 0

    while (true) {
      val (jobs, chunks) =
        executeInNewTransaction(transactionManager) {
          deleteJobBatch(statuses, cutoffDate)
        }
      if (jobs == 0) break

      totalDeleted += jobs
      totalChunksDeleted += chunks
      logger.info("Deleted batch of $jobs old $jobType batch jobs with $chunks chunks")
    }

    if (totalDeleted == 0) return

    logger.info(
      "Total deleted $totalDeleted old $jobType batch jobs with $totalChunksDeleted chunks older than $cutoffDate",
    )
    recordMetrics(jobType, totalDeleted, totalChunksDeleted)
  }

  private fun deleteJobBatch(
    statuses: List<String>,
    cutoffDate: Date,
  ): Pair<Int, Int> {
    val jobs = findJobsToDelete(statuses, cutoffDate)
    if (jobs.isEmpty()) return Pair(0, 0)

    val jobIds = jobs.map { it.jobId }
    val executionIds = findExecutionIds(jobIds)
    nullifyActivityRevisionReferences(jobIds, executionIds)
    deleteChunkExecutions(jobIds, executionIds)
    deleteBatchJobs(jobIds)
    releaseProjectLocks(jobs)

    return Pair(jobIds.size, executionIds.size)
  }

  private fun findJobsToDelete(
    statuses: List<String>,
    cutoffDate: Date,
  ): List<JobToDelete> {
    val statusesPlaceholder = statuses.mapIndexed { index, _ -> ":status$index" }.joinToString(", ")
    val query =
      entityManager
        .createNativeQuery(
          """
          SELECT id, project_id FROM tolgee_batch_job
          WHERE status IN ($statusesPlaceholder)
          AND updated_at < :cutoffDate
          LIMIT :batchSize
          """,
        )

    statuses.forEachIndexed { index, status ->
      query.setParameter("status$index", status)
    }

    @Suppress("UNCHECKED_CAST")
    val rows =
      query
        .setParameter("cutoffDate", cutoffDate)
        .setParameter("batchSize", batchProperties.jobCleanupBatchSize)
        .resultList as List<Array<Any?>>

    return rows.map {
      JobToDelete(
        jobId = (it[0] as Number).toLong(),
        projectId = (it[1] as Number?)?.toLong(),
      )
    }
  }

  /**
   * Releases any project lock still held by the deleted jobs. Normally the lock is released
   * when the job completes, but if that ever failed (process crash, exception in the cancel
   * path, etc.), the entry in `project_batch_job_locks` would point at a job id whose DB row
   * is now gone — permanently blocking every future batch job for that project. This is a
   * belt-and-suspenders cleanup tied to the same transaction boundary as the row deletion.
   */
  private fun releaseProjectLocks(jobs: List<JobToDelete>) {
    jobs.forEach { lockingManager.unlockJobForProject(it.projectId, it.jobId) }
  }

  private data class JobToDelete(
    val jobId: Long,
    val projectId: Long?,
  )

  private fun findExecutionIds(jobIds: List<Long>): List<Long> {
    @Suppress("UNCHECKED_CAST")
    return entityManager
      .createNativeQuery(
        """
        SELECT id FROM tolgee_batch_job_chunk_execution
        WHERE batch_job_id IN :jobIds
        """,
      ).setParameter("jobIds", jobIds)
      .resultList as List<Long>
  }

  private fun nullifyActivityRevisionReferences(
    jobIds: List<Long>,
    executionIds: List<Long>,
  ) {
    if (executionIds.isEmpty()) {
      nullifyActivityRevisionBatchJobReferences(jobIds)
      return
    }
    nullifyActivityRevisionAllReferences(jobIds)
  }

  private fun nullifyActivityRevisionBatchJobReferences(jobIds: List<Long>) {
    entityManager
      .createNativeQuery(
        """
        UPDATE activity_revision
        SET batch_job_id = NULL
        WHERE batch_job_id IN :jobIds
        """,
      ).setParameter("jobIds", jobIds)
      .executeUpdate()
  }

  private fun nullifyActivityRevisionAllReferences(jobIds: List<Long>) {
    entityManager
      .createNativeQuery(
        """
        UPDATE activity_revision ar
        SET batch_job_id = NULL, batch_job_chunk_execution_id = NULL
        WHERE ar.batch_job_id IN :jobIds
        OR ar.batch_job_chunk_execution_id IN (
          SELECT id FROM tolgee_batch_job_chunk_execution WHERE batch_job_id IN :jobIds
        )
        """,
      ).setParameter("jobIds", jobIds)
      .executeUpdate()
  }

  private fun deleteChunkExecutions(
    jobIds: List<Long>,
    executionIds: List<Long>,
  ) {
    if (executionIds.isEmpty()) return

    entityManager
      .createNativeQuery(
        """
        DELETE FROM tolgee_batch_job_chunk_execution
        WHERE batch_job_id IN :jobIds
        """,
      ).setParameter("jobIds", jobIds)
      .executeUpdate()
  }

  private fun deleteBatchJobs(jobIds: List<Long>) {
    entityManager
      .createNativeQuery(
        """
        DELETE FROM tolgee_batch_job
        WHERE id IN :jobIds
        """,
      ).setParameter("jobIds", jobIds)
      .executeUpdate()
  }

  private fun recordMetrics(
    jobType: String,
    jobsDeleted: Int,
    chunksDeleted: Int,
  ) {
    meterRegistry
      .counter(METRIC_DELETED_JOBS, "job_type", jobType)
      .increment(jobsDeleted.toDouble())
    meterRegistry
      .counter(METRIC_DELETED_CHUNKS, "job_type", jobType)
      .increment(chunksDeleted.toDouble())
  }

  companion object {
    private const val METRIC_DELETED_JOBS = "tolgee_batch_job_cleanup_deleted_jobs_total"
    private const val METRIC_DELETED_CHUNKS = "tolgee_batch_job_cleanup_deleted_chunks_total"
    private const val METRIC_DURATION = "tolgee_batch_job_cleanup_duration_seconds"
    private const val CLEANUP_LOCK_NAME = "old_batch_job_cleanup_lock"
  }

  // Metrics - placed at end of class per coding conventions
  private val lastCleanupTimestamp = AtomicLong(0)

  private val cleanupTimer: Timer =
    Timer
      .builder(METRIC_DURATION)
      .description("Duration of cleanup operation")
      .register(meterRegistry)
}
