package io.tolgee.performance

import io.tolgee.AbstractDatabaseTest
import io.tolgee.service.PerformanceTestService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
@Tag("performance")
class PerformanceTest : AbstractDatabaseTest() {

    @Autowired
    private lateinit var performanceTestService: PerformanceTestService

    @Test
    fun `measure database performance`() {
        // Create test data
        val totalProjects = performanceTestService.createBulkTestData(10, 10)
        println("Created $totalProjects test projects")
        
        // Measure single-threaded performance
        val singleThreadTime = performanceTestService.measureQueryPerformance(10)
        println("Single-threaded query time: $singleThreadTime ms")
        
        // Measure multi-threaded performance
        val multiThreadTime = performanceTestService.measureParallelQueryPerformance(10, 4)
        println("Multi-threaded query time: $multiThreadTime ms")
    }
} 