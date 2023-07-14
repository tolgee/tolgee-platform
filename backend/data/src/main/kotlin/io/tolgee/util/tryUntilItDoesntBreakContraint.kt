package io.tolgee.util

import org.springframework.dao.DataIntegrityViolationException
import javax.persistence.PersistenceException

inline fun <T> tryUntilItDoesntBreakConstraint(fn: () -> T): T {
  var exception: Exception? = null
  var repeats = 0
  for (it in 1..100) {
    try {
      return fn()
    } catch (e: DataIntegrityViolationException) {
      repeats++
      exception = e
    } catch (e: PersistenceException) {
      repeats++
      exception = e
    }
  }

  throw RepeatedlyThrowingConstraintViolationException(exception!!, repeats)
}

class RepeatedlyThrowingConstraintViolationException(cause: Throwable, repeats: Int) :
  RuntimeException("Retry failed $repeats times.", cause)
