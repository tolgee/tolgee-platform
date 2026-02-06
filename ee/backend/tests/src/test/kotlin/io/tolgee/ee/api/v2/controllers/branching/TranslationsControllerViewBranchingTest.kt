package io.tolgee.ee.api.v2.controllers.branching

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.TranslationsTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.node
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
class TranslationsControllerViewBranchingTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: TranslationsTestData

  @Autowired
  lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @BeforeEach
  fun setup() {
    testData = TranslationsTestData()
    this.projectSupplier = { testData.project }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `return translations from featured branch only`() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.BRANCHING)
    testData.generateBranchedData(10)
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    performProjectAuthGet("/translations?sort=id&branch=feature-branch").andPrettyPrint.andIsOk.andAssertThatJson {
      // 10 keys from the feature branch should be returned
      node("_embedded.keys").isArray.hasSize(10)
      node("_embedded.keys[0].keyName").isEqualTo("key from branch feature-branch 1")
      node("_embedded.keys[0].translations.en") {
        node("text").isEqualTo("I am key 1's english translation from branch feature-branch.")
      }
      node("_embedded.keys[1].translations.de") {
        node("text").isEqualTo("I am key 2's german translation from branch feature-branch.")
      }
    }
  }

  /**
   * Edge-case testing returning correct translations if there is soft-deleted branch with same name as active branch
   * (deleted is in the process of hard-deleting)
   */
  @Test
  @ProjectJWTAuthTestMethod
  fun `return translations from active branch only`() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.BRANCHING)
    testData.generateBranchedData(10)
    testData.addDeletedBranch()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    performProjectAuthGet("/translations?sort=id&branch=feature-branch").andPrettyPrint.andIsOk.andAssertThatJson {
      // 10 keys from feature-branch (translation from soft-deleted feature-branch is ignored)
      node("_embedded.keys").isArray.hasSize(10)
    }
  }
}
