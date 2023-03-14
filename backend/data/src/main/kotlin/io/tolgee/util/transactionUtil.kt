package io.tolgee.util

import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate

fun <T> executeInNewTransaction(
  transactionManager: PlatformTransactionManager,
  isolationLevel: Int = TransactionDefinition.ISOLATION_DEFAULT,
  fn: () -> T
): T {

  val tt = TransactionTemplate(transactionManager)
  tt.propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
  tt.isolationLevel = isolationLevel

  return tt.execute {
    fn()
  } as T
}
