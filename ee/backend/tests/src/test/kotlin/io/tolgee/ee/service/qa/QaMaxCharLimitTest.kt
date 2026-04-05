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
class QaMaxCharLimitTest : ProjectAuthControllerTest("/v2/projects/") {
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
  fun `setting max char limit marks translations stale and triggers QA check`() {
    // Clear stale flags first so we can verify they get set by the maxCharLimit change
    executeInNewTransaction(platformTransactionManager) {
      val en = translationService.find(testData.enTranslation.id)!!
      en.qaChecksStale = false
      entityManager.persist(en)
      val fr = translationService.find(testData.frTranslation.id)!!
      fr.qaChecksStale = false
      entityManager.persist(fr)
    }

    performProjectAuthPut(
      "keys/${testData.testKey.id}/complex-update",
      mapOf("name" to testData.testKey.name, "maxCharLimit" to 5),
    ).andIsOk

    // All translations for this key should be marked stale
    waitForNotThrowing(timeout = 10_000, pollTime = 500) {
      executeInNewTransaction(platformTransactionManager) {
        val enTranslation = translationService.find(testData.enTranslation.id)!!
        assertThat(enTranslation.qaChecksStale).isTrue()

        val frTranslation = translationService.find(testData.frTranslation.id)!!
        assertThat(frTranslation.qaChecksStale).isTrue()
      }
    }

    // QA batch job should be created
    waitForNotThrowing(timeout = 10_000, pollTime = 500) {
      executeInNewTransaction(platformTransactionManager) {
        val jobs = batchJobService.getAllByProjectId(testData.project.id)
        val qaJobs = jobs.filter { it.type == BatchJobType.QA_CHECK }
        qaJobs.assert.isNotEmpty
      }
    }
  }
}
