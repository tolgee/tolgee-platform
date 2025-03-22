package io.tolgee.performance

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Aspect
@Component
@ConditionalOnProperty(name = ["test.performance.monitoring.enabled"], havingValue = "true", matchIfMissing = false)
class TestPerformanceMonitor {

    private val performanceData = mutableMapOf<String, MutableList<Long>>()
    private val reportDir = File("build/reports/performance")

    init {
        reportDir.mkdirs()
    }

    @Around("execution(* io.tolgee..*.*(..)) && @annotation(org.junit.jupiter.api.Test)")
    fun monitorTestPerformance(joinPoint: ProceedingJoinPoint): Any? {
        val start = Instant.now()
        val testName = "${joinPoint.signature.declaringTypeName}.${joinPoint.signature.name}"
        
        try {
            return joinPoint.proceed()
        } finally {
            val end = Instant.now()
            val duration = Duration.between(start, end).toMillis()
            
            performanceData.getOrPut(testName) { mutableListOf() }.add(duration)
            
            println("Test $testName executed in $duration ms")
            
            // Save after each test to ensure data is not lost
            savePerformanceReport()
        }
    }

    private fun savePerformanceReport() {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
        val reportFile = File(reportDir, "performance-report-$timestamp.txt")
        
        reportFile.writeText("Tolgee Test Performance Report\n")
        reportFile.appendText("Generated at: $timestamp\n\n")
        reportFile.appendText("Test Performance Metrics:\n")
        
        performanceData.entries.sortedByDescending { it.value.average() }.forEach { (testName, durations) ->
            val avgDuration = durations.average()
            val maxDuration = durations.maxOrNull() ?: 0
            val minDuration = durations.minOrNull() ?: 0
            
            reportFile.appendText("$testName:\n")
            reportFile.appendText("  Executions: ${durations.size}\n")
            reportFile.appendText("  Average: $avgDuration ms\n")
            reportFile.appendText("  Min: $minDuration ms\n")
            reportFile.appendText("  Max: $maxDuration ms\n\n")
        }
    }
} 