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

class WaitNotSatisfiedStillThrowingException(
  throwable: Throwable,
) : RuntimeException(throwable)

inline fun <reified T> waitForNotThrowing(
  throwableClass: KClass<out Throwable> = Throwable::class,
  timeout: Long = 10000,
  pollTime: Long = 10,
  crossinline fn: () -> T,
): T {
  lateinit var throwable: Throwable
  var result: T? = null
  try {
    waitFor(timeout, pollTime) {
      try {
        result = fn()
        true
      } catch (t: Throwable) {
        if (throwableClass.java.isAssignableFrom(t::class.java)) {
          throwable = t
          return@waitFor false
        }
        throw t
      }
    }
    return result!!
  } catch (e: WaitNotSatisfiedException) {
    throw WaitNotSatisfiedStillThrowingException(throwable)
  }
}
