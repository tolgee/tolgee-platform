package io.tolgee.batch

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.Metrics
import io.tolgee.batch.data.BatchJobChunkExecutionDto
import io.tolgee.batch.data.ExecutionQueueItem
import io.tolgee.batch.data.QueueEventType
import io.tolgee.batch.events.JobQueueItemsEvent
import io.tolgee.batch.events.JobQueueNewChunkExecutionsEvent
import io.tolgee.batch.events.NewJobEvent
import io.tolgee.batch.events.OnBatchJobCreated
import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.UsingRedisProvider
import io.tolgee.configuration.tolgee.BatchProperties
import io.tolgee.model.batch.BatchJobChunkExecution
import io.tolgee.model.batch.BatchJobChunkExecutionStatus
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.pubSub.RedisPubSubReceiverConfiguration
import io.tolgee.util.Logging
import io.tolgee.util.addMinutes
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.logger
import io.tolgee.util.trace
import jakarta.persistence.EntityManager
import org.hibernate.LockMode
import org.hibernate.LockOptions
import org.hibernate.Session
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Lazy
import org.springframework.context.event.EventListener
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionalEventListener
import java.util.concurrent.ConcurrentLinkedQueue

@Component
class BatchJobChunkExecutionQueue(
  private val batchProperties: BatchProperties,
  private val entityManager: EntityManager,
  private val usingRedisProvider: UsingRedisProvider,
  @Lazy
  private val redisTemplate: StringRedisTemplate,
  private val metrics: Metrics,
  private val applicationContext: ApplicationContext,
  private val platformTransactionManager: PlatformTransactionManager,
  private val currentDateProvider: CurrentDateProvider,
) : Logging,
  InitializingBean {
  companion object {
    /**
     * It's static
     */
    private val queue = ConcurrentLinkedQueue<ExecutionQueueItem>()
  }

  @EventListener
  fun onJobItemEvent(event: JobQueueItemsEvent) {
    when (event.type) {
      // will actually not be used at all
      QueueEventType.ADD -> this.addItemsToQueue(event.items)

      // still makes sense, because in very rare cases (for example on app restart)
      // we can have executionItem in multiple app instances
      QueueEventType.REMOVE -> queue.removeAll(event.items.toSet())
    }
  }

  @EventListener
  fun onNewJobEvent(event: NewJobEvent) {
    fetchAndQueueNewJobExecutions(event.jobId)
  }

  @TransactionalEventListener
  fun onNewChunkFromDb(event: JobQueueNewChunkExecutionsEvent) {
    addExecutionsToQueue(event.items)
  }

  @Scheduled(fixedDelay = 60000)
  @Transactional
  fun populateQueue() {
    logger.debug("Running scheduled populate queue")
    val data =
      // creating query from hibernate session, in order to use the setLockMode per table,
      // which is not available in the jpa Query class.
      entityManager
        .unwrap(Session::class.java)
        // select only NEW execution items
        // select pending only which are in pending state for too long (stuck probably due to pod restart on deployment)
        .createQuery(
          """
          from BatchJobChunkExecution bjce
          join bjce.batchJob bk
          where bjce.status = :newExecutionStatus
              or (bjce.status = :pendingExecutionStatus and bjce.createdAt < :isStuckBefore)
          order by 
            case when bk.status = :runningStatus then 0 else 1 end,
            bjce.createdAt asc, 
            bjce.executeAfter asc, 
            bjce.id asc
          """.trimIndent(),
          BatchJobChunkExecution::class.java,
        ).setParameter("newExecutionStatus", BatchJobChunkExecutionStatus.NEW)
        .setParameter("pendingExecutionStatus", BatchJobChunkExecutionStatus.PENDING)
        .setParameter("isStuckBefore", currentDateProvider.date.addMinutes(-2))
        .setParameter("runningStatus", BatchJobStatus.RUNNING)
        // setLockMode here is per alias of the tables from the queryString.
        // will generate: "select ... for no key update of bjce skip locked"
        .setLockMode("bjce", LockMode.PESSIMISTIC_WRITE) // block selected rows from the chunk table
        .setLockMode("bk", LockMode.NONE) // don't block job table, so that other pods could select smth too
        .setHint(
          "jakarta.persistence.lock.timeout",
          LockOptions.SKIP_LOCKED,
        )
        // Limit to get pending batches faster
        .setMaxResults(batchProperties.chunkQueuePopulationSize)
        .resultList

    pendExecutions(data)
  }

  @TransactionalEventListener
  fun populateQueueByChunksFromNewJob(event: OnBatchJobCreated) {
    if (usingRedisProvider.areWeUsingRedis) {
      val event = NewJobEvent(event.job.id)
      redisTemplate.convertAndSend(
        RedisPubSubReceiverConfiguration.NEW_JOB_QUEUE_TOPIC,
        jacksonObjectMapper().writeValueAsString(event),
      )
      return
    }

    fetchAndQueueNewJobExecutions(event.job.id)
  }

  private fun fetchAndQueueNewJobExecutions(jobId: Long) {
    executeInNewTransaction(platformTransactionManager) {
      val data =
        entityManager
          .createNativeQuery(
            """
            SELECT bjce.* FROM tolgee_batch_job_chunk_execution bjce
            WHERE bjce.batch_job_id = :jobId
            ORDER BY bjce.id
            FOR NO KEY UPDATE SKIP LOCKED
            LIMIT :limit
            """,
            BatchJobChunkExecution::class.java,
          ).setParameter("jobId", jobId)
          .setParameter("limit", batchProperties.chunkQueuePopulationSize)
          .resultList as List<BatchJobChunkExecution>

      pendExecutions(data)
    }
  }

  private fun pendExecutions(executions: List<BatchJobChunkExecution>) {
    if (executions.isNotEmpty()) {
      logger.debug("Attempt to add ${executions.size} items to queue ${System.identityHashCode(this)}")
      executions.forEach { it.status = BatchJobChunkExecutionStatus.PENDING }
      applicationContext.publishEvent(
        JobQueueNewChunkExecutionsEvent(
          executions.map {
            BatchJobChunkExecutionDto(it.id, it.batchJob.id, it.executeAfter, it.batchJob.jobCharacter)
          },
        ),
      )
    }
  }

  fun addExecutionsToQueue(data: List<BatchJobChunkExecutionDto>) {
    addItemsToQueue(data.map { it.toItem() })
  }

  fun addItemsToQueue(data: List<ExecutionQueueItem>) {
    val toAdd = mutableListOf<ExecutionQueueItem>()
    val filteredOut = mutableListOf<ExecutionQueueItem>()

    data.forEach {
      if (!queue.contains(it)) {
        toAdd.add(it)
      } else {
        filteredOut.add(it)
      }
    }

    logger.trace {
      val itemsString = toAdd.joinToString(", ") { it.chunkExecutionId.toString() }
      val filteredOutString = filteredOut.joinToString(", ") { it.chunkExecutionId.toString() }
      "Adding chunks [$itemsString] to queue.\n Not Added Items (already in the queue): [$filteredOutString]"
    }

    queue.addAll(toAdd)
  }

  fun addToQueue(
    execution: BatchJobChunkExecution,
    jobCharacter: JobCharacter,
  ) {
    val item = execution.toItem(jobCharacter)
    addItemsToQueue(listOf(item))
  }

  fun addToQueue(executions: List<BatchJobChunkExecution>) {
    val items = executions.map { it.toItem() }
    addItemsToQueue(items)
  }

  fun removeJobExecutions(jobId: Long) {
    logger.debug("Removing job $jobId from queue, queue size: ${queue.size}")
    queue.removeIf { it.jobId == jobId }
    logger.debug("Removed job $jobId from queue, queue size: ${queue.size}")
  }

  private fun BatchJobChunkExecution.toItem(
    // Yes. jobCharacter is part of the batchJob entity.
    // However, we don't want to fetch it here, because it would be a waste of resources.
    // So we can provide the jobCharacter here.
    jobCharacter: JobCharacter? = null,
  ) =
    ExecutionQueueItem(id, batchJob.id, executeAfter?.time, jobCharacter ?: batchJob.jobCharacter)

  private fun BatchJobChunkExecutionDto.toItem(providedJobCharacter: JobCharacter? = null) =
    ExecutionQueueItem(id, batchJobId, executeAfter?.time, providedJobCharacter ?: jobCharacter)

  val size get() = queue.size

  fun joinToString(
    separator: String = ", ",
    transform: (item: ExecutionQueueItem) -> String,
  ) = queue.joinToString(separator, transform = transform)

  fun poll(): ExecutionQueueItem? {
    return queue.poll()
  }

  fun clear() {
    logger.debug("Clearing queue")
    queue.clear()
  }

  fun find(function: (ExecutionQueueItem) -> Boolean): ExecutionQueueItem? {
    return queue.find(function)
  }

  fun peek(): ExecutionQueueItem = queue.peek()

  fun contains(item: ExecutionQueueItem?): Boolean = queue.contains(item)

  fun isEmpty(): Boolean = queue.isEmpty()

  fun getJobCharacterCounts(): Map<JobCharacter, Int> {
    return queue.groupBy { it.jobCharacter }.map { it.key to it.value.size }.toMap()
  }

  override fun afterPropertiesSet() {
    metrics.registerJobQueue(queue)
  }

  fun getQueuedJobItems(jobId: Long): List<ExecutionQueueItem> {
    return queue.filter { it.jobId == jobId }
  }

  fun getAllQueueItems(): List<ExecutionQueueItem> {
    return queue.toList()
  }
}
