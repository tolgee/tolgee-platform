package io.tolgee.api.v2.controllers.batch

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.batch.BatchJobActionService
import io.tolgee.batch.BatchJobConcurrentLauncher
import io.tolgee.batch.BatchJobDto
import io.tolgee.batch.BatchJobService
import io.tolgee.batch.BatchJobType
import io.tolgee.batch.JobChunkExecutionQueue
import io.tolgee.batch.processors.TranslationChunkProcessor
import io.tolgee.batch.request.BatchTranslateRequest
import io.tolgee.batch.state.BatchJobStateProvider
import io.tolgee.development.testDataBuilder.data.BatchJobsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.isValidId
import io.tolgee.fixtures.node
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import java.util.concurrent.ConcurrentHashMap

@SpringBootTest
@AutoConfigureMockMvc
@ContextRecreatingTest
class BatchJobManagementControllerTest : ProjectAuthControllerTest("/v2/projects/") {

  lateinit var testData: BatchJobsTestData

  @Autowired
  lateinit var batchJobActionService: BatchJobActionService

  @Autowired
  lateinit var batchJobService: BatchJobService

  @MockBean
  @Autowired
  lateinit var translationChunkProcessor: TranslationChunkProcessor

  @Autowired
  lateinit var batchJobStateProvider: BatchJobStateProvider

  @Autowired
  lateinit var jobChunkExecutionQueue: JobChunkExecutionQueue

  @Autowired
  lateinit var batchJobConcurrentLauncher: BatchJobConcurrentLauncher

  @BeforeEach
  fun setup() {
    testData = BatchJobsTestData()
    jobChunkExecutionQueue.populateQueue()
    whenever(translationChunkProcessor.getParams(any(), any())).thenCallRealMethod()
    whenever(translationChunkProcessor.getTarget(any())).thenCallRealMethod()
  }

  @AfterEach
  fun after() {
    batchJobConcurrentLauncher.pause = false
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `cancels a job`() {
    val keys = testData.addTranslationOperationData(10)
    saveAndPrepare()

    val keyIds = keys.map { it.id }.toList()

    batchJobConcurrentLauncher.pause = true

    performProjectAuthPut(
      "start-batch-job/translate",
      mapOf(
        "keyIds" to keyIds,
        "targetLanguageIds" to listOf(
          testData.projectBuilder.getLanguageByTag("cs")!!.self.id
        )
      )
    ).andIsOk

    val job = getSingleJob()

    performProjectAuthPut("batch-jobs/${job.id}/cancel")
      .andIsOk

    waitForNotThrowing {
      getSingleJob().status.assert.isEqualTo(BatchJobStatus.CANCELLED)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `cannot cancel other's job`() {
    saveAndPrepare()

    batchJobConcurrentLauncher.pause = true

    val job = runChunkedJob(10)

    userAccount = testData.anotherUser

    performProjectAuthPut("batch-jobs/${job.id}/cancel")
      .andIsForbidden
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `returns list of jobs`() {
    saveAndPrepare()

    val jobIds = ConcurrentHashMap.newKeySet<Long>()
    var wait = true
    whenever(
      translationChunkProcessor.process(any(), any(), any(), any())
    ).then {
      val id = it.getArgument<BatchJobDto>(0).id
      if (jobIds.size == 2 && !jobIds.contains(id)) {
        while (wait) {
          Thread.sleep(100)
        }
      } else {
        jobIds.add(id)
      }
    }

    val jobs = (1..3).map { runChunkedJob(50) }

    waitForNotThrowing(pollTime = 1000, timeout = 10000) {
      val dtos = jobs.map { batchJobService.getJobDto(it.id) }
      dtos.forEach {
        val state = batchJobStateProvider.getCached(it.id)
        println(
          "Job ${it.id} status ${it.status} progress: ${state?.values?.sumOf { it.successTargets.size }}"
        )
      }
      dtos.count { it.status == BatchJobStatus.SUCCESS }.assert.isEqualTo(2)
      dtos.count { it.status == BatchJobStatus.RUNNING }.assert.isEqualTo(1)
    }

    performProjectAuthGet("batch-jobs?sort=status&sort=id")
      .andIsOk.andAssertThatJson {
        node("_embedded.batchJobs") {
          isArray.hasSize(3)
          node("[0].status").isEqualTo("RUNNING")
          node("[0].progress").isEqualTo(0)
          node("[1].id").isValidId
          node("[1].status").isEqualTo("SUCCESS")
          node("[1].progress").isEqualTo(50)
        }
      }

    wait = false

    waitForNotThrowing(pollTime = 1000, timeout = 10000) {
      val dtos = jobs.map { batchJobService.getJobDto(it.id) }
      dtos.count { it.status == BatchJobStatus.SUCCESS }.assert.isEqualTo(3)
    }

    performProjectAuthGet("batch-jobs?sort=status&sort=id")
      .andIsOk.andAssertThatJson {
        node("_embedded.batchJobs") {
          isArray.hasSize(3)
          node("[0].status").isEqualTo("SUCCESS")
        }
      }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `returns list of my jobs`() {
    saveAndPrepare()

    val jobs = (1..3).map { runChunkedJob(50) }

    waitForNotThrowing(pollTime = 1000, timeout = 10000) {
      val dtos = jobs.map { batchJobService.getJobDto(it.id) }
      dtos.count { it.status == BatchJobStatus.SUCCESS }.assert.isEqualTo(3)
    }

    performProjectAuthGet("my-batch-jobs?sort=status&sort=id")
      .andIsOk.andAssertThatJson {
        node("_embedded.batchJobs") {
          isArray.hasSize(3)
          node("[0].status").isEqualTo("SUCCESS")
        }
      }

    userAccount = testData.anotherUser

    performProjectAuthGet("my-batch-jobs?sort=status&sort=id")
      .andIsOk.andAssertThatJson {
        node("_embedded.batchJobs").isAbsent()
      }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `returns single job`() {
    saveAndPrepare()

    val job = runChunkedJob(50)

    waitForNotThrowing(pollTime = 1000, timeout = 10000) {
      getSingleJob().status.assert.isEqualTo(BatchJobStatus.SUCCESS)
    }

    performProjectAuthGet("batch-jobs/${job.id}")
      .andIsOk.andAssertThatJson {
        node("status").isEqualTo("SUCCESS")
      }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `cannot get other's job`() {
    saveAndPrepare()

    val job = runChunkedJob(10)

    waitForNotThrowing(pollTime = 100, timeout = 10000) {
      getSingleJob().status.assert.isEqualTo(BatchJobStatus.SUCCESS)
    }

    userAccount = testData.anotherUser

    performProjectAuthGet("batch-jobs/${job.id}")
      .andIsForbidden
  }

  private fun getSingleJob(): BatchJob =
    entityManager.createQuery("""from BatchJob""", BatchJob::class.java).singleResult

  private fun saveAndPrepare() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    this.projectSupplier = { testData.projectBuilder.self }
  }

  protected fun runChunkedJob(keyCount: Int): BatchJob {
    return executeInNewTransaction {
      batchJobService.startJob(
        request = BatchTranslateRequest().apply {
          keyIds = (1L..keyCount).map { it }
        },
        project = testData.projectBuilder.self,
        author = testData.user,
        type = BatchJobType.TRANSLATION
      )
    }
  }
}
