package io.tolgee.api.v2.controllers.batch

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.batch.BatchJobChunkExecutionQueue
import io.tolgee.batch.BatchJobService
import io.tolgee.config.BatchJobBaseConfiguration
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.ResultActions
import java.util.function.Consumer

@AutoConfigureMockMvc
@ContextRecreatingTest
@Import(BatchJobBaseConfiguration::class)
class BatchMoveToNamespaceTest : ProjectAuthControllerTest("/v2/projects/") {
  @Autowired
  lateinit var batchJobOperationQueue: BatchJobChunkExecutionQueue

  @Autowired
  lateinit var batchJobService: BatchJobService

  @Autowired
  lateinit var batchJobTestBase: BatchJobTestBase

  @BeforeEach
  fun setup() {
    batchJobTestBase.setup()
  }

  val testData
    get() = batchJobTestBase.testData

  @Test
  @ProjectJWTAuthTestMethod
  fun `it moves to other namespace`() {
    val keys = testData.addNamespaceData()
    batchJobTestBase.saveAndPrepare(this)

    val allKeyIds = keys.map { it.id }.toList()
    val keyIds = allKeyIds.take(700)

    val result =
      performProjectAuthPost(
        "start-batch-job/set-keys-namespace",
        mapOf(
          "keyIds" to keyIds,
          "namespace" to "other-namespace",
        ),
      ).andIsOk

    batchJobTestBase.waitForJobCompleted(result)
    val all = keyService.find(keyIds)
    all.count { it.namespace?.name == "other-namespace" }.assert.isEqualTo(keyIds.size)
    namespaceService.find(testData.projectBuilder.self.id, "namespace1").assert.isNull()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it fails on collision when setting namespaces`() {
    testData.addNamespaceData()
    val key = testData.projectBuilder.addKey(keyName = "key").self
    batchJobTestBase.saveAndPrepare(this)

    val result =
      performProjectAuthPost(
        "start-batch-job/set-keys-namespace",
        mapOf(
          "keyIds" to listOf(key.id),
          "namespace" to "namespace",
        ),
      ).andIsOk

    batchJobTestBase.waitForJobCompleted(result)

    val jobId = result.jobId
    keyService
      .get(key.id)
      .namespace.assert
      .isNull()
    batchJobService
      .findJobDto(jobId)
      ?.status.assert
      .isEqualTo(BatchJobStatus.FAILED)
  }

  val ResultActions.jobId: Long
    get() {
      var jobId: Long? = null
      this.andAssertThatJson {
        node("id").isNumber.satisfies(
          Consumer {
            jobId = it.toLong()
          },
        )
      }
      return jobId!!
    }
}
