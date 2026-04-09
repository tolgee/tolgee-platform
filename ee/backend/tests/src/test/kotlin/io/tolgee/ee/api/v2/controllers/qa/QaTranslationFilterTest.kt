package io.tolgee.ee.api.v2.controllers.qa

import com.posthog.server.PostHog
import io.tolgee.constants.Feature
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.development.QaTestData
import io.tolgee.ee.utils.QaTestUtil
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.enums.qa.QaIssueState
import io.tolgee.repository.qa.TranslationQaIssueRepository
import io.tolgee.testing.AuthorizedControllerTest
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
class QaTranslationFilterTest : AuthorizedControllerTest() {
  @MockitoBean
  @Autowired
  lateinit var postHog: PostHog

  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  lateinit var qa: QaTestUtil

  @Autowired
  private lateinit var qaIssueRepository: TranslationQaIssueRepository

  lateinit var testData: QaTestData

  private val translationsUrl
    get() = "/v2/projects/${testData.project.id}/translations"

  @BeforeEach
  fun setup() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.QA_CHECKS)
    testData = QaTestData()
    // Add a second key with clean translation
    testData.projectBuilder
      .addKey {
        name = "clean-key"
      }.build {
        addTranslation("en", "Clean text.")
        addTranslation("fr", "Texte propre.")
      }
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
  fun `filters translations with QA issues in language`() {
    // Create QA issues on the FR translation of test-key
    qa.runChecksAndPersist(testData.frTranslation)

    performAuthGet(
      "$translationsUrl?filterHasQaIssuesInLang=fr",
    ).andIsOk.andAssertThatJson {
      node("_embedded.keys").isArray.hasSize(1)
      node("_embedded.keys[0].keyName").isEqualTo("test-key")
    }
  }

  @Test
  fun `excludes translations with only ignored issues`() {
    qa.runChecksAndPersist(testData.frTranslation)

    // Ignore all issues
    executeInNewTransaction(platformTransactionManager) {
      val issues = qaIssueRepository.findAll().filter { it.translation.id == testData.frTranslation.id }
      issues.forEach { it.state = QaIssueState.IGNORED }
      qaIssueRepository.saveAll(issues)
    }

    performAuthGet(
      "$translationsUrl?filterHasQaIssuesInLang=fr",
    ).andIsOk.andAssertThatJson {
      node("_embedded.keys").isAbsent()
    }
  }

  @Test
  fun `filters by specific check type`() {
    qa.runChecksAndPersist(testData.frTranslation)

    performAuthGet(
      "$translationsUrl?filterQaCheckType=PUNCTUATION_MISMATCH",
    ).andIsOk.andAssertThatJson {
      node("_embedded.keys").isArray.hasSize(1)
      node("_embedded.keys[0].keyName").isEqualTo("test-key")
    }
  }

  @Test
  fun `filters by multiple check types`() {
    qa.runChecksAndPersist(testData.frTranslation)

    performAuthGet(
      "$translationsUrl?filterQaCheckType=PUNCTUATION_MISMATCH&filterQaCheckType=CHARACTER_CASE_MISMATCH",
    ).andIsOk.andAssertThatJson {
      node("_embedded.keys").isArray.hasSize(1)
      node("_embedded.keys[0].keyName").isEqualTo("test-key")
    }
  }

  @Test
  fun `returns all translations when no QA filter`() {
    qa.runChecksAndPersist(testData.frTranslation)

    performAuthGet(translationsUrl).andIsOk.andAssertThatJson {
      node("_embedded.keys").isArray.hasSize(2)
    }
  }

  @Test
  fun `combines QA filter with language filter`() {
    qa.runChecksAndPersist(testData.frTranslation)

    performAuthGet(
      "$translationsUrl?filterHasQaIssuesInLang=fr&languages=fr",
    ).andIsOk.andAssertThatJson {
      node("_embedded.keys").isArray.hasSize(1)
    }
  }
}
