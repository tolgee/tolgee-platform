package io.tolgee.model.batch

enum class OpenAiBatchTrackerStatus {
  /** Batch has been submitted to OpenAI. */
  SUBMITTED,

  /** OpenAI is actively processing the batch. */
  IN_PROGRESS,

  /** Results have been downloaded and parsed. */
  RESULTS_READY,

  /** Results are being applied to translations. */
  APPLYING,

  /** All results applied successfully. */
  COMPLETED,

  /** The batch failed at OpenAI or during application. */
  FAILED,

  /** The batch was cancelled. */
  CANCELLED,
}
