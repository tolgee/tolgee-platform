package io.tolgee.api.v2.controllers.batch

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.config.BatchJobBaseConfiguration
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.enums.TranslationState
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import

@Import(BatchJobBaseConfiguration::class)
class BatchClearTranslationsTest : ProjectAuthControllerTest("/v2/projects/") {
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
  fun `it clears translations`() {
    val keyCount = 1000
    val keys = testData.addStateChangeData(keyCount)
    batchJobTestBase.saveAndPrepare(this)

    val allKeyIds = keys.map { it.id }.toList()
    val keyIds = allKeyIds.take(10)
    val allLanguageIds =
      testData.projectBuilder.data.languages
        .map { it.self.id }
    val languagesToClearIds = listOf(testData.germanLanguage.id, testData.englishLanguage.id)

    performProjectAuthPost(
      "start-batch-job/clear-translations",
      mapOf(
        "keyIds" to keyIds,
        "languageIds" to languagesToClearIds,
      ),
    ).andIsOk

    waitForNotThrowing(pollTime = 1000, timeout = 10000) {
      val all =
        translationService.getTranslations(
          keys.map { it.id },
          allLanguageIds,
        )
      all.count { it.state == TranslationState.UNTRANSLATED && it.text == null }.assert.isEqualTo(keyIds.size * 2)
    }
  }
}
