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
import org.hibernate.Session
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.annotation.Lazy
import org.springframework.context.event.EventListener
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

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

    /**
     * O(1) counter for job characters in queue - avoids O(n) iteration on every chunk
     */
    private val jobCharacterCounts = ConcurrentHashMap<JobCharacter, AtomicInteger>()
  }

  private fun incrementCharacterCount(character: JobCharacter) {
    jobCharacterCounts.computeIfAbsent(character) { AtomicInteger(0) }.incrementAndGet()
  }

  private fun decrementCharacterCount(character: JobCharacter) {
    jobCharacterCounts[character]?.decrementAndGet()
  }

  @EventListener
  fun onJobItemEvent(event: JobQueueItemsEvent) {
    when (event.type) {
      QueueEventType.ADD -> {
        this.addItemsToLocalQueue(event.items)
      }

      QueueEventType.REMOVE -> {
        // Remove and decrement atomically per item to prevent double-decrement
        // if poll() removes an item between removeAll and forEach
        event.items.forEach { item ->
          if (queue.remove(item)) {
            decrementCharacterCount(item.jobCharacter)
          }
        }
      }
    }
  }

  @Scheduled(fixedDelay = 60000)
  @Transactional(readOnly = true)
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
        val item = it.toItem()
        queue.add(item)
        incrementCharacterCount(item.jobCharacter)
        count++
      }
    }
    metrics.batchJobManagementItemAlreadyQueuedCounter.increment(data.size - count.toDouble())
    logger.debug("Added $count new items to queue ${System.identityHashCode(this)}")
  }

  fun addItemsToLocalQueue(data: List<ExecutionQueueItem>) {
    // Use Set for O(1) lookup instead of O(n) queue.contains()
    val existingIds = queue.mapTo(HashSet()) { it.chunkExecutionId }
    val toAdd = mutableListOf<ExecutionQueueItem>()
    var filteredOutCount = 0

    data.forEach {
      if (!existingIds.contains(it.chunkExecutionId)) {
        toAdd.add(it)
        existingIds.add(it.chunkExecutionId) // Prevent duplicates within the batch
      } else {
        filteredOutCount++
      }
    }
    metrics.batchJobManagementItemAlreadyQueuedCounter.increment(filteredOutCount.toDouble())
    logger.trace {
      val itemsString = toAdd.joinToString(", ") { it.chunkExecutionId.toString() }
      "Adding ${toAdd.size} chunks to queue. Filtered out: $filteredOutCount"
    }

    queue.addAll(toAdd)
    toAdd.forEach { incrementCharacterCount(it.jobCharacter) }
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
      // Batch Redis messages to avoid serializing huge JSON payloads
      // For 100k items, sending one message would be ~10-15MB of JSON
      val batchSize = 1000
      items.chunked(batchSize).forEach { batch ->
        val event = JobQueueItemsEvent(batch, QueueEventType.ADD)
        redisTemplate.convertAndSend(
          RedisPubSubReceiverConfiguration.JOB_QUEUE_TOPIC,
          jacksonObjectMapper().writeValueAsString(event),
        )
      }
      return
    }

    this.addItemsToLocalQueue(items)
  }

  fun removeJobExecutions(jobId: Long) {
    logger.debug("Removing job $jobId from queue, queue size: ${queue.size}")
    val iterator = queue.iterator()
    while (iterator.hasNext()) {
      val item = iterator.next()
      if (item.jobId == jobId) {
        iterator.remove()
        decrementCharacterCount(item.jobCharacter)
      }
    }
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
    val item = queue.poll()
    item?.let { decrementCharacterCount(it.jobCharacter) }
    return item
  }

  fun clear() {
    logger.debug("Clearing queue")
    queue.clear()
    jobCharacterCounts.clear()
  }

  fun find(function: (ExecutionQueueItem) -> Boolean): ExecutionQueueItem? {
    return queue.find(function)
  }

  fun peek(): ExecutionQueueItem = queue.peek()

  fun contains(item: ExecutionQueueItem?): Boolean = queue.contains(item)

  fun isEmpty(): Boolean = queue.isEmpty()

  fun getJobCharacterCounts(): Map<JobCharacter, Int> {
    return jobCharacterCounts.mapValues { it.value.get() }
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
