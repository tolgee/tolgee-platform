package io.tolgee.batch

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.component.UsingRedisProvider
import io.tolgee.model.batch.BatchJobChunkExecution
import io.tolgee.model.batch.BatchJobChunkExecutionStatus
import io.tolgee.pubSub.RedisPubSubReceiverConfiguration
import io.tolgee.util.Logging
import org.hibernate.LockOptions
import org.springframework.context.annotation.Lazy
import org.springframework.context.event.EventListener
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import javax.persistence.EntityManager

@Component
class JobChunkExecutionQueue(
  private val entityManager: EntityManager,
  private val usingRedisProvider: UsingRedisProvider,
  @Lazy
  private val redisTemplate: StringRedisTemplate

) : Logging {
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

  @Scheduled(fixedDelay = 60000)
  fun populateQueue() {
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
        "javax.persistence.lock.timeout",
        LockOptions.SKIP_LOCKED
      ).resultList
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

  fun addToQueue(executions: List<BatchJobChunkExecution>) {
    if (usingRedisProvider.areWeUsingRedis) {
      val items = executions.map { it.toItem() }
      val event = JobQueueItemsEvent(items, QueueEventType.ADD)
      redisTemplate.convertAndSend(
        RedisPubSubReceiverConfiguration.JOB_QUEUE_TOPIC,
        jacksonObjectMapper().writeValueAsString(event)
      )
      return
    }
    this.addExecutionsToLocalQueue(executions)
  }

  fun cancelJob(jobId: Long) {
    queue.removeIf { it.jobId == jobId }
  }

  private fun BatchJobChunkExecution.toItem() =
    ExecutionQueueItem(id, batchJob.id, executeAfter?.time)

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
}
