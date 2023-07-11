package io.tolgee.batch

import io.sentry.Sentry
import io.tolgee.component.CurrentDateProvider
import io.tolgee.configuration.tolgee.BatchProperties
import io.tolgee.util.Logging
import io.tolgee.util.logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.PreDestroy
import kotlin.coroutines.CoroutineContext

@Component
class BatchJobConcurrentLauncher(
  private val batchProperties: BatchProperties,
  private val jobChunkExecutionQueue: JobChunkExecutionQueue,
  private val currentDateProvider: CurrentDateProvider
) : Logging {
  companion object {
    val runningInstances: ConcurrentHashMap.KeySetView<BatchJobConcurrentLauncher, Boolean> =
      ConcurrentHashMap.newKeySet()
  }

  val runningJobs: ConcurrentHashMap<Long, Pair<Long, Job>> = ConcurrentHashMap()
  var pause = false
  var masterRunJob: Job? = null
  var run = true

  fun stop() {
    logger.trace("Stopping batch job launcher ${System.identityHashCode(this)}}")
    run = false
    runBlocking(Dispatchers.IO) {
      masterRunJob?.join()
    }
    logger.trace("Batch job launcher stopped ${System.identityHashCode(this)}")
    runningInstances.remove(this)
  }

  @PreDestroy
  fun preDestroy() {
    this.stop()
  }

  fun repeatForever(fn: () -> Unit) {
    runningInstances.forEach { it.stop() }
    runningInstances.add(this)

    logger.trace("Started batch job action service ${System.identityHashCode(this)}")
    while (run) {
      try {
        val startTime = System.currentTimeMillis()
        fn()
        val sleepTime = getSleepTime(startTime)
        if (sleepTime > 0) {
          Thread.sleep(sleepTime)
        }
      } catch (e: Throwable) {
        Sentry.captureException(e)
        logger.error("Error in batch job action service", e)
      }
    }
  }

  private fun getSleepTime(startTime: Long): Long {
    if (!jobChunkExecutionQueue.isEmpty() && jobsToLaunch > 0) {
      return 0
    }
    return BatchJobActionService.MIN_TIME_BETWEEN_OPERATIONS - (System.currentTimeMillis() - startTime)
  }

  fun run(processExecution: (executionItem: ExecutionQueueItem, coroutineContext: CoroutineContext) -> Unit) {
    @Suppress("OPT_IN_USAGE")
    masterRunJob = GlobalScope.launch(Dispatchers.IO) {
      repeatForever {
        if (pause) {
          return@repeatForever
        }

        val jobsToLaunch = jobsToLaunch
        if (jobsToLaunch <= 0) {
          return@repeatForever
        }

        logger.trace("Jobs to launch: $jobsToLaunch")
        val items = (1..jobsToLaunch)
          .mapNotNull { jobChunkExecutionQueue.poll() }

        logItemsPulled(items)

        items.forEach { executionItem ->
          handleItem(executionItem, processExecution)
        }
      }
    }
  }

  private fun logItemsPulled(items: List<ExecutionQueueItem>) {
    if (items.isNotEmpty()) {
      logger.debug(
        "Pulled ${items.size} items from queue: " +
          items.joinToString(", ") { it.chunkExecutionId.toString() }
      )
    }
    logger.debug(
      "${jobChunkExecutionQueue.size} is left in the queue (${System.identityHashCode(jobChunkExecutionQueue)}): " +
        jobChunkExecutionQueue.joinToString(", ") { it.chunkExecutionId.toString() }
    )
  }

  private fun CoroutineScope.handleItem(
    executionItem: ExecutionQueueItem,
    processExecution: (executionItem: ExecutionQueueItem, coroutineContext: CoroutineContext) -> Unit
  ) {
    if (!executionItem.isTimeToExecute()) {
      logger.debug(
        """Execution ${executionItem.chunkExecutionId} not ready to execute, adding back to queue:
                    | Difference ${executionItem.executeAfter!! - currentDateProvider.date.time}""".trimMargin()
      )
      jobChunkExecutionQueue.addItemsToLocalQueue(listOf(executionItem))
      return
    }

    val job = launch {
      processExecution(executionItem, this.coroutineContext)
    }

    runningJobs[executionItem.chunkExecutionId] = executionItem.jobId to job

    job.invokeOnCompletion {
      onJobCompleted(executionItem)
    }
    logger.debug("Execution ${executionItem.chunkExecutionId} launched. Running jobs: ${runningJobs.size}")
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
}
