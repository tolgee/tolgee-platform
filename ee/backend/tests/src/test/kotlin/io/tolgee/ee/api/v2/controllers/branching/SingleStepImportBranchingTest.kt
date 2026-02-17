package io.tolgee.ee.api.v2.controllers.branching

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.constants.Feature
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.dataImport.SingleStepImportBranchTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import io.tolgee.util.performSingleStepImport
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.test.web.servlet.ResultActions

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
class SingleStepImportBranchingTest : ProjectAuthControllerTest("/v2/projects/") {
  @Value("classpath:import/simple.json")
  lateinit var simpleJson: Resource

  @Value("classpath:import/new.json")
  lateinit var newJson: Resource

  lateinit var testData: SingleStepImportBranchTestData

  @Autowired
  lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @BeforeEach
  fun setup() {
    testData = SingleStepImportBranchTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    projectSupplier = { testData.project }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `imports new keys to specified branch`() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.BRANCHING)
    performImport(
      listOf(Pair("en.json", simpleJson)),
      params = mapOf("branch" to testData.featureBranch.name),
    ).andIsOk

    executeInNewTransaction {
      val key = keyService.getAllByBranch(testData.project.id, "feature").find { it.name == "test" }
      key.assert.isNotNull
      key!!
        .translations
        .find { it.language.tag == "en" }!!
        .text.assert
        .isEqualTo("test")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `imports to default branch when no branch specified`() {
    performImport(
      listOf(Pair("en.json", simpleJson)),
    ).andIsOk

    executeInNewTransaction {
      val key = keyService.getAllByBranch(testData.project.id, "main").find { it.name == "test" }
      key.assert.isNotNull
      key!!
        .translations
        .find { it.language.tag == "en" }!!
        .text.assert
        .isEqualTo("test")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `keys imported to branch are not visible on default branch`() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.BRANCHING)
    performImport(
      listOf(Pair("en.json", simpleJson)),
      params = mapOf("branch" to testData.featureBranch.name),
    ).andIsOk

    executeInNewTransaction {
      val keysOnDefault = keyService.getAllByBranch(testData.project.id, "main")
      keysOnDefault.find { it.name == "test" }.assert.isNull()

      val keysOnFeature = keyService.getAllByBranch(testData.project.id, "feature")
      keysOnFeature.find { it.name == "test" }.assert.isNotNull
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `fails when branch specified but feature not enabled`() {
    enabledFeaturesProvider.forceEnabled = emptySet()
    performImport(
      listOf(Pair("en.json", simpleJson)),
      params = mapOf("branch" to testData.featureBranch.name),
    ).andIsBadRequest.andHasErrorMessage(Message.FEATURE_NOT_ENABLED)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `succeeds without branch when feature not enabled`() {
    enabledFeaturesProvider.forceEnabled = emptySet()
    performImport(
      listOf(Pair("en.json", simpleJson)),
    ).andIsOk
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `fails when non-existent branch specified`() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.BRANCHING)
    performImport(
      listOf(Pair("en.json", simpleJson)),
      params = mapOf("branch" to "non-existent"),
    ).andIsNotFound
  }

  private fun performImport(
    files: List<Pair<String, Resource>>?,
    params: Map<String, Any?> = mapOf(),
  ): ResultActions {
    return performSingleStepImport(mvc, testData.project.id, files, params)
  }
}
