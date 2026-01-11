package io.tolgee.batch.state

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Scheduled task that periodically cleans up unused batch job states.
 * If the state of all executions is completed, it's highly probable
 * the state is not needed anymore and can be cleaned up.
 */
@Component
class BatchJobStateCleanupScheduler(
  private val batchJobStateProvider: BatchJobStateProvider,
) {
  @Scheduled(fixedRate = 10000)
  fun clearUnusedStates() {
    batchJobStateProvider.clearUnusedStates()
  }
}
