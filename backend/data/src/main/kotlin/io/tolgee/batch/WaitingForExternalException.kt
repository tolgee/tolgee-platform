package io.tolgee.batch

/**
 * Thrown by chunk processors to signal that the chunk has been submitted to an
 * external service and should enter WAITING_FOR_EXTERNAL status.
 * The ChunkProcessingUtil catches this and sets the appropriate status
 * instead of marking the chunk as FAILED.
 */
class WaitingForExternalException(
  message: String = "Chunk submitted to external service, awaiting results",
) : RuntimeException(message)
