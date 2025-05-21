package io.tolgee.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

fun <T> withTimeoutRetrying(
  attempts: List<Long>,
  fn: () -> T,
): T {
  var repeats = 0
  for (timeout in attempts) {
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
