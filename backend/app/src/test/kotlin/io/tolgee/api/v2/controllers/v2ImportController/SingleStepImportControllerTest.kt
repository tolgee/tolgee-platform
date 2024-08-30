package io.tolgee.api.v2.controllers.v2ImportController

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.batch.data.BatchJobType
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.SingleStepImportTestData
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.enums.Scope
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import io.tolgee.util.performSingleStepImport
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.test.web.servlet.ResultActions

class SingleStepImportControllerTest : ProjectAuthControllerTest("/v2/projects/") {
  @Value("classpath:import/simple.json")
  lateinit var simpleJson: Resource

  @Value("classpath:import/new.json")
  lateinit var newJson: Resource

  @Value("classpath:import/apple/Localizable.strings")
  lateinit var appleStringsFile: Resource

  @Value("classpath:import/apple/en.xliff")
  lateinit var appleXliffFile: Resource

  @Value("classpath:import/xliff/simple.xliff")
  lateinit var simpleXliff: Resource

  lateinit var testData: SingleStepImportTestData

  private val xliffFileName: String = "file.xliff"
  private val jsonFileName = "en.json"

  @BeforeEach
  fun beforeEach() {
    testData = SingleStepImportTestData()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `import simple json`() {
    saveAndPrepare()
    performImport(
      projectId = testData.project.id,
      listOf(Pair(jsonFileName, simpleJson)),
      params = mapOf("tagNewKeys" to listOf("new-tag")),
    )
    executeInNewTransaction {
      assertJsonImported()
      getTestTranslation().key.keyMeta!!.tags.map { it.name }.assert.contains("new-tag")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `does not create new key if option isn't enabled`() {
    saveAndPrepare()
    performImport(
      projectId = testData.project.id,
      listOf(Pair(jsonFileName, simpleJson)),
      params = mapOf("createNewKeys" to false),
    )
    executeInNewTransaction {
      keyService.find(testData.project.id, "test", null).assert.isNull()
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `correctly maps language in single language file`() {
    saveAndPrepare()
    performImport(
      projectId = testData.project.id,
      listOf(Pair(jsonFileName, simpleJson)),
      getFileMappings(
        jsonFileName,
        languageTag = "de",
      ),
    )
    executeInNewTransaction {
      getTestTranslation().language.tag.assert.isEqualTo("de")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `correctly maps language in multi language file`() {
    saveAndPrepare()
    val fileName = xliffFileName
    performImport(
      projectId = testData.project.id,
      listOf(Pair(fileName, simpleXliff)),
      getSimpleXliffMapping(mapOf("cs" to "de", "en" to "en")),
    )
    executeInNewTransaction {
      assertXliffDataImported()
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `throws when language not mapped`() {
    saveAndPrepare()
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
    saveAndPrepare()
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
    executeInNewTransaction {
      assertXliffDataImported()
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `maps namespace`() {
    saveAndPrepare()
    performImport(
      projectId = testData.project.id,
      listOf(Pair(jsonFileName, simpleJson)),
      getFileMappings(jsonFileName, namespace = "test"),
    ).andIsOk
    executeInNewTransaction {
      getTestTranslation(namespace = "test").assert.isNotNull
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `maps null namespace from non-null mapping`() {
    saveAndPrepare()
    val fileName = "guessed-ns/en.json"
    performImport(
      projectId = testData.project.id,
      listOf(Pair(fileName, simpleJson)),
      getFileMappings(fileName, namespace = null),
    ).andIsOk

    executeInNewTransaction {
      getTestTranslation().assert.isNotNull
    }
    performImport(
      projectId = testData.project.id,
      listOf(Pair(fileName, simpleJson)),
      mapOf(),
    ).andIsOk

    executeInNewTransaction {
      getTestTranslation(namespace = "guessed-ns").assert.isNotNull
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `respects provided format`() {
    saveAndPrepare()
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

    executeInNewTransaction {
      assertJsonImported()
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `imports apple strings file`() {
    saveAndPrepare()
    val fileName = "en/Localizable.strings"
    performImport(
      projectId = testData.project.id,
      listOf(Pair(fileName, appleStringsFile)),
      getFileMappings(fileName, format = "STRINGS", languageTag = "en"),
    ).andIsOk
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `imports xliff file`() {
    saveAndPrepare()
    importXliffFile()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `triggers auto translation`() {
    testData.projectBuilder.addAutoTranslationConfig {
      enableForImport = true
      usingPrimaryMtService = true
    }

    saveAndPrepare()
    importXliffFile()

    assertAutoTranslationTriggered()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `throws on conflict`() {
    testData.addConflictTranslation()
    saveAndPrepare()
    performImport(
      projectId = testData.project.id,
      listOf(Pair(jsonFileName, simpleJson)),
    ).andIsBadRequest.andHasErrorMessage(Message.CONFLICT_IS_NOT_RESOLVED)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `removes other keys`() {
    testData.addConflictTranslation()
    saveAndPrepare()
    val params = getFileMappings(jsonFileName)
    params["removeOtherKeys"] = true

    executeInNewTransaction {
      keyService.find(testData.project.id, "test", null).assert.isNotNull()
    }

    executeInNewTransaction {
      performImport(
        projectId = testData.project.id,
        files = listOf(Pair(jsonFileName, newJson)),
        params,
      ).andIsOk
    }

    executeInNewTransaction {
      // import with "new" key removes "test" key
      keyService.find(testData.project.id, "new", null).assert.isNotNull()
      keyService.find(testData.project.id, "test", null).assert.isNull()
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `doesn't allow deletion when no permission to do so`() {
    testData.addConflictTranslation()
    testData.setUserScopes(arrayOf(Scope.TRANSLATIONS_VIEW, Scope.KEYS_CREATE, Scope.KEYS_VIEW))
    saveAndPrepare()
    val params = getFileMappings(jsonFileName)
    params["removeOtherKeys"] = true

    executeInNewTransaction {
      performImport(
        projectId = testData.project.id,
        files = listOf(Pair(jsonFileName, newJson)),
        params,
      ).andIsForbidden
    }
  }

  private fun importXliffFile() {
    val fileName = "en.xliff"
    performImport(
      projectId = testData.project.id,
      listOf(Pair(fileName, appleXliffFile)),
      getFileMappings(fileName, format = "APPLE_XLIFF", languageTag = "en"),
    ).andIsOk
  }

  private fun assertAutoTranslationTriggered() {
    val job = entityManager.createQuery("from BatchJob bj", BatchJob::class.java).singleResult
    job.type.assert.isEqualTo(BatchJobType.AUTO_TRANSLATE)
  }

  private fun assertXliffDataImported() {
    getTestKeyTranslations().find { it.language.tag == "de" }!!.text.assert.isEqualTo("Test cs")
    getTestKeyTranslations().find { it.language.tag == "en" }!!.text.assert.isEqualTo("Test en")
  }

  private fun getSimpleXliffMapping(languageMappings: Map<String?, String>): MutableMap<String, Any?> {
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
  ): MutableMap<String, Any?> {
    return mutableMapOf(
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

  private fun saveAndPrepare() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    projectSupplier = { testData.project }
  }
}
