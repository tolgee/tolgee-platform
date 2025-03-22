package io.tolgee.testing

import org.springframework.test.context.TestContext
import org.springframework.test.context.TestExecutionListener
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * TestExecutionListener that provides unique identifiers for test data
 * to avoid conflicts between tests without truncating the database.
 */
class UniqueDataTestExecutionListener : TestExecutionListener {

    companion object {
        private val testIdCounter = AtomicLong(1)
        private val testMethodExecutionMap = ConcurrentHashMap<String, Long>()
    }

    override fun beforeTestMethod(testContext: TestContext) {
        val testClass = testContext.testClass.name
        val testMethod = testContext.testMethod.name
        val testKey = "$testClass.$testMethod"
        
        // Get or create a unique ID for this test method
        val testId = testMethodExecutionMap.computeIfAbsent(testKey) { 
            testIdCounter.getAndIncrement() 
        }
        
        // Store the test ID in the test context attributes
        testContext.setAttribute("testId", testId)
    }
} 