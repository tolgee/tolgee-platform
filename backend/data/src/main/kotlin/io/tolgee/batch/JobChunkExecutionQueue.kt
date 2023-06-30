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

) : Logging, Queue<ExecutionQueueItem> by ConcurrentLinkedQueue() {

  @EventListener(JobQueueItemEvent::class)
  fun onJobItemEvent(event: JobQueueItemEvent) {
    when (event.type) {
      QueueItemType.ADD -> this.add(event.item)
      QueueItemType.REMOVE -> this.remove(event.item)
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
    val ids = this.map { it.chunkExecutionId }.toSet()
    data.forEach {
      if (!ids.contains(it.id)) {
        this.add(it.toItem())
      }
    }
  }

  fun addToQueue(execution: BatchJobChunkExecution) {
    val item = execution.toItem()
    if (usingRedisProvider.areWeUsingRedis) {
      val event = JobQueueItemEvent(item, QueueItemType.ADD)
      redisTemplate.convertAndSend(
        RedisPubSubReceiverConfiguration.JOB_QUEUE_TOPIC,
        jacksonObjectMapper().writeValueAsString(event)
      )
      return
    }
    this.add(item)
  }

  private fun BatchJobChunkExecution.toItem() =
    ExecutionQueueItem(id, batchJob.id, executeAfter?.time)
}
