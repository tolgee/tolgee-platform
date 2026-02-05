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
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.service.translation.AutoTranslationService
import io.tolgee.testing.assert
import io.tolgee.util.Logging
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.logger
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.whenever
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.transaction.PlatformTransactionManager
import java.util.concurrent.atomic.AtomicInteger

/**
 * Base class for batch job concurrent execution tests.
 * Provides common setup, teardown, and helper methods.
 */
abstract class AbstractBatchJobConcurrentTest :
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
        TestPropertyValues
          .of("spring.data.redis.port=${RedisRunner.port}")
          .applyTo(configurableApplicationContext)
      }
    }
  }

  protected lateinit var testData: ConcurrentBatchJobsTestData

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

  @BeforeEach
  fun setup() {
    batchJobStateProvider.clearAllState()
    batchJobChunkExecutionQueue.clear()
    batchJobChunkExecutionQueue.populateQueue()

    resetAllMocks()

    testData = createTestData()
    testData.populateAllProjects()
    testDataService.saveTestData(testData.root)

    batchJobConcurrentLauncher.pause = false
  }

  @AfterEach
  fun teardown() {
    batchJobChunkExecutionQueue.clear()
    batchJobConcurrentLauncher.pause = true
  }

  protected open fun createTestData(): ConcurrentBatchJobsTestData {
    return ConcurrentBatchJobsTestData(
      projectAKeyCount = 100,
      projectBKeyCount = 80,
      projectCKeyCount = 50,
    )
  }

  protected fun resetAllMocks() {
    Mockito.reset(preTranslationByTmChunkProcessor)
    Mockito.reset(deleteKeysChunkProcessor)
    Mockito.reset(machineTranslationChunkProcessor)
    Mockito.reset(automationChunkProcessor)
    Mockito.reset(autoTranslationService)
  }

  // region Job Runners

  protected fun runPreTranslateJob(
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

  protected fun runMtJob(
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

  protected fun runDeleteKeysJob(
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

  protected fun runAutomationJob(project: Project): BatchJob =
    executeInNewTransaction(transactionManager) {
      batchJobService.startJob(
        request = AutomationBjRequest(1, 1, 1),
        project = project,
        author = testData.user,
        type = BatchJobType.AUTOMATION,
      )
    }

  protected fun runNoOpJob(
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

  // endregion

  // region Wait Methods

  protected fun waitForJobComplete(
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

  protected fun waitForJobCancelled(job: BatchJob) {
    waitFor(pollTime = 100, timeout = 10_000) {
      val jobDto =
        executeInNewTransaction(transactionManager) {
          batchJobService.getJobDto(job.id)
        }
      jobDto.status == BatchJobStatus.CANCELLED
    }
  }

  protected fun waitForJobCancelledOrCompleted(job: BatchJob) {
    waitFor(pollTime = 100, timeout = 10_000) {
      val jobDto =
        executeInNewTransaction(transactionManager) {
          batchJobService.getJobDto(job.id)
        }
      jobDto.status.completed
    }
  }

  protected fun waitForRunningJobsEmpty(timeoutMs: Long = 10_000) {
    waitFor(pollTime = 100, timeout = timeoutMs) {
      batchJobConcurrentLauncher.runningJobs.isEmpty()
    }
  }

  // endregion

  // region Assertions

  protected fun assertJobSuccess(job: BatchJob) {
    val jobDto = batchJobService.getJobDto(job.id)
    jobDto.status.assert.isEqualTo(BatchJobStatus.SUCCESS)
  }

  protected fun assertProgressCounterAccurate(job: BatchJob) {
    val jobView = batchJobService.getView(job.id)
    jobView.progress.assert.isEqualTo(jobView.batchJob.totalItems)
  }

  // endregion

  // region Processor Mocks - PreTranslate

  protected fun makePreTranslateProcessorPass() {
    doAnswer { }.whenever(preTranslationByTmChunkProcessor).process(any(), any(), any())
  }

  protected fun makePreTranslateProcessorPassWithDelay(delayMs: Long) {
    doAnswer {
      Thread.sleep(delayMs)
    }.whenever(preTranslationByTmChunkProcessor).process(any(), any(), any())
  }

  protected fun makePreTranslateProcessorPassWithCounter(counter: AtomicInteger) {
    doAnswer {
      counter.incrementAndGet()
    }.whenever(preTranslationByTmChunkProcessor).process(any(), any(), any())
  }

  // endregion

  // region Processor Mocks - DeleteKeys

  protected fun makeDeleteKeysProcessorPass() {
    doAnswer { }.whenever(deleteKeysChunkProcessor).process(any(), any(), any())
  }

  protected fun makeDeleteKeysProcessorPassWithDelay(delayMs: Long) {
    doAnswer {
      Thread.sleep(delayMs)
    }.whenever(deleteKeysChunkProcessor).process(any(), any(), any())
  }

  // endregion

  // region Processor Mocks - Machine Translation

  protected fun makeMtProcessorPass() {
    doAnswer { }.whenever(autoTranslationService).autoTranslateSync(any(), any(), any(), any(), any())
  }

  protected fun makeMtProcessorPassWithDelay(delayMs: Long) {
    doAnswer {
      Thread.sleep(delayMs)
    }.whenever(autoTranslationService).autoTranslateSync(any(), any(), any(), any(), any())
  }

  protected fun makeMtProcessorPassWithCounter(counter: AtomicInteger) {
    doAnswer {
      counter.incrementAndGet()
    }.whenever(autoTranslationService).autoTranslateSync(any(), any(), any(), any(), any())
  }

  protected fun makeMtProcessorPassWithConcurrencyTracking(
    currentConcurrency: AtomicInteger,
    maxConcurrency: AtomicInteger,
  ) {
    doAnswer {
      val current = currentConcurrency.incrementAndGet()
      maxConcurrency.updateAndGet { max -> maxOf(max, current) }
      try {
        Thread.sleep(50)
      } finally {
        currentConcurrency.decrementAndGet()
      }
    }.whenever(autoTranslationService).autoTranslateSync(any(), any(), any(), any(), any())
  }

  // endregion

  // region Processor Mocks - Automation

  protected fun makeAutomationProcessorPass() {
    doAnswer { }.whenever(automationChunkProcessor).process(any(), any(), any())
  }

  // endregion

  // region Redis State Management

  protected fun clearRedisBatchJobState(jobIds: List<Long>) {
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

  // endregion
}
