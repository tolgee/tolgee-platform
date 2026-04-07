package io.tolgee.batch.state

import io.tolgee.component.SchedulingManager
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * Periodically cleans up unused batch job states from Redis / local storage.
 * If the state of all executions is completed, it's highly probable
 * the state is not needed anymore and can be cleaned up.
 *
 * Uses [SchedulingManager.scheduleWithFixedDelayDistributed] so the task is not
 * active during tests by default and runs at most once per interval across all nodes.
 */
@Component
class BatchJobStateCleanupScheduler(
  private val batchJobStateProvider: BatchJobStateProvider,
  private val schedulingManager: SchedulingManager,
) : Logging {
  @EventListener(ApplicationReadyEvent::class)
  fun schedule() {
    schedulingManager.scheduleWithFixedDelayDistributed(
      "batch_job_state_cleanup",
      CLEANUP_INTERVAL,
      batchJobStateProvider::clearUnusedStates,
    )
    logger.debug("Scheduled batch job state cleanup task")
  }

  companion object {
    private val CLEANUP_INTERVAL = Duration.ofMinutes(10)
  }
}
