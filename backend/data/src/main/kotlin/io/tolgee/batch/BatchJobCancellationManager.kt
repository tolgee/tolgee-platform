package io.tolgee.batch

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.activity.ActivityHolder
import io.tolgee.batch.events.JobCancelEvent
import io.tolgee.component.UsingRedisProvider
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
  private val batchJobActionService: BatchJobActionService,
  private val progressManager: ProgressManager,
  private val activityHolder: ActivityHolder
) : Logging {
  @Transactional
  fun cancel(id: Long) {
    cancelJob(id)
    cancelLocalAndRemoteExecutions(id)
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
        jacksonObjectMapper().writeValueAsString(id)
      )
      return
    }
    batchJobActionService.cancelLocalJob(id)
  }

  @EventListener(JobCancelEvent::class)
  fun cancelJobListener(event: JobCancelEvent) {
    batchJobActionService.cancelLocalJob(event.jobId)
  }

  fun cancelJob(jobId: Long) {
    val executions = executeInNewTransaction(transactionManager) {
      entityManager.createNativeQuery("""SET enable_seqscan=off""")
      val executions = entityManager.createQuery(
        """
        from BatchJobChunkExecution bjce  
        where bjce.batchJob.id = :id
        and status = :status
      """,
        BatchJobChunkExecution::class.java
      )
        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
        .setHint(
          "jakarta.persistence.lock.timeout",
          LockOptions.SKIP_LOCKED
        )
        .setParameter("id", jobId)
        .setParameter("status", BatchJobChunkExecutionStatus.PENDING)
        .resultList

      executions.forEach { execution ->
        cancelExecution(execution)
      }

      logger.debug(
        "Cancelling job $jobId, cancelling locked execution ids: ${
        executions.map { it.id }.joinToString(", ")
        }"
      )
      executions
    }
    logger.debug("""Job $jobId cancellation committed. setting transaction committed for the executions.""")
    executions.forEach {
      progressManager.handleChunkCompletedCommitted(it, false)
    }

    progressManager.onJobCompletedCommitted(jobId)
  }

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
}
