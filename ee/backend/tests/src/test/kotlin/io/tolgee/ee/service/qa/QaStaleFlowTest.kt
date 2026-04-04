package io.tolgee.ee.service.qa

import com.posthog.server.PostHog
import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.constants.Feature
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.development.QaTestData
import io.tolgee.ee.utils.QaTestUtil
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.testing.AuthorizedControllerTest
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
class QaStaleFlowTest : AuthorizedControllerTest() {
  @MockitoBean
  @Autowired
  lateinit var postHog: PostHog

  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  lateinit var qa: QaTestUtil

  @Autowired
  private lateinit var batchJobService: BatchJobService

  lateinit var testData: QaTestData

  private val translationsUrl
    get() = "/v2/projects/${testData.project.id}/translations"

  @BeforeEach
  fun setup() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.QA_CHECKS)
    testData = QaTestData()
    testDataService.saveTestData(testData.root)
    qa.testData = testData
    qa.saveDefaultQaConfig()
    userAccount = testData.user
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
    userAccount = null
    enabledFeaturesProvider.forceEnabled = null
  }

  @Test
  fun `saving translation sets qaChecksStale to true`() {
    performAuthPut(
      translationsUrl,
      mapOf("key" to "test-key", "translations" to mapOf("fr" to "Bonjour le monde.")),
    ).andIsOk

    // Verify stale flag is visible in the translations list response
    waitForNotThrowing(timeout = 5_000, pollTime = 200) {
      executeInNewTransaction(platformTransactionManager) {
        val translation = translationService.find(testData.frTranslation.id)!!
        assertThat(translation.qaChecksStale).isTrue()
      }
    }
  }

  @Test
  fun `qaChecksStale is true initially`() {
    executeInNewTransaction(platformTransactionManager) {
      val translation = translationService.find(testData.frTranslation.id)!!
      assertThat(translation.qaChecksStale).isTrue()
    }
  }

  @Test
  fun `qaChecksStale is included in translations API response`() {
    performAuthGet("$translationsUrl?languages=en,fr").andIsOk.andAssertThatJson {
      node("_embedded.keys").isArray.isNotEmpty
      node("_embedded.keys[0].translations.fr.qaChecksStale").isBoolean
    }
  }

  @Test
  fun `full cycle - save sets stale, batch clears stale and persists issues`() {
    // Save translation with DIFFERENT text to trigger QA checks via activity system
    performAuthPut(
      translationsUrl,
      mapOf("key" to "test-key", "translations" to mapOf("fr" to "bonjour le monde")),
    ).andIsOk

    // Verify stale is set to true immediately
    waitForNotThrowing(timeout = 5_000, pollTime = 200) {
      executeInNewTransaction(platformTransactionManager) {
        val translation = translationService.find(testData.frTranslation.id)!!
        assertThat(translation.qaChecksStale).isTrue()
      }
    }

    // Wait for batch job to complete — stale should become false and issues should be persisted
    waitForNotThrowing<Unit>(timeout = 30_000, pollTime = 500) {
      executeInNewTransaction(platformTransactionManager) {
        val translation = translationService.find(testData.frTranslation.id)!!
        assertThat(translation.qaChecksStale).isFalse()
        assertThat(translation.qaIssues).isNotEmpty
      }
    }
  }

  @Test
  fun `base translation change marks siblings as stale`() {
    // Update the base (English) translation
    performAuthPut(
      translationsUrl,
      mapOf("key" to "test-key", "translations" to mapOf("en" to "Hello World!")),
    ).andIsOk

    // The French (sibling) translation should be marked stale
    waitForNotThrowing(timeout = 10_000, pollTime = 500) {
      executeInNewTransaction(platformTransactionManager) {
        val frTranslation = translationService.find(testData.frTranslation.id)!!
        assertThat(frTranslation.qaChecksStale).isTrue()
      }
    }
  }

  @Test
  fun `marks translation stale even when QA feature is disabled`() {
    // Disable QA feature
    enabledFeaturesProvider.forceEnabled = emptySet()

    // Disable QA for the project
    executeInNewTransaction(platformTransactionManager) {
      val project = projectService.get(testData.project.id)
      project.useQaChecks = false
      entityManager.persist(project)
    }

    // Clear stale flag first so we can verify it gets set
    executeInNewTransaction(platformTransactionManager) {
      val translation = translationService.find(testData.frTranslation.id)!!
      translation.qaChecksStale = false
      entityManager.persist(translation)
    }

    // Save translation — should still mark stale even without QA feature
    performAuthPut(
      translationsUrl,
      mapOf("key" to "test-key", "translations" to mapOf("fr" to "Bonjour tout le monde")),
    ).andIsOk

    waitForNotThrowing(timeout = 5_000, pollTime = 200) {
      executeInNewTransaction(platformTransactionManager) {
        val translation = translationService.find(testData.frTranslation.id)!!
        assertThat(translation.qaChecksStale).isTrue()
      }
    }
  }

  @Test
  fun `marks siblings stale even when QA feature is disabled`() {
    // Disable QA feature
    enabledFeaturesProvider.forceEnabled = emptySet()

    // Disable QA for the project
    executeInNewTransaction(platformTransactionManager) {
      val project = projectService.get(testData.project.id)
      project.useQaChecks = false
      entityManager.persist(project)
    }

    // Clear stale flag on French translation
    executeInNewTransaction(platformTransactionManager) {
      val translation = translationService.find(testData.frTranslation.id)!!
      translation.qaChecksStale = false
      entityManager.persist(translation)
    }

    // Change base (English) translation — should mark French sibling as stale
    performAuthPut(
      translationsUrl,
      mapOf("key" to "test-key", "translations" to mapOf("en" to "Hello World!")),
    ).andIsOk

    waitForNotThrowing(timeout = 10_000, pollTime = 500) {
      executeInNewTransaction(platformTransactionManager) {
        val frTranslation = translationService.find(testData.frTranslation.id)!!
        assertThat(frTranslation.qaChecksStale).isTrue()
      }
    }
  }

  @Test
  fun `does not create QA batch jobs when QA is disabled but still marks stale`() {
    // Disable QA for the project
    executeInNewTransaction(platformTransactionManager) {
      val project = projectService.get(testData.project.id)
      project.useQaChecks = false
      entityManager.persist(project)
    }

    // Save translation
    performAuthPut(
      translationsUrl,
      mapOf("key" to "test-key", "translations" to mapOf("fr" to "Bonjour tout le monde")),
    ).andIsOk

    // Translation should be stale
    waitForNotThrowing(timeout = 5_000, pollTime = 200) {
      executeInNewTransaction(platformTransactionManager) {
        val translation = translationService.find(testData.frTranslation.id)!!
        assertThat(translation.qaChecksStale).isTrue()
      }
    }

    // But no QA batch jobs should have been created
    Thread.sleep(1000)
    executeInNewTransaction(platformTransactionManager) {
      val jobs = batchJobService.getAllByProjectId(testData.project.id)
      val qaJobs = jobs.filter { it.type == BatchJobType.QA_CHECK }
      assertThat(qaJobs).isEmpty()
    }
  }
}
