package io.tolgee.batch

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.component.UsingRedisProvider
import io.tolgee.model.batch.BatchJobChunkExecution
import io.tolgee.model.batch.BatchJobChunkExecutionStatus
import io.tolgee.pubSub.RedisPubSubReceiverConfiguration
import io.tolgee.util.executeInNewTransaction
import org.hibernate.LockOptions
import org.springframework.context.annotation.Lazy
import org.springframework.context.event.EventListener
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager
import javax.persistence.LockModeType

@Component
class BatchJobCancellationManager(
  private val usingRedisProvider: UsingRedisProvider,
  @Lazy
  private val redisTemplate: StringRedisTemplate,
  private val entityManager: EntityManager,
  private val transactionManager: PlatformTransactionManager,
  private val batchJobActionService: BatchJobActionService,
  private val progressManager: ProgressManager
) {
  @Transactional
  fun cancel(id: Long) {
    cancelJob(id)
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
    executeInNewTransaction(
      transactionManager = transactionManager,
      isolationLevel = TransactionDefinition.ISOLATION_DEFAULT
    ) {
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
          "javax.persistence.lock.timeout",
          LockOptions.SKIP_LOCKED
        )
        .setParameter("id", jobId)
        .setParameter("status", BatchJobChunkExecutionStatus.PENDING)
        .resultList

      executions.forEach { execution ->
        cancelExecution(execution)
      }
    }
  }

  fun cancelExecution(execution: BatchJobChunkExecution) {
    execution.status = BatchJobChunkExecutionStatus.CANCELLED
    entityManager.persist(execution)
    progressManager.handleProgress(execution)
  }
}
