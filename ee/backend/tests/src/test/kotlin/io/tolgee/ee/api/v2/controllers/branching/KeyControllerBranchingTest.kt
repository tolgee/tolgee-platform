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
import io.tolgee.fixtures.isValidId
import io.tolgee.fixtures.node
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
class KeyControllerBranchingTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: KeysTestData

  @Autowired
  lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @BeforeEach
  fun setup() {
    testData = KeysTestData()
    testData.addNBranchedKeys(5, "feature")
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    this.projectSupplier = { testData.project }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `imports keys to branch`() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.BRANCHING)
    projectSupplier = { testData.project }
    performProjectAuthPost(
      "keys/import?branch=dev",
      mapOf(
        "keys" to
          listOf(
            mapOf(
              "name" to "first_key",
              "translations" to
                mapOf("en" to "hello"),
              "description" to "description",
              "tags" to listOf("tag1", "tag2"),
            ),
            mapOf(
              "name" to "new_key",
              "description" to "description",
              "translations" to
                mapOf("en" to "hello friend"),
              "tags" to listOf("tag1", "tag2"),
            ),
          ),
      ),
    ).andIsOk

    executeInNewTransaction {
      val project = projectService.get(testData.project.id)
      project.keys
        .filter { it.branch?.name != "dev" }
        .size.assert
        .isEqualTo(8)

      val firstKey =
        project.keys.find {
          it.name == "first_key" && it.branch?.name == "dev"
        }
      firstKey!!
        .translations
        .find { it.language.tag == "en" }
        .assert
        .isNull()
      firstKey.keyMeta
        ?.description.assert
        .isNull()

      val key =
        project.keys.find {
          it.name == "new_key" && it.branch?.name == "dev"
        }
      key!!
        .keyMeta!!
        .description.assert
        .isEqualTo("description")

      key.assert.isNotNull()
      key.keyMeta!!
        .tags.assert
        .hasSize(2)
      key.translations
        .find { it.language.tag == "en" }!!
        .text.assert
        .isEqualTo("hello friend")
      key.branch.assert.isNotNull
      key.branch
        ?.name.assert
        .isEqualTo("dev")
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `returns all keys from branch`() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.BRANCHING)
    performProjectAuthGet("keys?branch=feature")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.keys") {
          isArray.hasSize(5)
          node("[0].id").isValidId
          node("[1].name").isEqualTo("branch_key_2")
          node("[1].description").isEqualTo("description of branched key")
          node("[2].namespace").isEqualTo("null")
        }
        node("page.totalElements").isNumber.isEqualTo(BigDecimal(5))
      }
    performProjectAuthGet("keys?page=1&size=2&branch=feature")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.keys") {
          isArray.hasSize(2)
          node("[0].id").isValidId
          node("[1].name").isEqualTo("branch_key_4")
          node("[1].namespace").isEqualTo("null")
        }
      }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `search returns keys from specified branch`() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.BRANCHING)
    performProjectAuthGet("keys/search?search=branch_key&languageTag=en&branch=feature")
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
      "keys/info?branch=feature",
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
    performProjectAuthGet("keys/search?search=branch_key&languageTag=en&branch=feature")
      .andPrettyPrint
      .andIsBadRequest
      .andHasErrorMessage(Message.FEATURE_NOT_ENABLED)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `info fails when branch parameter used but feature not enabled`() {
    enabledFeaturesProvider.forceEnabled = emptySet()
    performProjectAuthPost(
      "keys/info?branch=feature",
      mapOf(
        "keys" to listOf(mapOf("name" to "branch_key_1")),
        "languageTags" to listOf("en"),
      ),
    ).andPrettyPrint.andIsBadRequest.andHasErrorMessage(Message.FEATURE_NOT_ENABLED)
  }
}
