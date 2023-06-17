package io.tolgee.util

import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate

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
