package io.tolgee.activity

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.activity.rootActivity.KeyActivityTreeDefinitionItem
import io.tolgee.activity.rootActivity.RootActivityProvider
import io.tolgee.development.testDataBuilder.data.dataImport.ImportTestData
import io.tolgee.fixtures.andIsOk
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Pageable

class RootActivityProviderTest : ProjectAuthControllerTest("/v2/projects/") {
  @Test
  fun `it applies the import`() {
    importData()
    val latestRevisionId = getLatestRevisionId()
    val items =
      RootActivityProvider(
        applicationContext,
        latestRevisionId!!,
        KeyActivityTreeDefinitionItem,
        Pageable.ofSize(100),
      ).provide()
    items
  }

  private fun getLatestRevisionId(): Long? {
    return entityManager.createQuery(
      "select max(r.id) from ActivityRevision r",
      Long::class.java,
    ).singleResult
  }

  private fun importData() {
    val testData = ImportTestData()
    testData.addFilesWithNamespaces().importFrenchInNs.existingLanguage = testData.french
    testData.addKeyMetadata()
    testData.setAllResolved()
    testData.setAllOverride()
    testDataService.saveTestData(testData.root)
    val user = testData.root.data.userAccounts[0].self
    val projectId = testData.project.id
    loginAsUser(user.username)
    val path = "/v2/projects/$projectId/import/apply"
    performAuthPut(path, null).andIsOk
  }
}
