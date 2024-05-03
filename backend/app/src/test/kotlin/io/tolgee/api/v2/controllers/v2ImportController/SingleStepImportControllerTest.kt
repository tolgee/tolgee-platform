package io.tolgee.api.v2.controllers.v2ImportController

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.SingleStepImportTestData
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import io.tolgee.util.performSingleStepImport
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.test.web.servlet.ResultActions
import org.springframework.transaction.annotation.Transactional

@Transactional
class SingleStepImportControllerTest : ProjectAuthControllerTest("/v2/projects/") {
  @Value("classpath:import/simple.json")
  lateinit var simpleJson: Resource

  @Value("classpath:import/xliff/simple.xliff")
  lateinit var simpleXliff: Resource

  lateinit var testData: SingleStepImportTestData

  private val xliffFileName: String = "file.xliff"
  private val jsonFileName = "en.json"

  @BeforeEach
  fun beforeEach() {
    testData = SingleStepImportTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    projectSupplier = { testData.project }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `import simple json`() {
    performImport(projectId = testData.project.id, listOf(Pair(jsonFileName, simpleJson)))
    assertJsonImported()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `correctly maps language in single language file`() {
    performImport(
      projectId = testData.project.id,
      listOf(Pair(jsonFileName, simpleJson)),
      getFileMappings(
        jsonFileName,
        languageTag = "de",
      ),
    )
    getTestTranslation().language.tag.assert.isEqualTo("de")
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `correctly maps language in multi language file`() {
    val fileName = xliffFileName
    performImport(
      projectId = testData.project.id,
      listOf(Pair(fileName, simpleXliff)),
      getSimpleXliffMapping(mapOf("cs" to "de", "en" to "en")),
    )
    assertXliffDataImported()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `throws when language not mapped`() {
    val fileName = xliffFileName
    performImport(
      projectId = testData.project.id,
      listOf(Pair(fileName, simpleXliff)),
      getSimpleXliffMapping(mapOf("en" to "en")),
    ).andIsBadRequest.andHasErrorMessage(Message.EXISTING_LANGUAGE_NOT_SELECTED)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `maps languages automatically when possible`() {
    val fileName = xliffFileName
    performImport(
      projectId = testData.project.id,
      listOf(Pair(fileName, simpleXliff)),
      getSimpleXliffMapping(
        mapOf(
          // en language is missing here, but is still mapped, because the language tags are equal
          "cs" to "de",
        ),
      ),
    )
    assertXliffDataImported()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `maps namespace`() {
    performImport(
      projectId = testData.project.id,
      listOf(Pair(jsonFileName, simpleJson)),
      getFileMappings(jsonFileName, namespace = "test"),
    ).andIsOk
    getTestTranslation(namespace = "test").assert.isNotNull
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `respects provided format`() {
    performImport(
      projectId = testData.project.id,
      listOf(Pair(jsonFileName, simpleJson)),
      getFileMappings(jsonFileName, namespace = "test", format = "ANDROID_XML"),
    ).andIsBadRequest.andHasErrorMessage(Message.IMPORT_FAILED)

    performImport(
      projectId = testData.project.id,
      listOf(Pair("import.xml", simpleJson)),
      getFileMappings(
        "import.xml",
        languageTag = "en",
        format = "JSON_ICU",
      ),
    ).andIsOk

    assertJsonImported()
  }

  private fun assertXliffDataImported() {
    getTestKeyTranslations().find { it.language.tag == "de" }!!.text.assert.isEqualTo("Test cs")
    getTestKeyTranslations().find { it.language.tag == "en" }!!.text.assert.isEqualTo("Test en")
  }

  private fun getSimpleXliffMapping(languageMappings: Map<String?, String>): Map<String, List<Map<String, Any?>>?> {
    val requestLanguageMappings = getRequestLanguageMappings(languageMappings)

    return getFileMappings(xliffFileName, requestLanguageMappings = requestLanguageMappings)
  }

  private fun getRequestLanguageMappings(languageMappings: Map<String?, String>) =
    languageMappings.map { (import, existing) ->
      mapOf(
        "importLanguage" to import,
        "platformLanguageTag" to existing,
      )
    }

  private fun getFileMappings(
    fileName: String,
    languageTag: String? = null,
    requestLanguageMappings: List<Map<String, String?>>? = null,
    namespace: String? = null,
    format: String? = null,
  ): Map<String, List<Map<String, Any?>>?> {
    return mapOf(
      "languageMappings" to requestLanguageMappings,
      "fileMappings" to
        listOf(
          mapOf(
            "fileName" to fileName,
            "namespace" to namespace,
            "languageTag" to languageTag,
            "format" to format,
          ),
        ),
    )
  }

  private fun getTestTranslation(namespace: String? = null) = getTestKeyTranslations(namespace).single()

  private fun getTestKeyTranslations(namespace: String? = null) =
    keyService.get(projectId = project.id, name = "test", namespace = namespace).translations

  private fun performImport(
    projectId: Long,
    files: List<Pair<String, Resource>>?,
    params: Map<String, Any?> = mapOf(),
  ): ResultActions {
    return performSingleStepImport(mvc, projectId, files, params)
  }

  private fun assertJsonImported() {
    getTestTranslation().text.assert.isEqualTo("test")
  }
}
