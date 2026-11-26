package io.tolgee.api.v2.controllers.v2ImportController

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.dataImport.ImportTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.node
import io.tolgee.model.translation.Translation
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class V2ImportControllerResolvedConflictChangedTest : ProjectAuthControllerTest("/v2/projects/") {
  private var testData: ImportTestData? = null

  @AfterEach
  fun clear() {
    testData?.let { testDataService.cleanTestData(it.root) }
  }

  @Test
  fun `apply fails while the result shows every conflict resolved`() {
    val testData = ImportTestData()
    this.testData = testData
    testData.setAllResolved()
    testData.setAllOverride()
    testDataService.saveTestData(testData.root)
    loginAsUser(
      testData.root.data.userAccounts[0]
        .self.username,
    )
    val projectId = testData.project.id

    executeInNewTransaction {
      entityManager.find(Translation::class.java, testData.conflict.id).text = "changed by someone else"
    }

    performAuthGet("/v2/projects/$projectId/import/result")
      .andPrettyPrint
      .andAssertThatJson {
        node("_embedded.languages[0].conflictCount").isEqualTo("3")
        node("_embedded.languages[0].resolvedCount").isEqualTo("3")
      }

    performAuthPut("/v2/projects/$projectId/import/apply", null)
      .andIsBadRequest
      .andHasErrorMessage(Message.CONFLICT_IS_NOT_RESOLVED)
  }
}
