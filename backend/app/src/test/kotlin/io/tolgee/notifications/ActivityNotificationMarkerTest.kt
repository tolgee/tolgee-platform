package io.tolgee.notifications

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.batch.BatchJobService
import io.tolgee.development.testDataBuilder.data.NotificationRecipientsTestData
import io.tolgee.fixtures.andGetContentAsString
import io.tolgee.fixtures.andIsCreated
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.waitFor
import io.tolgee.service.notification.activity.ActivityNotificationQueue
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

class ActivityNotificationMarkerTest : ProjectAuthControllerTest("/v2/projects/") {
  @Autowired
  private lateinit var queue: ActivityNotificationQueue

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
    jdbcTemplate.update("delete from notification_activity_queue")
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `creating a key writes one marker for its revision`() {
    performProjectAuthPost("keys", mapOf("name" to "new-key")).andIsCreated
    val latestRevisionId = jdbcTemplate.queryForObject("select max(id) from activity_revision", Long::class.java)
    queue.findAll().assert.containsExactly(latestRevisionId)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `revision without key or translation changes writes no marker`() {
    val revisionsBefore = revisionCount()
    performProjectAuthPut(
      "languages/${testData.german.id}",
      mapOf(
        "name" to "Deutsch 2",
        "tag" to "de",
        "originalName" to "Deutsch",
        "flagEmoji" to "🇩🇪",
      ),
    ).andIsOk
    revisionCount().assert.isEqualTo(revisionsBefore + 1)
    queue.findAll().assert.isEmpty()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `batch job writes exactly one marker for the merged revision`() {
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
    waitFor(pollTime = 200) { queue.findAll().isNotEmpty() }
    val mergedRevisionId =
      jdbcTemplate.queryForObject(
        "select id from activity_revision where batch_job_id = ?",
        Long::class.java,
        jobId,
      )
    queue.findAll().assert.containsExactly(mergedRevisionId)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `queue insert joins the surrounding transaction and rolls back with it`() {
    executeInNewTransaction { status ->
      queue.add(Long.MAX_VALUE)
      status.setRollbackOnly()
    }
    queue.findAll().assert.isEmpty()
  }

  private fun revisionCount() =
    jdbcTemplate.queryForObject("select count(*) from activity_revision", Long::class.java)!!
}
