package io.tolgee.fixtures

fun waitFor(timeout: Long = 10000, pollTime: Long = 10, fn: () -> Boolean) {
  val time = System.currentTimeMillis()
  var done = false
  while (!done && System.currentTimeMillis() - time < timeout) {
    done = fn()
    if (!done) {
      Thread.sleep(pollTime)
    }
  }

  if (!done) {
    throw WaitNotSatisfiedException()
  }
}

class WaitNotSatisfiedException : Exception()
