package io.tolgee.api.v2.controllers.v2ImportController

import io.tolgee.development.testDataBuilder.data.ImportTestData
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.Test

class V2ImportControllerApplicationTest : AuthorizedControllerTest() {
  @Test
  fun `it applies the import`() {
    val testData = ImportTestData()
    testData.setAllResolved()
    testData.setAllOverride()
    testDataService.saveTestData(testData.root)
    val user = testData.root.data.userAccounts[0].self
    val projectId = testData.project.id
    loginAsUser(user.username)
    val path = "/v2/projects/$projectId/import/apply"
    performAuthPut(path, null).andIsOk
    this.importService.find(projectId, user.id).let {
      assertThat(it).isNull()
    }
  }

  @Test
  fun `it applies the import with force override`() {
    val testData = ImportTestData()
    testDataService.saveTestData(testData.root)
    val user = testData.root.data.userAccounts[0].self
    val projectId = testData.project.id
    loginAsUser(user.username)
    val path = "/v2/projects/$projectId/import/apply?forceMode=OVERRIDE"
    performAuthPut(path, null).andIsOk
    this.importService.find(projectId, user.id).let {
      assertThat(it).isNull()
    }
  }

  @Test
  fun `it applies the import with force keep`() {
    val testData = ImportTestData()
    testDataService.saveTestData(testData.root)
    val user = testData.root.data.userAccounts[0].self
    val projectId = testData.project.id
    loginAsUser(user.username)
    val path = "/v2/projects/$projectId/import/apply?forceMode=KEEP"
    performAuthPut(path, null).andIsOk
  }
}
