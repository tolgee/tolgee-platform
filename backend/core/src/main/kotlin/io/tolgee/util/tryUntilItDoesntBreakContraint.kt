package io.tolgee.util

import jakarta.persistence.PersistenceException
import org.springframework.dao.CannotAcquireLockException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.orm.ObjectOptimisticLockingFailureException

inline fun <T> tryUntilItDoesntBreakConstraint(
  maxRepeats: Int = 100,
  fn: () -> T,
): T {
  return tryUntil(
    maxRepeats = maxRepeats,
    shouldRetry = { e ->
      isCommonDbError(e)
    },
    exceptionToThrow = { repeats, cause -> RepeatedlyThrowingConstraintViolationException(cause, repeats) },
    fn = fn,
  )
}

fun isCommonDbError(e: Exception) =
  e is DataIntegrityViolationException ||
    e is PersistenceException ||
    e is CannotAcquireLockException ||
    e is ObjectOptimisticLockingFailureException

inline fun <T> tryUntil(
  maxRepeats: Int = 100,
  shouldRetry: (Exception) -> Boolean,
  exceptionToThrow: (repeats: Int, cause: Throwable) -> Throwable,
  fn: () -> T,
): T {
  var lastException: Exception? = null
  var repeats = 0
  for (it in 1..maxRepeats) {
    try {
      return fn()
    } catch (e: Exception) {
      if (shouldRetry(e)) {
        repeats++
        lastException = e
      } else {
        throw e
      }
    }
  }

  throw exceptionToThrow(repeats, lastException!!)
}

class RepeatedlyThrowingConstraintViolationException(
  cause: Throwable,
  repeats: Int,
) : RepeatedlyThrowingException(cause, repeats)

open class RepeatedlyThrowingException(
  cause: Throwable,
  repeats: Int,
) : RuntimeException("Retry failed $repeats times.", cause)
