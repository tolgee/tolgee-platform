package io.tolgee.model.batch

enum class BatchJobChunkExecutionStatus(
  val completed: Boolean
) {
  PENDING(false),
  SUCCESS(true),
  FAILED(true),
  CANCELLED(true),
}
