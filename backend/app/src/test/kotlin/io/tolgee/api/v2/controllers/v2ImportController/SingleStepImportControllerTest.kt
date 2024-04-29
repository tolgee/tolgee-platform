package io.tolgee.api.v2.controllers.v2ImportController

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import io.tolgee.util.performImport
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

  lateinit var testData: BaseTestData

  @BeforeEach
  fun beforeEach() {
    testData = BaseTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    projectSupplier = { testData.project }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `import simple json`() {
    val fileName = "en.json"
    performImport(projectId = testData.project.id, listOf(Pair(fileName, simpleJson)))
    keyService.get(projectId = project.id, name = "test", namespace = null)
      .translations.single().text.assert.isEqualTo("test")
  }

  private fun performImport(
    projectId: Long,
    files: List<Pair<String, Resource>>?,
    params: Map<String, Any?> = mapOf(),
  ): ResultActions {
    return performSingleStepImport(mvc, projectId, files, params)
  }
}
