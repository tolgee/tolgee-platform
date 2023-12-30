package io.tolgee.batch

import io.tolgee.batch.state.BatchJobStateProvider
import io.tolgee.component.CurrentDateProvider
import io.tolgee.util.Logging
import io.tolgee.util.addSeconds
import io.tolgee.util.logger
import io.tolgee.util.runSentryCatching
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ScheduledJobCleaner(
  private val batchJobService: BatchJobService,
  private val lockingManager: BatchJobProjectLockingManager,
  private val currentDateProvider: CurrentDateProvider,
  private val batchJobStateProvider: BatchJobStateProvider,
) : Logging {
  /**
   * Sometimes it doesn't unlock the job for project (for some reason)
   * For that reason, we have this scheduled task that unlocks all completed jobs
   */
  @Scheduled(fixedDelayString = """${'$'}{tolgee.batch.scheduled-unlock-job-delay:10000}""")
  fun cleanup() {
    runSentryCatching {
      handleCompletedJobs()
      handleStuckJobs()
    }
  }

  private fun handleStuckJobs() {
    batchJobService.getStuckJobIds(batchJobStateProvider.getCachedJobIds()).forEach {
      logger.warn("Removing stuck job state it using scheduled task")
      batchJobStateProvider.removeJobState(it)
    }
  }

  private fun handleCompletedJobs() {
    val lockedJobIds = lockingManager.getLockedJobIds() + batchJobStateProvider.getCachedJobIds()
    batchJobService.getJobsCompletedBefore(lockedJobIds, currentDateProvider.date.addSeconds(-10))
      .forEach {
        logger.warn("Unlocking completed job ${it.id} using scheduled task")
        lockingManager.unlockJobForProject(it.project.id, it.id)
        logger.warn("Removing completed job state ${it.id} using scheduled task")
        batchJobStateProvider.removeJobState(it.id)
      }
  }
}
