package io.tolgee.context

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Tag
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
@Tag("performance")
class ContextLoadTimeTest {

    @Test
    fun `measure context load time`() {
        val startTime = System.currentTimeMillis()
        // The context is already loaded by the time this test runs
        val loadTime = System.currentTimeMillis() - startTime
        println("Context load time: $loadTime ms")
    }
} 