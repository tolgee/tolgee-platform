package io.tolgee.batch

import io.sentry.Sentry
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.data.ExecutionQueueItem
import io.tolgee.component.CurrentDateProvider
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.fixtures.waitFor
import io.tolgee.util.Logging
import io.tolgee.util.logger
import io.tolgee.util.trace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.ceil

@Component
class BatchJobConcurrentLauncher(
  private val tolgeeProperties: TolgeeProperties,
  private val batchJobChunkExecutionQueue: BatchJobChunkExecutionQueue,
  private val currentDateProvider: CurrentDateProvider,
  private val batchJobProjectLockingManager: BatchJobProjectLockingManager,
  private val batchJobService: BatchJobService,
  private val progressManager: ProgressManager,
  private val batchJobActionService: BatchJobActionService,
) : Logging {
  private val batchProperties get() = tolgeeProperties.batch

  companion object {
    const val MIN_TIME_BETWEEN_OPERATIONS = 100
  }

  /**
   * execution id -> Pair(BatchJobDto, Job)
   *
   * Job is the result of launch method executing the execution in separate coroutine
   */
  val runningJobs: ConcurrentHashMap<Long, Pair<BatchJobDto, Job>> = ConcurrentHashMap()

  /**
   * O(1) counter for running jobs by character - avoids O(n) iteration on every chunk
   */
  private val runningJobCharacterCounts = ConcurrentHashMap<JobCharacter, AtomicInteger>()

  private fun incrementRunningCharacterCount(character: JobCharacter) {
    runningJobCharacterCounts.computeIfAbsent(character) { AtomicInteger(0) }.incrementAndGet()
  }

  private fun decrementRunningCharacterCount(character: JobCharacter) {
    runningJobCharacterCounts[character]?.decrementAndGet()
  }

  var pause = false
    set(value) {
      field = value
      if (value) {
        // Cancel all running coroutines for faster cleanup during test teardown
        // invokeOnCompletion will still be called, which triggers onJobCompleted
        runningJobs.values.forEach { (_, job) -> job.cancel() }
        waitFor(30000) {
          runningJobs.size == 0
        }
      }
    }

  var masterRunJob: Job? = null
  var run = true

  fun stop() {
    logger.trace("Stopping batch job launcher ${System.identityHashCode(this)}}")
    run = false
    runBlocking(Dispatchers.IO) {
      masterRunJob?.join()
    }
    logger.trace("Batch job launcher stopped ${System.identityHashCode(this)}")
  }

  fun repeatForever(fn: () -> Boolean) {
    logger.trace("Started batch job action service ${System.identityHashCode(this)}")
    while (run) {
      try {
        val startTime = System.currentTimeMillis()
        val somethingHandled = fn()
        val sleepTime = getSleepTime(startTime, somethingHandled)
        if (sleepTime > 0) {
          Thread.sleep(sleepTime)
        }
      } catch (e: Throwable) {
        Sentry.captureException(e)
        logger.error("Error in batch job action service", e)
      }
    }
  }

  private fun getSleepTime(
    startTime: Long,
    somethingHandled: Boolean,
  ): Long {
    if (!batchJobChunkExecutionQueue.isEmpty() && jobsToLaunch > 0 && somethingHandled) {
      return 0
    }
    return MIN_TIME_BETWEEN_OPERATIONS - (System.currentTimeMillis() - startTime)
  }

  fun run() {
    run = true
    pause = false
    @Suppress("OPT_IN_USAGE")
    masterRunJob =
      GlobalScope.launch(Dispatchers.IO) {
        repeatForever {
          if (pause) {
            return@repeatForever false
          }

          val jobsToLaunch = jobsToLaunch
          if (jobsToLaunch <= 0) {
            return@repeatForever false
          }

          val items =
            (1..jobsToLaunch)
              .mapNotNull { batchJobChunkExecutionQueue.poll() }

          logItemsPulled(items)

          // when something handled, return true
          items
            .map { executionItem ->
              handleItem(executionItem)
            }.any()
        }
      }
  }

  private fun logItemsPulled(items: List<ExecutionQueueItem>) {
    if (items.isNotEmpty()) {
      logger.trace(
        "Pulled ${items.size} items from queue: " +
          items.joinToString(", ") { it.chunkExecutionId.toString() },
      )
      logger.trace(
        "${batchJobChunkExecutionQueue.size} is left in the queue " +
          "(${System.identityHashCode(batchJobChunkExecutionQueue)}): " +
          batchJobChunkExecutionQueue.joinToString(", ") { it.chunkExecutionId.toString() },
      )
    }
  }

  /**
   * Returns true if item was handled
   */
  private fun CoroutineScope.handleItem(executionItem: ExecutionQueueItem): Boolean {
    logger.trace("Trying to run execution ${executionItem.chunkExecutionId}")
    if (!executionItem.isTimeToExecute()) {
      logger.trace {
        "Execution ${executionItem.chunkExecutionId} not ready to execute, adding back to queue:" +
          " Difference ${executionItem.executeAfter!! - currentDateProvider.date.time}"
      }
      addBackToQueue(executionItem)
      return false
    }

    // Fetch BatchJobDto once and reuse throughout the method
    val batchJobDto = batchJobService.getJobDto(executionItem.jobId)

    if (!executionItem.shouldNotBeDebounced(batchJobDto)) {
      logger.trace(
        """Execution ${executionItem.chunkExecutionId} not ready to execute (debouncing), adding back to queue""",
      )
      addBackToQueue(executionItem)
      return false
    }
    if (!canRunJobWithCharacter(executionItem.jobCharacter)) {
      logger.trace(
        """Execution ${executionItem.chunkExecutionId} cannot run concurrent job
          |(there are already max coroutines working on this specific job)
        """.trimMargin(),
      )
      addBackToQueue(executionItem)
      return false
    }

    if (!executionItem.trySetRunningState(batchJobDto)) {
      logger.trace(
        """Execution ${executionItem.chunkExecutionId} cannot run concurrent job
          |(there are already max concurrent executions running of this specific job)
        """.trimMargin(),
      )
      if (!batchJobDto.status.completed) {
        // e.g. job isn't canceled (check ProgressManager.trySetExecutionRunning returning false on competed job)
        addBackToQueue(executionItem)
      }
      return false
    }

    /**
     * Only single job can run in project at the same time
     */
    if (!batchJobProjectLockingManager.canLockJobForProject(executionItem.jobId)) {
      logger.debug(
        "⚠️ Cannot run execution ${executionItem.chunkExecutionId}. " +
          "Other job from the project is currently running, skipping",
      )

      // Rollback the state change made in trySetRunningState
      progressManager.rollbackSetToRunning(executionItem.chunkExecutionId, executionItem.jobId)

      // we haven't publish consuming, so we can add it only to the local queue
      batchJobChunkExecutionQueue.addItemsToLocalQueue(
        listOf(
          executionItem.also {
            it.executeAfter = currentDateProvider.date.time + 1000
          },
        ),
      )
      return false
    }

    // Publish OnBatchJobStarted event after all checks pass (including project exclusivity)
    progressManager.tryPublishJobStarted(executionItem.jobId, batchJobDto)

    val job =
      launch {
        batchJobActionService.handleItem(executionItem, batchJobDto)
      }

    runningJobs[executionItem.chunkExecutionId] = batchJobDto to job
    incrementRunningCharacterCount(batchJobDto.jobCharacter)

    job.invokeOnCompletion {
      onJobCompleted(executionItem, batchJobDto.jobCharacter)
    }
    logger.debug("Execution ${executionItem.chunkExecutionId} launched. Running jobs: ${runningJobs.size}")
    return true
  }

  private fun addBackToQueue(executionItem: ExecutionQueueItem) {
    logger.trace { "Adding execution $executionItem back to queue" }
    batchJobChunkExecutionQueue.addItemsToLocalQueue(listOf(executionItem))
  }

  private fun onJobCompleted(
    executionItem: ExecutionQueueItem,
    jobCharacter: JobCharacter,
  ) {
    runningJobs.remove(executionItem.chunkExecutionId)
    decrementRunningCharacterCount(jobCharacter)
    // Decrement running count when coroutine actually finishes to align with runningJobs
    progressManager.onExecutionCoroutineComplete(executionItem.jobId)
    logger.debug("Chunk ${executionItem.chunkExecutionId}: Completed")
    logger.debug("Running jobs: ${runningJobs.size}")
  }

  private val jobsToLaunch get() = batchProperties.concurrency - runningJobs.size

  fun ExecutionQueueItem.isTimeToExecute(): Boolean {
    val executeAfter = this.executeAfter ?: return true
    return executeAfter <= currentDateProvider.date.time
  }

  fun ExecutionQueueItem.shouldNotBeDebounced(dto: BatchJobDto): Boolean {
    val lastEventTime = dto.lastDebouncingEvent ?: dto.createdAt ?: return true
    val debounceDuration = dto.debounceDurationInMs ?: return true
    val executeAfter = lastEventTime + debounceDuration
    if (executeAfter <= currentDateProvider.date.time) {
      logger.debug(
        "Debouncing duration reached for job ${dto.id}, " +
          "execute after $executeAfter, " +
          "now ${currentDateProvider.date.time}",
      )
      return true
    }
    val createdAt = dto.createdAt ?: return true
    val debounceMaxWaitTimeInMs = dto.debounceMaxWaitTimeInMs ?: return true
    val maxTimeReached = createdAt + debounceMaxWaitTimeInMs <= currentDateProvider.date.time
    if (maxTimeReached) {
      logger.debug("Debouncing max wait time reached for job ${dto.id}")
    }
    return maxTimeReached
  }

  private fun canRunJobWithCharacter(character: JobCharacter): Boolean {
    val queueCharacterCounts = batchJobChunkExecutionQueue.getJobCharacterCounts()
    val otherCharactersInQueueCount = queueCharacterCounts.filter { it.key != character }.values.sum()
    if (otherCharactersInQueueCount == 0) {
      return true
    }
    val runningCount = runningJobCharacterCounts[character]?.get() ?: 0
    val allowedCharacterCounts = ceil(character.maxConcurrencyRatio * batchProperties.concurrency)
    return runningCount < allowedCharacterCounts
  }

  private fun ExecutionQueueItem.trySetRunningState(batchJobDto: BatchJobDto): Boolean {
    // Check maxPerJobConcurrency before trying to set running state
    val maxPerJobConcurrency = batchJobDto.maxPerJobConcurrency
    if (maxPerJobConcurrency != -1) {
      // Count only executions for THIS specific job, not all running executions globally
      val runningForThisJob = runningJobs.values.count { it.first.id == this.jobId }
      if (runningForThisJob >= maxPerJobConcurrency) {
        return false
      }
    }
    return progressManager.trySetExecutionRunning(this.chunkExecutionId, this.jobId)
  }
}
