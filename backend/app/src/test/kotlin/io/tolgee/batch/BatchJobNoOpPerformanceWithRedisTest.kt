package io.tolgee.batch

import io.tolgee.AbstractSpringTest
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.data.ExecutionQueueItem
import io.tolgee.batch.request.NoOpRequest
import io.tolgee.batch.state.BatchJobStateProvider
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.development.testDataBuilder.data.BatchJobsTestData
import io.tolgee.fixtures.RedisRunner
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
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
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.PlatformTransactionManager

/**
 * Diagnostic performance test for batch job orchestration overhead WITH REDIS.
 *
 * This test is disabled by default as it's not intended to run in the CI pipeline.
 * It's useful for diagnosing performance issues and optimizing batch job processing.
 *
 * To run manually:
 * ./gradlew :server-app:test --tests "io.tolgee.batch.BatchJobNoOpPerformanceWithRedisTest"
 */
@Disabled("Diagnostic test - not intended for CI pipeline, useful for performance optimization")
@SpringBootTest(
  properties = [
    "tolgee.cache.use-redis=true",
    "tolgee.cache.enabled=true",
    "tolgee.websocket.use-redis=true",
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
        TestPropertyValues
          .of("spring.data.redis.port=${RedisRunner.port}")
          .applyTo(configurableApplicationContext)
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

  @Test
  fun `multi-project jobs - measures cross-project parallelization`() {
    val projectCount = 20
    val chunksPerProject = 200

    logger.info("Starting multi-project test: $projectCount projects, $chunksPerProject chunks each...")

    // Create additional projects
    val projects = mutableListOf<Pair<Project, UserAccount>>()
    executeInNewTransaction(transactionManager) {
      // Use the default project as the first one
      projects.add(testData.projectBuilder.self to testData.user)

      // Create additional projects using BaseTestData which accepts constructor params
      for (i in 2..projectCount) {
        val additionalTestData = BaseTestData("user_$i", "project_$i")
        testDataService.saveTestData(additionalTestData.root)
        projects.add(additionalTestData.projectBuilder.self to additionalTestData.user)
      }
    }

    val startTime = System.currentTimeMillis()

    // Start one job per project
    val jobs =
      projects.map { (project, user) ->
        executeInNewTransaction(transactionManager) {
          batchJobService.startJob(
            request =
              NoOpRequest().apply {
                itemIds = (1L..chunksPerProject).toList()
              },
            project = project,
            author = user,
            type = BatchJobType.NO_OP,
          )
        }
      }

    val jobCreationTime = System.currentTimeMillis()
    logger.info("${jobs.size} jobs created in ${jobCreationTime - startTime}ms, waiting for completion...")

    // Wait for all jobs to complete
    waitForAllJobsComplete(jobs)

    val endTime = System.currentTimeMillis()
    val totalChunks = projectCount * chunksPerProject
    val executionTime = endTime - jobCreationTime
    val chunksPerSecond = totalChunks * 1000.0 / executionTime

    logger.info("=".repeat(60))
    logger.info("MULTI-PROJECT PERFORMANCE RESULTS:")
    logger.info("  Projects: $projectCount")
    logger.info("  Chunks per project: $chunksPerProject")
    logger.info("  Total chunks: $totalChunks")
    logger.info("  Job creation time: ${jobCreationTime - startTime}ms")
    logger.info("  Execution time: ${executionTime}ms")
    logger.info("  Throughput: ${"%.2f".format(chunksPerSecond)} chunks/second")
    logger.info("=".repeat(60))

    // Verify all jobs completed successfully
    jobs.forEach { job ->
      val finalJob = batchJobService.getJobDto(job.id)
      finalJob.status.assert.isEqualTo(BatchJobStatus.SUCCESS)
    }
  }

  @Test
  fun `queue operations micro-benchmark`() {
    val queueSize = 30_000
    val projectCount = 50
    val jobsPerProject = 10
    val chunksPerJob = queueSize / (projectCount * jobsPerProject)

    logger.info("Queue micro-benchmark: $queueSize items, $projectCount projects, $jobsPerProject jobs/project")

    // Build items
    val items = mutableListOf<ExecutionQueueItem>()
    var chunkIdCounter = 1L
    var jobIdCounter = 1L
    for (p in 1..projectCount) {
      val projectId = p.toLong()
      for (j in 1..jobsPerProject) {
        val jobId = jobIdCounter++
        for (c in 1..chunksPerJob) {
          items.add(
            ExecutionQueueItem(
              chunkExecutionId = chunkIdCounter++,
              jobId = jobId,
              executeAfter = null,
              jobCharacter = JobCharacter.FAST,
              projectId = projectId,
            ),
          )
        }
      }
    }

    // Benchmark: addItemsToLocalQueue
    batchJobChunkExecutionQueue.clear()
    val addStart = System.nanoTime()
    batchJobChunkExecutionQueue.addItemsToLocalQueue(items)
    val addTime = (System.nanoTime() - addStart) / 1_000_000.0

    logger.info("addItemsToLocalQueue($queueSize items): ${"%.2f".format(addTime)}ms")
    batchJobChunkExecutionQueue.size.assert.isEqualTo(queueSize)

    // Benchmark: addItemsToLocalQueue with all duplicates (should be fast O(1) check per item)
    val addDupStart = System.nanoTime()
    batchJobChunkExecutionQueue.addItemsToLocalQueue(items)
    val addDupTime = (System.nanoTime() - addDupStart) / 1_000_000.0

    logger.info("addItemsToLocalQueue($queueSize duplicates): ${"%.2f".format(addDupTime)}ms")
    batchJobChunkExecutionQueue.size.assert.isEqualTo(queueSize)

    // Benchmark: pollRoundRobin without locked projects
    val pollCount = 1000
    val pollStart = System.nanoTime()
    val polled = (1..pollCount).mapNotNull { batchJobChunkExecutionQueue.pollRoundRobin() }
    val pollTime = (System.nanoTime() - pollStart) / 1_000_000.0

    logger.info(
      "pollRoundRobin() x $pollCount: ${"%.2f".format(pollTime)}ms (${"%.4f".format(pollTime / pollCount)}ms/poll)",
    )
    polled.size.assert.isEqualTo(pollCount)

    // Benchmark: pollRoundRobin with locked projects (half of projects locked)
    val lockedProjects = (1..projectCount / 2).map { it.toLong() }.toSet()
    val pollLockedStart = System.nanoTime()
    val polledLocked = (1..pollCount).mapNotNull { batchJobChunkExecutionQueue.pollRoundRobin(lockedProjects) }
    val pollLockedTime = (System.nanoTime() - pollLockedStart) / 1_000_000.0

    logger.info(
      "pollRoundRobin(${lockedProjects.size} locked) x $pollCount: ${"%.2f".format(pollLockedTime)}ms " +
        "(${"%.4f".format(pollLockedTime / pollCount)}ms/poll)",
    )
    // All polled items should be from unlocked projects
    polledLocked.forEach { item ->
      assert(item.projectId !in lockedProjects) {
        "Polled item from locked project ${item.projectId}"
      }
    }

    // Benchmark: removeJobExecutions
    val jobToRemove = 1L
    val removeStart = System.nanoTime()
    batchJobChunkExecutionQueue.removeJobExecutions(jobToRemove)
    val removeTime = (System.nanoTime() - removeStart) / 1_000_000.0

    logger.info("removeJobExecutions(jobId=$jobToRemove): ${"%.2f".format(removeTime)}ms")

    logger.info("=".repeat(60))
    logger.info("QUEUE MICRO-BENCHMARK RESULTS:")
    logger.info("  Queue size: $queueSize")
    logger.info("  Add all: ${"%.2f".format(addTime)}ms")
    logger.info("  Add duplicates: ${"%.2f".format(addDupTime)}ms")
    logger.info("  Poll x$pollCount: ${"%.2f".format(pollTime)}ms (avg ${"%.4f".format(pollTime / pollCount)}ms)")
    logger.info(
      "  Poll with locks x$pollCount: ${"%.2f".format(
        pollLockedTime,
      )}ms (avg ${"%.4f".format(pollLockedTime / pollCount)}ms)",
    )
    logger.info("  Remove job: ${"%.2f".format(removeTime)}ms")
    logger.info("=".repeat(60))
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

  private fun waitForAllJobsComplete(
    jobs: List<BatchJob>,
    timeoutMs: Long = 300_000,
  ) {
    val startTime = System.currentTimeMillis()
    var lastLogTime = startTime
    val remainingJobs = jobs.map { it.id }.toMutableSet()

    while (remainingJobs.isNotEmpty()) {
      val currentTime = System.currentTimeMillis()

      if (currentTime - startTime > timeoutMs) {
        val statuses =
          remainingJobs.map { id ->
            "$id: ${batchJobService.getJobDto(id).status}"
          }
        throw AssertionError(
          "${remainingJobs.size} jobs did not complete within ${timeoutMs}ms. Remaining: $statuses",
        )
      }

      executeInNewTransaction(transactionManager) {
        val completed =
          remainingJobs.filter { id ->
            batchJobService.getJobDto(id).status.completed
          }
        remainingJobs.removeAll(completed.toSet())
      }

      if (currentTime - lastLogTime > 5000) {
        logger.info(
          "Multi-project progress: ${jobs.size - remainingJobs.size}/${jobs.size} jobs completed, " +
            "queueSize=${batchJobChunkExecutionQueue.size}",
        )
        lastLogTime = currentTime
      }

      if (remainingJobs.isNotEmpty()) {
        Thread.sleep(100)
      }
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
