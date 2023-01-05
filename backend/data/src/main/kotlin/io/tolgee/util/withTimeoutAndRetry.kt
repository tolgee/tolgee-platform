package io.tolgee.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

fun <T> withTimeoutAndRetry(timeoutInMs: Long = 4000, fn: () -> T): T {
  return runBlocking(Dispatchers.IO) {
    val retries = 3
    var exp: TimeoutCancellationException? = null
    (0..retries).forEach {
      val task = async { fn() }
      try {
        return@runBlocking withTimeout(timeoutInMs) {
          task.await()
        }
      } catch (e: TimeoutCancellationException) {
        exp = e
      }
    }
    exp?.let {
      throw it
    }
    throw IllegalStateException("No exception caught!")
  }
}
