package io.tolgee.batch

import io.tolgee.model.batch.BatchJobChunkExecution

/**
 * Interface for cancelling external batch API operations, implemented in the EE module.
 * Used by [BatchJobCancellationManager] to cancel OpenAI batch operations
 * when a user cancels a Tolgee batch job that has chunks in WAITING_FOR_EXTERNAL status.
 */
interface BatchApiCancellationHandler {
  /**
   * Cancel the external batch operation associated with the given chunk execution.
   * This sends a cancellation request to the external service (e.g., OpenAI Batch API)
   * and updates the tracker status. The poller will detect the cancellation on its next poll.
   */
  fun cancelExternalBatch(chunkExecution: BatchJobChunkExecution)
}
