package io.tolgee.ee.api.v2.controllers.branching

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.TagsTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
class TagsControllerComplexOperationBranchingTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: TagsTestData

  @Autowired
  lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @BeforeEach
  fun setup() {
    testData = TagsTestData()
    testData.addNamespacedKey()
    projectSupplier = { testData.projectBuilder.self }
  }

  private fun saveAndPrepare() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `works with branch - tag filtered within branch`() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.BRANCHING)
    saveAndPrepare()

    performProjectAuthPut(
      "tag-complex?branch=feature",
      mapOf(
        "filterTag" to listOf("existing tag 2"),
        "tagFiltered" to listOf("branch only"),
      ),
    ).andIsOk

    assertKeyTags(
      mapOf(
        (null to "branch key") to listOf("existing tag 2", "branch only"),
        // others keep untouched
        (null to "test key") to listOf("test", "existing tag", "existing tag 2"),
        ("namespace" to "namespaced key") to listOf("existing tag"),
        (null to "existing tag key 2") to listOf("existing tag 2"),
      ),
    )
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `works with branch - tag other within branch`() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.BRANCHING)
    saveAndPrepare()

    performProjectAuthPut(
      "tag-complex?branch=feature",
      mapOf(
        "filterTag" to listOf("oh no it doesnt exist"),
        "tagOther" to listOf("branch only"),
      ),
    ).andIsOk

    assertKeyTags(
      mapOf(
        (null to "branch key") to listOf("existing tag 2", "branch only"),
        // others keep untouched
        (null to "test key") to listOf("test", "existing tag", "existing tag 2"),
        ("namespace" to "namespaced key") to listOf("existing tag"),
        (null to "existing tag key 2") to listOf("existing tag 2"),
        (null to "no tag key") to listOf(),
      ),
    )
  }

  fun assertKeyTags(map: Map<Pair<String?, String>, List<String>>) {
    executeInNewTransaction {
      val projectKeys = projectService.get(project.id).keys
      map.forEach { (namespaceAndKey, tags) ->
        val key = projectKeys.find { it.name == namespaceAndKey.second && it.namespace?.name == namespaceAndKey.first }
        (key?.keyMeta?.tags?.map { it.name } ?: emptySet()).assert.containsExactlyInAnyOrder(*tags.toTypedArray())
      }
    }
  }
}
