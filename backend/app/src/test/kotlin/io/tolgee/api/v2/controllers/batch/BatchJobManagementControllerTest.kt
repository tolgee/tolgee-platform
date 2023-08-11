package io.tolgee.api.v2.controllers.batch

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.batch.BatchJobActionService
import io.tolgee.batch.BatchJobActivityFinalizer
import io.tolgee.batch.BatchJobChunkExecutionQueue
import io.tolgee.batch.BatchJobConcurrentLauncher
import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.processors.MachineTranslationChunkProcessor
import io.tolgee.batch.processors.PreTranslationByTmChunkProcessor
import io.tolgee.batch.request.PreTranslationByTmRequest
import io.tolgee.batch.state.BatchJobStateProvider
import io.tolgee.component.CurrentDateProvider
import io.tolgee.development.testDataBuilder.data.BatchJobsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.isValidId
import io.tolgee.fixtures.node
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.UserAccount
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.batch.BatchJobChunkExecutionStatus
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.service.machineTranslation.MtCreditBucketService
import io.tolgee.service.translation.AutoTranslationService
import io.tolgee.testing.LongRunningTest
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import io.tolgee.util.Logging
import io.tolgee.util.addMinutes
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.stereotype.Service
import org.springframework.test.annotation.DirtiesContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.transaction.Transactional
import kotlin.coroutines.CoroutineContext

@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@LongRunningTest
class BatchJobManagementControllerTest : ProjectAuthControllerTest("/v2/projects/"), Logging {

  lateinit var testData: BatchJobsTestData

  @Autowired
  lateinit var batchJobActionService: BatchJobActionService

  @Autowired
  lateinit var batchJobService: BatchJobService

  @Autowired
  @SpyBean
  lateinit var preTranslationByTmChunkProcessor: PreTranslationByTmChunkProcessor

  @Autowired
  @SpyBean
  lateinit var machineTranslationChunkProcessor: MachineTranslationChunkProcessor

  @Autowired
  lateinit var batchJobStateProvider: BatchJobStateProvider

  @Autowired
  lateinit var batchJobChunkExecutionQueue: BatchJobChunkExecutionQueue

  @Autowired
  lateinit var batchJobConcurrentLauncher: BatchJobConcurrentLauncher

  @Autowired
  lateinit var currentDateProvider: CurrentDateProvider

  @Suppress("LateinitVarOverridesLateinitVar")
  @Autowired
  @SpyBean
  override lateinit var mtCreditBucketService: MtCreditBucketService

  @Autowired
  lateinit var throwingService: ThrowingService

  @Autowired
  @SpyBean
  lateinit var autoTranslationService: AutoTranslationService

  @SpyBean
  @Autowired
  lateinit var batchJobActivityFinalizer: BatchJobActivityFinalizer

  @BeforeEach
  fun setup() {
    batchJobChunkExecutionQueue.clear()
    testData = BatchJobsTestData()
    batchJobChunkExecutionQueue.populateQueue()
    Mockito.reset(
      mtCreditBucketService,
      autoTranslationService,
      machineTranslationChunkProcessor,
      preTranslationByTmChunkProcessor,
      batchJobActivityFinalizer
    )
  }

  @AfterEach
  fun after() {
    batchJobConcurrentLauncher.pause = false
    currentDateProvider.forcedDate = null
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `cancels a job`() {
    val keys = testData.addTranslationOperationData(100)
    saveAndPrepare()

    val keyIds = keys.map { it.id }.toList()

    val count = AtomicInteger(0)

    doAnswer {
      if (count.incrementAndGet() > 5) {
        while (true) {
          val context = it.arguments[2] as CoroutineContext
          context.ensureActive()
          Thread.sleep(100)
        }
      }
      it.callRealMethod()
    }.whenever(machineTranslationChunkProcessor).process(any(), any(), any(), any())

    performProjectAuthPost(
      "start-batch-job/machine-translate",
      mapOf(
        "keyIds" to keyIds,
        "targetLanguageIds" to listOf(
          testData.projectBuilder.getLanguageByTag("cs")!!.self.id
        )
      )
    ).andIsOk

    Thread.sleep(2000)

    val job = getSingleJob()
    performProjectAuthPut("batch-jobs/${job.id}/cancel")
      .andIsOk

    waitForNotThrowing(pollTime = 1000) {
      executeInNewTransaction {
        getSingleJob().status.assert.isEqualTo(BatchJobStatus.CANCELLED)
        verify(batchJobActivityFinalizer, times(1)).finalizeActivityWhenJobCompleted(any())

        // assert activity stored
        entityManager.createQuery("""from ActivityRevision ar where ar.batchJob.id = :id""")
          .setParameter("id", job.id).resultList
          .assert.hasSize(1)
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `exception from inner transaction doesn't break it`() {
    val keys = testData.addTranslationOperationData(100)
    saveAndPrepare()

    val keyIds = keys.map { it.id }.toList()

    // although it passes once, there should be no successful targets, because the whole transaction is rolled back
    doAnswer { it.callRealMethod() }
      .doAnswer { throwingService.throwExceptionInTransaction() }
      .whenever(autoTranslationService).autoTranslateSync(any(), any(), any(), any(), any())

    performProjectAuthPost(
      "start-batch-job/machine-translate",
      mapOf(
        "keyIds" to keyIds,
        "targetLanguageIds" to listOf(
          testData.projectBuilder.getLanguageByTag("cs")!!.self.id
        )
      )
    ).andIsOk

    waitForNotThrowing(pollTime = 100) {
      // lets move time fast
      currentDateProvider.forcedDate = currentDateProvider.date.addMinutes(1)
      getSingleJob().status.assert.isEqualTo(BatchJobStatus.FAILED)
    }

    val executions = batchJobService.getExecutions(getSingleJob().id)
    executions.assert.hasSize(80)
    executions.forEach {
      it.status.assert.isEqualTo(BatchJobChunkExecutionStatus.FAILED)
      // no successful targets, since all was rolled back
      it.successTargets.assert.isEmpty()
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `cannot cancel other's job`() {
    saveAndPrepare()

    batchJobConcurrentLauncher.pause = true

    val job = runChunkedJob(100)

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

    try {
      doAnswer {
        val id = it.getArgument<BatchJobDto>(0).id
        if (jobIds.size == 2 && !jobIds.contains(id)) {
          while (wait) {
            Thread.sleep(100)
          }
        } else {
          jobIds.add(id)
        }
      }
        .whenever(preTranslationByTmChunkProcessor)
        .process(any(), any(), any(), any())

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
    } finally {
      wait = false
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
  fun `returns list of current jobs`() {
    saveAndPrepare()

    var wait = true
    doAnswer {
      while (wait) {
        Thread.sleep(100)
      }
    }.whenever(preTranslationByTmChunkProcessor).process(any(), any(), any(), any())

    val adminsJobs = (1..3).map { runChunkedJob(50) }
    val anotherUsersJobs = (1..3).map { runChunkedJob(50, testData.anotherUser) }

    try {
      waitForNotThrowing {
        performProjectAuthGet("current-batch-jobs")
          .andIsOk.andPrettyPrint.andAssertThatJson {
            node("_embedded.batchJobs") {
              isArray.hasSize(6)
              node("[0].status").isEqualTo("RUNNING")
              node("[1].status").isEqualTo("PENDING")
              node("[2].status").isEqualTo("PENDING")
            }
          }
      }

      wait = false

      waitForNotThrowing(pollTime = 1000, timeout = 10000) {
        val dtos = (adminsJobs + anotherUsersJobs).map { batchJobService.getJobDto(it.id) }
        dtos.count { it.status == BatchJobStatus.SUCCESS }.assert.isEqualTo(6)
      }

      performProjectAuthGet("current-batch-jobs")
        .andIsOk.andAssertThatJson {
          node("_embedded.batchJobs") {
            isArray.hasSize(6)
            node("[0].status").isEqualTo("SUCCESS")
          }
        }

      userAccount = testData.anotherUser

      performProjectAuthGet("current-batch-jobs")
        .andIsOk.andAssertThatJson {
          node("_embedded.batchJobs").isArray.hasSize(3)
        }

      currentDateProvider.forcedDate = currentDateProvider.date.addMinutes(61)

      performProjectAuthGet("current-batch-jobs")
        .andIsOk.andAssertThatJson {
          node("_embedded.batchJobs").isAbsent()
        }
    } finally {
      wait = false
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

  protected fun runChunkedJob(keyCount: Int, author: UserAccount = testData.user): BatchJob {
    return executeInNewTransaction {
      batchJobService.startJob(
        request = PreTranslationByTmRequest().apply {
          keyIds = (1L..keyCount).map { it }
        },
        project = testData.projectBuilder.self,
        author = author,
        type = BatchJobType.PRE_TRANSLATE_BT_TM,
        isHidden = false
      )
    }
  }
}

@Service
class ThrowingService() {
  @Transactional
  fun throwExceptionInTransaction() {
    throw RuntimeException("test")
  }
}
