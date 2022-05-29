package io.tolgee.util

import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate

fun executeInNewTransaction(transactionManager: PlatformTransactionManager, fn: () -> Unit) {
  val tt = TransactionTemplate(transactionManager)
  tt.propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW

  tt.executeWithoutResult {
    fn()
  }
}
