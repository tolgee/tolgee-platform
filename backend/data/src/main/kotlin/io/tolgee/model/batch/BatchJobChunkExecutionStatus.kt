package io.tolgee.model.batch

/** Stored by ordinal in Redis batch job state: append only — reordering silently restatuses in-flight chunks. */
enum class BatchJobChunkExecutionStatus(
  val completed: Boolean,
) {
  PENDING(false),
  RUNNING(false),
  SUCCESS(true),
  FAILED(true),
  CANCELLED(true),
}
