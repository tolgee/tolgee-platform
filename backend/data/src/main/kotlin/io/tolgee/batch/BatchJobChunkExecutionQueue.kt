package io.tolgee.batch

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.Metrics
import io.tolgee.batch.data.BatchJobChunkExecutionDto
import io.tolgee.batch.data.ExecutionQueueItem
import io.tolgee.batch.data.QueueEventType
import io.tolgee.batch.events.JobQueueItemsEvent
import io.tolgee.component.UsingRedisProvider
import io.tolgee.configuration.tolgee.BatchProperties
import io.tolgee.model.batch.BatchJobChunkExecution
import io.tolgee.model.batch.BatchJobChunkExecutionStatus
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.pubSub.RedisPubSubReceiverConfiguration
import io.tolgee.util.Logging
import io.tolgee.util.logger
import io.tolgee.util.trace
import jakarta.persistence.EntityManager
import org.hibernate.LockMode
import org.hibernate.LockOptions
import org.hibernate.Session
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.annotation.Lazy
import org.springframework.context.event.EventListener
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.ConcurrentLinkedQueue

@Component
class BatchJobChunkExecutionQueue(
  private val batchProperties: BatchProperties,
  private val entityManager: EntityManager,
  private val usingRedisProvider: UsingRedisProvider,
  @Lazy
  private val redisTemplate: StringRedisTemplate,
  private val metrics: Metrics,
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
      QueueEventType.ADD -> this.addItemsToLocalQueue(event.items)
      QueueEventType.REMOVE -> queue.removeAll(event.items.toSet())
    }
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
        .createQuery(
          """
          select new io.tolgee.batch.data.BatchJobChunkExecutionDto(bjce.id, bk.id, bjce.executeAfter, bk.jobCharacter)
          from BatchJobChunkExecution bjce
          join bjce.batchJob bk
          where bjce.status = :executionStatus
          order by 
            case when bk.status = :runningStatus then 0 else 1 end,
            bjce.createdAt asc, 
            bjce.executeAfter asc, 
            bjce.id asc
          """.trimIndent(),
          BatchJobChunkExecutionDto::class.java,
        ).setParameter("executionStatus", BatchJobChunkExecutionStatus.PENDING)
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

    if (data.size > 0) {
      logger.debug("Attempt to add ${data.size} items to queue ${System.identityHashCode(this)}")
      addExecutionsToLocalQueue(data)
    }
  }

  fun addExecutionsToLocalQueue(data: List<BatchJobChunkExecutionDto>) {
    val ids = queue.map { it.chunkExecutionId }.toSet()
    var count = 0
    data.forEach {
      if (!ids.contains(it.id)) {
        queue.add(it.toItem())
        count++
      }
    }

    logger.debug("Added $count new items to queue ${System.identityHashCode(this)}")
  }

  fun addItemsToLocalQueue(data: List<ExecutionQueueItem>) {
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

  fun addItemsToQueue(items: List<ExecutionQueueItem>) {
    if (usingRedisProvider.areWeUsingRedis) {
      val event = JobQueueItemsEvent(items, QueueEventType.ADD)
      redisTemplate.convertAndSend(
        RedisPubSubReceiverConfiguration.JOB_QUEUE_TOPIC,
        jacksonObjectMapper().writeValueAsString(event),
      )
      return
    }

    this.addItemsToLocalQueue(items)
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
