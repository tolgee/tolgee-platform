package io.tolgee

import org.springframework.test.context.transaction.TestTransaction
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

@Transactional
abstract class AbstractTransactionalTest {
    // Counter for generating unique IDs
    private val uniqueCounter = AtomicLong(System.currentTimeMillis())
    
    /**
     * Generate a unique ID for test data
     */
    protected fun uniqueId(): Long {
        return uniqueCounter.incrementAndGet()
    }
    
    /**
     * Generate a unique string for test data
     */
    protected fun uniqueString(prefix: String = ""): String {
        return "$prefix-${UUID.randomUUID()}"
    }
    
    /**
     * Commit the current transaction and start a new one
     */
    protected fun commitAndStartNewTransaction() {
        TestTransaction.flagForCommit()
        TestTransaction.end()
        TestTransaction.start()
    }
    
    /**
     * Execute code in a new transaction and commit it
     */
    protected fun <T> executeInNewTransaction(block: () -> T): T {
        val wasActive = TestTransaction.isActive()
        
        if (wasActive) {
            TestTransaction.flagForCommit()
            TestTransaction.end()
        }
        
        TestTransaction.start()
        
        try {
            val result = block()
            TestTransaction.flagForCommit()
            TestTransaction.end()
            return result
        } finally {
            if (wasActive) {
                TestTransaction.start()
            }
        }
    }
} 