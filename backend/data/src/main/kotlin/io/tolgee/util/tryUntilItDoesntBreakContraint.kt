package io.tolgee.util

import org.springframework.dao.DataIntegrityViolationException

fun <T> tryUntilItDoesntBreakConstraint(fn: () -> T): T {
  var exception: DataIntegrityViolationException? = null
  for (it in 0..100) {
    try {
      return fn()
    } catch (e: DataIntegrityViolationException) {
      Thread.sleep(10)
      exception = e
    }
  }

  throw exception!!
}

class RepeatedlyThrowingConstraintViolationException : RuntimeException()
