package io.tolgee.ee.api.v2.controllers.branching

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.constants.Feature
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.KeysTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.node
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
class KeyControllerBranchingTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: KeysTestData

  @Autowired
  lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @BeforeEach
  fun setup() {
    testData = KeysTestData()
    testData.addNBranchedKeys(5, "feature-branch")
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    this.projectSupplier = { testData.project }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `search returns keys from specified branch`() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.BRANCHING)
    performProjectAuthGet("keys/search?search=branch_key&languageTag=en&branch=feature-branch")
      .andPrettyPrint
      .andIsOk
      .andAssertThatJson {
        node("_embedded.keys") {
          isArray.hasSize(5)
          node("[0].name").isEqualTo("branch_key_1")
        }
      }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `search returns only default branch keys when no branch specified`() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.BRANCHING)
    performProjectAuthGet("keys/search?search=first&languageTag=en")
      .andPrettyPrint
      .andIsOk
      .andAssertThatJson {
        node("_embedded.keys") {
          isArray.hasSize(1)
          node("[0].name").isEqualTo("first_key")
        }
      }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `search does not return branched keys without branch parameter`() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.BRANCHING)
    performProjectAuthGet("keys/search?search=branch_key&languageTag=en")
      .andPrettyPrint
      .andIsOk
      .andAssertThatJson {
        node("_embedded").isAbsent()
      }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `info returns keys from specified branch`() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.BRANCHING)
    performProjectAuthPost(
      "keys/info?branch=feature-branch",
      mapOf(
        "keys" to listOf(mapOf("name" to "branch_key_1")),
        "languageTags" to listOf("en"),
      ),
    ).andPrettyPrint.andIsOk.andAssertThatJson {
      node("_embedded.keys") {
        isArray.hasSize(1)
        node("[0].name").isEqualTo("branch_key_1")
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `info does not return branched keys without branch parameter`() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.BRANCHING)
    performProjectAuthPost(
      "keys/info",
      mapOf(
        "keys" to listOf(mapOf("name" to "branch_key_1")),
        "languageTags" to listOf("en"),
      ),
    ).andPrettyPrint.andIsOk.andAssertThatJson {
      node("_embedded").isAbsent()
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `info returns default branch keys when no branch specified`() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.BRANCHING)
    performProjectAuthPost(
      "keys/info",
      mapOf(
        "keys" to listOf(mapOf("name" to "first_key")),
        "languageTags" to listOf("en"),
      ),
    ).andPrettyPrint.andIsOk.andAssertThatJson {
      node("_embedded.keys") {
        isArray.hasSize(1)
        node("[0].name").isEqualTo("first_key")
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `search fails when branch parameter used but feature not enabled`() {
    enabledFeaturesProvider.forceEnabled = emptySet()
    performProjectAuthGet("keys/search?search=branch_key&languageTag=en&branch=feature-branch")
      .andPrettyPrint
      .andIsBadRequest
      .andHasErrorMessage(Message.FEATURE_NOT_ENABLED)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `info fails when branch parameter used but feature not enabled`() {
    enabledFeaturesProvider.forceEnabled = emptySet()
    performProjectAuthPost(
      "keys/info?branch=feature-branch",
      mapOf(
        "keys" to listOf(mapOf("name" to "branch_key_1")),
        "languageTags" to listOf("en"),
      ),
    ).andPrettyPrint.andIsBadRequest.andHasErrorMessage(Message.FEATURE_NOT_ENABLED)
  }
}
