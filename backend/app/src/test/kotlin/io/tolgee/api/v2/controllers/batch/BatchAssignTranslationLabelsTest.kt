package io.tolgee.api.v2.controllers.batch

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.fixtures.*
import io.tolgee.model.translation.Translation
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class BatchAssignTranslationLabelsTest : ProjectAuthControllerTest("/v2/projects/") {
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
  fun `it assigns labels to translations`() {
    val labelCount = 5
    val keyCount = 100
    val labels = testData.addLabels(labelCount)
    val keys = testData.addKeysWithLanguages(keyCount)

    batchJobTestBase.saveAndPrepare(this)

    val allKeyIds = keys.map { it.id }.toList()
    val allLanguageIds = testData.projectBuilder.data.languages.map { it.self.id }

    val keyIds = allKeyIds.take(50)
    val languageIds = allLanguageIds.take(2)
    val labelIds = labels.map { it.id }

    performProjectAuthPost(
      "start-batch-job/assign-translation-label",
      mapOf(
        "keyIds" to keyIds,
        "languageIds" to languageIds,
        "labelIds" to labelIds,
      ),
    ).andIsOk

    waitForNotThrowing(pollTime = 1000, timeout = 10000) {
      translationService.getTranslationsWithLabels(keyIds, languageIds).forEach { translation: Translation ->
        translation.labels.map { it.id }.containsAll(labelIds).assert.isTrue
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it validates missing labels`() {
    val keyCount = 50
    val keys = testData.addKeysWithLanguages(keyCount)
    batchJobTestBase.saveAndPrepare(this)

    val allKeyIds = keys.map { it.id }.toList()
    val keyIds = allKeyIds.take(10)
    val languageIds = testData.projectBuilder.data.languages.map { it.self.id }

    performProjectAuthPost(
      "start-batch-job/assign-translation-label",
      mapOf(
        "keyIds" to keyIds,
        "languageIds" to languageIds,
        "labelIds" to listOf(9999), // Non-existing label ID
      ),
    ).andIsNotFound
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it validates empty keys or languages`() {
    performProjectAuthPost(
      "start-batch-job/assign-translation-label",
      mapOf(
        "keyIds" to emptyList<Long>(),
        "languageIds" to emptyList<Long>(),
        "labelIds" to listOf(1, 2, 3),
      ),
    ).andIsBadRequest
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it validates empty label IDs`() {
    val keyCount = 50
    val keys = testData.addKeysWithLanguages(keyCount)
    batchJobTestBase.saveAndPrepare(this)

    val allKeyIds = keys.map { it.id }.toList()
    val keyIds = allKeyIds.take(10)
    val languageIds = testData.projectBuilder.data.languages.map { it.self.id }

    performProjectAuthPost(
      "start-batch-job/assign-translation-label",
      mapOf(
        "keyIds" to keyIds,
        "languageIds" to languageIds,
        "labelIds" to emptyList(),
      ),
    ).andIsBadRequest
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it unassigns labels from translations`() {
    val labelCount = 5
    val keyCount = 100
    val labels = testData.addLabels(labelCount)
    val keys = testData.addKeysWithLanguages(keyCount, labels)

    batchJobTestBase.saveAndPrepare(this)

    val allKeyIds = keys.map { it.id }.toList()
    val allLanguageIds = testData.projectBuilder.data.languages.map { it.self.id }

    val keyIds = allKeyIds.take(50)
    val languageIds = allLanguageIds.take(2)
    val unassignLabelsIds = labels.map { it.id }.takeLast(3)

    val assignedLabelsIds = labels.map { it.id }.take(2)

    performProjectAuthPost(
      "start-batch-job/unassign-translation-label",
      mapOf(
        "keyIds" to keyIds,
        "languageIds" to languageIds,
        "labelIds" to unassignLabelsIds,
      ),
    ).andIsOk

    waitForNotThrowing(pollTime = 1000, timeout = 10000) {
      translationService.getTranslationsWithLabels(keyIds, languageIds).forEach { translation: Translation ->
        translation.labels.map { it.id }.assert.doesNotContainSequence(unassignLabelsIds)
        translation.labels.map { it.id }.containsAll(assignedLabelsIds).assert.isTrue
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it validates unassigning missing labels`() {
    val keyCount = 50
    val keys = testData.addKeysWithLanguages(keyCount)
    batchJobTestBase.saveAndPrepare(this)

    val allKeyIds = keys.map { it.id }.toList()
    val keyIds = allKeyIds.take(10)
    val languageIds = testData.projectBuilder.data.languages.map { it.self.id }

    performProjectAuthPost(
      "start-batch-job/unassign-translation-label",
      mapOf(
        "keyIds" to keyIds,
        "languageIds" to languageIds,
        "labelIds" to listOf(9999), // Non-existing label ID
      ),
    ).andIsNotFound
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it validates unassigning empty keys or languages`() {
    performProjectAuthPost(
      "start-batch-job/unassign-translation-label",
      mapOf(
        "keyIds" to emptyList<Long>(),
        "languageIds" to emptyList<Long>(),
        "labelIds" to listOf(1, 2, 3),
      ),
    ).andIsBadRequest
  }

}

