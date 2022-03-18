package io.tolgee.api.v2.controllers.v2ImportController

import io.tolgee.component.CurrentDateProvider
import io.tolgee.development.testDataBuilder.data.ImportTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.testing.AuthorizedControllerTest
import org.apache.commons.lang3.time.DateUtils
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import java.util.*

class V2ImportControllerResultTest : AuthorizedControllerTest() {
  @MockBean
  @Autowired
  lateinit var currentDateProvider: CurrentDateProvider

  @BeforeEach
  fun setup() {
    whenever(currentDateProvider.getDate()).then {
      Date()
    }
  }

  @Test
  fun `it returns correct result data`() {
    val testData = ImportTestData()
    testDataService.saveTestData(testData.root)

    loginAsUser(testData.root.data.userAccounts[0].self.username)

    performAuthGet("/v2/projects/${testData.project.id}/import/result")
      .andPrettyPrint.andAssertThatJson.node("_embedded.languages").let { languages ->
        languages.isArray.isNotEmpty
        languages.node("[0]").let {
          it.node("name").isEqualTo("en")
          it.node("existingLanguageName").isEqualTo("English")
          it.node("importFileName").isEqualTo("multilang.json")
          it.node("totalCount").isEqualTo("6")
          it.node("conflictCount").isEqualTo("4")
        }
      }
  }

  @Test
  fun `it removes expired import`() {
    val testData = ImportTestData()
    testDataService.saveTestData(testData.root)

    whenever(currentDateProvider.getDate()).then {
      DateUtils.addHours(Date(), 2)
    }

    loginAsUser(testData.root.data.userAccounts[0].self.username)

    performAuthGet("/v2/projects/${testData.project.id}/import/result")
      .andIsNotFound
  }

  @Test
  fun `it returns correct specific language`() {
    val testData = ImportTestData()
    testDataService.saveTestData(testData.root)

    loginAsUser(testData.root.data.userAccounts[0].self.username)

    performAuthGet(
      "/v2/projects/${testData.project.id}" +
        "/import/result/languages/${testData.importEnglish.id}"
    )
      .andPrettyPrint.andAssertThatJson.let { it ->
        it.node("name").isEqualTo("en")
        it.node("existingLanguageName").isEqualTo("English")
        it.node("importFileName").isEqualTo("multilang.json")
        it.node("totalCount").isEqualTo("6")
        it.node("conflictCount").isEqualTo("4")
      }
  }

  @Test
  fun `it paginates result`() {
    val testData = ImportTestData()
    testDataService.saveTestData(testData.root)

    loginAsUser(testData.root.data.userAccounts[0].self.username)

    performAuthGet("/v2/projects/${testData.project.id}/import/result?page=0&size=2")
      .andPrettyPrint.andAssertThatJson.node("_embedded.languages").isArray.isNotEmpty.hasSize(2)
  }

  @Test
  fun `it return correct translation data`() {
    val testData = ImportTestData()
    testDataService.saveTestData(testData.root)

    loginAsUser(testData.root.data.userAccounts[0].self.username)

    performAuthGet(
      "/v2/projects/${testData.project.id}" +
        "/import/result/languages/${testData.importEnglish.id}/translations?onlyConflicts=true"
    ).andIsOk
      .andPrettyPrint.andAssertThatJson.node("_embedded.translations").let { translations ->
        translations.isArray.isNotEmpty.hasSize(4)
        translations.node("[2]").let {
          it.node("id").isNotNull
          it.node("text").isEqualTo("Overridden")
          it.node("keyName").isEqualTo("what a key")
          it.node("keyId").isNotNull
          it.node("conflictId").isNotNull
          it.node("conflictText").isEqualTo("What a text")
          it.node("override").isEqualTo(false)
        }
      }
  }

  @Test
  fun `it searches for translation data`() {
    val testData = ImportTestData()
    testDataService.saveTestData(testData.root)

    loginAsUser(testData.root.data.userAccounts[0].self.username)

    performAuthGet(
      "/v2/projects/${testData.project.id}" +
        "/import/result/languages/${testData.importEnglish.id}" +
        "/translations?search=extraordinary"
    ).andIsOk
      .andPrettyPrint.andAssertThatJson.node("_embedded.translations").let { translations ->
        translations.isArray.isNotEmpty.hasSize(1)
        translations.node("[0]").let {
          it.node("keyName").isEqualTo("extraordinary key")
        }
      }

    performAuthGet(
      "/v2/projects/${testData.project.id}" +
        "/import/result/languages/${testData.importEnglish.id}" +
        "/translations?search=Imported"
    ).andIsOk
      .andPrettyPrint.andAssertThatJson.node("_embedded.translations").let { translations ->
        translations.isArray.isNotEmpty.hasSize(1)
        translations.node("[0]").let {
          it.node("text").isEqualTo("Imported text")
        }
      }
  }

  @Test
  fun `it pages translation data`() {
    val testData = ImportTestData()
    testDataService.saveTestData(testData.root)

    loginAsUser(testData.root.data.userAccounts[0].self.username)

    performAuthGet(
      "/v2/projects/${testData.project.id}" +
        "/import/result/languages/${testData.importEnglish.id}/translations?size=2"
    ).andIsOk
      .andPrettyPrint.andAssertThatJson.node("_embedded.translations").isArray.hasSize(2)
  }

  @Test
  fun `onlyConflict filter on translations works`() {
    val testData = ImportTestData()
    testDataService.saveTestData(testData.root)

    loginAsUser(testData.root.data.userAccounts[0].self.username)

    performAuthGet(
      "/v2/projects/${testData.project.id}" +
        "/import/result/languages/${testData.importEnglish.id}/translations?onlyConflicts=false"
    ).andIsOk
      .andPrettyPrint.andAssertThatJson.node("_embedded.translations").isArray.hasSize(6)

    performAuthGet(
      "/v2/projects/${testData.project.id}" +
        "/import/result/languages/${testData.importEnglish.id}/translations?onlyConflicts=true"
    ).andIsOk
      .andPrettyPrint.andAssertThatJson.node("_embedded.translations").isArray.hasSize(4)
  }

  @Test
  fun `onlyUnresolved filter on translations works`() {
    val testData = ImportTestData()
    val resolvedText = "Hello, I am resolved"

    testData {
      data.importFiles[0].addImportTranslation {

        conflict = testData.conflict
        this.resolve()
        key = data.importFiles[0].data.importKeys[0].self
        text = resolvedText
        language = testData.importEnglish
      }.self
    }

    testDataService.saveTestData(testData.root)
    loginAsUser(testData.root.data.userAccounts[0].self.username)

    performAuthGet(
      "/v2/projects/${testData.project.id}" +
        "/import/result/languages/${testData.importEnglish.id}/" +
        "translations?onlyConflicts=true"
    ).andIsOk
      .andPrettyPrint.andAssertThatJson.node("_embedded.translations").isArray.hasSize(5)

    performAuthGet(
      "/v2/projects/${testData.project.id}" +
        "/import/result/languages/${testData.importEnglish.id}/translations?onlyUnresolved=true"
    ).andIsOk
      .andPrettyPrint.andAssertThatJson.node("_embedded.translations").isArray.hasSize(4)
  }

  @Test
  fun `import is isolated`() {
    val testData = ImportTestData()
    testDataService.saveTestData(testData.root)
    loginAsUser(testData.root.data.userAccounts[0].self.username)

    performAuthGet("/v2/projects/${testData.project.id}/import/result").andIsOk

    val testData2 = ImportTestData()
    testData2.userAccount.username = "user2"
    testDataService.saveTestData(testData2.root)
    loginAsUser(testData2.root.data.userAccounts[0].self.username)

    performAuthGet("/v2/projects/${testData2.project.id}/import/result").andIsOk
      .andPrettyPrint.andAssertThatJson.node("_embedded.languages").let {
        it.isArray.hasSize(3)
        it.node("[0].totalCount").isEqualTo(6)
      }

    performAuthDelete("/v2/projects/${testData2.project.id}/import", null).andIsOk

    loginAsUser(testData.root.data.userAccounts[0].self.username)

    performAuthGet("/v2/projects/${testData.project.id}/import/result").andIsOk
      .andPrettyPrint.andAssertThatJson.node("_embedded.languages").let {
        it.isArray.hasSize(3)
        it.node("[0].totalCount").isEqualTo(6)
      }
  }

  @Test
  fun `it returns correct file issues`() {
    val testData = ImportTestData()
    testData.addManyFileIssues()
    testData.setAllResolved()
    testData.setAllOverride()
    testDataService.saveTestData(testData.root)
    val user = testData.root.data.userAccounts[0].self
    val projectId = testData.project.id
    val fileId = testData.importBuilder.data.importFiles[0].self.id
    loginAsUser(user.username)
    val path = "/v2/projects/$projectId/import/result/files/$fileId/issues"
    performAuthGet(path).andIsOk.andPrettyPrint.andAssertThatJson {
      node("page.totalElements").isEqualTo(204)
      node("page.size").isEqualTo(20)
      node("_embedded.importFileIssues[0].params").isEqualTo(
        """
               [{
                 "value" : "1",
                 "type" : "KEY_INDEX"
              }]
        """.trimIndent()
      )
    }
  }
}
