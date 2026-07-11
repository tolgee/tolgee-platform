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
class BatchChangeTranslationStateTest : ProjectAuthControllerTest("/v2/projects/") {
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
  fun `it changes translation state`() {
    val keyCount = 100
    val keys = testData.addStateChangeData(keyCount)
    batchJobTestBase.saveAndPrepare(this)

    val allKeyIds = keys.map { it.id }.toList()
    val keyIds = allKeyIds.take(10)
    val allLanguageIds =
      testData.projectBuilder.data.languages
        .map { it.self.id }
    val languagesToChangeStateIds = listOf(testData.germanLanguage.id, testData.englishLanguage.id)

    performProjectAuthPost(
      "start-batch-job/set-translation-state",
      mapOf(
        "keyIds" to keyIds,
        "languageIds" to languagesToChangeStateIds,
        "state" to "REVIEWED",
      ),
    ).andIsOk

    waitForNotThrowing(pollTime = 1000, timeout = 10000) {
      val all =
        translationService.getTranslations(
          keys.map { it.id },
          allLanguageIds,
        )
      all.count { it.state == TranslationState.REVIEWED }.assert.isEqualTo(keyIds.size * 2)
    }
  }
}
