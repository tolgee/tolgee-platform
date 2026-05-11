package io.tolgee.ee.api.v2.controllers.qa

import com.posthog.server.PostHog
import io.tolgee.constants.Feature
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.development.QaTestData
import io.tolgee.ee.utils.QaTestUtil
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.translation.Translation
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.util.executeInNewTransaction
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest
@AutoConfigureMockMvc
class QaDashboardStatsTest : AuthorizedControllerTest() {
  @MockitoBean
  @Autowired
  lateinit var postHog: PostHog

  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  lateinit var qa: QaTestUtil

  lateinit var testData: QaTestData

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
  fun `language stats include qaIssueCount`() {
    qa.runChecksAndPersist(testData.frTranslation)
    languageStatsService.refreshLanguageStats(testData.project.id)

    performAuthGet(
      "/v2/projects/${testData.project.id}/stats",
    ).andIsOk.andAssertThatJson {
      node("languageStats").isArray.isNotEmpty
      node("languageStats").isArray.anySatisfy {
        assertThatJson(it).node("languageTag").isEqualTo("fr")
        assertThatJson(it).node("qaIssueCount").isNumber.isNotEqualTo(java.math.BigDecimal.ZERO)
      }
    }
  }

  @Test
  fun `language stats include qaChecksStaleCount`() {
    executeInNewTransaction(platformTransactionManager) {
      val translation = entityManager.find(Translation::class.java, testData.frTranslation.id)
      translation.qaChecksStale = true
      entityManager.persist(translation)
    }
    languageStatsService.refreshLanguageStats(testData.project.id)

    performAuthGet(
      "/v2/projects/${testData.project.id}/stats",
    ).andIsOk.andAssertThatJson {
      node("languageStats").isArray.anySatisfy {
        assertThatJson(it).node("languageTag").isEqualTo("fr")
        assertThatJson(it).node("qaChecksStaleCount").isNumber.isNotEqualTo(java.math.BigDecimal.ZERO)
      }
    }
  }

  @Test
  fun `language stats QA counts are zero when QA is disabled`() {
    qa.runChecksAndPersist(testData.frTranslation)

    executeInNewTransaction(platformTransactionManager) {
      val project = projectService.get(testData.project.id)
      project.useQaChecks = false
      entityManager.persist(project)
    }
    languageStatsService.refreshLanguageStats(testData.project.id)

    performAuthGet(
      "/v2/projects/${testData.project.id}/stats",
    ).andIsOk.andAssertThatJson {
      node("languageStats").isArray.anySatisfy {
        assertThatJson(it).node("languageTag").isEqualTo("fr")
        assertThatJson(it).node("qaIssueCount").isEqualTo(0)
        assertThatJson(it).node("qaChecksStaleCount").isEqualTo(0)
      }
    }
  }
}
