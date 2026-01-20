package io.tolgee.batch

import io.tolgee.AbstractSpringTest
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.request.NoOpRequest
import io.tolgee.batch.state.BatchJobStateProvider
import io.tolgee.development.testDataBuilder.data.BatchJobsTestData
import io.tolgee.fixtures.RedisRunner
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.batch.BatchJobChunkExecutionStatus
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.assert
import io.tolgee.util.Logging
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.logger
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.PlatformTransactionManager

/**
 * Large-scale performance test for batch job orchestration with 100k chunks.
 *
 * This test creates a job with a very large number of chunks, runs it for a short time,
 * then cancels it to measure throughput and identify bottlenecks.
 *
 * Run with:
 * ./gradlew :server-app:test --tests "io.tolgee.batch.BatchJobLargeScalePerformanceTest"
 */
@SpringBootTest(
  properties = [
    "tolgee.cache.use-redis=true",
    "tolgee.cache.enabled=true",
    "tolgee.websocket.use-redis=true",
    "spring.redis.port=56379",
  ],
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
@ContextConfiguration(initializers = [BatchJobLargeScalePerformanceTest.Companion.Initializer::class])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ContextRecreatingTest
@Import(BatchJobLargeScalePerformanceTest.TimingConfiguration::class)
class BatchJobLargeScalePerformanceTest :
  AbstractSpringTest(),
  Logging {
  @TestConfiguration
  @EnableAspectJAutoProxy
  class TimingConfiguration {
    @Bean
    fun batchJobOperationTimer(): BatchJobOperationTimer = BatchJobOperationTimer()

    @Bean
    fun batchJobTimingAspect(timer: BatchJobOperationTimer): BatchJobTimingAspect = BatchJobTimingAspect(timer)
  }

  companion object {
    val redisRunner = RedisRunner()

    @AfterAll
    @JvmStatic
    fun stopRedis() {
      redisRunner.stop()
    }

    class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
      override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
        redisRunner.run()
      }
    }
  }

  private lateinit var testData: BatchJobsTestData

  @Autowired
  lateinit var batchJobService: BatchJobService

  @Autowired
  lateinit var batchJobChunkExecutionQueue: BatchJobChunkExecutionQueue

  @Autowired
  lateinit var batchJobConcurrentLauncher: BatchJobConcurrentLauncher

  @Autowired
  lateinit var batchJobCancellationManager: BatchJobCancellationManager

  @Autowired
  lateinit var transactionManager: PlatformTransactionManager

  @Autowired
  lateinit var operationTimer: BatchJobOperationTimer

  @Autowired
  lateinit var batchJobStateProvider: BatchJobStateProvider

  @BeforeEach
  fun setup() {
    batchJobChunkExecutionQueue.clear()
    testData = BatchJobsTestData()
    testDataService.saveTestData(testData.root)
    batchJobConcurrentLauncher.pause = false
    operationTimer.reset()
  }

  @AfterEach
  fun teardown() {
    batchJobChunkExecutionQueue.clear()
    batchJobConcurrentLauncher.pause = true
    operationTimer.printReport()
  }

  @Test
  fun `large job - measure throughput with cancellation`() {
    val chunkCount = 10_000
    val runTimeMs = 10_000L // Run for 10 seconds before cancelling

    logger.info("=" .repeat(60))
    logger.info("STARTING LARGE-SCALE PERFORMANCE TEST")
    logger.info("  Chunk count: $chunkCount")
    logger.info("  Run time before cancel: ${runTimeMs}ms")
    logger.info("=" .repeat(60))

    val startTime = System.currentTimeMillis()

    // Phase 1: Job Creation
    logger.info("Phase 1: Creating job with $chunkCount chunks...")
    val job = runNoOpJob(chunkCount)
    val jobCreationTime = System.currentTimeMillis()
    val creationDuration = jobCreationTime - startTime
    logger.info("Job created in ${creationDuration}ms")

    // Phase 2: Wait for queue to be populated
    logger.info("Phase 2: Waiting for queue population...")
    waitForQueuePopulation(chunkCount, 30_000)
    val queuePopulatedTime = System.currentTimeMillis()
    val queuePopulationDuration = queuePopulatedTime - jobCreationTime
    logger.info("Queue populated in ${queuePopulationDuration}ms, queue size: ${batchJobChunkExecutionQueue.size}")

    // Phase 3: Let job run for specified time
    logger.info("Phase 3: Running job for ${runTimeMs}ms...")
    val executionStartTime = System.currentTimeMillis()
    var lastProgressLog = executionStartTime
    var progressSamples = mutableListOf<Pair<Long, Int>>() // timestamp to completed count

    while (System.currentTimeMillis() - executionStartTime < runTimeMs) {
      Thread.sleep(500)

      val currentTime = System.currentTimeMillis()
      val executions = batchJobService.getExecutions(job.id)
      val completed = executions.count { it.status.completed }
      val running = executions.count { it.status == BatchJobChunkExecutionStatus.RUNNING }
      val pending = executions.count { it.status == BatchJobChunkExecutionStatus.PENDING }
      val queueSize = batchJobChunkExecutionQueue.size

      progressSamples.add(currentTime to completed)

      if (currentTime - lastProgressLog >= 2000) {
        logger.info("Progress: completed=$completed, running=$running, pending=$pending, queueSize=$queueSize")
        lastProgressLog = currentTime
      }
    }

    // Phase 4: Cancel job
    logger.info("Phase 4: Cancelling job...")
    val cancelStartTime = System.currentTimeMillis()

    executeInNewTransaction(transactionManager) {
      batchJobCancellationManager.cancel(job.id)
    }

    val cancelEndTime = System.currentTimeMillis()
    val cancelDuration = cancelEndTime - cancelStartTime
    logger.info("Job cancelled in ${cancelDuration}ms")

    // Phase 5: Calculate results
    val executions = batchJobService.getExecutions(job.id)
    val completedCount = executions.count { it.status == BatchJobChunkExecutionStatus.SUCCESS }
    val cancelledCount = executions.count { it.status == BatchJobChunkExecutionStatus.CANCELLED }
    val failedCount = executions.count { it.status == BatchJobChunkExecutionStatus.FAILED }
    val pendingCount = executions.count { it.status == BatchJobChunkExecutionStatus.PENDING }

    val actualExecutionTime = cancelStartTime - executionStartTime
    val chunksPerSecond = if (actualExecutionTime > 0) completedCount * 1000.0 / actualExecutionTime else 0.0

    // Calculate throughput from samples (to detect initial delay)
    val throughputOverTime = calculateThroughputOverTime(progressSamples)

    logger.info("=" .repeat(60))
    logger.info("PERFORMANCE RESULTS (100k CHUNKS WITH REDIS):")
    logger.info("=" .repeat(60))
    logger.info("TIMING:")
    logger.info("  Job creation time: ${creationDuration}ms")
    logger.info("  Queue population time: ${queuePopulationDuration}ms")
    logger.info("  Execution time: ${actualExecutionTime}ms")
    logger.info("  Cancellation time: ${cancelDuration}ms")
    logger.info("")
    logger.info("EXECUTION STATS:")
    logger.info("  Total chunks: $chunkCount")
    logger.info("  Completed: $completedCount")
    logger.info("  Cancelled: $cancelledCount")
    logger.info("  Failed: $failedCount")
    logger.info("  Pending: $pendingCount")
    logger.info("")
    logger.info("THROUGHPUT:")
    logger.info("  Overall: ${"%.2f".format(chunksPerSecond)} chunks/second")
    logger.info("")
    logger.info("THROUGHPUT OVER TIME (chunks/sec in 2-second windows):")
    throughputOverTime.forEach { (windowStart, throughput) ->
      logger.info("  ${windowStart}ms: ${"%.2f".format(throughput)} chunks/sec")
    }
    logger.info("=" .repeat(60))

    // Verify job is in cancelled state
    val finalJob = batchJobService.getJobDto(job.id)
    finalJob.status.assert.isEqualTo(BatchJobStatus.CANCELLED)
  }

  @Test
  fun `measure queue addition performance for large batches`() {
    val chunkCount = 100_000

    logger.info("=" .repeat(60))
    logger.info("QUEUE ADDITION PERFORMANCE TEST")
    logger.info("=" .repeat(60))

    val startTime = System.currentTimeMillis()

    // Just measure job creation and queue population
    val job = runNoOpJob(chunkCount)
    val jobCreationTime = System.currentTimeMillis()
    logger.info("Job created in ${jobCreationTime - startTime}ms")

    // Check queue size over time
    var lastSize = 0
    val sizeOverTime = mutableListOf<Pair<Long, Int>>()
    val measureStartTime = System.currentTimeMillis()

    while (System.currentTimeMillis() - measureStartTime < 30_000) {
      val currentSize = batchJobChunkExecutionQueue.size
      sizeOverTime.add(System.currentTimeMillis() - measureStartTime to currentSize)

      if (currentSize != lastSize) {
        logger.info("Queue size at ${System.currentTimeMillis() - measureStartTime}ms: $currentSize")
        lastSize = currentSize
      }

      if (currentSize >= chunkCount) {
        logger.info("Queue fully populated!")
        break
      }

      Thread.sleep(100)
    }

    val finalQueueTime = System.currentTimeMillis()
    logger.info("=" .repeat(60))
    logger.info("Queue population completed:")
    logger.info("  Total time: ${finalQueueTime - startTime}ms")
    logger.info("  Job creation: ${jobCreationTime - startTime}ms")
    logger.info("  Queue population: ${finalQueueTime - jobCreationTime}ms")
    logger.info("  Final queue size: ${batchJobChunkExecutionQueue.size}")
    logger.info("=" .repeat(60))

    // Cancel to cleanup
    batchJobConcurrentLauncher.pause = true
    executeInNewTransaction(transactionManager) {
      batchJobCancellationManager.cancel(job.id)
    }
  }

  private fun runNoOpJob(itemCount: Int): BatchJob {
    return executeInNewTransaction(transactionManager) {
      batchJobService.startJob(
        request =
          NoOpRequest().apply {
            itemIds = (1L..itemCount).toList()
          },
        project = testData.projectBuilder.self,
        author = testData.user,
        type = BatchJobType.NO_OP,
      )
    }
  }

  private fun waitForQueuePopulation(
    expectedSize: Int,
    timeoutMs: Long,
  ) {
    val startTime = System.currentTimeMillis()
    while (System.currentTimeMillis() - startTime < timeoutMs) {
      val currentSize = batchJobChunkExecutionQueue.size
      if (currentSize >= expectedSize) {
        return
      }
      Thread.sleep(100)
    }
    logger.warn("Queue population timeout! Expected $expectedSize, got ${batchJobChunkExecutionQueue.size}")
  }

  private fun calculateThroughputOverTime(samples: List<Pair<Long, Int>>): List<Pair<Long, Double>> {
    if (samples.size < 2) return emptyList()

    val windowSize = 2000L // 2 second windows
    val results = mutableListOf<Pair<Long, Double>>()

    var windowStart = samples.first().first
    var windowStartCompleted = samples.first().second

    for ((timestamp, completed) in samples) {
      if (timestamp - windowStart >= windowSize) {
        val chunksInWindow = completed - windowStartCompleted
        val actualWindowTime = timestamp - windowStart
        val throughput = chunksInWindow * 1000.0 / actualWindowTime
        results.add((windowStart - samples.first().first) to throughput)

        windowStart = timestamp
        windowStartCompleted = completed
      }
    }

    return results
  }
}
