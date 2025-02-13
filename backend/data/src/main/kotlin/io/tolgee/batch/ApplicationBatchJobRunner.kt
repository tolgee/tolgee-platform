package io.tolgee.batch

import jakarta.annotation.PreDestroy
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationContext
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * This class handles running of batch jobs when application context is ready
 *
 * - It handles the stopping when application is being destroyed
 * - It helps us to stop all running instances of this runner for testing purposes, where we can
 *   get to unexpected situations
 */
@Component
class ApplicationBatchJobRunner(
  private val applicationContext: ApplicationContext,
) {
  companion object {
    /**
     * This helps us to keep track of all running instances across all existing application contexts
     *
     * In tests, spring caches the application context and creates a may create a new instance
     * of ApplicationBatchJobRunner
     *
     * We need to prevent this machine running in cached contexts, so we need this property to keep track
     * of all running instances to be safe that we can stop them all
     */
    val runningInstances: ConcurrentHashMap.KeySetView<ApplicationBatchJobRunner, Boolean> =
      ConcurrentHashMap.newKeySet()

    fun stopAll() {
      runningInstances.forEach { it.stop() }
    }
  }

  var isRunning = false

  @EventListener(ApplicationReadyEvent::class)
  fun run() {
    if (isRunning) {
      return
    }

    stopAll()
    isRunning = true
    runningInstances.add(this)
    batchJobConcurrentLauncher.run()
  }

  fun stop() {
    runningInstances.remove(this)
    batchJobConcurrentLauncher.stop()
    isRunning = false
  }

  /**
   * Whether the batch job queue is empty and there are no running job
   */
  val settled
    get() = batchJobConcurrentLauncher.runningJobs.isEmpty() && batchJobChunkExecutionQueue.isEmpty()

  @PreDestroy
  fun preDestroy() {
    stop()

    // To be super safe, rather stop them all if by any chance there is more than one instance running
    stopAll()
  }

  // We want to keep the same instance of BatchJobConcurrentLauncher for all instances of ApplicationBatchJobRunner
  // This should prevent spring from magically giving us different instances
  private val batchJobConcurrentLauncher: BatchJobConcurrentLauncher by lazy {
    applicationContext.getBean(BatchJobConcurrentLauncher::class.java)
  }

  // We want to keep the same instance of BatchJobChunkExecutionQueue for all instances of ApplicationBatchJobRunner
  private val batchJobChunkExecutionQueue: BatchJobChunkExecutionQueue by lazy {
    applicationContext.getBean(BatchJobChunkExecutionQueue::class.java)
  }
}
