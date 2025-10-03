package io.tolgee.batch

import io.sentry.Sentry
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.data.ExecutionQueueItem
import io.tolgee.component.CurrentDateProvider
import io.tolgee.configuration.tolgee.BatchProperties
import io.tolgee.fixtures.waitFor
import io.tolgee.model.batch.BatchJobChunkExecutionStatus
import io.tolgee.util.Logging
import io.tolgee.util.logger
import io.tolgee.util.trace
import kotlinx.coroutines.*
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil

@Component
class BatchJobConcurrentLauncher(
  private val batchProperties: BatchProperties,
  private val batchJobChunkExecutionQueue: BatchJobChunkExecutionQueue,
  private val currentDateProvider: CurrentDateProvider,
  private val batchJobProjectLockingManager: BatchJobProjectLockingManager,
  private val batchJobService: BatchJobService,
  private val progressManager: ProgressManager,
  private val batchJobActionService: BatchJobActionService,
) : Logging {
  companion object {
    const val MIN_TIME_BETWEEN_OPERATIONS = 100
  }

  /**
   * execution id -> Pair(BatchJobDto, Job)
   *
   * Job is the result of launch method executing the execution in separate coroutine
   */
  val runningJobs: ConcurrentHashMap<Long, Pair<BatchJobDto, Job>> = ConcurrentHashMap()

  var pause = false
    set(value) {
      field = value
      if (value) {
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

          // This trace will spam the logging output
          // (one log every 100ms), so it's commented out for now
          // logger.trace("Jobs to launch: $jobsToLaunch")
          val items =
            (1..jobsToLaunch)
              .mapNotNull { batchJobChunkExecutionQueue.poll() }

          logItemsPulled(items)

          // when something handled, return true
          items.map { executionItem ->
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
    if (!executionItem.shouldNotBeDebounced()) {
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

    if (!executionItem.trySetRunningState()) {
      logger.trace(
        """Execution ${executionItem.chunkExecutionId} cannot run concurrent job 
          |(there are already max concurrent executions running of this specific job)
        """.trimMargin(),
      )
      addBackToQueue(executionItem)
      return false
    }

    /**
     * There is a project level lock with configurable n concurrent locks allowed.
     */
    if (!batchJobProjectLockingManager.canLockJobForProject(executionItem.jobId)) {
      logger.debug(
        "⚠️ Cannot run execution ${executionItem.chunkExecutionId}. " +
          "Other job from the project is currently running, skipping",
      )

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

    val job =
      launch {
        batchJobActionService.handleItem(executionItem)
      }

    val batchJobDto = batchJobService.getJobDto(executionItem.jobId)
    runningJobs[executionItem.chunkExecutionId] = batchJobDto to job

    job.invokeOnCompletion {
      onJobCompleted(executionItem)
    }
    logger.debug("Execution ${executionItem.chunkExecutionId} launched. Running jobs: ${runningJobs.size}")
    return true
  }

  private fun addBackToQueue(executionItem: ExecutionQueueItem) {
    logger.trace { "Adding execution $executionItem back to queue" }
    batchJobChunkExecutionQueue.addItemsToLocalQueue(listOf(executionItem))
  }

  private fun onJobCompleted(executionItem: ExecutionQueueItem) {
    runningJobs.remove(executionItem.chunkExecutionId)
    logger.debug("Chunk ${executionItem.chunkExecutionId}: Completed")
    logger.debug("Running jobs: ${runningJobs.size}")
  }

  private val jobsToLaunch get() = batchProperties.concurrency - runningJobs.size

  fun ExecutionQueueItem.isTimeToExecute(): Boolean {
    val executeAfter = this.executeAfter ?: return true
    return executeAfter <= currentDateProvider.date.time
  }

  fun ExecutionQueueItem.shouldNotBeDebounced(): Boolean {
    val dto = batchJobService.getJobDto(this.jobId)
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
    val runningJobCharacterCounts = runningJobs.values.filter { it.first.jobCharacter == character }.size
    val allowedCharacterCounts = ceil(character.maxConcurrencyRatio * batchProperties.concurrency)
    return runningJobCharacterCounts < allowedCharacterCounts
  }

  private fun ExecutionQueueItem.trySetRunningState(): Boolean {
    return progressManager.trySetExecutionRunning(this.chunkExecutionId, this.jobId) {
      val count =
        it.values.count { executionState -> executionState.status == BatchJobChunkExecutionStatus.RUNNING }
      if (count == 0) {
        return@trySetExecutionRunning true
      }
      batchJobService.getJobDto(this.jobId).maxPerJobConcurrency > count
    }
  }
}
