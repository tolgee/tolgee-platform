package io.tolgee.api.v2.controllers.batch

import io.tolgee.batch.data.BatchJobDto
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.isValidId
import io.tolgee.fixtures.node
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.batch.BatchJobChunkExecutionStatus
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import io.tolgee.util.Logging
import io.tolgee.util.addMinutes
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.ConcurrentHashMap

class BatchJobManagementControllerTest :
  AbstractBatchJobManagementControllerTest("/v2/projects/"),
  Logging {
  @Autowired
  lateinit var throwingService: ThrowingService

  @AfterEach
  fun after() {
    batchJobConcurrentLauncher.pause = false
    clearForcedDate()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `exception from inner transaction doesn't break it`() {
    batchDumper.finallyDump {
      val keys = testData.addTranslationOperationData(100)
      saveAndPrepare()

      val keyIds = keys.map { it.id }.toList()

      // although it passes once, there should be no successful targets, because the whole transaction is rolled back
      doAnswer { it.callRealMethod() }
        .doAnswer { throwingService.throwExceptionInTransaction() }
        .whenever(autoTranslationService)
        .autoTranslateSync(any(), any(), any(), any(), any())

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

      waitForNotThrowing(pollTime = 100) {
        // lets move time fast
        setForcedDate(currentDateProvider.date.addMinutes(1))
        util
          .getSingleJob()
          .status.assert
          .isEqualTo(BatchJobStatus.FAILED)
      }

      val executions = batchJobService.getExecutions(util.getSingleJob().id)
      executions.assert.hasSize(80)
      executions.forEach {
        it.status.assert.isEqualTo(BatchJobChunkExecutionStatus.FAILED)
        // no successful targets, since all was rolled back
        it.successTargets.assert.isEmpty()
      }
    }
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
      }.whenever(preTranslationByTmChunkProcessor)
        .process(any(), any(), any(), any())

      val jobs = (1..3).map { util.runChunkedJob(50) }

      waitForNotThrowing(pollTime = 1000, timeout = 10000) {
        val dtos = jobs.map { batchJobService.getJobDto(it.id) }
        dtos.forEach {
          val state = batchJobStateProvider.getCached(it.id)
          println(
            "Job ${it.id} status ${it.status} progress: ${state?.values?.sumOf { it.successTargets.size }}",
          )
        }
        dtos.count { it.status == BatchJobStatus.SUCCESS }.assert.isEqualTo(2)
        dtos.count { it.status == BatchJobStatus.RUNNING }.assert.isEqualTo(1)
      }

      performProjectAuthGet("batch-jobs?sort=status&sort=id")
        .andIsOk
        .andAssertThatJson {
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
        .andIsOk
        .andAssertThatJson {
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

    val jobs = (1..3).map { util.runChunkedJob(50) }

    waitForNotThrowing(pollTime = 1000, timeout = 10000) {
      val dtos = jobs.map { batchJobService.getJobDto(it.id) }
      dtos.count { it.status == BatchJobStatus.SUCCESS }.assert.isEqualTo(3)
    }

    performProjectAuthGet("my-batch-jobs?sort=status&sort=id")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.batchJobs") {
          isArray.hasSize(3)
          node("[0].status").isEqualTo("SUCCESS")
        }
      }

    userAccount = testData.anotherUser

    performProjectAuthGet("my-batch-jobs?sort=status&sort=id")
      .andIsOk
      .andAssertThatJson {
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

    val adminsJobs = (1..3).map { util.runChunkedJob(50) }
    val anotherUsersJobs = (1..3).map { util.runChunkedJob(50, testData.anotherUser) }

    try {
      waitForNotThrowing {
        performProjectAuthGet("current-batch-jobs")
          .andIsOk.andPrettyPrint
          .andAssertThatJson {
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
        .andIsOk
        .andAssertThatJson {
          node("_embedded.batchJobs") {
            isArray.hasSize(6)
            node("[0].status").isEqualTo("SUCCESS")
          }
        }

      userAccount = testData.anotherUser

      performProjectAuthGet("current-batch-jobs")
        .andIsOk
        .andAssertThatJson {
          node("_embedded.batchJobs").isArray.hasSize(3)
        }

      setForcedDate(currentDateProvider.date.addMinutes(61))

      performProjectAuthGet("current-batch-jobs")
        .andIsOk
        .andAssertThatJson {
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

    val job = util.runChunkedJob(50)

    waitForNotThrowing(pollTime = 1000, timeout = 10000) {
      util
        .getSingleJob()
        .status.assert
        .isEqualTo(BatchJobStatus.SUCCESS)
    }

    performProjectAuthGet("batch-jobs/${job.id}")
      .andIsOk
      .andAssertThatJson {
        node("status").isEqualTo("SUCCESS")
      }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `cannot get other's job`() {
    saveAndPrepare()

    val job = util.runChunkedJob(10)

    waitForNotThrowing(pollTime = 100, timeout = 10000) {
      util
        .getSingleJob()
        .status.assert
        .isEqualTo(BatchJobStatus.SUCCESS)
    }

    userAccount = testData.anotherUser

    performProjectAuthGet("batch-jobs/${job.id}")
      .andIsForbidden
  }
}

@Service
class ThrowingService {
  @Transactional
  fun throwExceptionInTransaction() {
    throw RuntimeException("test")
  }
}
