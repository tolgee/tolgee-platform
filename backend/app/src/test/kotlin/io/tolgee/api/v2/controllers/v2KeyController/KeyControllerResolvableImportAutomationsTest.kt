package io.tolgee.api.v2.controllers.v2KeyController

import io.tolgee.development.testDataBuilder.data.ResolvableImportTestData
import io.tolgee.fixtures.MachineTranslationTest
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class KeyControllerResolvableImportAutomationsTest : MachineTranslationTest() {
  companion object {
    private const val INITIAL_BUCKET_CREDITS = 150000L
  }

  lateinit var testData: ResolvableImportTestData

  @BeforeEach
  fun setup() {
    testData = ResolvableImportTestData()
    initMachineTranslationMocks()
    initMachineTranslationProperties(INITIAL_BUCKET_CREDITS)
    this.projectSupplier = { testData.projectBuilder.self }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `auto translates on import`() {
    val keyName = "test"
    saveTestData()
    doImport(keyName)
    assertThatKeyAutoTranslated(keyName)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `sets outdated flag on base changed`() {
    val keyName = "keyWith2Translations"
    saveTestData()
    doImport(keyName)
    val translation =
      keyService
        .get(testData.projectBuilder.self.id, keyName, null)
        .getLangTranslation(testData.secondLanguage)
    Assertions.assertThat(translation.outdated).isTrue
  }

  private fun assertThatKeyAutoTranslated(keyName: String) {
    waitForNotThrowing(timeout = 3_000) {
      transactionTemplate.execute {
        val translatedText =
          keyService
            .get(testData.projectBuilder.self.id, keyName, null)
            .getLangTranslation(testData.secondLanguage)
            .text

        Assertions
          .assertThat(
            translatedText,
          ).isEqualTo("Translated with Google")
      }
    }
  }

  fun saveTestData() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  fun doImport(keyName: String) {
    performProjectAuthPost(
      "keys/import-resolvable",
      mapOf(
        "keys" to
          listOf(
            mapOf(
              "name" to keyName,
              "translations" to
                mapOf(
                  "en" to
                    mapOf(
                      "text" to "changed",
                      "resolution" to "OVERRIDE",
                    ),
                ),
            ),
          ),
      ),
    ).andIsOk
  }
}
