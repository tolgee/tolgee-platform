package io.tolgee.api.v2.controllers.v2ImportController

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.SingleStepImportTestData
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import io.tolgee.util.performSingleStepImport
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.test.web.servlet.ResultActions

class SingleStepImportControlleMultiLanguageFileTest : ProjectAuthControllerTest("/v2/projects/") {
  @Value("classpath:import/apple/example.xcstrings")
  lateinit var appleXcStrings: Resource
  lateinit var testData: SingleStepImportTestData

  @BeforeEach
  fun beforeEach() {
    testData = SingleStepImportTestData()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `import single language from xcstrings file`() {
    saveAndPrepare()
    performImportWithLanguages(listOf("en")).andIsOk
    executeInNewTransaction {
      val translations = getTestKeyTranslations()
      translations.assert.hasSize(1)
      translations
        .single()
        .text.assert
        .isEqualTo("Hello, World!")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `import both languages from xcstrings file`() {
    saveAndPrepare()
    performImportWithLanguages(listOf("en", "de")).andIsOk
    executeInNewTransaction {
      val translations = getTestKeyTranslations()
      translations.assert.hasSize(2)
      translations
        .map { it.text }
        .assert
        .contains("Hello, World!")
        .contains("Hallo, Welt!")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `import both languages from xcstrings file when languages are null`() {
    saveAndPrepare()
    performImportWithLanguages(null).andIsOk
    executeInNewTransaction {
      val translations = getTestKeyTranslations()
      translations.assert.hasSize(2)
      translations
        .map { it.text }
        .assert
        .contains("Hello, World!")
        .contains("Hallo, Welt!")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `import no language from xcstrings file when languages are empty`() {
    saveAndPrepare()
    performImportWithLanguages(emptyList()).andIsOk
    executeInNewTransaction {
      val translations = getTestKeyTranslations()
      translations.assert.hasSize(0)
    }
  }

  private fun performImportWithLanguages(languages: List<String>?): ResultActions {
    return performImport(
      projectId = testData.project.id,
      listOf(Pair("Localizable.xcstrings", appleXcStrings)),
      params =
        mapOf(
          "fileMappings" to
            listOf(
              mapOf(
                "fileName" to "Localizable.xcstrings",
                "format" to "APPLE_XCSTRINGS",
                "languageTagsToImport" to languages,
              ),
            ),
        ),
    )
  }

  private fun getTestKeyTranslations(namespace: String? = null) =
    keyService.get(projectId = project.id, name = "test", namespace = namespace).translations

  private fun performImport(
    projectId: Long,
    files: List<Pair<String, Resource>>?,
    params: Map<String, Any?> = mapOf(),
  ): ResultActions {
    return performSingleStepImport(mvc, projectId, files, params)
  }

  private fun saveAndPrepare() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    projectSupplier = { testData.project }
  }
}
