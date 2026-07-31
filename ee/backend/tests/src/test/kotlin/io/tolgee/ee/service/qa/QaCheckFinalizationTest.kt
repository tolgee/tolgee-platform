package io.tolgee.ee.service.qa

import com.posthog.server.PostHog
import io.tolgee.ActivityTestUtil
import io.tolgee.ProjectAuthControllerTest
import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.data.BatchTranslationTargetItem
import io.tolgee.batch.events.OnBatchJobFinalized
import io.tolgee.batch.request.QaCheckRequest
import io.tolgee.constants.Feature
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.development.QaTestData
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.batch.BatchJob
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.event.EventListener
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.util.concurrent.CopyOnWriteArrayList

@SpringBootTest
@AutoConfigureMockMvc
class QaCheckFinalizationTest : ProjectAuthControllerTest("/v2/projects/") {
  class FinalizedRecorder {
    val events = CopyOnWriteArrayList<OnBatchJobFinalized>()

    @EventListener
    fun onFinalized(event: OnBatchJobFinalized) {
      events.add(event)
    }
  }

  @TestConfiguration
  class RecorderConfiguration {
    @Bean
    fun finalizedRecorder() = FinalizedRecorder()
  }

  @MockitoBean
  @Autowired
  lateinit var postHog: PostHog

  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  private lateinit var batchJobService: BatchJobService

  @Autowired
  private lateinit var finalizedRecorder: FinalizedRecorder

  @Autowired
  private lateinit var activityTestUtil: ActivityTestUtil

  lateinit var testData: QaTestData

  @BeforeEach
  fun setup() {
    finalizedRecorder.events.clear()
    enabledFeaturesProvider.forceEnabled = setOf(Feature.QA_CHECKS)
    testData = QaTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    projectSupplier = { testData.project }
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
    userAccount = null
    enabledFeaturesProvider.forceEnabled = null
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `finalizes a QA_CHECK job that produced no activity revision`() {
    val job = startQaCheck(testData.freshFrKey.id, testData.frenchLanguage.id)

    waitForNotThrowing(pollTime = 100, timeout = 30_000) {
      executeInNewTransaction(platformTransactionManager) {
        batchJobService
          .getJobDtoNoCache(job.id)
          .status.completed.assert
          .isTrue()
      }
    }

    executeInNewTransaction(platformTransactionManager) {
      activityTestUtil.countRevisionsOfJob(job.id).assert.isEqualTo(0)
    }

    waitForNotThrowing(pollTime = 100, timeout = 20_000) {
      finalizedRecorder.events
        .first { it.job.id == job.id }
        .activityRevisionId
        .assert
        .isNull()
    }
  }

  private fun startQaCheck(
    keyId: Long,
    languageId: Long,
  ): BatchJob =
    executeInNewTransaction(platformTransactionManager) {
      batchJobService.startJob(
        request =
          QaCheckRequest(
            target = listOf(BatchTranslationTargetItem(keyId = keyId, languageId = languageId)),
          ),
        project = testData.project,
        author = null,
        type = BatchJobType.QA_CHECK,
        isHidden = true,
      )
    }
}
