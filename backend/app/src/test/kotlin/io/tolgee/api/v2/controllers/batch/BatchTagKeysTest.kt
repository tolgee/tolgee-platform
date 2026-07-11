package io.tolgee.api.v2.controllers.batch

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.config.BatchJobBaseConfiguration
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import

@Import(BatchJobBaseConfiguration::class)
class BatchTagKeysTest : ProjectAuthControllerTest("/v2/projects/") {
  @Autowired
  lateinit var batchJobTestBase: BatchJobTestBase

  @BeforeEach
  fun setup() {
    batchJobTestBase.setup()
  }

  val testData
    get() = batchJobTestBase.testData

  @Test
  @ProjectJWTAuthTestMethod
  fun `it validates tag length`() {
    performProjectAuthPost(
      "start-batch-job/tag-keys",
      mapOf(
        "keyIds" to listOf(1),
        "tags" to listOf("a".repeat(101)),
      ),
    ).andIsBadRequest.andPrettyPrint
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it tags keys`() {
    val keyCount = 1000
    val keys = testData.addTagKeysData(keyCount)
    batchJobTestBase.saveAndPrepare(this)

    val allKeyIds = keys.map { it.id }.toList()
    val keyIds = allKeyIds.take(500)
    val newTags = listOf("tag1", "tag3", "a-tag", "b-tag")

    performProjectAuthPost(
      "start-batch-job/tag-keys",
      mapOf(
        "keyIds" to keyIds,
        "tags" to newTags,
      ),
    ).andIsOk

    waitForNotThrowing(pollTime = 1000, timeout = 10000) {
      val all = keyService.getKeysWithTagsById(testData.project.id, keyIds)
      all.assert.hasSize(keyIds.size)
      all
        .count {
          it.keyMeta
            ?.tags
            ?.map { it.name }
            ?.containsAll(newTags) == true
        }.assert
        .isEqualTo(keyIds.size)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it untags keys`() {
    val keyCount = 1000
    val keys = testData.addTagKeysData(keyCount)
    batchJobTestBase.saveAndPrepare(this)

    val allKeyIds = keys.map { it.id }.toList()
    val keyIds = allKeyIds.take(300)
    val tagsToRemove = listOf("tag1", "a-tag", "b-tag")

    performProjectAuthPost(
      "start-batch-job/untag-keys",
      mapOf(
        "keyIds" to keyIds,
        "tags" to tagsToRemove,
      ),
    ).andIsOk

    waitForNotThrowing(pollTime = 1000, timeout = 10000) {
      val all = keyService.getKeysWithTagsById(testData.project.id, keyIds)
      all.assert.hasSize(keyIds.size)
      all
        .count {
          it.keyMeta
            ?.tags
            ?.map { it.name }
            ?.any { tagsToRemove.contains(it) } == false &&
            it.keyMeta
              ?.tags
              ?.map { it.name }
              ?.contains("tag3") == true
        }.assert
        .isEqualTo(keyIds.size)
    }

    keyService.get(testData.projectBuilder.self.id, "a-key", null)
    performProjectAuthPost(
      "start-batch-job/untag-keys",
      mapOf(
        "keyIds" to keyIds,
        "tags" to listOf("a-tag"),
      ),
    ).andIsOk
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it deletes tags when not used`() {
    val keyCount = 1000
    testData.addTagKeysData(keyCount)
    batchJobTestBase.saveAndPrepare(this)

    val aKeyId = keyService.get(testData.projectBuilder.self.id, "a-key", null).id
    performProjectAuthPost(
      "start-batch-job/untag-keys",
      mapOf(
        "keyIds" to listOf(aKeyId),
        "tags" to listOf("a-tag"),
      ),
    ).andIsOk
    waitForNotThrowing { tagService.find(testData.projectBuilder.self, "a-tag").assert.isNull() }
  }
}
