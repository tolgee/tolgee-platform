package io.tolgee.ee.service.qa

import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.data.BatchTranslationTargetItem
import io.tolgee.batch.request.QaCheckRequest
import io.tolgee.configuration.tolgee.BatchProperties
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.QaAutoTranslateRaceTestData
import io.tolgee.dtos.request.translation.SetTranslationsWithKeyDto
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.component.qa.QaActivityListener
import io.tolgee.fixtures.MachineTranslationTest
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.waitFor
import io.tolgee.model.Project
import io.tolgee.model.translation.Translation
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/** Lost update between the AUTO_TRANSLATE and QA_CHECK batch jobs. */
@SpringBootTest
@AutoConfigureMockMvc
class QaCheckAutoTranslateRaceTest : MachineTranslationTest() {
  @Autowired
  private lateinit var qaCheckBatchService: QaCheckBatchServiceImpl

  @Autowired
  private lateinit var batchJobService: BatchJobService

  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  private lateinit var batchProperties: BatchProperties

  @Autowired
  private lateinit var qaActivityListener: QaActivityListener

  private var originalConcurrency = 1

  private lateinit var testData: QaAutoTranslateRaceTestData

  @BeforeEach
  fun setup() {
    originalConcurrency = batchProperties.concurrency
    batchProperties.concurrency = 10
    enabledFeaturesProvider.forceEnabled = setOf(Feature.QA_CHECKS)
    testData = QaAutoTranslateRaceTestData()
    testDataService.saveTestData(testData.root)
    // saveTestData resets every bypassable listener to false when it finishes
    qaActivityListener.bypass = true
    userAccount = testData.user
    projectSupplier = { testData.project }
    initMachineTranslationProperties(INITIAL_BUCKET_CREDITS)
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
    userAccount = null
    enabledFeaturesProvider.forceEnabled = null
    batchProperties.concurrency = originalConcurrency
    qaActivityListener.bypass = false
  }

  @Test
  fun `QA check persist keeps the text written by a concurrent transaction`() {
    val translation = testData.autoTranslations.first()
    assertThat(load(translation.id).qaChecksStale).isTrue
    val updateFlushed = CountDownLatch(1)
    val releaseLock = CountDownLatch(1)

    val writer =
      thread {
        executeInNewTransaction {
          entityManager
            .createNativeQuery("update translation set text = :text where id = :id")
            .setParameter("text", MT_TEXT)
            .setParameter("id", translation.id)
            .executeUpdate()
          updateFlushed.countDown()
          releaseLock.await(30, TimeUnit.SECONDS)
        }
      }
    assertThat(updateFlushed.await(10, TimeUnit.SECONDS)).isTrue

    var qaError: Throwable? = null
    val qa =
      thread {
        try {
          executeInNewTransaction {
            qaCheckBatchService.runChecksAndPersistChunk(
              projectId = testData.project.id,
              checkTypes = null,
              items = listOf(BatchTranslationTargetItem(testData.raceKey.id, translation.language.id)),
            )
          }
        } catch (e: Throwable) {
          qaError = e
        }
      }

    var sawLockWait = false
    waitFor(timeout = 15_000) {
      sawLockWait = isSomeoneWaitingOnTranslationRowLock()
      sawLockWait || !qa.isAlive
    }
    releaseLock.countDown()
    writer.join()
    qa.join()
    qaError?.let { throw it }
    assertThat(sawLockWait).isTrue

    val stored = load(translation.id)
    assertThat(stored.text).isEqualTo(MT_TEXT)
    assertThat(stored.qaChecksStale).isTrue
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `auto translate batch job result survives a concurrent QA check batch job`() {
    val secondMtCallStarted = CountDownLatch(1)
    val releaseMt = CountDownLatch(1)
    initMachineTranslationMocks { callIndex ->
      if (callIndex == 2) {
        secondMtCallStarted.countDown()
        releaseMt.await(30, TimeUnit.SECONDS)
      }
    }

    performProjectAuthPut(
      "translations",
      SetTranslationsWithKeyDto(key = testData.raceKey.name, translations = mapOf("en" to NEW_BASE)),
    ).andIsOk
    assertThat(secondMtCallStarted.await(20, TimeUnit.SECONDS)).isTrue

    val qaJob =
      executeInNewTransaction {
        batchJobService.startJob(
          request =
            QaCheckRequest(
              target =
                testData.autoTranslations.map {
                  BatchTranslationTargetItem(
                    testData.raceKey.id,
                    it.language.id,
                  )
                },
            ),
          project = entityManager.getReference(Project::class.java, testData.project.id),
          author = null,
          type = BatchJobType.QA_CHECK,
          isHidden = true,
        )
      }
    var sawLockWait = false
    try {
      waitFor(timeout = 15_000) {
        sawLockWait = isSomeoneWaitingOnTranslationRowLock()
        sawLockWait || executeInNewTransaction { batchJobService.getJobDtoNoCache(qaJob.id).status.completed }
      }
    } finally {
      releaseMt.countDown()
    }
    waitFor(timeout = 30_000) {
      executeInNewTransaction { batchJobService.getAllIncompleteJobIds(testData.project.id).isEmpty() }
    }
    assertThat(sawLockWait).isTrue

    val stored = testData.autoTranslations.map { load(it.id) }
    assertThat(stored.map { it.language.tag to it.text }).allSatisfy { (_, text) ->
      assertThat(text).isEqualTo(MT_TEXT)
    }
  }

  private fun load(id: Long): Translation =
    executeInNewTransaction {
      entityManager.find(Translation::class.java, id).also { it.language.tag }
    }

  private fun isSomeoneWaitingOnTranslationRowLock(): Boolean =
    executeInNewTransaction {
      val waiting =
        entityManager
          .createNativeQuery(
            """
            select count(*) from pg_locks waiter
            where not waiter.granted and waiter.locktype = 'transactionid'
              and exists (
                select 1 from pg_locks held
                where held.pid = waiter.pid and held.locktype = 'tuple' and held.relation = 'translation'::regclass
              )
            """,
          ).singleResult as Number
      waiting.toLong() > 0
    }

  companion object {
    private const val INITIAL_BUCKET_CREDITS = 150000L
    private const val NEW_BASE = "New base"
    private const val MT_TEXT = "Translated with Google"
  }
}
