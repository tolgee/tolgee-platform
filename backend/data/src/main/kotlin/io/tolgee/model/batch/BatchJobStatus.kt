package io.tolgee.model.batch

enum class BatchJobStatus(
  val completed: Boolean,
) {
  PENDING(false),
  RUNNING(false),
  SUCCESS(true),
  FAILED(true),
  CANCELLED(true),
  DEBOUNCED(true),
}
