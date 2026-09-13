package io.tolgee.exceptions

import java.util.concurrent.RejectedExecutionException

/** The streaming pool cannot take this request now; the HTTP layer answers 503 rather than 500. */
open class StreamingUnavailableException(
  message: String,
) : RejectedExecutionException(message)

class StreamingCapacityExceededException(
  message: String,
) : StreamingUnavailableException(message)
