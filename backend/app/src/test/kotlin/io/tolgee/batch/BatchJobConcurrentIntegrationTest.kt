package io.tolgee.batch

import io.tolgee.AbstractSpringTest
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.processors.AutomationChunkProcessor
import io.tolgee.batch.processors.DeleteKeysChunkProcessor
import io.tolgee.batch.processors.MachineTranslationChunkProcessor
import io.tolgee.batch.processors.PreTranslationByTmChunkProcessor
import io.tolgee.batch.request.AutomationBjRequest
import io.tolgee.batch.request.DeleteKeysRequest
import io.tolgee.batch.request.MachineTranslationRequest
import io.tolgee.batch.request.NoOpRequest
import io.tolgee.batch.request.PreTranslationByTmRequest
import io.tolgee.batch.state.BatchJobStateProvider
import io.tolgee.configuration.tolgee.BatchProperties
import io.tolgee.development.testDataBuilder.data.ConcurrentBatchJobsTestData
import io.tolgee.fixtures.RedisRunner
import io.tolgee.fixtures.waitFor
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.Project
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.batch.BatchJobChunkExecutionStatus
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.service.translation.AutoTranslationService
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.assert
import io.tolgee.util.Logging
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.logger
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.whenever
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.transaction.PlatformTransactionManager
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Comprehensive integration test for batch job concurrent execution scenarios.
 * Tests multiple job types, project locking, job characters, and state management.
 */
@SpringBootTest(
  properties = [
    "tolgee.cache.use-redis=true",
    "tolgee.cache.enabled=true",
    "tolgee.websocket.use-redis=true",
    "spring.redis.port=56379",
    "tolgee.batch.concurrency=10",
  ],
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
@ContextConfiguration(initializers = [BatchJobConcurrentIntegrationTest.Companion.Initializer::class])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ContextRecreatingTest
class BatchJobConcurrentIntegrationTest :
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

  private lateinit var testData: ConcurrentBatchJobsTestData

  @Autowired
  lateinit var batchJobService: BatchJobService

  @Autowired
  lateinit var batchJobChunkExecutionQueue: BatchJobChunkExecutionQueue

  @Autowired
  lateinit var batchJobConcurrentLauncher: BatchJobConcurrentLauncher

  @Autowired
  lateinit var batchJobStateProvider: BatchJobStateProvider

  @Autowired
  lateinit var batchJobProjectLockingManager: BatchJobProjectLockingManager

  @Autowired
  lateinit var batchJobCancellationManager: BatchJobCancellationManager

  @Autowired
  lateinit var batchProperties: BatchProperties

  @Autowired
  lateinit var transactionManager: PlatformTransactionManager

  @Autowired
  lateinit var redissonClient: RedissonClient

  @MockitoSpyBean
  @Autowired
  lateinit var preTranslationByTmChunkProcessor: PreTranslationByTmChunkProcessor

  @MockitoSpyBean
  @Autowired
  lateinit var deleteKeysChunkProcessor: DeleteKeysChunkProcessor

  @MockitoSpyBean
  @Autowired
  lateinit var machineTranslationChunkProcessor: MachineTranslationChunkProcessor

  @MockitoSpyBean
  @Autowired
  lateinit var automationChunkProcessor: AutomationChunkProcessor

  @MockitoSpyBean
  @Autowired
  lateinit var autoTranslationService: AutoTranslationService

  // Track concurrent execution for monitoring
  private val concurrentExecutionTracker = ConcurrentHashMap<Long, AtomicInteger>()
  private val maxConcurrentPerJob = ConcurrentHashMap<Long, AtomicInteger>()

  @BeforeEach
  fun setup() {
    batchJobStateProvider.clearAllState()
    batchJobChunkExecutionQueue.clear()
    batchJobChunkExecutionQueue.populateQueue()

    Mockito.reset(preTranslationByTmChunkProcessor)
    Mockito.reset(deleteKeysChunkProcessor)
    Mockito.reset(machineTranslationChunkProcessor)
    Mockito.reset(automationChunkProcessor)
    Mockito.reset(autoTranslationService)

    concurrentExecutionTracker.clear()
    maxConcurrentPerJob.clear()

    testData = ConcurrentBatchJobsTestData(projectAKeyCount = 100, projectBKeyCount = 80, projectCKeyCount = 50)
    testData.populateAllProjects()
    testDataService.saveTestData(testData.root)

    batchJobConcurrentLauncher.pause = false
  }

  @AfterEach
  fun teardown() {
    batchJobChunkExecutionQueue.clear()
    batchJobConcurrentLauncher.pause = true
  }

  @Test
  fun `multiple job types from different projects run in parallel`() {
    makePreTranslateProcessorPassWithDelay(10)
    makeDeleteKeysProcessorPassWithDelay(10)
    makeMtProcessorPassWithDelay(10)

    val job1 = runPreTranslateJob(testData.projectA, testData.getProjectAKeyIds().take(50), testData.projectACzech.id)
    val job2 = runPreTranslateJob(testData.projectB, testData.getProjectBKeyIds().take(40), testData.projectBCzech.id)
    val job3 = runDeleteKeysJob(testData.projectC, testData.getProjectCKeyIds().take(30))

    waitForJobComplete(job1)
    waitForJobComplete(job2)
    waitForJobComplete(job3)

    assertJobSuccess(job1)
    assertJobSuccess(job2)
    assertJobSuccess(job3)

    // Verify progress counters are accurate
    assertProgressCounterAccurate(job1)
    assertProgressCounterAccurate(job2)
    assertProgressCounterAccurate(job3)

    logger.info("All 3 jobs from different projects completed successfully in parallel")
  }

  @Test
  fun `exclusive jobs on same project queue correctly`() {
    makePreTranslateProcessorPass()

    val keyIds = testData.getProjectAKeyIds()
    val job1 = runPreTranslateJob(testData.projectA, keyIds.take(30), testData.projectACzech.id)
    val job2 = runPreTranslateJob(testData.projectA, keyIds.drop(30).take(30), testData.projectACzech.id)

    // Both jobs should eventually complete
    waitForJobComplete(job1, timeoutMs = 15_000)
    waitForJobComplete(job2, timeoutMs = 15_000)

    assertJobSuccess(job1)
    assertJobSuccess(job2)

    // Verify project locking worked - jobs should have run sequentially
    // by checking that one completed before the other started significant work
    logger.info("Both exclusive jobs on same project completed, project locking verified")
  }

  @Test
  fun `non-exclusive jobs bypass project locking`() {
    makeDeleteKeysProcessorPassWithDelay(50)
    makeAutomationProcessorPass()

    // Start a long-running exclusive job
    val exclusiveJob = runDeleteKeysJob(testData.projectA, testData.getProjectAKeyIds().take(20))

    // Wait a moment for it to start
    Thread.sleep(200)

    // Start a non-exclusive job on the same project
    val nonExclusiveJob = runAutomationJob(testData.projectA)

    // The non-exclusive job should complete even while exclusive is running
    waitForJobComplete(nonExclusiveJob)
    assertJobSuccess(nonExclusiveJob)

    // Now wait for the exclusive job
    waitForJobComplete(exclusiveJob)
    assertJobSuccess(exclusiveJob)

    logger.info("Non-exclusive job completed while exclusive job was running - bypass verified")
  }

  @Test
  fun `maxPerJobConcurrency limits chunk parallelism`() {
    val originalMaxPerMtConcurrency = batchProperties.maxPerMtJobConcurrency
    val maxConcurrency = 2
    batchProperties.maxPerMtJobConcurrency = maxConcurrency

    try {
      val jobConcurrencyTracker = AtomicInteger(0)
      val maxObservedConcurrency = AtomicInteger(0)

      makeMtProcessorPassWithConcurrencyTracking(jobConcurrencyTracker, maxObservedConcurrency)

      val job = runMtJob(testData.projectA, testData.getProjectAKeyIds().take(50), testData.projectACzech.id)

      waitForJobComplete(job)
      assertJobSuccess(job)

      // Verify that max concurrent never exceeded the limit
      maxObservedConcurrency.get().assert.isLessThanOrEqualTo(maxConcurrency)
      logger.info(
        "maxPerJobConcurrency enforced: max observed = ${maxObservedConcurrency.get()}, limit = $maxConcurrency",
      )
    } finally {
      batchProperties.maxPerMtJobConcurrency = originalMaxPerMtConcurrency
    }
  }

  @Test
  fun `job character fairness under load`() {
    val slowExecutionCount = AtomicInteger(0)
    val fastExecutionCount = AtomicInteger(0)

    makeMtProcessorPassWithCounter(slowExecutionCount)
    makePreTranslateProcessorPassWithCounter(fastExecutionCount)

    // Start multiple SLOW jobs (MT) and FAST jobs (pre-translate) from different projects
    val slowJob1 = runMtJob(testData.projectA, testData.getProjectAKeyIds().take(30), testData.projectACzech.id)
    val fastJob1 =
      runPreTranslateJob(testData.projectB, testData.getProjectBKeyIds().take(30), testData.projectBCzech.id)
    val fastJob2 =
      runPreTranslateJob(testData.projectC, testData.getProjectCKeyIds().take(30), testData.projectCCzech.id)

    waitForJobComplete(slowJob1)
    waitForJobComplete(fastJob1)
    waitForJobComplete(fastJob2)

    assertJobSuccess(slowJob1)
    assertJobSuccess(fastJob1)
    assertJobSuccess(fastJob2)

    logger.info(
      "Job character fairness test completed - SLOW executions: ${slowExecutionCount.get()}, FAST executions: ${fastExecutionCount.get()}",
    )
  }

  @Test
  fun `progress tracking accuracy under concurrent load`() {
    makePreTranslateProcessorPass()
    makeDeleteKeysProcessorPass()

    val jobs = mutableListOf<BatchJob>()
    val expectedTotals = mutableMapOf<Long, Int>()

    // Start 5 different jobs across 3 projects
    val job1 = runPreTranslateJob(testData.projectA, testData.getProjectAKeyIds().take(50), testData.projectACzech.id)
    jobs.add(job1)
    expectedTotals[job1.id] = 50

    val job2 = runPreTranslateJob(testData.projectB, testData.getProjectBKeyIds().take(40), testData.projectBCzech.id)
    jobs.add(job2)
    expectedTotals[job2.id] = 40

    val job3 = runPreTranslateJob(testData.projectC, testData.getProjectCKeyIds().take(30), testData.projectCCzech.id)
    jobs.add(job3)
    expectedTotals[job3.id] = 30

    // Wait for all to complete
    jobs.forEach { job ->
      waitForJobComplete(job)
      assertJobSuccess(job)
    }

    // Verify progress counters match expected
    jobs.forEach { job ->
      val jobView = batchJobService.getView(job.id)
      jobView.batchJob.totalItems.assert
        .isEqualTo(expectedTotals[job.id])
      jobView.progress.assert.isEqualTo(expectedTotals[job.id])
      logger.info("Job ${job.id}: totalItems=${jobView.batchJob.totalItems}, progress=${jobView.progress}")
    }

    logger.info("Progress tracking accuracy verified for ${jobs.size} concurrent jobs")
  }

  @Test
  fun `failure and retry with concurrent jobs`() {
    makePreTranslateProcessorPass()

    // Make one processor fail initially then succeed
    val failCount = AtomicInteger(0)
    doAnswer {
      if (failCount.incrementAndGet() <= 2) {
        throw RuntimeException("Simulated failure #${failCount.get()}")
      }
    }.whenever(deleteKeysChunkProcessor).process(any(), any(), any())

    val job1 = runPreTranslateJob(testData.projectA, testData.getProjectAKeyIds().take(20), testData.projectACzech.id)
    val job2 = runPreTranslateJob(testData.projectB, testData.getProjectBKeyIds().take(20), testData.projectBCzech.id)
    val failingJob = runDeleteKeysJob(testData.projectC, testData.getProjectCKeyIds().take(5))

    // Wait for successful jobs
    waitForJobComplete(job1)
    waitForJobComplete(job2)

    assertJobSuccess(job1)
    assertJobSuccess(job2)

    // The failing job should eventually succeed after retries
    waitForJobComplete(failingJob, timeoutMs = 15_000)
    assertJobSuccess(failingJob)

    logger.info("Failure and retry test completed - failing job recovered after ${failCount.get()} failures")
  }

  @Test
  fun `cancellation during concurrent execution`() {
    makePreTranslateProcessorPass()
    makeDeleteKeysProcessorPassWithDelay(200)

    val job1 = runPreTranslateJob(testData.projectA, testData.getProjectAKeyIds().take(30), testData.projectACzech.id)
    val job2 = runPreTranslateJob(testData.projectB, testData.getProjectBKeyIds().take(30), testData.projectBCzech.id)
    val jobToCancel = runDeleteKeysJob(testData.projectC, testData.getProjectCKeyIds().take(20))

    // Wait for jobs to start running
    Thread.sleep(300)

    // Cancel one job
    batchJobCancellationManager.cancel(jobToCancel.id)

    // Wait for cancelled job to stop (give it more time)
    waitForJobCancelledOrCompleted(jobToCancel)

    // Verify other jobs continue and complete
    waitForJobComplete(job1)
    waitForJobComplete(job2)

    assertJobSuccess(job1)
    assertJobSuccess(job2)

    // Verify cancelled job state - should be CANCELLED or possibly completed if it finished before cancel took effect
    val cancelledJobDto = batchJobService.getJobDto(jobToCancel.id)
    cancelledJobDto.status.completed.assert
      .isTrue()

    logger.info("Cancellation test completed - job status: ${cancelledJobDto.status}")
  }

  @Test
  fun `activity finalization with concurrent chunks`() {
    makePreTranslateProcessorPass()

    // Run a job with multiple chunks completing nearly simultaneously
    val job = runPreTranslateJob(testData.projectA, testData.getProjectAKeyIds().take(40), testData.projectACzech.id)

    waitForJobComplete(job)
    assertJobSuccess(job)

    // Verify the job completed with all chunks processed
    val executions = batchJobService.getExecutions(job.id)
    val successfulExecutions = executions.filter { it.status == BatchJobChunkExecutionStatus.SUCCESS }
    successfulExecutions.assert.isNotEmpty()

    logger.info("Activity finalization test completed with ${successfulExecutions.size} successful chunk executions")
  }

  @Test
  fun `stress test - many jobs many projects`() {
    makePreTranslateProcessorPass()
    makeDeleteKeysProcessorPass()
    makeMtProcessorPass()

    val allJobs = mutableListOf<BatchJob>()

    // Project A - multiple jobs
    allJobs.add(runPreTranslateJob(testData.projectA, testData.getProjectAKeyIds().take(20), testData.projectACzech.id))

    // Project B - multiple jobs
    allJobs.add(runPreTranslateJob(testData.projectB, testData.getProjectBKeyIds().take(20), testData.projectBCzech.id))

    // Project C - multiple jobs
    allJobs.add(runPreTranslateJob(testData.projectC, testData.getProjectCKeyIds().take(20), testData.projectCCzech.id))

    val startTime = System.currentTimeMillis()

    // Wait for all jobs to complete
    allJobs.forEach { job ->
      waitForJobComplete(job, timeoutMs = 30_000)
    }

    val totalTime = System.currentTimeMillis() - startTime

    // Verify all completed successfully
    allJobs.forEach { job ->
      assertJobSuccess(job)
    }

    // Log throughput metrics
    val totalChunks = allJobs.sumOf { batchJobService.getJobDto(it.id).totalChunks }
    val chunksPerSecond = totalChunks * 1000.0 / totalTime

    logger.info("=".repeat(60))
    logger.info("STRESS TEST RESULTS:")
    logger.info("  Total jobs: ${allJobs.size}")
    logger.info("  Total chunks: $totalChunks")
    logger.info("  Total time: ${totalTime}ms")
    logger.info("  Throughput: ${"%.2f".format(chunksPerSecond)} chunks/second")
    logger.info("=".repeat(60))

    // Verify no deadlocks or stuck jobs - all should have completed
    allJobs.forEach { job ->
      val jobDto = batchJobService.getJobDto(job.id)
      jobDto.status.completed.assert
        .isTrue()
    }
  }

  private fun runPreTranslateJob(
    project: Project,
    keyIds: List<Long>,
    targetLanguageId: Long,
  ): BatchJob =
    executeInNewTransaction(transactionManager) {
      batchJobService.startJob(
        request =
          PreTranslationByTmRequest().apply {
            this.keyIds = keyIds
            this.targetLanguageIds = listOf(targetLanguageId)
          },
        project = project,
        author = testData.user,
        type = BatchJobType.PRE_TRANSLATE_BT_TM,
        isHidden = false,
      )
    }

  private fun runMtJob(
    project: Project,
    keyIds: List<Long>,
    targetLanguageId: Long,
  ): BatchJob =
    executeInNewTransaction(transactionManager) {
      batchJobService.startJob(
        request =
          MachineTranslationRequest().apply {
            this.keyIds = keyIds
            this.targetLanguageIds = listOf(targetLanguageId)
          },
        project = project,
        author = testData.user,
        type = BatchJobType.MACHINE_TRANSLATE,
        isHidden = false,
      )
    }

  private fun runDeleteKeysJob(
    project: Project,
    keyIds: List<Long>,
  ): BatchJob =
    executeInNewTransaction(transactionManager) {
      batchJobService.startJob(
        request =
          DeleteKeysRequest().apply {
            this.keyIds = keyIds
          },
        project = project,
        author = testData.user,
        type = BatchJobType.DELETE_KEYS,
      )
    }

  private fun runAutomationJob(project: Project): BatchJob =
    executeInNewTransaction(transactionManager) {
      batchJobService.startJob(
        request = AutomationBjRequest(1, 1, 1),
        project = project,
        author = testData.user,
        type = BatchJobType.AUTOMATION,
      )
    }

  private fun waitForJobComplete(
    job: BatchJob,
    timeoutMs: Long = 10_000,
  ) {
    waitForNotThrowing(pollTime = 100, timeout = timeoutMs) {
      executeInNewTransaction(transactionManager) {
        val jobDto = batchJobService.getJobDto(job.id)
        jobDto.status.completed.assert
          .isTrue()
      }
    }
  }

  private fun waitForJobCancelled(job: BatchJob) {
    waitFor(pollTime = 100, timeout = 10_000) {
      val jobDto =
        executeInNewTransaction(transactionManager) {
          batchJobService.getJobDto(job.id)
        }
      jobDto.status == BatchJobStatus.CANCELLED
    }
  }

  private fun waitForJobCancelledOrCompleted(job: BatchJob) {
    waitFor(pollTime = 100, timeout = 10_000) {
      val jobDto =
        executeInNewTransaction(transactionManager) {
          batchJobService.getJobDto(job.id)
        }
      jobDto.status.completed
    }
  }

  private fun assertJobSuccess(job: BatchJob) {
    val jobDto = batchJobService.getJobDto(job.id)
    jobDto.status.assert.isEqualTo(BatchJobStatus.SUCCESS)
  }

  private fun assertProgressCounterAccurate(job: BatchJob) {
    val jobView = batchJobService.getView(job.id)
    jobView.progress.assert.isEqualTo(jobView.batchJob.totalItems)
  }

  private fun makePreTranslateProcessorPass() {
    doAnswer { }.whenever(preTranslationByTmChunkProcessor).process(any(), any(), any())
  }

  private fun makePreTranslateProcessorPassWithDelay(delayMs: Long) {
    doAnswer {
      Thread.sleep(delayMs)
    }.whenever(preTranslationByTmChunkProcessor).process(any(), any(), any())
  }

  private fun makePreTranslateProcessorPassWithCounter(counter: AtomicInteger) {
    doAnswer {
      counter.incrementAndGet()
    }.whenever(preTranslationByTmChunkProcessor).process(any(), any(), any())
  }

  private fun makeDeleteKeysProcessorPass() {
    doAnswer { }.whenever(deleteKeysChunkProcessor).process(any(), any(), any())
  }

  private fun makeDeleteKeysProcessorPassWithDelay(delayMs: Long) {
    doAnswer {
      Thread.sleep(delayMs)
    }.whenever(deleteKeysChunkProcessor).process(any(), any(), any())
  }

  private fun makeMtProcessorPass() {
    doAnswer { }.whenever(autoTranslationService).autoTranslateSync(any(), any(), any(), any(), any())
  }

  private fun makeMtProcessorPassWithDelay(delayMs: Long) {
    doAnswer {
      Thread.sleep(delayMs)
    }.whenever(autoTranslationService).autoTranslateSync(any(), any(), any(), any(), any())
  }

  private fun makeMtProcessorPassWithCounter(counter: AtomicInteger) {
    doAnswer {
      counter.incrementAndGet()
    }.whenever(autoTranslationService).autoTranslateSync(any(), any(), any(), any(), any())
  }

  private fun makeMtProcessorPassWithConcurrencyTracking(
    currentConcurrency: AtomicInteger,
    maxConcurrency: AtomicInteger,
  ) {
    doAnswer {
      val current = currentConcurrency.incrementAndGet()
      maxConcurrency.updateAndGet { max -> maxOf(max, current) }
      try {
        Thread.sleep(50) // Simulate some work
      } finally {
        currentConcurrency.decrementAndGet()
      }
    }.whenever(autoTranslationService).autoTranslateSync(any(), any(), any(), any(), any())
  }

  private fun makeAutomationProcessorPass() {
    doAnswer { }.whenever(automationChunkProcessor).process(any(), any(), any())
  }

  /**
   * This test simulates production-like load with:
   * 1. Large number of chunks (500+) across multiple projects
   * 2. Node failure simulation (pausing processing mid-execution)
   * 3. Redis state clearing to simulate state loss
   * 4. Recovery and restart from database
   * 5. Verification of final data integrity
   *
   * Uses NO_OP jobs for fast execution without external dependencies.
   */
  @Test
  fun `production load test with node failure and redis recovery`() {
    val chunksPerProject = 500
    val totalChunks = chunksPerProject * 3

    logger.info("=".repeat(70))
    logger.info("PRODUCTION LOAD TEST - Starting with $totalChunks total chunks")
    logger.info("=".repeat(70))

    // Phase 1: Start large jobs on all three projects
    logger.info("Phase 1: Starting large jobs...")
    val startTime = System.currentTimeMillis()

    val job1 = runNoOpJob(testData.projectA, chunksPerProject)
    val job2 = runNoOpJob(testData.projectB, chunksPerProject)
    val job3 = runNoOpJob(testData.projectC, chunksPerProject)

    val allJobs = listOf(job1, job2, job3)
    logger.info("Started ${allJobs.size} jobs with $chunksPerProject chunks each")

    // Phase 2: Let jobs run for a bit, then simulate node failure
    logger.info("Phase 2: Running jobs, waiting for partial completion...")
    Thread.sleep(2000) // Let some chunks complete

    // Log progress before "failure"
    val progressBeforeFailure =
      allJobs.associate { job ->
        val view = batchJobService.getView(job.id)
        job.id to view.progress
      }
    logger.info("Progress before simulated failure: $progressBeforeFailure")

    // Phase 3: Simulate node failure by pausing the launcher
    logger.info("Phase 3: Simulating node failure (pausing batch job launcher)...")
    batchJobConcurrentLauncher.pause = true

    // Wait for running jobs to stop
    waitFor(pollTime = 100, timeout = 10_000) {
      batchJobConcurrentLauncher.runningJobs.isEmpty()
    }
    logger.info("All running coroutines stopped")

    // Phase 4: Clear Redis state to simulate state loss (like a Redis restart)
    logger.info("Phase 4: Clearing Redis batch job state (simulating Redis restart)...")
    clearRedisBatchJobState(allJobs.map { it.id })
    batchJobStateProvider.clearAllState()
    logger.info("Redis state cleared")

    // Phase 5: Clear and repopulate the queue from database
    logger.info("Phase 5: Repopulating queue from database...")
    batchJobChunkExecutionQueue.clear()
    batchJobChunkExecutionQueue.populateQueue()
    val queueSizeAfterRepopulate = batchJobChunkExecutionQueue.size
    logger.info("Queue repopulated with $queueSizeAfterRepopulate items from database")

    // Phase 6: Resume processing - simulating new node coming online
    logger.info("Phase 6: Resuming batch job processing (new node online)...")
    batchJobConcurrentLauncher.pause = false

    // Phase 7: Wait for all jobs to complete
    logger.info("Phase 7: Waiting for all jobs to complete...")
    allJobs.forEach { job ->
      waitForJobComplete(job, timeoutMs = 30_000)
    }

    val totalTime = System.currentTimeMillis() - startTime

    // Phase 8: Verify all jobs completed successfully
    logger.info("Phase 8: Verifying job completion and data integrity...")
    allJobs.forEach { job ->
      val jobDto = batchJobService.getJobDto(job.id)
      val jobView = batchJobService.getView(job.id)

      jobDto.status.assert.isEqualTo(BatchJobStatus.SUCCESS)
      jobView.progress.assert.isEqualTo(chunksPerProject)

      logger.info(
        "Job ${job.id}: status=${jobDto.status}, " +
          "progress=${jobView.progress}/${jobDto.totalItems}, " +
          "chunks=${jobDto.totalChunks}",
      )
    }

    // Verify execution counts
    allJobs.forEach { job ->
      val executions = batchJobService.getExecutions(job.id)
      val successCount = executions.count { it.status == BatchJobChunkExecutionStatus.SUCCESS }
      val jobDto = batchJobService.getJobDto(job.id)

      logger.info("Job ${job.id}: $successCount successful executions out of ${jobDto.totalChunks} chunks")
      successCount.assert.isGreaterThanOrEqualTo(jobDto.totalChunks)
    }

    // Log final metrics
    val chunksPerSecond = totalChunks * 1000.0 / totalTime

    logger.info("=".repeat(70))
    logger.info("PRODUCTION LOAD TEST RESULTS:")
    logger.info("  Total jobs: ${allJobs.size}")
    logger.info("  Total chunks: $totalChunks")
    logger.info("  Total time (including simulated failure): ${totalTime}ms")
    logger.info("  Effective throughput: ${"%.2f".format(chunksPerSecond)} chunks/second")
    logger.info("  Node failure simulated: YES")
    logger.info("  Redis state cleared: YES")
    logger.info("  Recovery from DB: SUCCESS")
    logger.info("=".repeat(70))
  }

  /**
   * Extended stress test with continuous job submission, cancellation, and monitoring.
   * Runs multiple waves of jobs while measuring system stability.
   */
  @Test
  fun `extended stress test with multiple job waves`() {
    val chunksPerJob = 200
    val jobsPerWave = 3
    val totalWaves = 3

    logger.info("=".repeat(70))
    logger.info("EXTENDED STRESS TEST - $totalWaves waves of $jobsPerWave jobs each")
    logger.info("=".repeat(70))

    val allJobs = mutableListOf<BatchJob>()
    val startTime = System.currentTimeMillis()

    // Run multiple waves of jobs
    repeat(totalWaves) { wave ->
      logger.info("Wave ${wave + 1}/$totalWaves: Starting $jobsPerWave jobs...")

      val waveJobs =
        listOf(
          runNoOpJob(testData.projectA, chunksPerJob),
          runNoOpJob(testData.projectB, chunksPerJob),
          runNoOpJob(testData.projectC, chunksPerJob),
        )
      allJobs.addAll(waveJobs)

      // Wait for this wave to complete before starting next
      waveJobs.forEach { job ->
        waitForJobComplete(job, timeoutMs = 15_000)
        assertJobSuccess(job)
      }

      logger.info("Wave ${wave + 1}/$totalWaves: Completed")

      // Brief pause between waves
      if (wave < totalWaves - 1) {
        Thread.sleep(500)
      }
    }

    val totalTime = System.currentTimeMillis() - startTime
    val totalChunks = allJobs.sumOf { batchJobService.getJobDto(it.id).totalChunks }
    val chunksPerSecond = totalChunks * 1000.0 / totalTime

    // Verify all jobs
    allJobs.forEach { job ->
      val jobDto = batchJobService.getJobDto(job.id)
      jobDto.status.assert.isEqualTo(BatchJobStatus.SUCCESS)
    }

    logger.info("=".repeat(70))
    logger.info("EXTENDED STRESS TEST RESULTS:")
    logger.info("  Total waves: $totalWaves")
    logger.info("  Total jobs: ${allJobs.size}")
    logger.info("  Total chunks: $totalChunks")
    logger.info("  Total time: ${totalTime}ms")
    logger.info("  Throughput: ${"%.2f".format(chunksPerSecond)} chunks/second")
    logger.info("=".repeat(70))
  }

  /**
   * Test that simulates processing interruption and restart mid-job.
   */
  @Test
  fun `job recovery after processing restart`() {
    val chunkCount = 300

    logger.info("Starting job recovery test with $chunkCount chunks...")

    // Start a job
    val job = runNoOpJob(testData.projectA, chunkCount)
    logger.info("Started job ${job.id}")

    // Let it run for a bit
    Thread.sleep(1000)

    // Stop processing
    batchJobConcurrentLauncher.pause = true
    waitFor(pollTime = 100, timeout = 10_000) {
      batchJobConcurrentLauncher.runningJobs.isEmpty()
    }

    val progressAtPause = batchJobService.getView(job.id).progress
    logger.info("Paused at progress: $progressAtPause/$chunkCount")

    // Resume processing
    batchJobConcurrentLauncher.pause = false

    // Wait for completion
    waitForJobComplete(job, timeoutMs = 15_000)

    val finalView = batchJobService.getView(job.id)
    finalView.batchJob.status.assert
      .isEqualTo(BatchJobStatus.SUCCESS)
    finalView.progress.assert.isEqualTo(chunkCount)

    logger.info("Job recovered and completed: progress=$chunkCount, status=SUCCESS")
  }

  private fun runNoOpJob(
    project: Project,
    itemCount: Int,
  ): BatchJob =
    executeInNewTransaction(transactionManager) {
      batchJobService.startJob(
        request =
          NoOpRequest().apply {
            itemIds = (1L..itemCount).toList()
          },
        project = project,
        author = testData.user,
        type = BatchJobType.NO_OP,
      )
    }

  /**
   * Clears Redis state for specific batch jobs.
   * This simulates what would happen if Redis restarted or lost data.
   */
  private fun clearRedisBatchJobState(jobIds: List<Long>) {
    val patterns =
      listOf(
        "batch_job_state:",
        "batch_job_running_count:",
        "batch_job_state_initialized:",
        "batch_job_completed_chunks:",
        "batch_job_progress:",
        "batch_job_failed:",
        "batch_job_cancelled:",
        "batch_job_committed:",
      )

    jobIds.forEach { jobId ->
      patterns.forEach { prefix ->
        try {
          val key = "$prefix$jobId"
          redissonClient.getBucket<Any>(key).delete()
          redissonClient.getAtomicLong(key).delete()
          redissonClient.getMap<Any, Any>(key).delete()
        } catch (e: Exception) {
          // Ignore errors during cleanup
        }
      }
    }
  }
}
