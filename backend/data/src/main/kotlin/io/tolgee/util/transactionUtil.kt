package io.tolgee.util

import jakarta.persistence.OptimisticLockException
import org.springframework.core.NestedExceptionUtils
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.PessimisticLockingFailureException
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import java.sql.SQLException

fun <T> executeInNewTransaction(
  transactionManager: PlatformTransactionManager,
  isolationLevel: Int = TransactionDefinition.ISOLATION_DEFAULT,
  propagationBehavior: Int = TransactionDefinition.PROPAGATION_REQUIRES_NEW,
  readOnly: Boolean = false,
  fn: (ts: TransactionStatus) -> T,
): T {
  val tt = TransactionTemplate(transactionManager)
  tt.propagationBehavior = propagationBehavior
  tt.isolationLevel = isolationLevel
  tt.isReadOnly = readOnly

  return tt.execute { ts ->
    fn(ts)
  }
}

fun <T> executeInNewTransaction(
  transactionManager: PlatformTransactionManager,
  readOnly: Boolean = false,
  fn: (ts: TransactionStatus) -> T,
): T =
  executeInNewTransaction(
    transactionManager = transactionManager,
    propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW,
    readOnly = readOnly,
    fn = fn,
  )

private const val MAX_ATTEMPTS = 100
private const val SERIALIZATION_FAILURE = "40001"
private const val DEADLOCK_DETECTED = "40P01"

fun <T> executeInNewRepeatableTransaction(
  transactionManager: PlatformTransactionManager,
  propagationBehavior: Int = TransactionDefinition.PROPAGATION_REQUIRES_NEW,
  isolationLevel: Int = TransactionDefinition.ISOLATION_REPEATABLE_READ,
  fn: () -> T,
): T {
  var exception: Exception? = null
  repeat(MAX_ATTEMPTS) {
    try {
      return executeInNewTransaction(
        transactionManager,
        propagationBehavior = propagationBehavior,
        isolationLevel = isolationLevel,
      ) {
        fn()
      }
    } catch (e: Exception) {
      if (!e.isRetryable()) throw e
      exception = e
    }
  }
  throw RepeatedlyCannotSerializeTransactionException(exception!!, MAX_ATTEMPTS)
}

private fun Exception.isRetryable(): Boolean {
  if (this is OptimisticLockException || this is PessimisticLockingFailureException ||
    this is DataIntegrityViolationException
  ) {
    return true
  }
  val sqlState = (NestedExceptionUtils.getMostSpecificCause(this) as? SQLException)?.sqlState
  return sqlState == SERIALIZATION_FAILURE || sqlState == DEADLOCK_DETECTED
}

class RepeatedlyCannotSerializeTransactionException(
  cause: Throwable,
  repeats: Int,
) : RuntimeException("Retry failed $repeats times.", cause)
