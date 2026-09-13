package io.tolgee.configuration

import io.tolgee.exceptions.StreamingCapacityExceededException
import io.tolgee.exceptions.StreamingUnavailableException
import java.util.concurrent.RejectedExecutionHandler
import java.util.concurrent.ThreadPoolExecutor

class StreamingAbortPolicy(
  private val onReject: () -> Unit,
) : RejectedExecutionHandler {
  override fun rejectedExecution(
    runnable: Runnable,
    executor: ThreadPoolExecutor,
  ) {
    // ExecutorConfigurationSupport stops before the web server, so requests still in flight during
    // shutdown reach this handler; they are not capacity pressure.
    if (executor.isShutdown) {
      throw StreamingUnavailableException("Streaming pool is shutting down")
    }
    onReject()
    throw StreamingCapacityExceededException(
      "Streaming pool saturated (queued=${executor.queue.size}, active=${executor.activeCount})",
    )
  }
}
