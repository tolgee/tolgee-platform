package io.tolgee.exceptions

import java.util.concurrent.RejectedExecutionException

/**
 * Marks a rejection as coming from the streaming pool, so the HTTP layer can answer 503 without also
 * claiming every other executor's rejection.
 */
class StreamingCapacityExceededException(
  message: String,
) : RejectedExecutionException(message)
