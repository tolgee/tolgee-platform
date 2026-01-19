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
 * Performance test for batch job orchestration overhead WITH REDIS.
 *
 * Run with:
 * ./gradlew :server-app:test --tests "io.tolgee.batch.BatchJobNoOpPerformanceWithRedisTest"
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
@ContextConfiguration(initializers = [BatchJobNoOpPerformanceWithRedisTest.Companion.Initializer::class])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ContextRecreatingTest
@Import(BatchJobNoOpPerformanceWithRedisTest.TimingConfiguration::class)
class BatchJobNoOpPerformanceWithRedisTest :
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
  fun `NO_OP job with lot of chunks - measure throughput WITH REDIS`() {
    val chunkCount = 5000

    logger.info("Starting NO_OP job with $chunkCount chunks (WITH REDIS)...")

    val startTime = System.currentTimeMillis()

    val job = runNoOpJob(chunkCount)

    val jobCreationTime = System.currentTimeMillis()
    logger.info("Job created in ${jobCreationTime - startTime}ms, waiting for completion...")

    // Wait for job to complete
    waitForJobComplete(job)

    val endTime = System.currentTimeMillis()
    val totalTime = endTime - startTime
    val executionTime = endTime - jobCreationTime
    val chunksPerSecond = chunkCount * 1000.0 / executionTime

    logger.info("=".repeat(60))
    logger.info("PERFORMANCE RESULTS (WITH REDIS):")
    logger.info("  Total chunks: $chunkCount")
    logger.info("  Job creation time: ${jobCreationTime - startTime}ms")
    logger.info("  Execution time: ${executionTime}ms")
    logger.info("  Total time: ${totalTime}ms")
    logger.info("  Throughput: ${"%.2f".format(chunksPerSecond)} chunks/second")
    logger.info("=".repeat(60))

    // Verify job completed successfully
    val finalJob = batchJobService.getJobDto(job.id)
    finalJob.status.assert.isEqualTo(BatchJobStatus.SUCCESS)
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

  private fun waitForJobComplete(
    job: BatchJob,
    timeoutMs: Long = 120_000,
  ) {
    val startTime = System.currentTimeMillis()
    var lastLogTime = startTime
    var allCompletedCount = 0

    while (true) {
      val currentTime = System.currentTimeMillis()

      if (currentTime - startTime > timeoutMs) {
        val finalStatus = batchJobService.getJobDto(job.id)
        throw AssertionError("Job did not complete within ${timeoutMs}ms. Status: ${finalStatus.status}")
      }

      val jobDto =
        executeInNewTransaction(transactionManager) {
          batchJobService.getJobDto(job.id)
        }

      if (jobDto.status.completed) {
        return
      }

      // Log progress every 5 seconds
      if (currentTime - lastLogTime > 5000) {
        val executions = batchJobService.getExecutions(job.id)
        val completed = executions.count { it.status.completed }
        val pending = executions.count { it.status == BatchJobChunkExecutionStatus.PENDING }
        val running = executions.count { it.status == BatchJobChunkExecutionStatus.RUNNING }
        val queueSize = batchJobChunkExecutionQueue.size

        logger.info("Progress: completed=$completed, running=$running, pending=$pending, queueSize=$queueSize")
        lastLogTime = currentTime

        // Fail fast if all chunks completed but job didn't transition
        if (completed == jobDto.totalChunks && running == 0 && pending == 0) {
          allCompletedCount++
          if (allCompletedCount >= 2) {
            throw AssertionError(
              "All ${jobDto.totalChunks} chunks completed but job status is still ${jobDto.status}. " +
                "Counter values - completedChunks: ${batchJobStateProvider.getCompletedChunksCount(job.id)}, " +
                "committedCount: ${batchJobStateProvider.getCommittedCount(job.id)}",
            )
          }
        } else {
          // Reset counter if condition is no longer met (non-consecutive detection)
          allCompletedCount = 0
        }
      }

      Thread.sleep(100)
    }
  }
}
