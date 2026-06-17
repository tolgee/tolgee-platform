package io.tolgee.api.v2.controllers.batch

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.config.BatchJobBaseConfiguration
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import

@Import(BatchJobBaseConfiguration::class)
class BatchCopyTranslationsTest : ProjectAuthControllerTest("/v2/projects/") {
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
  fun `it copies translations`() {
    val keyCount = 1000
    val keys = testData.addStateChangeData(keyCount)
    batchJobTestBase.saveAndPrepare(this)

    val allKeyIds = keys.map { it.id }.toList()
    val keyIds = allKeyIds.take(10)
    val allLanguageIds =
      testData.projectBuilder.data.languages
        .map { it.self.id }
    val languagesToChangeStateIds = listOf(testData.germanLanguage.id, testData.czechLanguage.id)

    performProjectAuthPost(
      "start-batch-job/copy-translations",
      mapOf(
        "keyIds" to keyIds,
        "sourceLanguageId" to testData.englishLanguage.id,
        "targetLanguageIds" to languagesToChangeStateIds,
      ),
    ).andIsOk

    waitForNotThrowing(pollTime = 1000, timeout = 10000) {
      val all =
        translationService.getTranslations(
          keys.map { it.id },
          allLanguageIds,
        )
      all.count { it.text?.startsWith("en") == true }.assert.isEqualTo(allKeyIds.size + keyIds.size * 2)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it resets the state for key`() {
    val key = testData.addKeyWithTranslationsReviewed()
    batchJobTestBase.saveAndPrepare(this)

    assertGermanAKeyState(key, TranslationState.REVIEWED)

    val languagesToChangeStateIds = listOf(testData.germanLanguage.id)

    performProjectAuthPost(
      "start-batch-job/copy-translations",
      mapOf(
        "keyIds" to listOf(key.id),
        "sourceLanguageId" to testData.englishLanguage.id,
        "targetLanguageIds" to languagesToChangeStateIds,
      ),
    ).andIsOk

    waitForNotThrowing(pollTime = 1000, timeout = 10000) {
      assertGermanAKeyState(key, TranslationState.TRANSLATED)
    }
  }

  private fun assertGermanAKeyState(
    key: Key,
    translationState: TranslationState,
  ) {
    translationService
      .getTranslations(listOf(key.id), listOf(testData.germanLanguage.id))
      .single()
      .state.assert
      .isEqualTo(translationState)
  }
}
