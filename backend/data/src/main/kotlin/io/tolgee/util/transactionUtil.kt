package io.tolgee.util

import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate

fun <T> executeInNewTransaction(transactionManager: PlatformTransactionManager, fn: () -> T): T {
  val tt = TransactionTemplate(transactionManager)
  tt.propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW

  return tt.execute {
    fn()
  } as T
}
