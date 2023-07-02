package io.tolgee.util

import org.springframework.dao.CannotAcquireLockException
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import javax.persistence.OptimisticLockException

fun <T> executeInNewTransaction(
  transactionManager: PlatformTransactionManager,
  isolationLevel: Int = TransactionDefinition.ISOLATION_SERIALIZABLE,
  propagationBehavior: Int = TransactionDefinition.PROPAGATION_REQUIRES_NEW,
  fn: () -> T
): T {
  val tt = TransactionTemplate(transactionManager)
  tt.propagationBehavior = propagationBehavior
  tt.isolationLevel = isolationLevel

  return tt.execute {
    fn()
  } as T
}

fun <T> executeInNewRepeatableTransaction(
  transactionManager: PlatformTransactionManager,
  propagationBehavior: Int = TransactionDefinition.PROPAGATION_REQUIRES_NEW,
  fn: () -> T
): T {
  var exception: Exception? = null
  var repeats = 0
  for (it in 1..100) {
    try {
      return executeInNewTransaction(transactionManager, propagationBehavior = propagationBehavior) {
        fn()
      }
    } catch (e: Exception) {
      when (e) {
        is OptimisticLockException, is CannotAcquireLockException -> {
          exception = e
          repeats++
        }
        else -> throw e
      }
    }
  }
  throw RepeatedlyCannotSerializeTransactionException(exception!!, repeats)
}

fun <T> executeInNewTransaction(
  transactionManager: PlatformTransactionManager,
  fn: () -> T
): T {
  return executeInNewTransaction(
    transactionManager = transactionManager,
    fn = fn,
    propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
  )
}

class RepeatedlyCannotSerializeTransactionException(cause: Throwable, repeats: Int) :
  RuntimeException("Retry failed $repeats times.", cause)
