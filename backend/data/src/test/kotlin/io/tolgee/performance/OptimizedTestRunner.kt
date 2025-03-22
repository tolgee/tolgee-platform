package io.tolgee.performance

import org.junit.platform.engine.discovery.DiscoverySelectors
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import org.junit.platform.launcher.listeners.SummaryGeneratingListener
import org.junit.platform.launcher.listeners.TestExecutionSummary
import java.io.PrintWriter
import java.time.Duration
import java.time.Instant

object OptimizedTestRunner {
    @JvmStatic
    fun main(args: Array<String>) {
        val startTime = Instant.now()
        
        val packageName = args.getOrElse(0) { "io.tolgee" }
        println("Running tests in package: $packageName")
        
        val request = LauncherDiscoveryRequestBuilder.request()
            .selectors(DiscoverySelectors.selectPackage(packageName))
            .build()
        
        val launcher = LauncherFactory.create()
        val listener = SummaryGeneratingListener()
        
        launcher.registerTestExecutionListeners(listener)
        launcher.execute(request)
        
        val summary = listener.summary
        printSummary(summary)
        
        val endTime = Instant.now()
        val duration = Duration.between(startTime, endTime)
        println("Total execution time: ${duration.toSeconds()} seconds")
    }
    
    private fun printSummary(summary: TestExecutionSummary) {
        val writer = PrintWriter(System.out)
        summary.printTo(writer)
        writer.flush()
        
        println("\nTest run finished:")
        println("  Tests found: ${summary.testsFoundCount}")
        println("  Tests started: ${summary.testsStartedCount}")
        println("  Tests skipped: ${summary.testsSkippedCount}")
        println("  Tests aborted: ${summary.testsAbortedCount}")
        println("  Tests succeeded: ${summary.testsSucceededCount}")
        println("  Tests failed: ${summary.testsFailedCount}")
        println("  Time: ${summary.timeFinished - summary.timeStarted}ms")
        
        // Print performance metrics
        println("\nPerformance metrics:")
        println("  Average test execution time: ${(summary.timeFinished - summary.timeStarted) / summary.testsStartedCount.coerceAtLeast(1)}ms per test")
        
        // Print failed tests if any
        if (summary.testsFailedCount > 0) {
            println("\nFailed tests:")
            summary.failures.forEach { failure ->
                println("  - ${failure.testIdentifier.displayName}: ${failure.exception.message}")
            }
        }
    }
} 