package io.tolgee.api.v2.controllers.batch

import io.tolgee.batch.BatchJobProjectLockingManager
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
import kotlinx.coroutines.ensureActive
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

@SpringBootTest
class BatchJobManagementControllerCancellationTest :
  AbstractBatchJobManagementControllerTest(
  "/v2/projects/"
),
  Logging {

  @Autowired
  lateinit var stuckBatchJobTestUtil: StuckBatchJobTestUtil

  @Autowired
  lateinit var batchJobProjectLockingManager: BatchJobProjectLockingManager

  @Autowired
  lateinit var batchProperties: io.tolgee.configuration.tolgee.BatchProperties

  var simulateLongRunningChunkRun = true
  var originalProjectConcurrency = 1

  @BeforeEach
  fun setup() {
    simulateLongRunningChunkRun = true
    originalProjectConcurrency = batchProperties.projectConcurrency
  }

  @AfterEach
  fun clean() {
    simulateLongRunningChunkRun = false
    batchProperties.projectConcurrency = originalProjectConcurrency
  }

  @ParameterizedTest
  @ValueSource(ints = [1, 2])
  @ProjectJWTAuthTestMethod
  fun `cancels a job`(projectConcurrency: Int) {
    val keys = testData.addTranslationOperationData(100)
    saveAndPrepare()

    batchProperties.projectConcurrency = projectConcurrency

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

    // Verify the job is locked
    val lockedJobs = batchJobProjectLockingManager.getLockedForProject(testData.project.id)
    lockedJobs.assert.contains(job.id)

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

    // Verify the job lock was released
    val lockedJobsAfterCancel = batchJobProjectLockingManager.getLockedForProject(testData.project.id)
    lockedJobsAfterCancel.assert.doesNotContain(job.id)
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

  @Test
  @ProjectJWTAuthTestMethod
  fun `cancels one of two running jobs with projectConcurrency=2`() {
    val keys = testData.addTranslationOperationData(100)
    saveAndPrepare()

    batchProperties.projectConcurrency = 2

    val keyIds = keys.map { it.id }.toList()

    val firstJobCount = AtomicInteger(0)
    val secondJobCount = AtomicInteger(0)

    doAnswer {
      val jobId = it.getArgument<io.tolgee.batch.data.BatchJobDto>(0).id
      val allJobs = executeInNewTransaction {
        batchJobService.getAllByProjectId(testData.project.id)
      }
      val isFirstJob = allJobs.size > 0 && jobId == allJobs[0].id

      val counter = if (isFirstJob) firstJobCount else secondJobCount

      if (counter.incrementAndGet() > 5) {
        while (simulateLongRunningChunkRun) {
          // this simulates long-running operation, which checks for active context
          val context = it.arguments[2] as CoroutineContext
          context.ensureActive()
          Thread.sleep(10)
        }
      }
      it.callRealMethod()
    }.whenever(machineTranslationChunkProcessor).process(any(), any(), any(), any())

    // Start first job
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

    waitFor(timeout = 5000) {
      batchJobConcurrentLauncher.runningJobs.size >= 5
    }

    // Start second job
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

    waitFor(timeout = 5000) {
      batchJobConcurrentLauncher.runningJobs.size >= 10
    }

    val jobs = executeInNewTransaction {
      batchJobService.getAllByProjectId(testData.project.id)
    }
    jobs.size.assert.isEqualTo(2)

    // Verify both jobs are locked
    val lockedJobs = batchJobProjectLockingManager.getLockedForProject(testData.project.id)
    lockedJobs.size.assert.isEqualTo(2)
    lockedJobs.assert.contains(jobs[0].id)
    lockedJobs.assert.contains(jobs[1].id)

    // Cancel the first job
    val firstJob = jobs[0]
    performProjectAuthPut("batch-jobs/${firstJob.id}/cancel")
      .andIsOk

    waitForNotThrowing(pollTime = 100) {
      executeInNewTransaction {
        batchJobService.getJobDto(firstJob.id).status.assert.isEqualTo(BatchJobStatus.CANCELLED)
      }
    }

    // Verify the first job lock was released but second job is still locked
    val lockedJobsAfterCancel = batchJobProjectLockingManager.getLockedForProject(testData.project.id)
    lockedJobsAfterCancel.assert.doesNotContain(firstJob.id)
    lockedJobsAfterCancel.assert.contains(jobs[1].id)

    // Let the second job complete
    simulateLongRunningChunkRun = false

    waitForNotThrowing(pollTime = 100, timeout = 10000) {
      executeInNewTransaction {
        batchJobService.getJobDto(jobs[1].id).status.assert.isEqualTo(BatchJobStatus.SUCCESS)
      }
    }
  }
}
