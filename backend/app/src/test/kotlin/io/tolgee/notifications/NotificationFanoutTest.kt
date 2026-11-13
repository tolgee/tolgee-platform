package io.tolgee.notifications

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.events.OnBatchJobFinalized
import io.tolgee.batch.processors.NotificationFanoutChunkProcessor
import io.tolgee.batch.request.NoOpRequest
import io.tolgee.batch.request.NotificationFanoutRequest
import io.tolgee.development.testDataBuilder.data.NotificationRecipientsTestData
import io.tolgee.fixtures.andGetContentAsString
import io.tolgee.fixtures.andIsCreated
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.waitFor
import io.tolgee.model.Project
import io.tolgee.model.notifications.NotificationChannel
import io.tolgee.model.notifications.NotificationType
import io.tolgee.repository.notification.NotificationRepository
import io.tolgee.service.notification.NotificationSettingsService
import io.tolgee.service.notification.activity.NotificationFanoutListener
import io.tolgee.service.notification.activity.NotificationFanoutService
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.coroutines.EmptyCoroutineContext

class NotificationFanoutTest : ProjectAuthControllerTest("/v2/projects/") {
  @Autowired
  private lateinit var batchJobService: BatchJobService

  @Autowired
  private lateinit var fanoutProcessor: NotificationFanoutChunkProcessor

  @Autowired
  private lateinit var notificationRepository: NotificationRepository

  @Autowired
  private lateinit var settingsService: NotificationSettingsService

  @Autowired
  private lateinit var jdbcTemplate: JdbcTemplate

  @Autowired
  private lateinit var fanoutService: NotificationFanoutService

  @Autowired
  private lateinit var fanoutListener: NotificationFanoutListener

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
    fanoutListener.bypass = false
    testDataService.cleanTestData(testData.root)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `creating a key starts one hidden fanout job for its revision, which notifies translators`() {
    performProjectAuthPost("keys", mapOf("name" to "new-key")).andIsCreated
    val revisionId = fanoutJobs.latestRevisionId(testData.project.id)

    val job = fanoutJobs.waitForAllSucceeded(testData.project.id).single()
    job.target.assert.isEqualTo("[$revisionId]")
    job.hidden.assert.isTrue()
    job.authorId.assert.isNull()

    val rows = notificationsOf(NotificationType.KEYS_ADDED)
    rows.map { it.user.id }.assert.containsExactlyInAnyOrder(
      testData.translatorFr.id,
      testData.reviewerFr.id,
      testData.reviewerAll.id,
    )
    rows
      .single { it.user.id == testData.reviewerAll.id }
      .emailPending.assert
      .isTrue()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `revision without key or translation changes starts no job`() {
    performProjectAuthPut(
      "languages/${testData.german.id}",
      mapOf(
        "name" to "Deutsch 2",
        "tag" to "de",
        "originalName" to "Deutsch",
        "flagEmoji" to "🇩🇪",
      ),
    ).andIsOk
    fanoutJobs.ofProject(testData.project.id).assert.isEmpty()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `batch job chunks start no job, its merged revision starts exactly one`() {
    val jobId =
      finishSetStateBatchJob(batchJobService, testData.existingKey.id, listOf(testData.french.id, testData.german.id))

    val mergedRevisionId =
      jdbcTemplate.queryForObject(
        "select id from activity_revision where batch_job_id = ?",
        Long::class.java,
        jobId,
      )
    val job = fanoutJobs.waitForAllSucceeded(testData.project.id).single()
    job.target.assert.isEqualTo("[$mergedRevisionId]")
    notificationsOf(NotificationType.STRINGS_REVIEWED).map { it.user.id }.assert.contains(testData.reviewerAll.id)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `finalizing a fanout job starts no further job`() {
    performProjectAuthPost("keys", mapOf("name" to "new-key")).andIsCreated
    val job = fanoutJobs.waitForAllSucceeded(testData.project.id).single()

    executeInNewTransaction {
      applicationContext.publishEvent(
        OnBatchJobFinalized(batchJobService.getJobDto(job.id), fanoutJobs.latestRevisionId(testData.project.id)),
      )
    }

    fanoutJobs.ofProject(testData.project.id).assert.hasSize(1)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `in-app disabled type creates no row for that user`() {
    settingsService.saveForType(testData.reviewerAll, NotificationType.KEYS_ADDED, NotificationChannel.IN_APP, false)
    performProjectAuthPost("keys", mapOf("name" to "k1")).andIsCreated
    fanoutJobs.waitForAllSucceeded(testData.project.id)
    notificationsOf(NotificationType.KEYS_ADDED).map { it.user.id }.assert.doesNotContain(testData.reviewerAll.id)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `email-disabled type creates a row without email pending`() {
    settingsService.saveForType(
      testData.reviewerAll,
      NotificationType.STRINGS_REVIEWED,
      NotificationChannel.EMAIL,
      false,
    )
    performProjectAuthPut("translations/${testData.existingFrench.id}/set-state/REVIEWED").andIsOk
    fanoutJobs.waitForAllSucceeded(testData.project.id)
    notificationsOf(NotificationType.STRINGS_REVIEWED)
      .single { it.user.id == testData.reviewerAll.id }
      .emailPending.assert
      .isFalse()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `separate changes share one unseen row per recipient`() {
    repeat(3) { performProjectAuthPost("keys", mapOf("name" to "merged-$it")).andIsCreated }
    fanoutJobs.waitForAllSucceeded(testData.project.id, atLeast = 3).assert.hasSize(3)
    val row = notificationsOf(NotificationType.KEYS_ADDED).single { it.user.id == testData.reviewerAll.id }
    fanoutJobs.entityCount(row.id).assert.isEqualTo(3)
    fanoutJobs.revisionIdsOf(row.id).assert.hasSize(3)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `re-running a finished fanout chunk does not duplicate linked entities or revision ids`() {
    performProjectAuthPost("keys", mapOf("name" to "k1")).andIsCreated
    performProjectAuthPost("keys", mapOf("name" to "k2")).andIsCreated
    val jobs = fanoutJobs.waitForAllSucceeded(testData.project.id, atLeast = 2)
    val rowsBefore = notificationRepository.findAll().map { it.id to fanoutJobs.entityCount(it.id) }
    val row = notificationsOf(NotificationType.KEYS_ADDED).single { it.user.id == testData.reviewerAll.id }
    val revisionIdsBefore = fanoutJobs.revisionIdsOf(row.id)

    repeat(2) {
      executeInNewTransaction {
        val job = batchJobService.getJobDto(jobs.first().id)
        fanoutProcessor.process(job, job.target.map { (it as Number).toLong() }, EmptyCoroutineContext)
      }
    }

    notificationRepository
      .findAll()
      .map {
        it.id to fanoutJobs.entityCount(it.id)
      }.assert
      .containsExactlyInAnyOrderElementsOf(
        rowsBefore,
      )
    fanoutJobs.revisionIdsOf(row.id).assert.isEqualTo(revisionIdsBefore)
    fanoutJobs.entityCount(row.id).assert.isEqualTo(2)
  }

  @Test
  fun `revision that no longer exists is skipped`() {
    executeInNewTransaction {
      fanoutProcessor.process(
        batchJobDtoOfNewFanoutJob(Long.MAX_VALUE),
        listOf(Long.MAX_VALUE),
        EmptyCoroutineContext,
      )
    }
    jdbcTemplate
      .queryForObject("select count(*) from notification where project_id = ?", Int::class.java, testData.project.id)
      .assert
      .isEqualTo(0)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `older revision processed after a newer one keeps the newer author`() {
    fanoutListener.bypass = true
    performProjectAuthPost("keys", mapOf("name" to "older")).andIsCreated
    val olderRevisionId = fanoutJobs.latestRevisionId(testData.project.id)
    performProjectAuthPost("keys", mapOf("name" to "newer")).andIsCreated
    val newerRevisionId = fanoutJobs.latestRevisionId(testData.project.id)
    jdbcTemplate.update(
      "update activity_revision set author_id = ? where id = ?",
      testData.reviewerFr.id,
      newerRevisionId,
    )

    executeInNewTransaction { fanoutService.fanOut(newerRevisionId) }
    executeInNewTransaction { fanoutService.fanOut(olderRevisionId) }

    notificationsOf(NotificationType.KEYS_ADDED)
      .single { it.user.id == testData.reviewerAll.id }
      .originatingUser
      ?.id
      .assert
      .isEqualTo(testData.reviewerFr.id)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `concurrent fanouts to the same recipients and types finish and share one row each`() {
    fanoutListener.bypass = true
    val revisionIds =
      (1..2).map {
        performProjectAuthPost(
          "keys",
          mapOf("name" to "concurrent-$it", "translations" to mapOf("en" to "en $it", "fr" to "fr $it")),
        ).andIsCreated
        fanoutJobs.latestRevisionId(testData.project.id)
      }

    val start = CountDownLatch(1)
    val pool = Executors.newFixedThreadPool(revisionIds.size)
    try {
      val results =
        revisionIds.map { revisionId ->
          pool.submit {
            start.await()
            executeInNewTransaction { fanoutService.fanOut(revisionId) }
          }
        }
      start.countDown()
      results.forEach { it.get(30, TimeUnit.SECONDS) }
    } finally {
      pool.shutdownNow()
    }

    val rows = notificationRepository.findAll().filter { it.project?.id == testData.project.id }
    rows
      .groupBy { it.user.id to it.type }
      .values.assert
      .allMatch { it.size == 1 }
    val keysAdded = notificationsOf(NotificationType.KEYS_ADDED).single { it.user.id == testData.reviewerAll.id }
    fanoutJobs.entityCount(keysAdded.id).assert.isEqualTo(2)
    fanoutJobs.revisionIdsOf(keysAdded.id).assert.containsExactlyInAnyOrderElementsOf(revisionIds)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `failed fanout job is not listed to users, a failed hidden job of another type is`() {
    val fanoutJobId =
      executeInNewTransaction {
        startHiddenJob(NotificationFanoutRequest(Long.MAX_VALUE), BatchJobType.NOTIFICATION_FANOUT)
      }
    val otherJobId =
      executeInNewTransaction {
        startHiddenJob(NoOpRequest().apply { itemIds = listOf(1L) }, BatchJobType.NO_OP)
      }
    waitFor(pollTime = 100) {
      listOf(fanoutJobId, otherJobId).all { batchJobService.findJobDto(it)?.status?.completed == true }
    }
    jdbcTemplate.update("update tolgee_batch_job set status = 'FAILED' where id in (?, ?)", fanoutJobId, otherJobId)

    listedJobIds("current-batch-jobs").assert.contains(otherJobId).doesNotContain(fanoutJobId)
    listedJobIds("batch-jobs").assert.contains(otherJobId).doesNotContain(fanoutJobId)
  }

  @Test
  fun `fanout job cannot be read or cancelled by id, another hidden job can`() {
    val fanoutJobId =
      executeInNewTransaction {
        startHiddenJob(NotificationFanoutRequest(Long.MAX_VALUE), BatchJobType.NOTIFICATION_FANOUT)
      }
    val otherJobId =
      executeInNewTransaction {
        startHiddenJob(NoOpRequest().apply { itemIds = listOf(1L) }, BatchJobType.NO_OP)
      }

    performProjectAuthGet("batch-jobs/$fanoutJobId").andIsNotFound
    performProjectAuthPut("batch-jobs/$fanoutJobId/cancel", null).andIsNotFound
    performProjectAuthGet("batch-jobs/$otherJobId").andIsOk
    performProjectAuthPut("batch-jobs/$otherJobId/cancel", null).andIsOk
  }

  private fun startHiddenJob(
    request: Any,
    type: BatchJobType,
  ): Long =
    batchJobService
      .startJob(
        request = request,
        project = entityManager.getReference(Project::class.java, testData.project.id),
        author = testData.author,
        type = type,
        isHidden = true,
      ).id

  private fun listedJobIds(path: String): List<Long> {
    val body = objectMapper.readTree(performProjectAuthGet(path).andIsOk.andGetContentAsString)
    return body
      .path("_embedded")
      .path("batchJobs")
      .iterator()
      .asSequence()
      .map { it.path("id").asLong() }
      .toList()
  }

  private fun batchJobDtoOfNewFanoutJob(revisionId: Long) = batchJobService.getJobDto(startFanoutJob(revisionId).id)

  private fun startFanoutJob(revisionId: Long) =
    batchJobService.startJob(
      request = NotificationFanoutRequest(revisionId),
      project = entityManager.getReference(Project::class.java, testData.project.id),
      type = BatchJobType.NOTIFICATION_FANOUT,
      isHidden = true,
    )

  private fun notificationsOf(type: NotificationType) = notificationRepository.findAll().filter { it.type == type }
}
