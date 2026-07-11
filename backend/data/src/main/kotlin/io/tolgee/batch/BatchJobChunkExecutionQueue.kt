package io.tolgee.batch

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.Metrics
import io.tolgee.batch.data.BatchJobChunkExecutionDto
import io.tolgee.batch.data.BatchJobType
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
     * atomicity, keeping jobsByType / typeOrder and jobQueues in sync under concurrent access.
     *
     * Invariant: jobId is in jobsByType[type] ↔ jobQueues[jobId] is non-null and non-empty.
     */
    private val jobQueues = ConcurrentHashMap<Long, ConcurrentLinkedDeque<ExecutionQueueItem>>()

    /**
     * Two-level round-robin order. Outer level rotates job *types*; inner level rotates
     * jobs within a type. This makes scheduling fair across types regardless of how many
     * jobs of each type are queued — a flood of one type cannot starve another.
     *
     * jobsByType: type -> rotation of jobIds that currently have queued chunks of that type.
     * typeOrder:  rotation of types that currently have queued jobs.
     *
     * Invariants (eventually-consistent, self-correcting on poll like the job level):
     *   I1: jobId in jobsByType[type]  ↔ jobQueues[jobId] is non-null and non-empty
     *   I2: type  in typeOrder         ↔ jobsByType[type] has at least one jobId
     * A type/job that lingers after a concurrent drain is skipped on the next poll.
     * Both sets are [ConcurrentOrderedSet] so addLast() is idempotent under races.
     * Empty per-type sets are left in jobsByType (≤ number of BatchJobTypes) rather than
     * removed, to avoid a remove/computeIfAbsent race; typeOrder membership is what gates
     * scheduling.
     */
    private val jobsByType = ConcurrentHashMap<BatchJobType, ConcurrentOrderedSet<Long>>()
    private val typeOrder = ConcurrentOrderedSet<BatchJobType>()

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
   * ensuring jobsByType / typeOrder registration, incrementCharacterCount(), and
   * totalSize.incrementAndGet() are called exactly once per new item, atomically
   * with the deque mutation.
   */
  private fun addSingleItem(item: ExecutionQueueItem): Boolean {
    if (!queuedExecutionIds.add(item.chunkExecutionId)) return false

    jobQueues.compute(item.jobId) { jobId, existing ->
      val deque =
        existing ?: ConcurrentLinkedDeque<ExecutionQueueItem>().also {
          // New job for this jobId — register it in its type's rotation (and the type in
          // typeOrder). Called atomically inside compute — only once per new job.
          // addLast is idempotent, so a concurrent re-add is a safe no-op.
          jobsByType.computeIfAbsent(item.jobType) { ConcurrentOrderedSet() }.addLast(jobId)
          typeOrder.addLast(item.jobType)
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
              // Job drained — drop it from its type rotation. typeOrder self-corrects on poll.
              jobsByType[item.jobType]?.remove(item.jobId)
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
          select new io.tolgee.batch.data.BatchJobChunkExecutionDto(bjce.id, bk.id, bjce.executeAfter, bk.jobCharacter, bk.type)
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
    jobType: BatchJobType,
  ) {
    addItemsToQueue(listOf(execution.toItem(jobCharacter, jobType)))
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
        // All chunks of a job share its type; read it from any queued item.
        val type = deque.peek()?.jobType
        deque.forEach { item ->
          queuedExecutionIds.remove(item.chunkExecutionId)
          decrementCharacterCount(item.jobCharacter)
        }
        removedCount = deque.size
        totalSize.addAndGet(-removedCount)
        // Inside compute to prevent a concurrent addSingleItem from re-adding jobId between
        // the compute returning and the remove call. typeOrder self-corrects on next poll.
        if (type != null) jobsByType[type]?.remove(jobId)
      }
      null // remove entry from map
    }
    logger.debug("Removed job $jobId from queue ($removedCount items), queue size: ${totalSize.get()}")
  }

  private fun BatchJobChunkExecution.toItem(
    // Yes. jobCharacter is part of the batchJob entity.
    // However, we don't want to fetch it here, because it would be a waste of resources.
    // So we can provide the jobCharacter here.
    // Same for jobType: on the retry path batchJob is a detached lazy proxy (the chunk runs
    // outside a Hibernate session), so accessing batchJob.type would throw
    // LazyInitializationException — callers that already have the type must pass it in.
    jobCharacter: JobCharacter? = null,
    jobType: BatchJobType? = null,
  ) = ExecutionQueueItem(
    id,
    batchJob.id,
    executeAfter?.time,
    jobCharacter ?: batchJob.jobCharacter,
    jobType = jobType ?: batchJob.type,
  )

  private fun BatchJobChunkExecutionDto.toItem(providedJobCharacter: JobCharacter? = null) =
    ExecutionQueueItem(
      id,
      batchJobId,
      executeAfter?.time,
      providedJobCharacter ?: jobCharacter,
      jobType = jobType,
    )

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
   * O(1) two-level round-robin poll: rotate types, then jobs within the chosen type, then
   * serve one chunk. Fair across types regardless of per-type job count (see #3428 for the
   * job level this builds on).
   *
   * Thread-safe: the chunk removal and all counters run inside jobQueues.compute(); the
   * rotation sets are ConcurrentOrderedSets with idempotent addLast(). A type or job that
   * drained concurrently is skipped; maxAttempts bounds the skip loop.
   */
  fun pollRoundRobin(): ExecutionQueueItem? {
    if (isEmpty()) return null

    // Each iteration removes one type from typeOrder. We may skip types whose bucket drained
    // concurrently (≤ number of types) and jobs that drained concurrently (≤ number of jobs).
    val maxAttempts = jobQueues.size + jobsByType.size + 1
    var attempts = 0
    while (attempts++ <= maxAttempts) {
      val type = typeOrder.pollFirst() ?: return null
      val jobsForType = jobsByType[type]
      val jobId = jobsForType?.pollFirst()
      if (jobId == null) {
        // Type bucket drained concurrently; type already removed from typeOrder. Try next.
        continue
      }

      var item: ExecutionQueueItem? = null
      jobQueues.compute(jobId) { _, deque ->
        if (deque.isNullOrEmpty()) return@compute null
        item = deque.removeFirst()
        val capturedItem = item!!
        queuedExecutionIds.remove(capturedItem.chunkExecutionId)
        decrementCharacterCount(capturedItem.jobCharacter)
        totalSize.decrementAndGet()
        if (deque.isNotEmpty()) {
          jobsForType.addLast(jobId)
          deque
        } else {
          null
        }
      }

      if (jobsForType.peekFirst() != null) {
        typeOrder.addLast(type)
      }

      if (item != null) return item
    }
    return null
  }

  fun clear() {
    logger.debug("Clearing queue")
    jobQueues.clear()
    jobsByType.clear()
    typeOrder.clear()
    queuedExecutionIds.clear()
    totalSize.set(0)
    jobCharacterCounts.clear()
  }

  fun find(function: (ExecutionQueueItem) -> Boolean): ExecutionQueueItem? = getAllQueueItems().find(function)

  fun peek(): ExecutionQueueItem {
    val type = typeOrder.peekFirst() ?: throw NoSuchElementException("Queue is empty")
    val jobId = jobsByType[type]?.peekFirst() ?: throw NoSuchElementException("Queue is empty")
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
