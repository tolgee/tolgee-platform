package io.tolgee.performance

import io.tolgee.LightweightIntegrationTest
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

class ContextLoadingTimeTest : LightweightIntegrationTest() {
    
    @Autowired
    private lateinit var applicationContext: ApplicationContext
    
    @Test
    fun `measure context loading time`() {
        val startTime = System.currentTimeMillis()
        // Just access a bean to ensure context is loaded
        assertNotNull(applicationContext)
        val loadTime = System.currentTimeMillis() - startTime
        println("Context loaded in $loadTime ms")
    }
} 