package io.tolgee.util

import org.springframework.dao.DataIntegrityViolationException

fun <T> tryUntilItDoesntBreakConstraint(fn: () -> T): T {
  var exception: DataIntegrityViolationException? = null
  var repeats = 0
  for (it in 0..100) {
    try {
      return fn()
    } catch (e: DataIntegrityViolationException) {
      Thread.sleep(10)
      repeats++
      exception = e
    }
  }

  throw RepeatedlyThrowingConstraintViolationException(exception!!, repeats)
}

class RepeatedlyThrowingConstraintViolationException(cause: Throwable, repeats: Int) :
  RuntimeException("Retry failed $repeats times.", cause)
