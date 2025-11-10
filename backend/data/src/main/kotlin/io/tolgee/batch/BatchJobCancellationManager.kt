package io.tolgee.batch

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.activity.ActivityHolder
import io.tolgee.batch.cleaning.BatchJobStatusProvider
import io.tolgee.batch.events.JobCancelEvent
import io.tolgee.component.UsingRedisProvider
import io.tolgee.fixtures.waitFor
import io.tolgee.model.batch.BatchJobChunkExecution
import io.tolgee.model.batch.BatchJobChunkExecutionStatus
import io.tolgee.pubSub.RedisPubSubReceiverConfiguration
import io.tolgee.util.Logging
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.logger
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import org.hibernate.LockOptions
import org.springframework.context.annotation.Lazy
import org.springframework.context.event.EventListener
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional

@Component
class BatchJobCancellationManager(
  private val usingRedisProvider: UsingRedisProvider,
  @Lazy
  private val redisTemplate: StringRedisTemplate,
  private val entityManager: EntityManager,
  private val transactionManager: PlatformTransactionManager,
  private val progressManager: ProgressManager,
  private val activityHolder: ActivityHolder,
  private val batchJobService: BatchJobService,
  private val batchJobStatusProvider: BatchJobStatusProvider,
  private val batchJobChunkExecutionQueue: BatchJobChunkExecutionQueue,
  private val concurrentExecutionLauncher: BatchJobConcurrentLauncher,
) : Logging {
  @Transactional
  fun cancel(id: Long) {
    try {
      tryCancel(id)
    } catch (e: CancellationTimeoutException) {
      tryCancel(id)
    }
  }

  private fun tryCancel(id: Long) {
    cancelLocalAndRemoteExecutions(id)
    cancelJob(id)
  }

  /**
   * Sends request to cancel job executions on all instances
   *
   * If using redis, it rends a message to redis channel
   * If not, it just cancels local jobs
   */
  private fun cancelLocalAndRemoteExecutions(id: Long) {
    if (usingRedisProvider.areWeUsingRedis) {
      redisTemplate.convertAndSend(
        RedisPubSubReceiverConfiguration.JOB_CANCEL_TOPIC,
        jacksonObjectMapper().writeValueAsString(id),
      )
      return
    }
    cancelLocalJob(id)
  }

  @EventListener(JobCancelEvent::class)
  fun cancelJobListener(event: JobCancelEvent) {
    cancelLocalJob(event.jobId)
  }

  fun cancelJob(jobId: Long) {
    val executions = cancelExecutions(jobId)
    logger.debug("""Job $jobId cancellation committed. setting transaction committed for the executions.""")
    executions.forEach {
      progressManager.handleChunkCompletedCommitted(it, false)
    }

    handleJobStatusAfterCancellation(jobId)
  }

  private fun cancelExecutions(jobId: Long): MutableList<BatchJobChunkExecution> =
    executeInNewTransaction(transactionManager) {
      entityManager.createNativeQuery("""SET enable_seqscan=off""")
      val executions = getUnlockedPendingExecutions(jobId)

      logger.debug(
        "Cancelling job $jobId, cancelling unlocked execution ids: ${
          executions.map { it.id }.joinToString(", ")
        }",
      )

      executions.forEach { execution ->
        cancelExecution(execution)
      }

      executions
    }

  private fun getUnlockedPendingExecutions(jobId: Long): MutableList<BatchJobChunkExecution> =
    entityManager
      .createQuery(
        """
            from BatchJobChunkExecution bjce  
            where bjce.batchJob.id = :id
            and status = :status
          """,
        BatchJobChunkExecution::class.java,
      ).setLockMode(LockModeType.PESSIMISTIC_WRITE)
      .setHint(
        "jakarta.persistence.lock.timeout",
        LockOptions.SKIP_LOCKED,
      ).setParameter("id", jobId)
      .setParameter("status", BatchJobChunkExecutionStatus.PENDING)
      .resultList

  private fun handleJobStatusAfterCancellation(jobId: Long) {
    var statusUpdated = tryUpdateJobStatusToCompleted(jobId)
    if (statusUpdated) {
      return
    }

    try {
      tryWaitForBatchJobCompletedStatus(jobId)
    } catch (e: Exception) {
      statusUpdated = tryUpdateJobStatusToCompleted(jobId)
      if (!statusUpdated) {
        throw CancellationTimeoutException()
      }
    }
  }

  private fun tryWaitForBatchJobCompletedStatus(jobId: Long) {
    waitFor(pollTime = 200, timeout = 2000) {
      executeInNewTransaction(transactionManager, readOnly = true) {
        batchJobService.getJobDto(jobId).status.completed
      }
    }
  }

  private fun tryUpdateJobStatusToCompleted(jobId: Long): Boolean {
    return executeInNewTransaction(transactionManager) {
      val statuses = getStatuses(jobId)
      if (statuses.all { it.completed }) {
        updateJobStatusToCompleted(jobId, statuses)
        return@executeInNewTransaction true
      }
      return@executeInNewTransaction false
    }
  }

  private fun updateJobStatusToCompleted(
    jobId: Long,
    statuses: MutableList<BatchJobChunkExecutionStatus>,
  ) {
    val entity = batchJobService.getJobEntity(jobId)
    entity.status = batchJobStatusProvider.getNewStatus(statuses)
    batchJobService.save(entity)
    progressManager.onJobCompletedCommitted(jobId)
  }

  private fun getStatuses(jobId: Long): MutableList<BatchJobChunkExecutionStatus> =
    entityManager
      .createQuery(
        """
            select e.status from BatchJobChunkExecution e 
            where e.batchJob.id = :id 
            group by e.status
            """,
        BatchJobChunkExecutionStatus::class.java,
      ).setParameter("id", jobId)
      .resultList

  fun cancelExecution(execution: BatchJobChunkExecution) {
    execution.status = BatchJobChunkExecutionStatus.CANCELLED
    entityManager.persist(execution)
    progressManager.handleProgress(execution)
    incrementCancelledCount()
  }

  private fun incrementCancelledCount() {
    val current = activityHolder.activityRevision.cancelledBatchJobExecutionCount ?: 0
    activityHolder.activityRevision.cancelledBatchJobExecutionCount = current + 1
  }

  fun cancelLocalJob(jobId: Long) {
    batchJobChunkExecutionQueue.removeJobExecutions(jobId)
    concurrentExecutionLauncher.runningJobs.filter { it.value.first.id == jobId }.forEach {
      it.value.second.cancel()
    }
  }
}
