package io.tolgee.ee.api.v2.controllers.branching

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.constants.Feature
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.dataImport.SingleStepImportBranchTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
class SingleStepImportResolvableBranchingTest : ProjectAuthControllerTest("/v2/projects/") {
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
    performProjectAuthPost(
      "single-step-import-resolvable",
      mapOf(
        "branch" to testData.featureBranch.name,
        "keys" to
          listOf(
            mapOf(
              "name" to "new_key",
              "translations" to
                mapOf(
                  "en" to
                    mapOf(
                      "text" to "hello",
                      "resolution" to "OVERRIDE",
                    ),
                ),
            ),
          ),
      ),
    ).andIsOk

    executeInNewTransaction {
      val key = keyService.getAllByBranch(testData.project.id, "feature").find { it.name == "new_key" }
      key.assert.isNotNull
      key!!
        .translations
        .find { it.language.tag == "en" }!!
        .text.assert
        .isEqualTo("hello")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `imports to default branch when no branch specified`() {
    performProjectAuthPost(
      "single-step-import-resolvable",
      mapOf(
        "keys" to
          listOf(
            mapOf(
              "name" to "new_key",
              "translations" to
                mapOf(
                  "en" to
                    mapOf(
                      "text" to "hello",
                      "resolution" to "OVERRIDE",
                    ),
                ),
            ),
          ),
      ),
    ).andIsOk

    executeInNewTransaction {
      val key = keyService.getAllByBranch(testData.project.id, "main").find { it.name == "new_key" }
      key.assert.isNotNull
      key!!
        .translations
        .find { it.language.tag == "en" }!!
        .text.assert
        .isEqualTo("hello")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `keys imported to branch are not visible on default branch`() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.BRANCHING)
    performProjectAuthPost(
      "single-step-import-resolvable",
      mapOf(
        "branch" to testData.featureBranch.name,
        "keys" to
          listOf(
            mapOf(
              "name" to "branch_only_key",
              "translations" to
                mapOf(
                  "en" to
                    mapOf(
                      "text" to "on branch",
                      "resolution" to "OVERRIDE",
                    ),
                ),
            ),
          ),
      ),
    ).andIsOk

    executeInNewTransaction {
      val keysOnDefault = keyService.getAllByBranch(testData.project.id, "main")
      keysOnDefault.find { it.name == "branch_only_key" }.assert.isNull()

      val keysOnFeature = keyService.getAllByBranch(testData.project.id, "feature")
      keysOnFeature.find { it.name == "branch_only_key" }.assert.isNotNull
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `fails when branch specified but feature not enabled`() {
    enabledFeaturesProvider.forceEnabled = emptySet()
    performProjectAuthPost(
      "single-step-import-resolvable",
      mapOf(
        "branch" to testData.featureBranch.name,
        "keys" to
          listOf(
            mapOf(
              "name" to "some_key",
              "translations" to
                mapOf(
                  "en" to
                    mapOf(
                      "text" to "hello",
                      "resolution" to "OVERRIDE",
                    ),
                ),
            ),
          ),
      ),
    ).andIsBadRequest.andHasErrorMessage(Message.FEATURE_NOT_ENABLED)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `succeeds without branch when feature not enabled`() {
    enabledFeaturesProvider.forceEnabled = emptySet()
    performProjectAuthPost(
      "single-step-import-resolvable",
      mapOf(
        "keys" to
          listOf(
            mapOf(
              "name" to "some_key",
              "translations" to
                mapOf(
                  "en" to
                    mapOf(
                      "text" to "hello",
                      "resolution" to "OVERRIDE",
                    ),
                ),
            ),
          ),
      ),
    ).andIsOk
  }
}
