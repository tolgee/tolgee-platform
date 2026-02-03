package io.tolgee.util

import jakarta.persistence.OptimisticLockException
import org.springframework.dao.CannotAcquireLockException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionTemplate

fun <T> executeInNewTransaction(
  transactionManager: PlatformTransactionManager,
  isolationLevel: Int = TransactionDefinition.ISOLATION_READ_COMMITTED,
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
  } as T
}

fun <T> executeInNewTransaction(
  transactionManager: PlatformTransactionManager,
  readOnly: Boolean = false,
  fn: (ts: TransactionStatus) -> T,
): T {
  return executeInNewTransaction(
    transactionManager = transactionManager,
    fn = fn,
    propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW,
    readOnly = readOnly,
  )
}

fun <T> executeInNewRepeatableTransaction(
  transactionManager: PlatformTransactionManager,
  propagationBehavior: Int = TransactionDefinition.PROPAGATION_REQUIRES_NEW,
  isolationLevel: Int = TransactionDefinition.ISOLATION_READ_COMMITTED,
  fn: () -> T,
): T {
  var exception: Exception? = null
  var repeats = 0
  for (it in 1..100) {
    try {
      return executeInNewTransaction(
        transactionManager,
        propagationBehavior = propagationBehavior,
        isolationLevel = isolationLevel,
      ) {
        fn()
      }
    } catch (e: Exception) {
      when (e) {
        is OptimisticLockException, is CannotAcquireLockException, is DataIntegrityViolationException -> {
          exception = e
          repeats++
        }

        else -> throw e
      }
    }
  }
  throw RepeatedlyCannotSerializeTransactionException(exception!!, repeats)
}

class RepeatedlyCannotSerializeTransactionException(
  cause: Throwable,
  repeats: Int,
) : RuntimeException("Retry failed $repeats times.", cause)
