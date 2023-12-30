package io.tolgee.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

fun <T> withTimeoutRetrying(
  timeout: Long,
  retries: Int = 3,
  fn: () -> T,
): T {
  var repeats = 0
  for (it in 0..retries) {
    try {
      return runBlocking(Dispatchers.IO) {
        withTimeout(timeout) {
          fn()
        }
      }
    } catch (e: TimeoutCancellationException) {
      repeats++
    }
  }

  throw RepeatedTimeoutException(repeats)
}

class RepeatedTimeoutException(repeats: Int) :
  RuntimeException("Retry failed $repeats times")
