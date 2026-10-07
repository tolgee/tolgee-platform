package io.tolgee.notifications

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.batch.BatchJobService
import io.tolgee.development.testDataBuilder.data.NotificationRecipientsTestData
import io.tolgee.fixtures.andGetContentAsString
import io.tolgee.fixtures.andIsCreated
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.waitFor
import io.tolgee.model.notifications.NotificationType
import io.tolgee.service.notification.activity.RevisionNotificationClassifier
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

class RevisionNotificationClassifierTest : ProjectAuthControllerTest("/v2/projects/") {
  @Autowired
  private lateinit var classifier: RevisionNotificationClassifier

  @Autowired
  private lateinit var batchJobService: BatchJobService

  @Autowired
  private lateinit var jdbcTemplate: JdbcTemplate

  private lateinit var testData: NotificationRecipientsTestData

  @BeforeEach
  fun setup() {
    testData = NotificationRecipientsTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.author
    projectSupplier = { testData.project }
  }

  private fun latestRevisionId() =
    jdbcTemplate.queryForObject("select max(id) from activity_revision", Long::class.java)!!

  @Test
  @ProjectJWTAuthTestMethod
  fun `new key with base and french text`() {
    performProjectAuthPost("keys", mapOf("name" to "k1", "translations" to mapOf("en" to "Hi", "fr" to "Salut")))
      .andIsCreated
    val result = classifier.classify(latestRevisionId())!!
    result.items.keys.assert
      .containsExactlyInAnyOrder(NotificationType.KEYS_ADDED, NotificationType.STRINGS_TRANSLATED)
    result.items[NotificationType.KEYS_ADDED]!![null]!!.assert.hasSize(1)
    result.items[NotificationType.STRINGS_TRANSLATED]!!
      .keys.assert
      .containsExactly(testData.french.id)
    result.authorId.assert.isEqualTo(testData.author.id)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `base text change is source changed, not translated`() {
    performProjectAuthPut("translations", mapOf("key" to "existing-key", "translations" to mapOf("en" to "Hello!")))
      .andIsOk
    val result = classifier.classify(latestRevisionId())!!
    result.items.keys.assert
      .containsExactly(NotificationType.SOURCE_CHANGED)
    result.items[NotificationType.SOURCE_CHANGED]!![null]!!.assert.containsExactly(testData.existingKey.id)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `clearing a translation is not translated`() {
    performProjectAuthPut("translations", mapOf("key" to "existing-key", "translations" to mapOf("fr" to "")))
      .andIsOk
    classifier
      .classify(latestRevisionId())!!
      .items.assert
      .doesNotContainKey(NotificationType.STRINGS_TRANSLATED)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `setting reviewed state`() {
    performProjectAuthPut("translations/${testData.existingFrench.id}/set-state/REVIEWED").andIsOk
    val result = classifier.classify(latestRevisionId())!!
    result.items.keys.assert
      .containsExactly(NotificationType.STRINGS_REVIEWED)
    result.items[NotificationType.STRINGS_REVIEWED]!![testData.french.id]!!
      .assert
      .containsExactly(testData.existingFrench.id)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `batch set state is bulk changed with language entities`() {
    val revisionId = runSetStateBatch()
    val result = classifier.classify(revisionId)!!
    result.items.keys.assert
      .containsExactly(NotificationType.BULK_CHANGED)
    result.items[NotificationType.BULK_CHANGED]!!
      .keys.assert
      .containsExactlyInAnyOrder(testData.french.id, testData.german.id)
    result.items[NotificationType.BULK_CHANGED]!![testData.french.id]!!.assert.containsExactly(
      testData.existingFrench.id,
    )
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `machine translation job type is automatically translated`() {
    val revisionId = runSetStateBatch()
    setJobType(revisionId, "MACHINE_TRANSLATE")
    classifier
      .classify(revisionId)!!
      .items.keys.assert
      .containsExactly(NotificationType.AUTOMATICALLY_TRANSLATED)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `tag job type creates nothing`() {
    val revisionId = runSetStateBatch()
    setJobType(revisionId, "TAG_KEYS")
    classifier
      .classify(revisionId)!!
      .items.assert
      .isEmpty()
  }

  @Test
  fun `missing revision returns null`() {
    classifier.classify(Long.MAX_VALUE).assert.isNull()
  }

  private fun setJobType(
    revisionId: Long,
    type: String,
  ) {
    jdbcTemplate.update(
      "update tolgee_batch_job set type = ? " +
        "where id = (select batch_job_id from activity_revision where id = ?)",
      type,
      revisionId,
    )
  }

  private fun runSetStateBatch(): Long {
    val response =
      performProjectAuthPost(
        "start-batch-job/set-translation-state",
        mapOf(
          "keyIds" to listOf(testData.existingKey.id),
          "languageIds" to listOf(testData.french.id, testData.german.id),
          "state" to "REVIEWED",
        ),
      ).andIsOk
    val jobId = (objectMapper.readValue(response.andGetContentAsString, Map::class.java)["id"] as Number).toLong()
    waitFor(pollTime = 200) { batchJobService.findJobDto(jobId)?.status?.completed == true }
    var revisionId: Long? = null
    waitFor(pollTime = 200) {
      revisionId =
        jdbcTemplate
          .queryForList("select id from activity_revision where batch_job_id = ?", Long::class.java, jobId)
          .firstOrNull()
      revisionId != null
    }
    return revisionId!!
  }
}
