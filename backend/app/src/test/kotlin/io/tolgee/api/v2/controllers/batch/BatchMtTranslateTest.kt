package io.tolgee.api.v2.controllers.batch

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.batch.data.BatchTranslationTargetItem
import io.tolgee.config.BatchJobBaseConfiguration
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.isValidId
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import

@Import(BatchJobBaseConfiguration::class)
class BatchMtTranslateTest : ProjectAuthControllerTest("/v2/projects/") {
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
  fun `it machine translates`() {
    val keyCount = 1000
    val keys = testData.addTranslationOperationData(keyCount)
    batchJobTestBase.saveAndPrepare(this)

    val keyIds = keys.map { it.id }.toList()

    performProjectAuthPost(
      "start-batch-job/machine-translate",
      mapOf(
        "keyIds" to keyIds,
        "targetLanguageIds" to
          listOf(
            testData.projectBuilder
              .getLanguageByTag("cs")!!
              .self.id,
            testData.projectBuilder
              .getLanguageByTag("de")!!
              .self.id,
          ),
      ),
    ).andIsOk
      .andAssertThatJson {
        node("id").isValidId
      }

    batchJobTestBase.waitForAllTranslated(keyIds, keyCount)
    executeInNewTransaction {
      val jobs =
        entityManager
          .createQuery("""from BatchJob""", BatchJob::class.java)
          .resultList
      jobs.assert.hasSize(1)
      val job = jobs[0]
      job.status.assert.isEqualTo(BatchJobStatus.SUCCESS)
      job.activityRevision.assert.isNotNull
      job.activityRevision!!
        .modifiedEntities.assert
        .hasSize(2000)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `stores the materialized target for nodes that read only the target column`() {
    val keys = testData.addTranslationOperationData(3)
    batchJobTestBase.saveAndPrepare(this)
    val keyIds = keys.map { it.id }
    val languageIds =
      listOf("cs", "de").map {
        testData.projectBuilder
          .getLanguageByTag(it)!!
          .self.id
      }

    performProjectAuthPost(
      "start-batch-job/machine-translate",
      mapOf("keyIds" to keyIds, "targetLanguageIds" to languageIds),
    ).andIsOk

    batchJobTestBase.waitForAllTranslated(keyIds, keyIds.size)
    val storedTarget =
      entityManager
        .createNativeQuery("select cast(target as text) from tolgee_batch_job")
        .singleResult as String
    objectMapper
      .readValue(storedTarget, Array<BatchTranslationTargetItem>::class.java)
      .toList()
      .assert
      .containsExactlyElementsOf(keyIds.flatMap { keyId -> languageIds.map { BatchTranslationTargetItem(keyId, it) } })
  }
}
