package io.tolgee.ee.service.qa

import com.posthog.server.PostHog
import io.tolgee.ee.development.QaTestData
import io.tolgee.ee.utils.QaTestUtil
import io.tolgee.fixtures.assertPostHogEventReported
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.repository.TranslationRepository
import io.tolgee.repository.qa.TranslationQaIssueRepository
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.util.executeInNewTransaction
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
class QaCheckBatchServiceTest : AuthorizedControllerTest() {
  @MockitoBean
  @Autowired
  lateinit var postHog: PostHog

  @Autowired
  private lateinit var qaCheckBatchService: QaCheckBatchServiceImpl

  @Autowired
  private lateinit var qaIssueRepository: TranslationQaIssueRepository

  @Autowired
  private lateinit var translationRepository: TranslationRepository

  @Autowired
  lateinit var qa: QaTestUtil

  lateinit var testData: QaTestData

  @BeforeEach
  fun setup() {
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
  }

  private fun refetchEntities() {
    testData.testKey = entityManager.find(Key::class.java, testData.testKey.id)
    testData.frTranslation = entityManager.find(Translation::class.java, testData.frTranslation.id)
  }

  private fun runChecks(languageId: Long = testData.frTranslation.language.id) {
    qaCheckBatchService.runChecksAndPersist(
      testData.project.id,
      testData.testKey.id,
      languageId,
    )
  }

  @Test
  @Transactional
  fun `creates QA issues when translation has problems`() {
    refetchEntities()

    runChecks()
    entityManager.flush()

    val issues = qaIssueRepository.findAllByTranslationId(testData.frTranslation.id)
    assertThat(issues).isNotEmpty
    assertThat(issues.map { it.type.name }).contains("CHARACTER_CASE_MISMATCH", "PUNCTUATION_MISMATCH")
  }

  @Test
  fun `creates no issues when translation is clean`() {
    // Update translation text in a separate transaction first to avoid deadlock:
    // runChecksAndPersist opens its own transaction which would conflict with
    // an outer @Transactional holding locks on the modified translation row
    executeInNewTransaction(platformTransactionManager) {
      val translation = entityManager.find(Translation::class.java, testData.frTranslation.id)
      translation.text = "Bonjour monde."
    }

    runChecks()

    executeInNewTransaction(platformTransactionManager) {
      val issues = qaIssueRepository.findAllByTranslationId(testData.frTranslation.id)
      assertThat(issues).isEmpty()
    }
  }

  @Test
  fun `replaces existing issues on re-check`() {
    // First check — translation has problems
    runChecks()

    executeInNewTransaction(platformTransactionManager) {
      val issuesBefore = qaIssueRepository.findAllByTranslationId(testData.frTranslation.id)
      assertThat(issuesBefore).isNotEmpty
    }

    // Update translation to be clean in a separate transaction
    executeInNewTransaction(platformTransactionManager) {
      val translation = entityManager.find(Translation::class.java, testData.frTranslation.id)
      translation.text = "Bonjour monde."
    }

    // Re-check — should find no issues
    runChecks()

    executeInNewTransaction(platformTransactionManager) {
      val issuesAfter = qaIssueRepository.findAllByTranslationId(testData.frTranslation.id)
      assertThat(issuesAfter).isEmpty()
    }
  }

  @Test
  fun `creates QA issues for key with no existing translation`() {
    // Delete the French translation so getOrCreate() will create a new one
    executeInNewTransaction(platformTransactionManager) {
      val translation = entityManager.find(Translation::class.java, testData.frTranslation.id)
      qaIssueRepository.deleteAllByTranslationId(translation.id)
      entityManager.remove(translation)
    }

    // This should NOT throw TransientPropertyValueException
    runChecks(testData.frenchLanguage.id)

    // Verify that a translation was created and QA issues were persisted
    executeInNewTransaction(platformTransactionManager) {
      val translation =
        translationRepository.findOneByProjectIdAndKeyIdAndLanguageId(
          testData.project.id,
          testData.testKey.id,
          testData.frenchLanguage.id,
        )
      assertThat(translation).isNotNull
      val issues = qaIssueRepository.findAllByTranslationId(translation!!.id)
      assertThat(issues).isNotEmpty
    }
  }

  @Test
  @Transactional
  fun `reports QA_CHECK_RUN event`() {
    refetchEntities()

    runChecks()
    entityManager.flush()

    assertPostHogEventReported(postHog, "QA_CHECK_RUN")
  }
}
