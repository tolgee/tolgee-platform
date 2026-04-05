package io.tolgee.ee.service.qa

import com.posthog.server.PostHog
import io.tolgee.ProjectAuthControllerTest
import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.constants.Feature
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.development.QaTestData
import io.tolgee.ee.utils.QaTestUtil
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions.assertThat
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest
@AutoConfigureMockMvc
class QaBatchOperationTest : ProjectAuthControllerTest("/v2/projects/") {
  @MockitoBean
  @Autowired
  lateinit var postHog: PostHog

  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  private lateinit var qa: QaTestUtil

  @Autowired
  private lateinit var batchJobService: BatchJobService

  lateinit var testData: QaTestData

  @BeforeEach
  fun setup() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.QA_CHECKS)
    testData = QaTestData()
    testDataService.saveTestData(testData.root)
    qa.testData = testData
    qa.saveDefaultQaConfig()
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
  fun `copy translations batch job triggers QA checks via OnBatchJobFinalized`() {
    // Copy English translations to French — this modifies French translation text
    // through a batch job, which goes through OnBatchJobFinalized path
    performProjectAuthPost(
      "start-batch-job/copy-translations",
      mapOf(
        "keyIds" to listOf(testData.testKey.id),
        "sourceLanguageId" to testData.englishLanguage.id,
        "targetLanguageIds" to listOf(testData.frenchLanguage.id),
      ),
    ).andIsOk

    // Wait for copy batch job to complete
    waitForNotThrowing(timeout = 30_000, pollTime = 500) {
      executeInNewTransaction(platformTransactionManager) {
        val jobs = batchJobService.getAllByProjectId(testData.project.id)
        val copyJobs = jobs.filter { it.type == BatchJobType.COPY_TRANSLATIONS }
        copyJobs.assert.isNotEmpty
        assertThat(copyJobs.all { it.status.completed }).isTrue()
      }
    }

    // QA_CHECK batch job should be created (from onBatchJobFinalized after copy completes)
    waitForNotThrowing(timeout = 30_000, pollTime = 500) {
      executeInNewTransaction(platformTransactionManager) {
        val jobs = batchJobService.getAllByProjectId(testData.project.id)
        val qaJobs = jobs.filter { it.type == BatchJobType.QA_CHECK }
        qaJobs.assert.isNotEmpty
      }
    }
  }
}
