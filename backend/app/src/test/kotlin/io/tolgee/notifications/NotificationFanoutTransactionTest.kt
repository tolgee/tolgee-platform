package io.tolgee.notifications

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.events.OnBatchJobCreated
import io.tolgee.batch.events.OnBatchJobFinalized
import io.tolgee.development.testDataBuilder.data.NotificationRecipientsTestData
import io.tolgee.events.OnNotificationsChangedForUser
import io.tolgee.fixtures.andGetContentAsString
import io.tolgee.fixtures.andIsCreated
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.waitFor
import io.tolgee.model.notifications.NotificationType
import io.tolgee.repository.notification.NotificationRepository
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.event.EventListener
import org.springframework.jdbc.core.JdbcTemplate

class NotificationFanoutTransactionTest : ProjectAuthControllerTest("/v2/projects/") {
  class FanoutFailureInjector(
    private val jdbcTemplate: JdbcTemplate,
  ) {
    var failTransactionAfterFanoutJobStored = false
    var fanoutJobsStoredBeforeRollback: Int? = null
    var failNextNotificationWrite = false
    var notificationWriteFailures = 0

    @EventListener
    fun onCreated(event: OnBatchJobCreated) {
      if (!failTransactionAfterFanoutJobStored || event.job.type != BatchJobType.NOTIFICATION_FANOUT) return
      fanoutJobsStoredBeforeRollback =
        jdbcTemplate.queryForObject(
          "select count(*) from tolgee_batch_job where id = ?",
          Int::class.java,
          event.job.id,
        )
      throw IllegalStateException("Simulated failure after the fanout job was stored")
    }

    @EventListener
    fun onNotificationWritten(event: OnNotificationsChangedForUser) {
      if (!failNextNotificationWrite) return
      failNextNotificationWrite = false
      notificationWriteFailures++
      throw IllegalStateException("Simulated failure after the first notification row was written")
    }
  }

  @TestConfiguration
  class FailureInjectorConfiguration {
    @Bean
    fun fanoutFailureInjector(jdbcTemplate: JdbcTemplate) = FanoutFailureInjector(jdbcTemplate)
  }

  @Autowired
  private lateinit var injector: FanoutFailureInjector

  @Autowired
  private lateinit var jdbcTemplate: JdbcTemplate

  @Autowired
  private lateinit var batchJobService: BatchJobService

  @Autowired
  private lateinit var notificationRepository: NotificationRepository

  private lateinit var testData: NotificationRecipientsTestData

  private val fanoutJobs by lazy { NotificationFanoutJobs(jdbcTemplate) }

  @BeforeEach
  fun setup() {
    testData = NotificationRecipientsTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.author
    projectSupplier = { testData.project }
  }

  @AfterEach
  fun cleanup() {
    injector.failTransactionAfterFanoutJobStored = false
    injector.fanoutJobsStoredBeforeRollback = null
    injector.failNextNotificationWrite = false
    injector.notificationWriteFailures = 0
    testDataService.cleanTestData(testData.root)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `committed change starts exactly one fanout job`() {
    performProjectAuthPost("keys", mapOf("name" to "committed-key")).andIsCreated
    fanoutJobs.waitForAllSucceeded(testData.project.id).assert.hasSize(1)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `rolled back change leaves no fanout job`() {
    injector.failTransactionAfterFanoutJobStored = true

    createKeyExpectingFailure("rolled-back-key")

    injector.fanoutJobsStoredBeforeRollback.assert.isEqualTo(1)
    fanoutJobs.ofProject(testData.project.id).assert.isEmpty()
    keyService.find(testData.project.id, "rolled-back-key", null).assert.isNull()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `rolled back batch job finalization leaves no fanout job`() {
    val batchJobId = finishSetStateBatchJob(batchJobService, testData.existingKey.id, listOf(testData.french.id))
    val fanoutJobsBefore = fanoutJobs.waitForAllSucceeded(testData.project.id)
    val mergedRevisionId =
      jdbcTemplate.queryForObject(
        "select id from activity_revision where batch_job_id = ?",
        Long::class.java,
        batchJobId,
      )!!
    injector.failTransactionAfterFanoutJobStored = true

    runCatching {
      executeInNewTransaction {
        applicationContext.publishEvent(OnBatchJobFinalized(batchJobService.getJobDto(batchJobId), mergedRevisionId))
      }
    }.isFailure.assert
      .isTrue()

    injector.fanoutJobsStoredBeforeRollback.assert.isEqualTo(1)
    fanoutJobs.ofProject(testData.project.id).assert.isEqualTo(fanoutJobsBefore)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `fanout that fails after writing a row is retried and ends with one complete row per recipient`() {
    injector.failNextNotificationWrite = true

    performProjectAuthPost("keys", mapOf("name" to "retried-key")).andIsCreated
    val revisionId = fanoutJobs.latestRevisionId(testData.project.id)
    fanoutJobs.waitForAllSucceeded(testData.project.id).assert.hasSize(1)

    injector.notificationWriteFailures.assert.isEqualTo(1)
    val rows = notificationRepository.findAll().filter { it.type == NotificationType.KEYS_ADDED }
    rows.map { it.user.id }.assert.containsExactlyInAnyOrder(
      testData.translatorFr.id,
      testData.reviewerFr.id,
      testData.reviewerAll.id,
    )
    rows.forEach { row ->
      fanoutJobs.entityCount(row.id).assert.isEqualTo(1)
      fanoutJobs.revisionIdsOf(row.id).assert.containsExactly(revisionId)
    }
  }

  private fun createKeyExpectingFailure(name: String) {
    val status =
      runCatching { performProjectAuthPost("keys", mapOf("name" to name)).andReturn().response.status }
        .getOrNull()
    status.assert.isNotEqualTo(201)
  }
}
