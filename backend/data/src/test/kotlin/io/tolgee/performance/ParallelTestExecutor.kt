package io.tolgee.performance

import org.junit.platform.engine.DiscoverySelector
import org.junit.platform.engine.discovery.DiscoverySelectors
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import org.junit.platform.launcher.listeners.SummaryGeneratingListener
import java.io.PrintWriter
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.Future

object ParallelTestExecutor {
    @JvmStatic
    fun main(args: Array<String>) {
        val startTime = Instant.now()
        println("Starting parallel test execution...")
        
        // Define test packages to run in parallel
        val testPackages = listOf(
            "io.tolgee.repository",
            "io.tolgee.service",
            "io.tolgee.controller",
            "io.tolgee.security"
        )
        
        // Create thread pool for parallel execution
        val threadCount = Runtime.getRuntime().availableProcessors().coerceAtLeast(2)
        val executor = Executors.newFixedThreadPool(threadCount)
        
        try {
            // Submit each package as a separate task
            val futures = testPackages.map { packageName ->
                executor.submit<TestResult> {
                    runTestsForPackage(packageName)
                }
            }
            
            // Collect and print results
            val results = futures.map { it.get() }
            printResults(results, startTime)
            
        } finally {
            executor.shutdown()
        }
    }
    
    private fun runTestsForPackage(packageName: String): TestResult {
        val startTime = Instant.now()
        println("Running tests for package: $packageName")
        
        val selector = DiscoverySelectors.selectPackage(packageName)
        val request = LauncherDiscoveryRequestBuilder.request()
            .selectors(selector)
            .build()
        
        val launcher = LauncherFactory.create()
        val listener = SummaryGeneratingListener()
        
        launcher.registerTestExecutionListeners(listener)
        launcher.execute(request)
        
        val summary = listener.summary
        val endTime = Instant.now()
        val duration = Duration.between(startTime, endTime)
        
        return TestResult(
            packageName = packageName,
            testsFound = summary.testsFoundCount,
            testsSucceeded = summary.testsSucceededCount,
            testsFailed = summary.testsFailedCount,
            duration = duration
        )
    }
    
    private fun printResults(results: List<TestResult>, startTime: Instant) {
        val endTime = Instant.now()
        val totalDuration = Duration.between(startTime, endTime)
        
        println("\n=== Parallel Test Execution Results ===")
        results.forEach { result ->
            println("Package: ${result.packageName}")
            println("  Tests found: ${result.testsFound}")
            println("  Tests succeeded: ${result.testsSucceeded}")
            println("  Tests failed: ${result.testsFailed}")
            println("  Duration: ${result.duration.seconds} seconds")
            println()
        }
        
        val totalTests = results.sumOf { it.testsFound }
        val totalSucceeded = results.sumOf { it.testsSucceeded }
        val totalFailed = results.sumOf { it.testsFailed }
        
        println("=== Summary ===")
        println("Total tests: $totalTests")
        println("Total succeeded: $totalSucceeded")
        println("Total failed: $totalFailed")
        println("Total duration: ${totalDuration.seconds} seconds")
        println("Parallel execution saved approximately ${results.sumOf { it.duration.seconds } - totalDuration.seconds} seconds")
    }
    
    data class TestResult(
        val packageName: String,
        val testsFound: Int,
        val testsSucceeded: Int,
        val testsFailed: Int,
        val duration: Duration
    )
} 