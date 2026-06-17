package io.tolgee.api.v2.controllers.batch

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.config.BatchJobBaseConfiguration
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.batch.BatchJob
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import

@Import(BatchJobBaseConfiguration::class)
class BatchDeleteKeysTest : ProjectAuthControllerTest("/v2/projects/") {
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
  fun `it deletes keys`() {
    val keyCount = 100
    val keys = testData.addTranslationOperationData(keyCount)
    batchJobTestBase.saveAndPrepare(this)

    val keyIds = keys.map { it.id }.toList()

    val result =
      performProjectAuthPost(
        "start-batch-job/delete-keys",
        mapOf(
          "keyIds" to keyIds,
        ),
      ).andIsOk

    batchJobTestBase.waitForJobCompleted(result)

    waitForNotThrowing(pollTime = 1000, timeout = 10000) {
      val all = keyService.getAll(testData.projectBuilder.self.id)
      all.assert.hasSize(1)
    }

    waitForNotThrowing(pollTime = 1000, timeout = 10000) {
      executeInNewTransaction {
        val data =
          entityManager
            .createQuery("""from BatchJob""", BatchJob::class.java)
            .resultList

        data.assert.hasSize(1)
        data[0].activityRevision.assert.isNotNull
      }
    }
  }
}
