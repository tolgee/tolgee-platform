package io.tolgee.model.batch

/** Ordinal-serialized by [io.tolgee.batch.state.RedisBatchJobStateStorage] — do not reorder. */
enum class BatchJobChunkExecutionStatus(
  val completed: Boolean,
) {
  PENDING(false),
  RUNNING(false),
  SUCCESS(true),
  FAILED(true),
  CANCELLED(true),
}
