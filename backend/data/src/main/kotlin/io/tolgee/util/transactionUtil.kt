package io.tolgee.util

import jakarta.persistence.OptimisticLockException
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
): T {
  return executeInNewTransaction(
    transactionManager = transactionManager,
    fn = fn,
    propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW,
    readOnly = readOnly,
  )
}

private const val SERIALIZATION_FAILURE = "40001"
private const val DEADLOCK_DETECTED = "40P01"
private const val UNIQUE_VIOLATION = "23505"
private val RETRYABLE_SQL_STATES = setOf(SERIALIZATION_FAILURE, DEADLOCK_DETECTED, UNIQUE_VIOLATION)

/**
 * "Repeatable" means retried: when an attempt fails on a locking, serialization or unique-violation conflict, [fn]
 * runs again in a fresh transaction, so it can run several times. Anything the rollback does not undo (notifications,
 * events, file storage) belongs after this call returns or in an afterCommit synchronization.
 */
fun <T> executeInNewRepeatableTransaction(
  transactionManager: PlatformTransactionManager,
  propagationBehavior: Int = TransactionDefinition.PROPAGATION_REQUIRES_NEW,
  isolationLevel: Int = TransactionDefinition.ISOLATION_REPEATABLE_READ,
  fn: () -> T,
): T =
  tryUntil(
    shouldRetry = { it.isRetryable() },
    exceptionToThrow = { repeats, cause -> RepeatedlyCannotSerializeTransactionException(cause, repeats) },
  ) {
    executeInNewTransaction(
      transactionManager,
      propagationBehavior = propagationBehavior,
      isolationLevel = isolationLevel,
    ) {
      fn()
    }
  }

private fun Exception.isRetryable(): Boolean {
  if (this is OptimisticLockException || this is PessimisticLockingFailureException) return true
  return generateSequence<Throwable>(this) { it.cause }
    .filterIsInstance<SQLException>()
    .any { it.sqlState in RETRYABLE_SQL_STATES }
}

class RepeatedlyCannotSerializeTransactionException(
  cause: Throwable,
  repeats: Int,
) : RuntimeException("Retry failed $repeats times.", cause)
