package io.tolgee.api.v2.controllers.batch

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.config.BatchJobBaseConfiguration
import io.tolgee.constants.Message
import io.tolgee.exceptions.NotFoundException
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.waitFor
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.service.key.TagService
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean

@Import(BatchJobBaseConfiguration::class)
class BatchTagKeysTest : ProjectAuthControllerTest("/v2/projects/") {
  @Autowired
  lateinit var batchJobTestBase: BatchJobTestBase

  @MockitoSpyBean
  @Autowired
  lateinit var spiedTagService: TagService

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

  @Test
  @ProjectJWTAuthTestMethod
  fun `a failed job leaves no new tag behind`() {
    val keys = testData.addTagKeysData(10)
    batchJobTestBase.saveAndPrepare(this)
    doThrow(NotFoundException(Message.KEY_NOT_FOUND)).whenever(spiedTagService).tagKeysById(any(), any())

    performProjectAuthPost(
      "start-batch-job/tag-keys",
      mapOf(
        "keyIds" to keys.map { it.id },
        "tags" to listOf("never-assigned"),
      ),
    ).andIsOk

    waitForJobsCompleted()
    entityManager
      .createQuery("select count(t) from Tag t where t.name = 'never-assigned'")
      .singleResult
      .assert
      .isEqualTo(0L)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `parallel chunks create a new tag without retrying`() {
    val keys = testData.addTagKeysData(12000)
    batchJobTestBase.saveAndPrepare(this)

    performProjectAuthPost(
      "start-batch-job/tag-keys",
      mapOf(
        "keyIds" to keys.map { it.id },
        "tags" to listOf("brand-new"),
      ),
    ).andIsOk

    waitForJobsCompleted()
    val job = batchJobTestBase.batchJobService.getAllByProjectId(testData.project.id).single()
    job.status.assert.isEqualTo(BatchJobStatus.SUCCESS)
    entityManager
      .createQuery("select count(e) from BatchJobChunkExecution e where e.batchJob.id = :id")
      .setParameter("id", job.id)
      .singleResult
      .assert
      .isEqualTo(job.totalChunks.toLong())
  }

  private fun waitForJobsCompleted() =
    waitFor(pollTime = 500, timeout = 60000) {
      batchJobTestBase.batchJobService.getAllByProjectId(testData.project.id).all { it.status.completed }
    }
}
