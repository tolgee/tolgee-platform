package io.tolgee.api.v2.controllers.batch

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.batch.BatchJobActivityFinalizer
import io.tolgee.batch.BatchJobChunkExecutionQueue
import io.tolgee.batch.BatchJobConcurrentLauncher
import io.tolgee.batch.BatchJobTestUtil
import io.tolgee.batch.processors.MachineTranslationChunkProcessor
import io.tolgee.development.testDataBuilder.data.BatchJobsTestData
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.waitFor
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.batch.BatchJobChunkExecutionStatus
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.service.machineTranslation.mtCreditsConsumption.MtCreditBucketService
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import io.tolgee.util.BatchDumper
import io.tolgee.util.Logging
import io.tolgee.util.StuckBatchJobTestUtil
import kotlinx.coroutines.ensureActive
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.SpyBean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

@SpringBootTest
class BatchJobManagementControllerCancellationTest : ProjectAuthControllerTest("/v2/projects/"), Logging {
  lateinit var testData: BatchJobsTestData

  @Autowired
  @SpyBean
  lateinit var machineTranslationChunkProcessor: MachineTranslationChunkProcessor

  @Autowired
  lateinit var batchJobConcurrentLauncher: BatchJobConcurrentLauncher

  @Suppress("LateinitVarOverridesLateinitVar")
  @Autowired
  @SpyBean
  override lateinit var mtCreditBucketService: MtCreditBucketService

  @SpyBean
  @Autowired
  lateinit var batchJobActivityFinalizer: BatchJobActivityFinalizer

  @Autowired
  lateinit var batchDumper: BatchDumper

  lateinit var util: BatchJobTestUtil

  @Autowired
  lateinit var batchJobChunkExecutionQueue: BatchJobChunkExecutionQueue

  @Autowired
  lateinit var stuckBatchJobTestUtil: StuckBatchJobTestUtil

  var simulateLongRunningChunkRun = true

  @BeforeEach
  fun setup() {
    batchJobChunkExecutionQueue.clear()
    testData = BatchJobsTestData()
    batchJobChunkExecutionQueue.populateQueue()
    Mockito.reset(
      mtCreditBucketService,
      machineTranslationChunkProcessor,
      batchJobActivityFinalizer,
    )
    util = BatchJobTestUtil(applicationContext, testData)
    simulateLongRunningChunkRun = true
  }

  @AfterEach
  fun clean() {
    simulateLongRunningChunkRun = false
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `cancels a job`() {
    batchDumper.finallyDump {
      val keys = testData.addTranslationOperationData(100)
      saveAndPrepare()

      val keyIds = keys.map { it.id }.toList()

      val count = AtomicInteger(0)

      doAnswer {
        if (count.incrementAndGet() > 5) {
          while (simulateLongRunningChunkRun) {
            // this simulates long-running operation, which checks for active context
            val context = it.arguments[2] as CoroutineContext
            context.ensureActive()
            Thread.sleep(10)
          }
        }
        it.callRealMethod()
      }.whenever(machineTranslationChunkProcessor).process(any(), any(), any(), any())

      performProjectAuthPost(
        "start-batch-job/machine-translate",
        mapOf(
          "keyIds" to keyIds,
          "targetLanguageIds" to
            listOf(
              testData.projectBuilder.getLanguageByTag("cs")!!.self.id,
            ),
        ),
      ).andIsOk

      waitFor {
        batchJobConcurrentLauncher.runningJobs.size >= 5
      }

      val job = util.getSingleJob()
      performProjectAuthPut("batch-jobs/${job.id}/cancel")
        .andIsOk

      waitForNotThrowing(pollTime = 100) {
        executeInNewTransaction {
          util.getSingleJob().status.assert.isEqualTo(BatchJobStatus.CANCELLED)
          verify(batchJobActivityFinalizer, times(1)).finalizeActivityWhenJobCompleted(any())

          // assert activity stored
          entityManager.createQuery("""from ActivityRevision ar where ar.batchJob.id = :id""")
            .setParameter("id", job.id).resultList
            .assert.hasSize(1)
        }
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `cannot cancel other's job`() {
    saveAndPrepare()

    batchJobConcurrentLauncher.pause = true

    val job = util.runChunkedJob(100)

    userAccount = testData.anotherUser

    performProjectAuthPut("batch-jobs/${job.id}/cancel")
      .andIsForbidden
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `stuck job can be cancelled`() {
    saveAndPrepare()
    batchJobConcurrentLauncher.pause = true
    val job =
      stuckBatchJobTestUtil.createBatchJobWithExecutionStatuses(
        testData.project,
        BatchJobStatus.RUNNING,
        setOf(BatchJobChunkExecutionStatus.CANCELLED),
      )

    executeInNewTransaction {
      val merged = entityManager.merge(job)
      merged.author = testData.user
      entityManager.persist(merged)
    }

    performProjectAuthPut("batch-jobs/${job.id}/cancel").andIsOk

    util.getSingleJob().status.assert.isEqualTo(BatchJobStatus.CANCELLED)
  }

  private fun saveAndPrepare() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    this.projectSupplier = { testData.projectBuilder.self }
  }
}
