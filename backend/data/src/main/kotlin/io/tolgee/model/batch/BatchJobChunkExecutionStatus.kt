package io.tolgee.model.batch

enum class BatchJobChunkExecutionStatus(
  val completed: Boolean,
) {
  PENDING(false),
  RUNNING(false),
  WAITING_FOR_EXTERNAL(false),
  SUCCESS(true),
  FAILED(true),
  CANCELLED(true),
}
