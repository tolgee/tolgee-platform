package io.tolgee.ee.api.v2.controllers.branching

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.TranslationsTestData
import io.tolgee.dtos.request.translation.SetTranslationsWithKeyDto
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.isValidId
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
class TranslationsControllerModificationBranchingTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: TranslationsTestData

  @Autowired
  lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @BeforeEach
  fun setup() {
    testData = TranslationsTestData()
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `sets translations for existing key in branch`() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.BRANCHING)

    saveTestData()
    performProjectAuthPut(
      "/translations",
      SetTranslationsWithKeyDto(
        "branch key",
        null,
        mutableMapOf("en" to "English branch key"),
        branch = "test-branch",
      ),
    ).andIsOk
      .andAssertThatJson {
        node("translations.en.text").isEqualTo("English branch key")
        node("translations.en.id").isValidId
        node("keyId").isValidId
        node("keyName").isEqualTo("branch key")
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `cannot set translations for key in default branch with different branch provided`() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.BRANCHING)
    saveTestData()
    performProjectAuthPut(
      "/translations",
      SetTranslationsWithKeyDto(
        "A key",
        null,
        mutableMapOf("en" to "Cannot do that"),
        branch = "test-branch",
      ),
    ).andIsNotFound
  }

  private fun saveTestData() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    this.projectSupplier = { testData.project }
  }
}
