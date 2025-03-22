package io.tolgee.util

import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.util.function.Supplier

@Component
class TransactionUtil(
    private val transactionManager: PlatformTransactionManager
) {
    fun <T> executeInTransaction(supplier: Supplier<T>): T {
        val transactionTemplate = TransactionTemplate(transactionManager)
        return transactionTemplate.execute { supplier.get() }!!
    }

    fun executeInTransaction(runnable: Runnable) {
        val transactionTemplate = TransactionTemplate(transactionManager)
        transactionTemplate.execute {
            runnable.run()
            null
        }
    }
} 