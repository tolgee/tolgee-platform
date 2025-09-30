package io.tolgee.batch

import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.processors.AutomationChunkProcessor
import io.tolgee.batch.processors.DeleteKeysChunkProcessor
import io.tolgee.batch.processors.PreTranslationByTmChunkProcessor
import io.tolgee.batch.request.AutomationBjRequest
import io.tolgee.batch.request.DeleteKeysRequest
import io.tolgee.batch.request.PreTranslationByTmRequest
import io.tolgee.batch.state.BatchJobStateProvider
import io.tolgee.component.CurrentDateProvider
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.BatchJobsTestData
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.OutOfCreditsException
import io.tolgee.fixtures.waitFor
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.UserAccount
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.batch.BatchJobChunkExecution
import io.tolgee.model.batch.BatchJobChunkExecutionStatus
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.security.authentication.JwtService
import io.tolgee.testing.assert
import io.tolgee.util.Logging
import io.tolgee.util.addMinutes
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.logger
import io.tolgee.websocket.WebsocketTestHelper
import jakarta.persistence.EntityManager
import kotlinx.coroutines.ensureActive
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationContext
import org.springframework.transaction.PlatformTransactionManager
import java.time.Duration
import java.util.*
import kotlin.coroutines.CoroutineContext

class BatchJobTestUtil(
  private val applicationContext: ApplicationContext,
  private val testData: BatchJobsTestData,
) : Logging {
  lateinit var websocketHelper: WebsocketTestHelper

  fun assertPreTranslationProcessExecutedTimes(times: Int) {
    waitForNotThrowing(pollTime = 1000) {
      verify(
        preTranslationByTmChunkProcessor,
        times(times),
      ).process(any(), any(), any(), any())
    }
  }

  fun waitForCompleted(batchJob: BatchJob): BatchJobDto {
    try {
      waitForNotThrowing(pollTime = 1000) {
        executeInNewTransaction(transactionManager) {
          val finishedJob = batchJobService.getJobDto(batchJob.id)
          logger.debug("Job status: ${finishedJob.status.name}")
          finishedJob.status.completed.assert.isTrue()
        }
      }
    } catch (e: Exception) {
      val storedStatus = batchJobService.findJobEntity(batchJob.id)?.status
      logger.debug("Wait not satisfied. Stored job status: ${storedStatus?.name}")
      throw e
    }
    return batchJobService.getJobDto(batchJob.id)
  }

  fun assertStatusReported(status: BatchJobStatus) {
    waitForNotThrowing(pollTime = 100, timeout = 2000) {
      assertMessagesContain(status.name)
    }
  }

  fun assertMessagesContain(string: String) {
    waitForNotThrowing(pollTime = 100, timeout = 2000) {
      websocketHelper.receivedMessages.assert.anyMatch { it.contains(string) }
    }
  }

  fun waitForJobFailed(job: BatchJob) {
    waitForNotThrowing(pollTime = 1000) {
      executeInNewTransaction(transactionManager) {
        val finishedJob = batchJobService.getJobDto(job.id)
        val finishedJobEntity = batchJobService.getJobEntity(job.id)
        finishedJobEntity.status.assert.isEqualTo(BatchJobStatus.FAILED)
        finishedJob.status.assert.isEqualTo(BatchJobStatus.FAILED)
      }
    }
  }

  fun assertTotalWebsocketMessagesCount(count: Int) {
    waitForNotThrowing(pollTime = 100, timeout = 2000) {
      websocketHelper.receivedMessages.assert.hasSize(count)
    }
  }

  fun assertTotalWebsocketMessagesCountGreaterThan(count: Int) {
    waitForNotThrowing(pollTime = 100, timeout = 2000) {
      websocketHelper.receivedMessages.assert.hasSizeGreaterThan(count)
    }
  }

  fun assertJobFailedWithMessage(
    job: BatchJob,
    message: Message,
  ) {
    waitForNotThrowing {
      executeInNewTransaction(transactionManager) {
        batchJobService.getView(job.id).errorMessage.assert.isEqualTo(message)
      }
    }
  }

  fun makePreTranslateProcessorThrowGenericException() {
    doThrow(RuntimeException("OMG! It failed"))
      .whenever(preTranslationByTmChunkProcessor)
      .process(
        any(),
        argThat { this.containsAll((1L..10).toList()) },
        any(),
        any(),
      )
  }

  fun makeAutomationChunkProcessorPass() {
    doAnswer { }
      .whenever(automationChunkProcessor)
      .process(any(), any(), any(), any())
  }

  fun verifyConstantRepeats(
    repeats: Int,
    timeout: Long = 2000,
  ) {
    repeat(repeats) {
      waitForNotThrowing {
        batchJobChunkExecutionQueue.find { it.executeAfter == currentDateProvider.date.time + timeout }.assert.isNotNull
      }
      currentDateProvider.forcedDate = Date(currentDateProvider.date.time + timeout)
    }
  }

  fun makeDeleteKeysProcessorThrowDifferentGenericExceptions() {
    val exceptions =
      (0..100).flatMap {
        listOf(
          IllegalStateException("a"),
          NotFoundException(Message.THIRD_PARTY_AUTH_ERROR_MESSAGE),
          RuntimeException("c"),
          RuntimeException(IllegalStateException("d")),
        )
      }

    doThrow(*exceptions.toTypedArray())
      .whenever(deleteKeysChunkProcessor).process(any(), any(), any(), any())
  }

  fun fastForwardToFailedJob(job: BatchJob) {
    waitForNotThrowing {
      currentDateProvider.forcedDate = currentDateProvider.date.addMinutes(1)
      batchJobService.getJobEntity(job.id).status.assert.isEqualTo(BatchJobStatus.FAILED)
    }
  }

  fun makeProgressManagerFail() {
    doThrow(RuntimeException("test"))
      .whenever(progressManager)
      .handleProgress(argThat { this.status != io.tolgee.model.batch.BatchJobChunkExecutionStatus.FAILED }, any())
  }

  fun waitForRetryExecutionCreated(afterMs: Int) {
    waitForNotThrowing {
      batchJobChunkExecutionQueue.find { it.executeAfter == currentDateProvider.date.time + afterMs }.assert.isNotNull
    }
  }

  fun assertTotalExecutionsCount(
    job: BatchJob,
    count: Int,
  ) {
    waitForNotThrowing(pollTime = 100, timeout = 2000) {
      entityManager.createQuery("""from BatchJobChunkExecution b where b.batchJob.id = :id""")
        .setParameter("id", job.id).resultList.assert.hasSize(count)
    }
  }

  fun makeDeleteChunkProcessorReportProgressOnEachItem() {
    doAnswer {
      @Suppress("UNCHECKED_CAST")
      val chunk = it.arguments[1] as List<Long>

      @Suppress("UNCHECKED_CAST")
      val onProgress = it.arguments[3] as ((progress: Int) -> Unit)

      chunk.forEachIndexed { index, _ ->
        onProgress(index + 1)
      }
    }.whenever(deleteKeysChunkProcessor).process(
      any(),
      any(),
      any(),
      any(),
    )
  }

  fun makePreTranslationProcessorWaitingAfter50Executions(): () -> Unit {
    var count = 0

    doAnswer {
      if (count++ > 50) {
        while (true) {
          val context = it.arguments[2] as CoroutineContext
          context.ensureActive()
          Thread.sleep(100)
        }
      }
    }
      .whenever(preTranslationByTmChunkProcessor)
      .process(any(), any(), any(), any())

    return {
      waitFor {
        count > 50
      }
    }
  }

  fun makePreTranslateProcessorRepeatedlyThrowRequeueException() {
    val throwingChunk = (1L..10).toList()

    doThrow(
      RequeueWithDelayException(
        message = Message.OUT_OF_CREDITS,
        successfulTargets = listOf(),
        cause = OutOfCreditsException(OutOfCreditsException.Reason.OUT_OF_CREDITS),
        delayInMs = 100,
        increaseFactor = 10,
        maxRetries = 3,
      ),
    ).whenever(preTranslationByTmChunkProcessor).process(
      any(),
      argThat { this.containsAll(throwingChunk) },
      any(),
      any(),
    )
  }

  fun initWebsocketHelper() {
    websocketHelper =
      WebsocketTestHelper(
        port,
        jwtService.emitToken(testData.user.id),
        testData.projectBuilder.self.id,
        testData.user.id,
      )
    websocketHelper.listenForBatchJobProgress()
  }

  fun waitForJobSuccess(job: BatchJob) {
    waitForCompleted(job).status.assert.isEqualTo(BatchJobStatus.SUCCESS)
  }

  fun waitForJobCancelled(job: BatchJob) {
    waitForCompleted(job).status.assert.isEqualTo(BatchJobStatus.CANCELLED)
  }

  fun makePreTranslateProcessorThrowOutOfCreditsTimes(times: Int) {
    val exceptions =
      (1..times).map { _ ->
        FailedDontRequeueException(
          Message.OUT_OF_CREDITS,
          cause = OutOfCreditsException(OutOfCreditsException.Reason.OUT_OF_CREDITS),
          successfulTargets = listOf(),
        )
      }

    doThrow(*exceptions.toTypedArray()).doAnswer { }
      .whenever(preTranslationByTmChunkProcessor)
      .process(any(), any(), any(), any())
  }

  fun assertJobStateCacheCleared(job: BatchJob) {
    Thread.sleep(100)
    waitForNotThrowing(timeout = 11000, pollTime = 1000) {
      batchJobStateProvider.hasCachedJobState(job.id).assert.isFalse()
    }
  }

  fun makePreTranslateProcessorPass() {
    doAnswer { }
      .whenever(preTranslationByTmChunkProcessor)
      .process(any(), any(), any(), any())
  }

  fun assertJobUnlocked() {
    batchJobProjectLockingManager.getLockedForProject(testData.projectBuilder.self.id).assert.isEqualTo(0L)
  }

  fun getExecutions(jobIds: List<Long>): Map<Long, List<BatchJobChunkExecution>> =
    entityManager.createQuery(
      """from BatchJobChunkExecution b left join fetch b.batchJob where b.batchJob.id in :ids""",
      BatchJobChunkExecution::class.java,
    )
      .setParameter("ids", jobIds).resultList.groupBy { it.batchJob.id }

  fun runChunkedJob(keyCount: Int): BatchJob {
    return executeInNewTransaction(transactionManager) {
      batchJobService.startJob(
        request =
          PreTranslationByTmRequest().apply {
            keyIds = (1L..keyCount).map { it }
          },
        project = testData.projectBuilder.self,
        author = testData.user,
        type = BatchJobType.PRE_TRANSLATE_BT_TM,
      )
    }
  }

  fun runSingleChunkJob(keyCount: Int): BatchJob {
    return executeInNewTransaction(transactionManager) {
      batchJobService.startJob(
        request =
          DeleteKeysRequest().apply {
            keyIds = (1L..keyCount).map { it }
          },
        project = testData.projectBuilder.self,
        author = testData.user,
        type = BatchJobType.DELETE_KEYS,
      )
    }
  }

  fun runDebouncedJob(): BatchJob {
    return runAutomationJob(Duration.ofSeconds(10))
  }

  fun runNonExclusiveJob(): BatchJob {
    return runAutomationJob(null)
  }

  private fun runAutomationJob(duration: Duration?): BatchJob {
    return executeInNewTransaction(transactionManager) {
      batchJobService.startJob(
        request = AutomationBjRequest(1, 1, 1),
        project = testData.projectBuilder.self,
        author = testData.user,
        type = BatchJobType.AUTOMATION,
        debounceDuration = duration,
      )
    }
  }

  fun waitForQueueSize(size: Int) {
    waitForNotThrowing(pollTime = 50, timeout = 2000) {
      batchJobChunkExecutionQueue.size.assert.isEqualTo(size)
    }
  }

  fun waitAndClearQueue(waitForQueueSize: Int) {
    this.waitForQueueSize(waitForQueueSize)
    batchJobChunkExecutionQueue.clear()
    this.waitForQueueSize(0)
    batchJobConcurrentLauncher.pause = false
  }

  fun waitForExecutionSuccess(execution: BatchJobChunkExecution) {
    waitFor(pollTime = 1000) {
      batchJobService.getExecution(execution.id).status == BatchJobChunkExecutionStatus.SUCCESS
    }
  }

  fun verifyJobLocked(job: BatchJob) {
    batchJobProjectLockingManager.getLockedForProject(testData.projectBuilder.self.id).assert.isEqualTo(job.id)
  }

  fun verifiedTriedToLockJob(jobId: Long) {
    waitForNotThrowing {
      verify(batchJobProjectLockingManager, atLeast(1))
        .canLockJobForProject(ArgumentMatchers.eq(jobId))
    }
  }

  fun verifyExecutionPending(execution: BatchJobChunkExecution) {
    batchJobService.getExecution(execution.id).status.assert.isEqualTo(BatchJobChunkExecutionStatus.PENDING)
  }

  fun verifyJobUnlocked(job: BatchJob) {
    waitForNotThrowing {
      // the project was unlocked before job2 acquired the job
      verify(batchJobProjectLockingManager, times(1)).unlockJobForProject(
        ArgumentMatchers.eq(job.project?.id),
        ArgumentMatchers.eq(job.id),
      )
    }
  }

  fun verifyProjectJobLockReleased() {
    waitFor(pollTime = 200, timeout = 1000) {
      batchJobProjectLockingManager.getLockedForProject(testData.projectBuilder.self.id).isEmpty()
    }
  }

  fun runChunkedJob(
    keyCount: Int,
    author: UserAccount = testData.user,
  ): BatchJob {
    return executeInNewTransaction(transactionManager) {
      batchJobService.startJob(
        request =
          PreTranslationByTmRequest().apply {
            keyIds = (1L..keyCount).map { it }
          },
        project = testData.projectBuilder.self,
        author = author,
        type = BatchJobType.PRE_TRANSLATE_BT_TM,
        isHidden = false,
      )
    }
  }

  fun getSingleJob(): BatchJob = entityManager.createQuery("""from BatchJob""", BatchJob::class.java).singleResult

  private val batchJobProjectLockingManager: BatchJobProjectLockingManager
    get() = applicationContext.getBean(BatchJobProjectLockingManager::class.java)

  private val progressManager: ProgressManager
    get() = applicationContext.getBean(ProgressManager::class.java)

  private val batchJobStateProvider: BatchJobStateProvider
    get() = applicationContext.getBean(BatchJobStateProvider::class.java)

  private val deleteKeysChunkProcessor: DeleteKeysChunkProcessor
    get() = applicationContext.getBean(DeleteKeysChunkProcessor::class.java)

  private val port: Int
    get() = applicationContext.environment.getProperty("local.server.port")!!.toInt()

  private val jwtService: JwtService
    get() = applicationContext.getBean(JwtService::class.java)

  private val entityManager
    get() = applicationContext.getBean(EntityManager::class.java)

  private val currentDateProvider: CurrentDateProvider
    get() = applicationContext.getBean(CurrentDateProvider::class.java)

  private val batchJobChunkExecutionQueue: BatchJobChunkExecutionQueue
    get() = applicationContext.getBean(BatchJobChunkExecutionQueue::class.java)

  private val preTranslationByTmChunkProcessor: PreTranslationByTmChunkProcessor
    get() = applicationContext.getBean(PreTranslationByTmChunkProcessor::class.java)

  private val automationChunkProcessor: AutomationChunkProcessor
    get() = applicationContext.getBean(AutomationChunkProcessor::class.java)

  private val transactionManager: PlatformTransactionManager
    get() = applicationContext.getBean(PlatformTransactionManager::class.java)

  private val batchJobService: BatchJobService
    get() = applicationContext.getBean(BatchJobService::class.java)

  private val batchJobConcurrentLauncher: BatchJobConcurrentLauncher
    get() = applicationContext.getBean(BatchJobConcurrentLauncher::class.java)
}
