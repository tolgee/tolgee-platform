package io.tolgee.api.v2.controllers.v2ImportController

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.dataImport.ImportCleanTestData
import io.tolgee.fixtures.AuthorizedRequestFactory
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.testing.annotations.ProjectApiKeyAuthTestMethod
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import java.util.function.Consumer

class V2ImportControllerConflictsBetweenFilesTest : ProjectAuthControllerTest("/v2/projects/") {
  @Value("classpath:import/simple.json")
  lateinit var simpleJson: Resource

  @Value("classpath:import/almost_simple.json")
  lateinit var almostSimpleJson: Resource

  @Value("classpath:import/apple/stringsStringsDictConflict/Localizable.strings")
  lateinit var stringsFile: Resource

  @Value("classpath:import/apple/stringsStringsDictConflict/Localizable.stringsdict")
  lateinit var stringsdictFile: Resource

  @Test
  @ProjectApiKeyAuthTestMethod
  fun `can add file with same keys and same language`() {
    val testData = prepareTestData()
    performImportWithConflicts(testData).andIsOk.andAssertThatJson {
      node("result._embedded.languages") {
        node("[0]") {
          node("existingLanguageTag").isEqualTo("en")
          node("totalCount").isEqualTo(1)
          node("importFileIssueCount").isEqualTo(0)
        }
        node("[1]") {
          node("existingLanguageTag").isEqualTo("en")
          node("totalCount").isEqualTo(0)
          node("importFileIssueCount").isEqualTo(1)
        }
      }
    }
  }

  @Test
  @ProjectApiKeyAuthTestMethod
  fun `works with namespace changes (file with conflicts)`() {
    val testData = prepareTestData()
    val importResult = createConflictingImport(testData)

    selectNamespace(importResult.file2Id)
    assertNoConflicts()
    resetNamespace(fileId = importResult.file2Id)
    assertHasConflicts()
  }

  @Test
  @ProjectApiKeyAuthTestMethod
  fun `works with language changes`() {
    val testData = prepareTestData()
    val importResult = createConflictingImport(testData)

    selectLanguage(importResult.language2Id, testData.french.id)
    assertNoConflicts()
    selectLanguage(importResult.language2Id, testData.english.id)
    assertHasConflicts()
    resetLanguage(importResult.language2Id)
    assertNoConflicts()
  }

  @Test
  @ProjectApiKeyAuthTestMethod
  fun `works with language deletion`() {
    val testData = prepareTestData()
    val importResult = createConflictingImport(testData)

    deleteLanguage(importResult.language1Id)

    assertNoConflicts()
  }

  private fun deleteLanguage(languageId: Long) {
    performProjectAuthDelete(
      "import/result/languages/$languageId",
    ).andIsOk
  }

  @Test
  @ProjectApiKeyAuthTestMethod
  fun `handles strings & stringsdict collision`() {
    val testData = prepareTestData()
    performImportWithAppleConflicts(testData)
    assertOnlyStringdictKeyToImport()
  }

  @Test
  @ProjectApiKeyAuthTestMethod
  fun `resets apple conflict when existing language selected`() {
    val testData = prepareTestData()
    val data = createAppleConflictingImport(testData)
    selectLanguage(data.language2Id, testData.french.id)
    assertAllToImport()
    selectLanguage(data.language2Id, testData.english.id)
    assertOnlyStringdictKeyToImport()
    selectLanguage(data.language1Id, testData.french.id)
    assertAllToImport()
    selectLanguage(data.language2Id, testData.french.id)
    assertOnlyStringdictKeyToImport()
  }

  @Test
  @ProjectApiKeyAuthTestMethod
  fun `resets apple conflict on existing language reset`() {
    val testData = prepareTestData()
    val data = createAppleConflictingImport(testData)
    resetLanguage(data.language2Id)
    assertAllToImport()
  }

  @Test
  @ProjectApiKeyAuthTestMethod
  fun `resets apple conflict when language deleted`() {
    val testData = prepareTestData()
    val data = createAppleConflictingImport(testData)
    deleteLanguage(data.language2Id)
    assertFirstToImport()
  }

  @Test
  @ProjectApiKeyAuthTestMethod
  fun `resets apple conflict when namespace selected`() {
    val testData = prepareTestData()
    val data = createAppleConflictingImport(testData)
    selectNamespace(data.file2Id)
    assertAllToImport()
    resetNamespace(data.file2Id)
    assertOnlyStringdictKeyToImport()
  }

  private fun resetLanguage(importLanguageId: Long) {
    performProjectAuthPut(
      "import/result/languages/$importLanguageId/reset-existing",
    ).andIsOk
  }

  private fun selectLanguage(
    importLanguageId: Long,
    existingLanguage: Long,
  ) {
    performProjectAuthPut(
      "import/result/languages/$importLanguageId/select-existing/$existingLanguage",
    ).andIsOk
  }

  private fun ResultActions.parseData(): ImportResult {
    var file1Id: Long? = null
    var file2Id: Long? = null
    var language1Id: Long? = null
    var language2Id: Long? = null
    andIsOk
      .andAssertThatJson {
        node("result._embedded.languages[0].importFileId").isNumber.satisfies(
          Consumer {
            file1Id = it.toLong()
          },
        )
        node("result._embedded.languages[1].importFileId").isNumber.satisfies(
          Consumer {
            file2Id = it.toLong()
          },
        )
        node("result._embedded.languages[0].id").isNumber.satisfies(
          Consumer {
            language1Id = it.toLong()
          },
        )
        node("result._embedded.languages[1].id").isNumber.satisfies(
          Consumer {
            language2Id = it.toLong()
          },
        )
      }.andIsOk
    return ImportResult(
      file1Id!!,
      file2Id!!,
      language1Id!!,
      language2Id!!,
    )
  }

  private fun createAppleConflictingImport(testData: ImportCleanTestData): ImportResult {
    return performImportWithAppleConflicts(testData).parseData()
  }

  private fun createConflictingImport(testData: ImportCleanTestData): ImportResult {
    return performImportWithConflicts(testData).parseData()
  }

  private data class ImportResult(
    val file1Id: Long,
    val file2Id: Long,
    val language1Id: Long,
    val language2Id: Long,
  )

  private fun assertNoConflicts() {
    performProjectAuthGet("import/result").andIsOk.andAssertThatJson {
      node("_embedded.languages[0].totalCount").isEqualTo(1)
      node("_embedded.languages[0].importFileIssueCount").isEqualTo(0)
      try {
        node("_embedded.languages[1].totalCount").isEqualTo(1)
        node("_embedded.languages[1].importFileIssueCount").isEqualTo(0)
      } catch (e: AssertionError) {
        node("_embedded.languages[1]").isAbsent()
      }
    }
  }

  private fun assertOnlyStringdictKeyToImport() {
    performProjectAuthGet("import/result").andIsOk.andAssertThatJson {
      node("_embedded.languages[0].totalCount").isEqualTo(0)
      node("_embedded.languages[0].importFileIssueCount").isEqualTo(0)
      node("_embedded.languages[1].totalCount").isEqualTo(1)
      node("_embedded.languages[1].importFileIssueCount").isEqualTo(0)
    }
  }

  private fun assertAllToImport() {
    assertFirstToImport()
    assertSecondToImport()
  }

  private fun assertSecondToImport() {
    performProjectAuthGet("import/result").andIsOk.andAssertThatJson {
      node("_embedded.languages[1].totalCount").isEqualTo(1)
      node("_embedded.languages[1].importFileIssueCount").isEqualTo(0)
    }
  }

  private fun assertFirstToImport() {
    performProjectAuthGet("import/result").andIsOk.andAssertThatJson {
      node("_embedded.languages[0].totalCount").isEqualTo(1)
      node("_embedded.languages[0].importFileIssueCount").isEqualTo(0)
    }
  }

  private fun assertHasConflicts() {
    performProjectAuthGet("import/result").andIsOk.andAssertThatJson {
      node("_embedded.languages[0].totalCount").isEqualTo(1)
      node("_embedded.languages[0].importFileIssueCount").isEqualTo(0)
      node("_embedded.languages[1].totalCount").isEqualTo(0)
      node("_embedded.languages[1].importFileIssueCount").isEqualTo(1)
    }
  }

  private fun prepareTestData(): ImportCleanTestData {
    val testData = ImportCleanTestData()
    testDataService.saveTestData(testData.root)
    loginAsUser(testData.userAccount.username)
    projectSupplier = { testData.projectBuilder.self }
    return testData
  }

  private fun performImportWithConflicts(testData: ImportCleanTestData) =
    performImport(
      projectId = testData.project.id,
      listOf(
        Pair("en.json", simpleJson),
        Pair("en.json", almostSimpleJson),
      ),
    )

  private fun performImportWithAppleConflicts(testData: ImportCleanTestData) =
    performImport(
      projectId = testData.project.id,
      listOf(
        Pair("en.lproj/Localizable.strings", stringsFile),
        Pair("en.lproj/Localizable.stringsdict", stringsdictFile),
      ),
    )

  private fun performImport(
    projectId: Long,
    files: List<Pair<String, Resource>>?,
    params: Map<String, Any?> = mapOf(),
  ): ResultActions {
    val builder =
      MockMvcRequestBuilders
        .multipart("/v2/projects/$projectId/import?${mapToQueryString(params)}")

    files?.forEach {
      builder.file(
        MockMultipartFile(
          "files",
          it.first,
          "application/zip",
          it.second.file.readBytes(),
        ),
      )
    }

    loginAsAdminIfNotLogged()
    return mvc.perform(AuthorizedRequestFactory.addToken(builder))
  }

  fun mapToQueryString(map: Map<String, Any?>): String {
    return map.entries.joinToString("&") { "${it.key}=${it.value}" }
  }

  private fun selectNamespace(fileId: Long) {
    performProjectAuthPut(
      "import/result/files/$fileId/select-namespace",
      mapOf("namespace" to "namespaced"),
    ).andIsOk
  }

  private fun resetNamespace(fileId: Long) {
    performProjectAuthPut(
      "import/result/files/$fileId/select-namespace",
      mapOf("namespace" to null),
    ).andIsOk
  }
}
