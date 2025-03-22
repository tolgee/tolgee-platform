package io.tolgee.testing

import org.springframework.test.context.TestContext
import org.springframework.test.context.TestExecutionListener
import org.springframework.test.context.transaction.TransactionalTestExecutionListener
import java.util.concurrent.ConcurrentHashMap

/**
 * Custom TestExecutionListener that ensures data isolation between tests
 * without truncating the database.
 */
class DataIsolationTestExecutionListener : TestExecutionListener {

    companion object {
        private val testMethodExecutionCounter = ConcurrentHashMap<String, Int>()
    }

    override fun beforeTestMethod(testContext: TestContext) {
        val testClass = testContext.testClass.name
        val testMethod = testContext.testMethod.name
        val testKey = "$testClass.$testMethod"
        
        // Increment the execution counter for this test method
        val executionCount = testMethodExecutionCounter.compute(testKey) { _, count ->
            (count ?: 0) + 1
        } ?: 1
        
        // Store the execution count in the test context attributes
        testContext.setAttribute("testExecutionCount", executionCount)
    }
} 