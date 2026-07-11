package io.tolgee.api.v2.controllers.v2ImportController

import io.tolgee.development.testDataBuilder.data.dataImport.ImportTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.node
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.apache.commons.lang3.time.DateUtils
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Date

class V2ImportControllerResultTest : AuthorizedControllerTest() {
  @BeforeEach
  fun setup() {
    setForcedDate(Date())
  }

  @AfterEach
  fun clear() {
    clearForcedDate()
  }

  @Test
  fun `it returns correct result data`() {
    val testData = ImportTestData()
    testDataService.saveTestData(testData.root)

    loginAsUser(
      testData.root.data.userAccounts[0]
        .self.username,
    )

    performAuthGet("/v2/projects/${testData.project.id}/import/result")
      .andPrettyPrint
      .andAssertThatJson {
        node("_embedded.languages") {
          isArray.isNotEmpty
          node("[0]") {
            node("name").isEqualTo("en")
            node("existingLanguageName").isEqualTo("English")
            node("importFileName").isEqualTo("multilang.json")
            node("totalCount").isEqualTo("5")
            node("conflictCount").isEqualTo("3")
          }
        }
      }
  }

  @Test
  fun `it removes expired import`() {
    val testData = ImportTestData()
    testDataService.saveTestData(testData.root)

    setForcedDate(DateUtils.addHours(Date(), 2))

    loginAsUser(
      testData.root.data.userAccounts[0]
        .self.username,
    )

    performAuthGet("/v2/projects/${testData.project.id}/import/result")
      .andIsNotFound

    val import = importService.find(testData.project.id, testData.userAccount.id)
    assertThat(import).isNull()
  }

  @Test
  fun `it returns correct specific language`() {
    val testData = ImportTestData()
    testDataService.saveTestData(testData.root)

    loginAsUser(
      testData.root.data.userAccounts[0]
        .self.username,
    )

    performAuthGet(
      "/v2/projects/${testData.project.id}" +
        "/import/result/languages/${testData.importEnglish.id}",
    ).andPrettyPrint
      .andAssertThatJson {
        node("name").isEqualTo("en")
        node("existingLanguageName").isEqualTo("English")
        node("importFileName").isEqualTo("multilang.json")
        node("totalCount").isEqualTo("5")
        node("conflictCount").isEqualTo("3")
      }
  }

  @Test
  fun `it paginates result`() {
    val testData = ImportTestData()
    testDataService.saveTestData(testData.root)

    loginAsUser(
      testData.root.data.userAccounts[0]
        .self.username,
    )

    performAuthGet("/v2/projects/${testData.project.id}/import/result?page=0&size=2")
      .andPrettyPrint
      .andAssertThatJson { node("_embedded.languages").isArray.isNotEmpty.hasSize(2) }
  }

  @Test
  fun `it return correct translation data (all)`() {
    val testData = ImportTestData()
    testData.addPluralImport()
    testDataService.saveTestData(testData.root)
    loginAsUser(
      testData.root.data.userAccounts[0]
        .self.username,
    )
    performAuthGet(
      "/v2/projects/${testData.project.id}" +
        "/import/result/languages/${testData.importEnglish.id}/translations",
    ).andIsOk
      .andPrettyPrint
      .andAssertThatJson {
        node("_embedded.translations") {
          isArray.isNotEmpty.hasSize(6)
          node("[1]") {
            node("id").isNotNull
            node("keyName").isEqualTo("plural key")
            node("keyId").isNotNull
            node("isPlural").isEqualTo(false)
            node("existingKeyIsPlural").isEqualTo(true)
          }
        }
      }
  }

  @Test
  fun `it return correct translation data (only conflicts)`() {
    val testData = ImportTestData()
    testData.addPluralImport()
    testDataService.saveTestData(testData.root)
    loginAsUser(
      testData.root.data.userAccounts[0]
        .self.username,
    )
    performAuthGet(
      "/v2/projects/${testData.project.id}" +
        "/import/result/languages/${testData.importEnglish.id}/translations?onlyConflicts=true",
    ).andIsOk
      .andPrettyPrint
      .andAssertThatJson {
        node("_embedded.translations") {
          isArray.isNotEmpty.hasSize(3)
          node("[1]") {
            node("id").isNotNull
            node("text").isEqualTo("Overridden")
            node("keyName").isEqualTo("what a key")
            node("keyId").isNotNull
            node("conflictId").isNotNull
            node("conflictText").isEqualTo("What a text")
            node("override").isEqualTo(false)
            node("isPlural").isEqualTo(false)
            node("existingKeyIsPlural").isEqualTo(false)
            node("keyDescription").isEqualTo("This is a key")
          }
        }
      }
  }

  @Test
  fun `it searches for translation data`() {
    val testData = ImportTestData()
    testDataService.saveTestData(testData.root)

    loginAsUser(
      testData.root.data.userAccounts[0]
        .self.username,
    )

    performAuthGet(
      "/v2/projects/${testData.project.id}" +
        "/import/result/languages/${testData.importEnglish.id}" +
        "/translations?search=extraordinary",
    ).andIsOk
      .andPrettyPrint
      .andAssertThatJson {
        node("_embedded.translations") {
          isArray.isNotEmpty.hasSize(1)
          node("[0]") {
            node("keyName").isEqualTo("extraordinary key")
          }
        }
      }

    performAuthGet(
      "/v2/projects/${testData.project.id}" +
        "/import/result/languages/${testData.importEnglish.id}" +
        "/translations?search=Imported",
    ).andIsOk
      .andPrettyPrint
      .andAssertThatJson {
        node("_embedded.translations") {
          isArray.isNotEmpty.hasSize(1)
          node("[0]") {
            node("text").isEqualTo("Imported text")
          }
        }
      }
  }

  @Test
  fun `it pages translation data`() {
    val testData = ImportTestData()
    testDataService.saveTestData(testData.root)

    loginAsUser(
      testData.root.data.userAccounts[0]
        .self.username,
    )

    performAuthGet(
      "/v2/projects/${testData.project.id}" +
        "/import/result/languages/${testData.importEnglish.id}/translations?size=2",
    ).andIsOk
      .andPrettyPrint
      .andAssertThatJson { node("_embedded.translations").isArray.hasSize(2) }
  }

  @Test
  fun `onlyConflict filter on translations works`() {
    val testData = ImportTestData()
    testDataService.saveTestData(testData.root)

    loginAsUser(
      testData.root.data.userAccounts[0]
        .self.username,
    )

    performAuthGet(
      "/v2/projects/${testData.project.id}" +
        "/import/result/languages/${testData.importEnglish.id}/translations?onlyConflicts=false",
    ).andIsOk
      .andPrettyPrint
      .andAssertThatJson { node("_embedded.translations").isArray.hasSize(5) }

    performAuthGet(
      "/v2/projects/${testData.project.id}" +
        "/import/result/languages/${testData.importEnglish.id}/translations?onlyConflicts=true",
    ).andIsOk
      .andPrettyPrint
      .andAssertThatJson { node("_embedded.translations").isArray.hasSize(3) }
  }

  @Test
  fun `onlyUnresolved filter on translations works`() {
    val testData = ImportTestData()
    testData.translationWithConflict.resolve()
    testDataService.saveTestData(testData.root)
    loginAsUser(
      testData.root.data.userAccounts[0]
        .self.username,
    )

    performAuthGet(
      "/v2/projects/${testData.project.id}" +
        "/import/result/languages/${testData.importEnglish.id}/" +
        "translations?onlyConflicts=true",
    ).andIsOk
      .andPrettyPrint
      .andAssertThatJson { node("_embedded.translations").isArray.hasSize(3) }

    performAuthGet(
      "/v2/projects/${testData.project.id}" +
        "/import/result/languages/${testData.importEnglish.id}/translations?onlyUnresolved=true",
    ).andIsOk
      .andPrettyPrint
      .andAssertThatJson { node("_embedded.translations").isArray.hasSize(2) }
  }

  @Test
  fun `import is isolated`() {
    val testData = ImportTestData()
    testDataService.saveTestData(testData.root)
    loginAsUser(
      testData.root.data.userAccounts[0]
        .self.username,
    )

    performAuthGet("/v2/projects/${testData.project.id}/import/result").andIsOk

    val testData2 = ImportTestData()
    testData2.userAccount.username = "user2"
    testDataService.saveTestData(testData2.root)
    loginAsUser(
      testData2.root.data.userAccounts[0]
        .self.username,
    )

    performAuthGet("/v2/projects/${testData2.project.id}/import/result")
      .andIsOk
      .andPrettyPrint
      .andAssertThatJson {
        node("_embedded.languages") {
          isArray.hasSize(3)
          node("[0].totalCount").isEqualTo(5)
        }
      }

    performAuthDelete("/v2/projects/${testData2.project.id}/import", null).andIsOk

    loginAsUser(
      testData.root.data.userAccounts[0]
        .self.username,
    )

    performAuthGet("/v2/projects/${testData.project.id}/import/result")
      .andIsOk
      .andPrettyPrint
      .andAssertThatJson {
        node("_embedded.languages") {
          isArray.hasSize(3)
          node("[0].totalCount").isEqualTo(5)
        }
      }
  }

  @Test
  fun `it returns correct file issues`() {
    val testData = ImportTestData()
    testData.addManyFileIssues()
    testData.setAllResolved()
    testData.setAllOverride()
    testDataService.saveTestData(testData.root)
    val user =
      testData.root.data.userAccounts[0]
        .self
    val projectId = testData.project.id
    val fileId =
      testData.importBuilder.data.importFiles[0]
        .self.id
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
        """.trimIndent(),
      )
    }
  }
}
