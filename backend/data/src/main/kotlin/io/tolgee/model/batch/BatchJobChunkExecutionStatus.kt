package io.tolgee.model.batch

enum class BatchJobChunkExecutionStatus(
  val completed: Boolean,
) {
  NEW(false),
  PENDING(false),
  RUNNING(false),
  SUCCESS(true),
  FAILED(true),
  CANCELLED(true),
}
