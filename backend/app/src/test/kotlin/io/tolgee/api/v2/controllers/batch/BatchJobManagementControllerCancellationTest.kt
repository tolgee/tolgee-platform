package io.tolgee.api.v2.controllers.batch

import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.waitFor
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.batch.BatchJobChunkExecutionStatus
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import io.tolgee.util.Logging
import io.tolgee.util.StuckBatchJobTestUtil
import io.tolgee.util.logger
import kotlinx.coroutines.ensureActive
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.RepetitionInfo
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

@SpringBootTest
class BatchJobManagementControllerCancellationTest :
  AbstractBatchJobManagementControllerTest(
    "/v2/projects/",
  ),
  Logging {
  @Autowired
  lateinit var stuckBatchJobTestUtil: StuckBatchJobTestUtil

  var simulateLongRunningChunkRun = true

  @BeforeEach
  fun setup() {
    simulateLongRunningChunkRun = true
  }

  @AfterEach
  fun clean() {
    simulateLongRunningChunkRun = false
  }

  @RepeatedTest(1000, failureThreshold = 1)
  @ProjectJWTAuthTestMethod
  fun `cancels a job`(repetitionInfo: RepetitionInfo) {
    val repetition = repetitionInfo.currentRepetition
    logger.info("=== Starting repetition $repetition of ${repetitionInfo.totalRepetitions} ===")

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
              testData.projectBuilder
                .getLanguageByTag("cs")!!
                .self.id,
            ),
        ),
      ).andIsOk

      waitFor {
        batchJobConcurrentLauncher.runningJobs.size >= 5
      }

      val job = util.getSingleJob()
      logger.info("Repetition $repetition: Cancelling job ${job.id}")
      performProjectAuthPut("batch-jobs/${job.id}/cancel")
        .andIsOk

      waitForNotThrowing(pollTime = 100) {
        executeInNewTransaction {
          val currentJob = util.getSingleJob()
          logger.info("Repetition $repetition: Job ${currentJob.id} status = ${currentJob.status}")
          currentJob.status.assert.isEqualTo(BatchJobStatus.CANCELLED)

          verify(batchJobActivityFinalizer, times(1)).finalizeActivityWhenJobCompleted(any())

          // assert activity stored
          val activityRevisions =
            entityManager
              .createQuery("""from ActivityRevision ar where ar.batchJob.id = :id""")
              .setParameter("id", job.id)
              .resultList

          // Also check for chunk execution-linked revisions (before merge)
          val chunkLinkedRevisions =
            entityManager
              .createQuery(
                """select ar from ActivityRevision ar
                   join ar.batchJobChunkExecution b
                   where b.batchJob.id = :jobId""",
              ).setParameter("jobId", job.id)
              .resultList

          logger.info(
            "Repetition $repetition: ActivityRevisions with batchJob.id=${job.id}: ${activityRevisions.size}, " +
              "chunk-linked revisions: ${chunkLinkedRevisions.size}",
          )

          if (activityRevisions.size != 1) {
            logger.error(
              "FLAKY_DEBUG FAILURE: Expected 1 ActivityRevision, found ${activityRevisions.size}. " +
                "Chunk-linked: ${chunkLinkedRevisions.size}. Job status: ${currentJob.status}",
            )
          }
          activityRevisions.assert.hasSize(1)
        }
      }
      logger.info("=== Repetition $repetition completed successfully ===")
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

    util
      .getSingleJob()
      .status.assert
      .isEqualTo(BatchJobStatus.CANCELLED)
  }
}
