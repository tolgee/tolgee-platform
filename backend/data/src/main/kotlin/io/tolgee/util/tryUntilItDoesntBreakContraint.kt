package io.tolgee.util

import jakarta.persistence.PersistenceException
import org.springframework.dao.CannotAcquireLockException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.orm.ObjectOptimisticLockingFailureException

inline fun <T> tryUntilItDoesntBreakConstraint(
  maxRepeats: Int = 100,
  fn: () -> T,
): T {
  var exception: Exception? = null
  var repeats = 0
  for (it in 1..maxRepeats) {
    try {
      return fn()
    } catch (e: Exception) {
      when (e) {
        is DataIntegrityViolationException,
        is PersistenceException,
        is CannotAcquireLockException,
        is ObjectOptimisticLockingFailureException,
        -> {
          repeats++
          exception = e
        }

        else -> throw e
      }
    }
  }

  throw RepeatedlyThrowingConstraintViolationException(exception!!, repeats)
}

class RepeatedlyThrowingConstraintViolationException(cause: Throwable, repeats: Int) :
  RuntimeException("Retry failed $repeats times.", cause)
