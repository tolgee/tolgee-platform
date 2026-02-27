package io.tolgee.batch.timing

/**
 * Interface for inline timing of internal operations.
 * Implemented by BatchJobOperationTimer in the development module.
 * When not available (production), operations run without timing overhead.
 */
interface BatchJobTimerProvider {
  fun <T> measure(
    operationName: String,
    block: () -> T,
  ): T
}
