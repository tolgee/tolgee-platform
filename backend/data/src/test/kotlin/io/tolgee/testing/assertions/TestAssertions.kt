package io.tolgee.testing.assertions

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.function.Executable
import java.time.Duration

object TestAssertions {
    fun assertExecutionTime(maxDurationMs: Long, executable: Executable) {
        val start = System.currentTimeMillis()
        executable.execute()
        val end = System.currentTimeMillis()
        val duration = end - start
        
        Assertions.assertTrue(
            duration <= maxDurationMs,
            "Execution took $duration ms, which exceeds the maximum allowed time of $maxDurationMs ms"
        )
    }
    
    fun <T> assertExecutionTime(maxDurationMs: Long, supplier: () -> T): T {
        val start = System.currentTimeMillis()
        val result = supplier()
        val end = System.currentTimeMillis()
        val duration = end - start
        
        Assertions.assertTrue(
            duration <= maxDurationMs,
            "Execution took $duration ms, which exceeds the maximum allowed time of $maxDurationMs ms"
        )
        
        return result
    }
} 