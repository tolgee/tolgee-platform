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

  val runningJobs: ConcurrentHashMap<Long, Pair<Long, Job>> = ConcurrentHashMap()
  var pause = false
  var masterRunJob: Job? = null
  var run = true

  @PreDestroy
  fun stop() {
    run = false
    runBlocking(Dispatchers.IO) {
      masterRunJob?.join()
    }
  }

  fun repeatForever(fn: () -> Unit) {
    while (run) {
      try {
        val startTime = System.currentTimeMillis()
        fn()
        val sleepTime = BatchJobActionService.MIN_TIME_BETWEEN_OPERATIONS - (System.currentTimeMillis() - startTime)
        if (sleepTime > 0) {
          Thread.sleep(sleepTime)
        }
      } catch (e: Throwable) {
        Sentry.captureException(e)
        logger.error("Error in batch job action service", e)
      }
    }
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
        (1..jobsToLaunch)
          .mapNotNull { jobChunkExecutionQueue.poll() }
          .forEach { executionItem ->
            handleItem(executionItem, processExecution)
          }
      }
    }
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
      jobChunkExecutionQueue.add(executionItem)
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
