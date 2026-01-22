package io.tolgee.api.v2.controllers.tags

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
import kotlin.time.measureTime

class TagsControllerComplexOperationTest : ProjectAuthControllerTest("/v2/projects/") {
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
  fun `tag filtered by tag`() {
    saveAndPrepare()
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
        // branched key keeps untouched
        (null to "branch key") to listOf("existing tag"),
      ),
    )
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `filterTag works with wildcard filtered by tag`() {
    saveAndPrepare()
    performProjectAuthPut(
      "tag-complex",
      mapOf(
        "filterTag" to listOf("existing*"),
        "tagFiltered" to listOf("new tag"),
        "tagOther" to listOf("other tag"),
      ),
    ).andIsOk

    assertKeyTags(
      mapOf(
        (null to "test key") to listOf("test", "existing tag", "existing tag 2", "new tag"),
        (null to "no tag key") to listOf("other tag"),
        (null to "existing tag key 2") to listOf("existing tag 2", "new tag"),
        (null to "branch key") to listOf("existing tag"),
      ),
    )
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `tag filtered by tag not`() {
    saveAndPrepare()
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
  fun `filterTagNot works with wildcard filtered by tag`() {
    saveAndPrepare()
    performProjectAuthPut(
      "tag-complex",
      mapOf(
        "filterTagNot" to listOf("existing*"),
        "tagOther" to listOf("other tag"),
        "tagFiltered" to listOf("new tag"),
      ),
    ).andIsOk

    assertKeyTags(
      mapOf(
        (null to "test key") to listOf("test", "existing tag", "existing tag 2", "other tag"),
        (null to "no tag key") to listOf("new tag"),
        (null to "existing tag key 2") to listOf("existing tag 2", "other tag"),
      ),
    )
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun untags() {
    saveAndPrepare()
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
        (null to "branch key") to listOf("existing tag"),
      ),
    )
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `untags with wildcards`() {
    saveAndPrepare()
    performProjectAuthPut(
      "tag-complex",
      mapOf(
        "filterTag" to listOf("existing tag"),
        "untagFiltered" to listOf("existing*"),
        "untagOther" to listOf("ex*2"),
      ),
    ).andIsOk

    assertKeyTags(
      mapOf(
        (null to "test key") to listOf("test"),
        (null to "no tag key") to listOf(),
        (null to "existing tag key 2") to listOf(),
        (null to "branch key") to listOf("existing tag"),
      ),
    )
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `tag filtered by key`() {
    saveAndPrepare()
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

  @Test
  @ProjectJWTAuthTestMethod
  fun `tag filtered by key not`() {
    saveAndPrepare()
    performProjectAuthPut(
      "tag-complex",
      mapOf(
        "filterKeysNot" to
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
        "tagFiltered" to listOf("other tag"),
        "tagOther" to listOf("new tag"),
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

  @Test
  @ProjectJWTAuthTestMethod
  fun `it doesnt take forever`() {
    testData.generateVeryLotOfData()
    saveAndPrepare()

    val time =
      measureTime {
        performProjectAuthPut(
          "tag-complex",
          mapOf(
            "filterTag" to listOf("tag from*"),
            "tagFiltered" to listOf("new tag"),
            "tagOther" to listOf("other tag"),
          ),
        ).andIsOk
      }

    time.inWholeSeconds.assert.isLessThan(30)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `works when nothing matched`() {
    saveAndPrepare()

    performProjectAuthPut(
      "tag-complex",
      mapOf(
        "filterTag" to listOf("oh no it doesnt exist"),
        "tagFiltered" to listOf("new tag"),
        "tagOther" to listOf("other tag"),
      ),
    ).andIsOk

    assertKeyTags(
      mapOf(
        (null to "test key") to listOf("test", "existing tag", "existing tag 2", "other tag"),
        (null to "no tag key") to listOf("other tag"),
        (null to "existing tag key 2") to listOf("existing tag 2", "other tag"),
        ("namespace" to "namespaced key") to listOf("existing tag", "other tag"),
      ),
    )
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `works with branch - tag filtered within branch`() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.BRANCHING)
    saveAndPrepare()

    performProjectAuthPut(
      "tag-complex?branch=feature",
      mapOf(
        "filterTag" to listOf("existing tag"),
        "tagFiltered" to listOf("branch only"),
      ),
    ).andIsOk

    assertKeyTags(
      mapOf(
        (null to "branch key") to listOf("existing tag", "branch only"),
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
        (null to "branch key") to listOf("existing tag", "branch only"),
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
