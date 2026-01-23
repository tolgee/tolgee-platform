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
import io.tolgee.util.Logging
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.logger
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.PlatformTransactionManager

/**
 * Diagnostic test for the startup delay issue where batch jobs stay in PENDING
 * status for several seconds before processing begins.
 *
 * This test is disabled by default as it's not intended to run in the CI pipeline.
 * It's useful for diagnosing startup delays and optimizing batch job initialization.
 *
 * To run manually with visible output:
 * ./gradlew :server-app:test --tests "io.tolgee.batch.BatchJobStartupDelayTest" --info 2>&1 | grep -E "(INFO|DELAY|Phase|Status|Queue|Time|====)"
 */
@Disabled("Diagnostic test - not intended for CI pipeline, useful for performance optimization")
@SpringBootTest(
  properties = [
    "tolgee.cache.use-redis=true",
    "tolgee.cache.enabled=true",
    "tolgee.websocket.use-redis=true",
    "spring.redis.port=56379",
  ],
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
@ContextConfiguration(initializers = [BatchJobStartupDelayTest.Companion.Initializer::class])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ContextRecreatingTest
class BatchJobStartupDelayTest :
  AbstractSpringTest(),
  Logging {
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
  lateinit var batchJobStateProvider: BatchJobStateProvider

  @BeforeEach
  fun setup() {
    batchJobChunkExecutionQueue.clear()
    testData = BatchJobsTestData()
    testDataService.saveTestData(testData.root)
    batchJobConcurrentLauncher.pause = false
    logger.info("====== TEST SETUP COMPLETE ======")
  }

  @AfterEach
  fun teardown() {
    batchJobChunkExecutionQueue.clear()
    batchJobConcurrentLauncher.pause = true
  }

  @Test
  fun `diagnose startup delay with 100k chunks`() {
    val chunkCount = 100_000

    logger.info("======================================================")
    logger.info("STARTUP DELAY DIAGNOSIS TEST - $chunkCount chunks")
    logger.info("======================================================")

    val timestamps = mutableMapOf<String, Long>()
    timestamps["test_start"] = System.currentTimeMillis()

    // Phase 1: Create job
    logger.info("Phase 1: Creating job...")
    val job = runNoOpJob(chunkCount)
    timestamps["job_created"] = System.currentTimeMillis()
    logger.info(
      "Phase 1 COMPLETE: Job ${job.id} created in ${timestamps["job_created"]!! - timestamps["test_start"]!!}ms",
    )

    // Phase 2: Monitor queue population
    logger.info("Phase 2: Monitoring queue population...")
    var queuePopulatedTime: Long? = null
    val queueCheckStart = System.currentTimeMillis()
    while (System.currentTimeMillis() - queueCheckStart < 5000) {
      val queueSize = batchJobChunkExecutionQueue.size
      if (queueSize > 0 && queuePopulatedTime == null) {
        queuePopulatedTime = System.currentTimeMillis()
        logger.info(
          "Phase 2: First items in queue at ${queuePopulatedTime - timestamps["job_created"]!!}ms after job creation, queue size: $queueSize",
        )
      }
      if (queueSize >= chunkCount * 0.9) {
        logger.info(
          "Phase 2 COMPLETE: Queue ~90% populated ($queueSize items) at ${System.currentTimeMillis() - timestamps["job_created"]!!}ms",
        )
        break
      }
      Thread.sleep(10)
    }
    timestamps["queue_populated"] = System.currentTimeMillis()

    // Phase 3: Monitor job status transition and first chunk completion
    logger.info("Phase 3: Monitoring job status and chunk processing...")
    var firstRunningTime: Long? = null
    var firstCompletedTime: Long? = null
    var lastStatus = BatchJobStatus.PENDING
    var lastCompletedCount = 0

    val monitorStart = System.currentTimeMillis()
    while (System.currentTimeMillis() - monitorStart < 30000) {
      val jobDto =
        executeInNewTransaction(transactionManager) {
          batchJobService.getJobDto(job.id)
        }

      val executions = batchJobService.getExecutions(job.id)
      val completed = executions.count { it.status == BatchJobChunkExecutionStatus.SUCCESS }
      val running = executions.count { it.status == BatchJobChunkExecutionStatus.RUNNING }
      val pending = executions.count { it.status == BatchJobChunkExecutionStatus.PENDING }
      val queueSize = batchJobChunkExecutionQueue.size

      // Log status changes
      if (jobDto.status != lastStatus) {
        val elapsed = System.currentTimeMillis() - timestamps["job_created"]!!
        logger.info(
          "Phase 3: Status changed to ${jobDto.status} at ${elapsed}ms (completed=$completed, running=$running, pending=$pending, queue=$queueSize)",
        )
        if (jobDto.status == BatchJobStatus.RUNNING && firstRunningTime == null) {
          firstRunningTime = System.currentTimeMillis()
          timestamps["first_running"] = firstRunningTime
        }
        lastStatus = jobDto.status
      }

      // Log first completion
      if (completed > 0 && firstCompletedTime == null) {
        firstCompletedTime = System.currentTimeMillis()
        timestamps["first_completed"] = firstCompletedTime
        logger.info(
          "Phase 3: First chunk completed at ${firstCompletedTime - timestamps["job_created"]!!}ms after job creation",
        )
      }

      // Log progress every 100 completions
      if (completed - lastCompletedCount >= 500) {
        val elapsed = System.currentTimeMillis() - timestamps["job_created"]!!
        val throughput = completed * 1000.0 / (System.currentTimeMillis() - (firstCompletedTime ?: monitorStart))
        val tps = "%.1f".format(throughput)
        logger.info(
          "Phase 3: Progress - completed=$completed, running=$running, " +
            "pending=$pending, queue=$queueSize, throughput=$tps/s, elapsed=${elapsed}ms",
        )
        lastCompletedCount = completed
      }

      // Stop after enough data collected
      if (completed >= 5000 || jobDto.status.completed) {
        break
      }

      Thread.sleep(100)
    }

    // Final summary
    logger.info("======================================================")
    logger.info("STARTUP DELAY ANALYSIS RESULTS")
    logger.info("======================================================")
    logger.info("Total chunks: $chunkCount")
    logger.info("Job creation time: ${timestamps["job_created"]!! - timestamps["test_start"]!!}ms")
    if (queuePopulatedTime != null) {
      logger.info("Time to first queue item: ${queuePopulatedTime - timestamps["job_created"]!!}ms")
    }
    logger.info("Time to queue populated: ${timestamps["queue_populated"]!! - timestamps["job_created"]!!}ms")
    if (timestamps["first_running"] != null) {
      logger.info("Time to RUNNING status: ${timestamps["first_running"]!! - timestamps["job_created"]!!}ms")
    }
    if (timestamps["first_completed"] != null) {
      logger.info("Time to first completion: ${timestamps["first_completed"]!! - timestamps["job_created"]!!}ms")
    }
    logger.info("======================================================")

    // Cancel job to cleanup
    batchJobConcurrentLauncher.pause = true
    executeInNewTransaction(transactionManager) {
      val cancellationManager = applicationContext.getBean(BatchJobCancellationManager::class.java)
      cancellationManager.cancel(job.id)
    }
  }

  @Test
  fun `diagnose startup delay with 10k chunks - faster iteration`() {
    val chunkCount = 10_000

    logger.info("======================================================")
    logger.info("STARTUP DELAY DIAGNOSIS TEST (FAST) - $chunkCount chunks")
    logger.info("======================================================")
    logger.info("")
    logger.info("EXPECTED BOTTLENECK: BatchJobStateProvider.ensureInitialized()")
    logger.info("  - getInitialState() loads ALL $chunkCount rows from DB")
    logger.info("  - Then puts $chunkCount entries into Redis hash one by one")
    logger.info("")

    val timestamps = mutableMapOf<String, Long>()
    timestamps["test_start"] = System.currentTimeMillis()

    logger.info("Phase 1: Creating job...")
    val job = runNoOpJob(chunkCount)
    timestamps["job_created"] = System.currentTimeMillis()
    logger.info("Phase 1 COMPLETE: Job created in ${timestamps["job_created"]!! - timestamps["test_start"]!!}ms")

    // Rapid monitoring
    var firstQueueTime: Long? = null
    var firstRunningTime: Long? = null
    var firstCompletedTime: Long? = null
    var lastLogTime = System.currentTimeMillis()

    val monitorStart = System.currentTimeMillis()
    while (System.currentTimeMillis() - monitorStart < 15000) {
      val queueSize = batchJobChunkExecutionQueue.size
      val jobDto = batchJobService.findJobDto(job.id)

      if (queueSize > 0 && firstQueueTime == null) {
        firstQueueTime = System.currentTimeMillis()
        logger.info("DELAY MARKER: First queue item at ${firstQueueTime - timestamps["job_created"]!!}ms")
      }

      if (jobDto?.status == BatchJobStatus.RUNNING && firstRunningTime == null) {
        firstRunningTime = System.currentTimeMillis()
        logger.info("DELAY MARKER: Job RUNNING at ${firstRunningTime - timestamps["job_created"]!!}ms")
      }

      val executions = batchJobService.getExecutions(job.id)
      val completed = executions.count { it.status == BatchJobChunkExecutionStatus.SUCCESS }

      if (completed > 0 && firstCompletedTime == null) {
        firstCompletedTime = System.currentTimeMillis()
        logger.info("DELAY MARKER: First completion at ${firstCompletedTime - timestamps["job_created"]!!}ms")
      }

      // Log every second
      if (System.currentTimeMillis() - lastLogTime >= 1000) {
        val pending = executions.count { it.status == BatchJobChunkExecutionStatus.PENDING }
        val running = executions.count { it.status == BatchJobChunkExecutionStatus.RUNNING }
        logger.info(
          "Status: ${jobDto?.status} (queue=$queueSize, pending=$pending, running=$running, completed=$completed)",
        )
        lastLogTime = System.currentTimeMillis()
      }

      if (completed >= 2000 || jobDto?.status?.completed == true) {
        break
      }

      Thread.sleep(50)
    }

    logger.info("======================================================")
    logger.info("DELAY ANALYSIS SUMMARY")
    logger.info("======================================================")
    logger.info("Job creation: ${timestamps["job_created"]!! - timestamps["test_start"]!!}ms")
    logger.info("Time to first queue item: ${(firstQueueTime ?: 0) - timestamps["job_created"]!!}ms")
    logger.info("Time to RUNNING: ${(firstRunningTime ?: 0) - timestamps["job_created"]!!}ms")
    logger.info("Time to first completion: ${(firstCompletedTime ?: 0) - timestamps["job_created"]!!}ms")
    logger.info("======================================================")

    // Cleanup
    batchJobConcurrentLauncher.pause = true
    executeInNewTransaction(transactionManager) {
      val cancellationManager = applicationContext.getBean(BatchJobCancellationManager::class.java)
      cancellationManager.cancel(job.id)
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
}
