package io.tolgee.configuration

import io.tolgee.exceptions.StreamingCapacityExceededException
import java.util.concurrent.RejectedExecutionHandler
import java.util.concurrent.ThreadPoolExecutor

class StreamingAbortPolicy(
  private val onReject: () -> Unit,
) : RejectedExecutionHandler {
  override fun rejectedExecution(
    runnable: Runnable,
    executor: ThreadPoolExecutor,
  ) {
    onReject()
    throw StreamingCapacityExceededException(
      "Streaming pool saturated (queued=${executor.queue.size}, active=${executor.activeCount})",
    )
  }
}
