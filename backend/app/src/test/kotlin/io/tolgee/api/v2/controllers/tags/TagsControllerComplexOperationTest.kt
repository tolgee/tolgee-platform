package io.tolgee.api.v2.controllers.tags

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.TagsTestData
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TagsControllerComplexOperationTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: TagsTestData

  @BeforeEach
  fun setup() {
    testData = TagsTestData()
    testData.addNamespacedKey()
    projectSupplier = { testData.projectBuilder.self }
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `tag filtered by tag`() {
    performProjectAuthPut(
      "tag-complex",
      mapOf(
        "filterTag" to listOf("existing tag"),
        "tagFiltered" to listOf("new tag"),
        "tagOther" to listOf("other tag"),
      ),
    ).andIsOk

    assertKeyTags(
      mapOf(
        (null to "test key") to listOf("test", "existing tag", "existing tag 2", "new tag"),
        (null to "no tag key") to listOf("other tag"),
        (null to "existing tag key 2") to listOf("existing tag 2", "other tag"),
      ),
    )
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `tag filtered by tag not`() {
    performProjectAuthPut(
      "tag-complex",
      mapOf(
        "filterTagNot" to listOf("existing tag"),
        "tagOther" to listOf("other tag"),
        "tagFiltered" to listOf("new tag"),
      ),
    ).andIsOk

    assertKeyTags(
      mapOf(
        (null to "test key") to listOf("test", "existing tag", "existing tag 2", "other tag"),
        (null to "no tag key") to listOf("new tag"),
        (null to "existing tag key 2") to listOf("existing tag 2", "new tag"),
      ),
    )
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun untags() {
    performProjectAuthPut(
      "tag-complex",
      mapOf(
        "filterTag" to listOf("existing tag"),
        "untagFiltered" to listOf("existing tag"),
        "untagOther" to listOf("existing tag 2"),
      ),
    ).andIsOk

    assertKeyTags(
      mapOf(
        (null to "test key") to listOf("test", "existing tag 2"),
        (null to "no tag key") to listOf(),
        (null to "existing tag key 2") to listOf(),
      ),
    )
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `tag filtered by key`() {
    performProjectAuthPut(
      "tag-complex",
      mapOf(
        "filterKeys" to
          listOf(
            mapOf(
              "id" to testData.existingTagKey.id,
            ),
            mapOf(
              "name" to testData.noTagKey.name,
              "namespace" to null,
            ),
            mapOf(
              "name" to "namespaced key",
              "namespace" to "namespace",
            ),
          ),
        "tagFiltered" to listOf("new tag"),
        "tagOther" to listOf("other tag"),
      ),
    ).andIsOk

    assertKeyTags(
      mapOf(
        (null to "test key") to listOf("test", "existing tag", "existing tag 2", "new tag"),
        (null to "no tag key") to listOf("new tag"),
        (null to "existing tag key 2") to listOf("existing tag 2", "other tag"),
        ("namespace" to "namespaced key") to listOf("existing tag", "new tag"),
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
