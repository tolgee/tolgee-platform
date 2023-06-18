package io.tolgee.api.v2.controllers.batch

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.BatchOperationsTestData
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.translation.Translation
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class BatchOperationControllerTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: BatchOperationsTestData
  var fakeBefore = false

  @BeforeEach
  fun setup() {
    testData = BatchOperationsTestData()
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
      "batch/translate",
      mapOf(
        "keyIds" to keyIds,
        "targetLanguageIds" to listOf(
          testData.projectBuilder.getLanguageByTag("cs")!!.self.id,
          testData.projectBuilder.getLanguageByTag("de")!!.self.id
        )
      )
    ).andIsOk

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
}
