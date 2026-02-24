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
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

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
    private val lock = ReentrantLock()

    /** jobId -> ordered chunks (insertion-order iteration for round-robin) */
    private val jobChunks = LinkedHashMap<Long, ArrayDeque<ExecutionQueueItem>>()

    /** chunkExecutionId -> jobId (O(1) duplicate check and lookup) */
    private val chunkIndex = HashMap<Long, Long>()

    /** projectId -> set of jobIds (O(1) "skip locked projects") */
    private val projectJobs = HashMap<Long, MutableSet<Long>>()

    /** O(1) counter for job characters in queue */
    private val jobCharacterCounts = ConcurrentHashMap<JobCharacter, AtomicInteger>()

    /** Tracks the last job that was served a chunk for round-robin fairness. */
    @Volatile
    private var lastServedJobId: Long? = null

    private val sizeCounter = AtomicInteger(0)
  }

  private fun incrementCharacterCount(character: JobCharacter) {
    jobCharacterCounts.computeIfAbsent(character) { AtomicInteger(0) }.incrementAndGet()
  }

  private fun decrementCharacterCount(character: JobCharacter) {
    jobCharacterCounts[character]?.decrementAndGet()
  }

  /**
   * Adds an item to internal indexes. Must be called while holding [lock].
   */
  private fun addItemInternal(item: ExecutionQueueItem) {
    chunkIndex[item.chunkExecutionId] = item.jobId
    jobChunks.getOrPut(item.jobId) { ArrayDeque() }.addLast(item)
    item.projectId?.let { pid ->
      projectJobs.getOrPut(pid) { mutableSetOf() }.add(item.jobId)
    }
    incrementCharacterCount(item.jobCharacter)
    sizeCounter.incrementAndGet()
  }

  /**
   * Removes a single item from internal indexes. Must be called while holding [lock].
   */
  private fun removeItemInternal(item: ExecutionQueueItem): Boolean {
    if (!chunkIndex.containsKey(item.chunkExecutionId)) return false
    chunkIndex.remove(item.chunkExecutionId)
    val deque = jobChunks[item.jobId]
    deque?.remove(item)
    if (deque != null && deque.isEmpty()) {
      jobChunks.remove(item.jobId)
      // Clean up projectJobs if no more jobs for this project
      item.projectId?.let { pid ->
        projectJobs[pid]?.remove(item.jobId)
        if (projectJobs[pid]?.isEmpty() == true) {
          projectJobs.remove(pid)
        }
      }
    }
    decrementCharacterCount(item.jobCharacter)
    sizeCounter.decrementAndGet()
    return true
  }

  @EventListener
  fun onJobItemEvent(event: JobQueueItemsEvent) {
    when (event.type) {
      QueueEventType.ADD -> {
        this.addItemsToLocalQueue(event.items)
      }

      QueueEventType.REMOVE -> {
        lock.withLock {
          event.items.forEach { item ->
            removeItemInternal(item)
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
      entityManager
        .unwrap(Session::class.java)
        .createQuery(
          """
          select new io.tolgee.batch.data.BatchJobChunkExecutionDto(
            bjce.id, bk.id, bjce.executeAfter, bk.jobCharacter, bk.project.id
          )
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
    var count = 0
    lock.withLock {
      data.forEach {
        if (!chunkIndex.containsKey(it.id)) {
          val item = it.toItem()
          addItemInternal(item)
          count++
        }
      }
    }
    metrics.batchJobManagementItemAlreadyQueuedCounter.increment(data.size - count.toDouble())
    logger.debug("Added $count new items to queue ${System.identityHashCode(this)}")
  }

  fun addItemsToLocalQueue(data: List<ExecutionQueueItem>) {
    var filteredOutCount = 0
    lock.withLock {
      data.forEach {
        if (!chunkIndex.containsKey(it.chunkExecutionId)) {
          addItemInternal(it)
        } else {
          filteredOutCount++
        }
      }
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
    val item = execution.toItem(jobCharacter)
    addItemsToQueue(listOf(item))
  }

  fun addToQueue(executions: List<BatchJobChunkExecution>) {
    val items = executions.map { it.toItem() }
    addItemsToQueue(items)
  }

  fun addItemsToQueue(items: List<ExecutionQueueItem>) {
    if (usingRedisProvider.areWeUsingRedis) {
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
    logger.debug("Removing job $jobId from queue, queue size: ${sizeCounter.get()}")
    lock.withLock {
      val deque = jobChunks.remove(jobId) ?: return@withLock
      deque.forEach { item ->
        chunkIndex.remove(item.chunkExecutionId)
        decrementCharacterCount(item.jobCharacter)
        sizeCounter.decrementAndGet()
      }
      // Clean up projectJobs
      val projectId = deque.firstOrNull()?.projectId
      projectId?.let { pid ->
        projectJobs[pid]?.remove(jobId)
        if (projectJobs[pid]?.isEmpty() == true) {
          projectJobs.remove(pid)
        }
      }
    }
    logger.debug("Removed job $jobId from queue, queue size: ${sizeCounter.get()}")
  }

  private fun BatchJobChunkExecution.toItem(jobCharacter: JobCharacter? = null) =
    ExecutionQueueItem(
      id,
      batchJob.id,
      executeAfter?.time,
      jobCharacter ?: batchJob.jobCharacter,
      batchJob.project?.id,
    )

  private fun BatchJobChunkExecutionDto.toItem(providedJobCharacter: JobCharacter? = null) =
    ExecutionQueueItem(
      id,
      batchJobId,
      executeAfter?.time,
      providedJobCharacter ?: jobCharacter,
      projectId,
    )

  val size get() = sizeCounter.get()

  fun joinToString(
    separator: String = ", ",
    transform: (item: ExecutionQueueItem) -> String,
  ): String =
    lock.withLock {
      jobChunks.values.flatMap { it }.joinToString(separator, transform = transform)
    }

  fun poll(): ExecutionQueueItem? {
    lock.withLock {
      val firstEntry = jobChunks.entries.firstOrNull() ?: return null
      val deque = firstEntry.value
      val item = deque.removeFirst()
      chunkIndex.remove(item.chunkExecutionId)
      if (deque.isEmpty()) {
        jobChunks.remove(firstEntry.key)
        item.projectId?.let { pid ->
          projectJobs[pid]?.remove(firstEntry.key)
          if (projectJobs[pid]?.isEmpty() == true) {
            projectJobs.remove(pid)
          }
        }
      }
      decrementCharacterCount(item.jobCharacter)
      sizeCounter.decrementAndGet()
      return item
    }
  }

  /**
   * Polls using round-robin across jobs for fair distribution, skipping jobs whose
   * project is in [lockedProjectIds].
   *
   * Iterates the [jobChunks] LinkedHashMap keys in insertion order, starting after
   * [lastServedJobId], and returns the first chunk from a job whose project is not locked.
   * All operations are O(1) per step (amortized).
   */
  fun pollRoundRobin(lockedProjectIds: Set<Long> = emptySet()): ExecutionQueueItem? {
    lock.withLock {
      if (jobChunks.isEmpty()) return null

      val jobIds = jobChunks.keys.toList()

      // Find next job in rotation
      val lastJobId = lastServedJobId
      val startIndex =
        if (lastJobId == null) {
          0
        } else {
          val idx = jobIds.indexOf(lastJobId)
          if (idx == -1) 0 else (idx + 1) % jobIds.size
        }

      for (i in jobIds.indices) {
        val jobIndex = (startIndex + i) % jobIds.size
        val targetJobId = jobIds[jobIndex]
        val deque = jobChunks[targetJobId] ?: continue
        val firstItem = deque.firstOrNull() ?: continue

        // Skip jobs whose project is currently locked
        if (firstItem.projectId != null && firstItem.projectId in lockedProjectIds) {
          continue
        }

        val item = deque.removeFirst()
        chunkIndex.remove(item.chunkExecutionId)
        if (deque.isEmpty()) {
          jobChunks.remove(targetJobId)
          item.projectId?.let { pid ->
            projectJobs[pid]?.remove(targetJobId)
            if (projectJobs[pid]?.isEmpty() == true) {
              projectJobs.remove(pid)
            }
          }
        }
        decrementCharacterCount(item.jobCharacter)
        sizeCounter.decrementAndGet()
        lastServedJobId = targetJobId
        return item
      }

      // All jobs are for locked projects — return null
      return null
    }
  }

  fun clear() {
    logger.debug("Clearing queue")
    lock.withLock {
      jobChunks.clear()
      chunkIndex.clear()
      projectJobs.clear()
      sizeCounter.set(0)
    }
    jobCharacterCounts.clear()
    lastServedJobId = null
  }

  fun find(function: (ExecutionQueueItem) -> Boolean): ExecutionQueueItem? {
    lock.withLock {
      for (deque in jobChunks.values) {
        val found = deque.find(function)
        if (found != null) return found
      }
      return null
    }
  }

  fun peek(): ExecutionQueueItem {
    lock.withLock {
      return jobChunks.values.first().first()
    }
  }

  fun contains(item: ExecutionQueueItem?): Boolean {
    if (item == null) return false
    lock.withLock {
      return chunkIndex.containsKey(item.chunkExecutionId)
    }
  }

  fun isEmpty(): Boolean = sizeCounter.get() == 0

  fun getJobCharacterCounts(): Map<JobCharacter, Int> {
    return jobCharacterCounts.mapValues { it.value.get() }
  }

  override fun afterPropertiesSet() {
    metrics.registerJobQueue { sizeCounter.get() }
  }

  fun getQueuedJobItems(jobId: Long): List<ExecutionQueueItem> {
    lock.withLock {
      return jobChunks[jobId]?.toList() ?: emptyList()
    }
  }

  fun getAllQueueItems(): List<ExecutionQueueItem> {
    lock.withLock {
      return jobChunks.values.flatMap { it }
    }
  }
}
