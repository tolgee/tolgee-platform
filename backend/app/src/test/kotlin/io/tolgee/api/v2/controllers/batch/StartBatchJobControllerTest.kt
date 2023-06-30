package io.tolgee.api.v2.controllers.batch

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.BatchJobsTestData
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.model.translation.Translation
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
@ContextRecreatingTest
class StartBatchJobControllerTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: BatchJobsTestData
  var fakeBefore = false

  @BeforeEach
  fun setup() {
    testData = BatchJobsTestData()
    fakeBefore = internalProperties.fakeMtProviders
    internalProperties.fakeMtProviders = true
    machineTranslationProperties.google.apiKey = "mock"
    machineTranslationProperties.google.defaultEnabled = true
    machineTranslationProperties.google.defaultPrimary = true
    machineTranslationProperties.aws.defaultEnabled = false
    machineTranslationProperties.aws.accessKey = "mock"
    machineTranslationProperties.aws.secretKey = "mock"
  }

  @AfterEach
  fun after() {
    internalProperties.fakeMtProviders = fakeBefore
  }

  fun saveAndPrepare() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    this.projectSupplier = { testData.projectBuilder.self }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it batch translates`() {
    val keyCount = 100
    val keys = testData.addTranslationOperationData(keyCount)
    saveAndPrepare()

    val keyIds = keys.map { it.id }.toList()

    performProjectAuthPut(
      "start-batch-job/translate",
      mapOf(
        "keyIds" to keyIds,
        "targetLanguageIds" to listOf(
          testData.projectBuilder.getLanguageByTag("cs")!!.self.id,
          testData.projectBuilder.getLanguageByTag("de")!!.self.id
        )
      )
    ).andIsOk

    waitForAllTranslated(keyIds, keyCount)
    executeInNewTransaction {
      val jobs = entityManager.createQuery("""from BatchJob""", BatchJob::class.java)
        .resultList
      jobs.assert.hasSize(1)
      val job = jobs[0]
      job.status.assert.isEqualTo(BatchJobStatus.SUCCESS)
      job.activityRevision.assert.isNotNull
      job.activityRevision!!.modifiedEntities.assert.hasSize(200)
    }
  }

  private fun waitForAllTranslated(keyIds: List<Long>, keyCount: Int) {
    waitForNotThrowing(pollTime = 1000) {
      @Suppress("UNCHECKED_CAST") val czechTranslations = entityManager.createQuery(
        """
        from Translation t where t.key.id in :keyIds and t.language.tag = 'cs'
        """.trimIndent()
      ).setParameter("keyIds", keyIds).resultList as List<Translation>
      czechTranslations.assert.hasSize(keyCount)
      czechTranslations.forEach {
        it.text.assert.contains("translated with GOOGLE from en to cs")
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it deletes keys`() {
    val keyCount = 100
    val keys = testData.addTranslationOperationData(keyCount)
    saveAndPrepare()

    val keyIds = keys.map { it.id }.toList()

    performProjectAuthPut(
      "start-batch-job/delete-keys",
      mapOf(
        "keyIds" to keyIds,
      )
    ).andIsOk

    waitForNotThrowing(pollTime = 1000, timeout = 10000) {
      val all = keyService.getAll(testData.projectBuilder.self.id)
      all.assert.isEmpty()
    }

    waitForNotThrowing(pollTime = 1000, timeout = 10000) {
      executeInNewTransaction {
        val data = entityManager
          .createQuery("""from BatchJob""", BatchJob::class.java)
          .resultList

        data.assert.hasSize(1)
        data[0].activityRevision.assert.isNotNull
      }
    }
  }
}
