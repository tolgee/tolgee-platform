package io.tolgee.ee.api.v2.controllers.qa

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
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.PlatformTransactionManager

@SpringBootTest
@AutoConfigureMockMvc
class QaKeysImportTest : ProjectAuthControllerTest("/v2/projects/") {
  @MockitoBean
  @Autowired
  lateinit var postHog: PostHog

  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  private lateinit var qa: QaTestUtil

  @Autowired
  private lateinit var batchJobService: BatchJobService

  @Autowired
  private lateinit var transactionManager: PlatformTransactionManager

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
  fun `keys import triggers QA checks`() {
    performProjectAuthPost(
      "keys/import",
      mapOf(
        "keys" to
          listOf(
            mapOf(
              "name" to "imported-key",
              "translations" to mapOf("en" to "Hello.", "fr" to "bonjour"),
            ),
          ),
      ),
    ).andIsOk

    waitForNotThrowing(timeout = 10_000, pollTime = 500) {
      executeInNewTransaction(transactionManager) {
        val jobs = batchJobService.getAllByProjectId(testData.project.id)
        val qaJobs = jobs.filter { it.type == BatchJobType.QA_CHECK }
        qaJobs.assert.isNotEmpty
      }
    }
  }
}
