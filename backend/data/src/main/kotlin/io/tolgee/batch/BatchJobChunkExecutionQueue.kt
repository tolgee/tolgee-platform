package io.tolgee.batch

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.Metrics
import io.tolgee.batch.data.BatchJobChunkExecutionDto
import io.tolgee.batch.data.ExecutionQueueItem
import io.tolgee.batch.data.QueueEventType
import io.tolgee.batch.events.JobQueueItemsEvent
import io.tolgee.component.UsingRedisProvider
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
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicInteger

@Component
class BatchJobChunkExecutionQueue(
  private val entityManager: EntityManager,
  private val usingRedisProvider: UsingRedisProvider,
  @Lazy
  private val redisTemplate: StringRedisTemplate,
  private val metrics: Metrics,
) : Logging,
  InitializingBean {
  companion object {
    /**
     * Per-job item queues. Key = jobId.
     *
     * All mutations go through ConcurrentHashMap.compute() which provides per-key
     * atomicity, keeping roundRobinOrder and jobQueues in sync under concurrent access.
     *
     * Invariant: jobId is in roundRobinOrder ↔ jobQueues[jobId] is non-null and non-empty.
     */
    private val jobQueues = ConcurrentHashMap<Long, ConcurrentLinkedDeque<ExecutionQueueItem>>()

    /**
     * Round-robin job order. Each jobId appears exactly once when its queue is non-empty.
     * pollFirst() picks the next job to serve; addLast() re-queues it after serving one chunk.
     * This gives O(1) fair scheduling without scanning all items.
     */
    private val roundRobinOrder = ConcurrentLinkedDeque<Long>()

    /**
     * O(1) duplicate detection — replaces the O(n) queue snapshot done on every add.
     */
    private val queuedExecutionIds = ConcurrentHashMap.newKeySet<Long>()

    /**
     * O(1) total item counter.
     */
    private val totalSize = AtomicInteger(0)

    /**
     * O(1) counter for job characters in queue.
     */
    private val jobCharacterCounts = ConcurrentHashMap<JobCharacter, AtomicInteger>()
  }

  private fun incrementCharacterCount(character: JobCharacter) {
    jobCharacterCounts.computeIfAbsent(character) { AtomicInteger(0) }.incrementAndGet()
  }

  private fun decrementCharacterCount(character: JobCharacter) {
    jobCharacterCounts[character]?.decrementAndGet()
  }

  /**
   * Adds a single item. Returns false if already queued (duplicate).
   *
   * Thread-safe: compute() serializes concurrent add/poll for the same jobId,
   * ensuring roundRobinOrder.addLast(), incrementCharacterCount(), and
   * totalSize.incrementAndGet() are called exactly once per new item, atomically
   * with the deque mutation.
   */
  private fun addSingleItem(item: ExecutionQueueItem): Boolean {
    if (!queuedExecutionIds.add(item.chunkExecutionId)) return false

    jobQueues.compute(item.jobId) { jobId, existing ->
      val deque =
        existing ?: ConcurrentLinkedDeque<ExecutionQueueItem>().also {
          // Called atomically inside compute — only once per new job
          roundRobinOrder.addLast(jobId)
        }
      deque.addLast(item)
      incrementCharacterCount(item.jobCharacter)
      totalSize.incrementAndGet()
      deque
    }

    return true
  }

  @EventListener
  fun onJobItemEvent(event: JobQueueItemsEvent) {
    when (event.type) {
      QueueEventType.ADD -> addItemsToLocalQueue(event.items)

      QueueEventType.REMOVE -> {
        event.items.forEach { item ->
          jobQueues.compute(item.jobId) { _, deque ->
            if (deque == null) return@compute null
            val removed = deque.removeIf { it.chunkExecutionId == item.chunkExecutionId }
            if (removed) {
              queuedExecutionIds.remove(item.chunkExecutionId)
              decrementCharacterCount(item.jobCharacter)
              totalSize.decrementAndGet()
            }
            if (deque.isEmpty()) {
              roundRobinOrder.remove(item.jobId)
              null
            } else {
              deque
            }
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

    if (data.isNotEmpty()) {
      logger.debug("Attempt to add ${data.size} items to queue ${System.identityHashCode(this)}")
      addExecutionsToLocalQueue(data)
    }
  }

  fun addExecutionsToLocalQueue(data: List<BatchJobChunkExecutionDto>) {
    var alreadyQueued = 0
    data.forEach { dto ->
      if (!addSingleItem(dto.toItem())) alreadyQueued++
    }
    metrics.batchJobManagementItemAlreadyQueuedCounter.increment(alreadyQueued.toDouble())
    logger.debug("Added ${data.size - alreadyQueued} new items to queue ${System.identityHashCode(this)}")
  }

  fun addItemsToLocalQueue(data: List<ExecutionQueueItem>) {
    var filteredOutCount = 0
    data.forEach { item ->
      if (!addSingleItem(item)) filteredOutCount++
    }
    metrics.batchJobManagementItemAlreadyQueuedCounter.increment(filteredOutCount.toDouble())
    logger.trace {
      "Adding ${data.size - filteredOutCount} chunks to queue. Filtered out: $filteredOutCount"
    }
  }

  fun addToQueue(
    execution: BatchJobChunkExecution,
    jobCharacter: JobCharacter,
  ) {
    addItemsToQueue(listOf(execution.toItem(jobCharacter)))
  }

  fun addToQueue(executions: List<BatchJobChunkExecution>) {
    addItemsToQueue(executions.map { it.toItem() })
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

    addItemsToLocalQueue(items)
  }

  fun removeJobExecutions(jobId: Long) {
    logger.debug("Removing job $jobId from queue, queue size: ${totalSize.get()}")
    var removedCount = 0
    jobQueues.compute(jobId) { _, deque ->
      if (deque != null) {
        deque.forEach { item ->
          queuedExecutionIds.remove(item.chunkExecutionId)
          decrementCharacterCount(item.jobCharacter)
        }
        removedCount = deque.size
        totalSize.addAndGet(-removedCount)
        // Inside compute to prevent a concurrent addSingleItem from re-adding jobId
        // to roundRobinOrder between the compute returning and the remove call.
        roundRobinOrder.remove(jobId)
      }
      null // remove entry from map
    }
    logger.debug("Removed job $jobId from queue ($removedCount items), queue size: ${totalSize.get()}")
  }

  private fun BatchJobChunkExecution.toItem(
    // Yes. jobCharacter is part of the batchJob entity.
    // However, we don't want to fetch it here, because it would be a waste of resources.
    // So we can provide the jobCharacter here.
    jobCharacter: JobCharacter? = null,
  ) = ExecutionQueueItem(id, batchJob.id, executeAfter?.time, jobCharacter ?: batchJob.jobCharacter)

  private fun BatchJobChunkExecutionDto.toItem(providedJobCharacter: JobCharacter? = null) =
    ExecutionQueueItem(id, batchJobId, executeAfter?.time, providedJobCharacter ?: jobCharacter)

  val size get() = totalSize.get()

  fun joinToString(
    separator: String = ", ",
    transform: (item: ExecutionQueueItem) -> String,
  ) = getAllQueueItems().joinToString(separator, transform = transform)

  /**
   * O(1) poll — delegates to pollRoundRobin.
   */
  fun poll(): ExecutionQueueItem? = pollRoundRobin()

  /**
   * O(1) round-robin poll across jobs.
   *
   * Rotates jobs fairly: each job gets one chunk served before any job gets a second.
   * Uses pollFirst/addLast on roundRobinOrder — no full-queue scan required.
   *
   * Thread-safe: compute() serializes concurrent add/poll for the same jobId.
   * If a job's deque turns out to be empty (concurrent drain), the job is skipped
   * and the next one is tried. maxAttempts prevents infinite loops in edge cases.
   */
  fun pollRoundRobin(): ExecutionQueueItem? {
    if (isEmpty()) return null

    // jobQueues.size is O(1) (ConcurrentHashMap maintains an internal counter).
    // It bounds the loop to the number of distinct jobs: in the worst case we try every
    // job once and find all deques empty (concurrent drain), then give up.
    // Do NOT use roundRobinOrder.size() — ConcurrentLinkedDeque.size() is O(n).
    val maxAttempts = jobQueues.size + 1
    var attempts = 0
    while (attempts++ <= maxAttempts) {
      val jobId = roundRobinOrder.pollFirst() ?: return null

      var item: ExecutionQueueItem? = null

      // All side-effects (queuedExecutionIds, counters, roundRobinOrder re-queue) are done
      // inside compute so they are atomic with the deque mutation, preventing races with
      // concurrent addSingleItem or removeJobExecutions calls for the same jobId.
      jobQueues.compute(jobId) { _, deque ->
        if (deque.isNullOrEmpty()) return@compute null
        item = deque.removeFirst()
        val capturedItem = item!!
        queuedExecutionIds.remove(capturedItem.chunkExecutionId)
        decrementCharacterCount(capturedItem.jobCharacter)
        totalSize.decrementAndGet()
        if (deque.isNotEmpty()) {
          roundRobinOrder.addLast(jobId)
          deque
        } else {
          null
        }
      }

      if (item != null) return item
      // deque was null or empty (concurrent drain) — don't re-add, try next job
    }
    return null
  }

  fun clear() {
    logger.debug("Clearing queue")
    jobQueues.clear()
    roundRobinOrder.clear()
    queuedExecutionIds.clear()
    totalSize.set(0)
    jobCharacterCounts.clear()
  }

  fun find(function: (ExecutionQueueItem) -> Boolean): ExecutionQueueItem? = getAllQueueItems().find(function)

  fun peek(): ExecutionQueueItem {
    val jobId = roundRobinOrder.peekFirst() ?: throw NoSuchElementException("Queue is empty")
    return jobQueues[jobId]?.peekFirst() ?: throw NoSuchElementException("Queue is empty")
  }

  fun contains(item: ExecutionQueueItem?): Boolean = item != null && queuedExecutionIds.contains(item.chunkExecutionId)

  fun isEmpty(): Boolean = totalSize.get() == 0

  fun getJobCharacterCounts(): Map<JobCharacter, Int> = jobCharacterCounts.mapValues { it.value.get() }

  override fun afterPropertiesSet() {
    metrics.registerJobQueue { totalSize.get() }
  }

  fun getQueuedJobItems(jobId: Long): List<ExecutionQueueItem> = jobQueues[jobId]?.toList() ?: emptyList()

  fun getAllQueueItems(): List<ExecutionQueueItem> = jobQueues.values.flatMap { it.toList() }
}
