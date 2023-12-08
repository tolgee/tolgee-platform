package io.tolgee.batch

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.Metrics
import io.tolgee.batch.data.ExecutionQueueItem
import io.tolgee.batch.data.QueueEventType
import io.tolgee.batch.events.JobQueueItemsEvent
import io.tolgee.component.UsingRedisProvider
import io.tolgee.model.batch.BatchJobChunkExecution
import io.tolgee.model.batch.BatchJobChunkExecutionStatus
import io.tolgee.pubSub.RedisPubSubReceiverConfiguration
import io.tolgee.util.Logging
import io.tolgee.util.logger
import jakarta.persistence.EntityManager
import org.hibernate.LockOptions
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.annotation.Lazy
import org.springframework.context.event.EventListener
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

@Component
class BatchJobChunkExecutionQueue(
  private val entityManager: EntityManager,
  private val usingRedisProvider: UsingRedisProvider,
  @Lazy
  private val redisTemplate: StringRedisTemplate,
  private val metrics: Metrics
) : Logging, InitializingBean {
  companion object {
    /**
     * It's static
     */
    private val queue = ConcurrentLinkedQueue<ExecutionQueueItem>()
  }

  @EventListener(JobQueueItemsEvent::class)
  fun onJobItemEvent(event: JobQueueItemsEvent) {
    when (event.type) {
      QueueEventType.ADD -> this.addItemsToLocalQueue(event.items)
      QueueEventType.REMOVE -> queue.removeAll(event.items.toSet())
    }
  }

  @Scheduled(fixedRate = 60000)
  fun populateQueue() {
    logger.debug("Running scheduled populate queue")
    val data = entityManager.createQuery(
      """
          from BatchJobChunkExecution bjce
          join fetch bjce.batchJob bk
          where bjce.status = :executionStatus
          order by bjce.createdAt asc, bjce.executeAfter asc, bjce.id asc
      """.trimIndent(),
      BatchJobChunkExecution::class.java
    ).setParameter("executionStatus", BatchJobChunkExecutionStatus.PENDING)
      .setHint(
        "jakarta.persistence.lock.timeout",
        LockOptions.SKIP_LOCKED
      ).resultList
    if (data.size > 0) {
      logger.debug("Adding ${data.size} items to queue ${System.identityHashCode(this)}")
    }
    addExecutionsToLocalQueue(data)
  }

  fun addExecutionsToLocalQueue(data: List<BatchJobChunkExecution>) {
    val ids = queue.map { it.chunkExecutionId }.toSet()
    data.forEach {
      if (!ids.contains(it.id)) {
        queue.add(it.toItem())
      }
    }
  }

  fun addItemsToLocalQueue(data: List<ExecutionQueueItem>) {
    data.forEach {
      if (!queue.contains(it)) {
        queue.add(it)
      }
    }
  }

  fun addToQueue(execution: BatchJobChunkExecution, jobCharacter: JobCharacter) {
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
        jacksonObjectMapper().writeValueAsString(event)
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
    jobCharacter: JobCharacter? = null
  ) =
    ExecutionQueueItem(id, batchJob.id, executeAfter?.time, jobCharacter ?: batchJob.jobCharacter)

  val size get() = queue.size

  fun joinToString(separator: String = ", ", transform: (item: ExecutionQueueItem) -> String) =
    queue.joinToString(separator, transform = transform)

  fun poll(): ExecutionQueueItem? {
    return queue.poll()
  }

  fun clear() {
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
}
