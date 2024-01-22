package io.tolgee.fixtures

import kotlin.reflect.KClass

fun waitFor(
  timeout: Long = 10000,
  pollTime: Long = 10,
  fn: () -> Boolean,
) {
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

class WaitNotSatisfiedException : RuntimeException()

class WaitNotSatisfiedStillThrowingException(throwable: Throwable) : RuntimeException(throwable)

fun waitForNotThrowing(
  throwableClass: KClass<out Throwable> = Throwable::class,
  timeout: Long = 10000,
  pollTime: Long = 10,
  fn: () -> Unit,
) {
  lateinit var throwable: Throwable
  try {
    waitFor(timeout, pollTime) {
      try {
        fn()
        true
      } catch (t: Throwable) {
        if (throwableClass.java.isAssignableFrom(t::class.java)) {
          throwable = t
          return@waitFor false
        }
        throw t
      }
    }
  } catch (e: WaitNotSatisfiedException) {
    throw WaitNotSatisfiedStillThrowingException(throwable)
  }
}
