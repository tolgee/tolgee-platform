package io.tolgee.ee.service.qa

import com.posthog.server.PostHog
import io.tolgee.ee.development.QaTestData
import io.tolgee.ee.utils.QaTestUtil
import io.tolgee.fixtures.assertPostHogEventReported
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.repository.qa.TranslationQaIssueRepository
import io.tolgee.testing.AuthorizedControllerTest
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
class QaBatchServiceTest : AuthorizedControllerTest() {
  @MockitoBean
  @Autowired
  lateinit var postHog: PostHog

  @Autowired
  private lateinit var qaCheckBatchService: QaCheckBatchServiceImpl

  @Autowired
  private lateinit var qaIssueRepository: TranslationQaIssueRepository

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

  @Test
  @Transactional
  fun `creates QA issues when translation has problems`() {
    refetchEntities()

    qaCheckBatchService.runChecksAndPersist(
      testData.project.id,
      testData.testKey.id,
      testData.frTranslation.language.id,
    )
    entityManager.flush()

    val issues = qaIssueRepository.findAllByTranslationId(testData.frTranslation.id)
    assertThat(issues).isNotEmpty
    assertThat(issues.map { it.type.name }).contains("CHARACTER_CASE_MISMATCH", "PUNCTUATION_MISMATCH")
  }

  @Test
  @Transactional
  fun `creates no issues when translation is clean`() {
    refetchEntities()

    testData.frTranslation.text = "Bonjour monde."
    entityManager.flush()

    qaCheckBatchService.runChecksAndPersist(
      testData.project.id,
      testData.testKey.id,
      testData.frTranslation.language.id,
    )
    entityManager.flush()

    val issues = qaIssueRepository.findAllByTranslationId(testData.frTranslation.id)
    assertThat(issues).isEmpty()
  }

  @Test
  @Transactional
  fun `replaces existing issues on re-check`() {
    refetchEntities()

    qaCheckBatchService.runChecksAndPersist(
      testData.project.id,
      testData.testKey.id,
      testData.frTranslation.language.id,
    )
    entityManager.flush()
    val issuesBefore = qaIssueRepository.findAllByTranslationId(testData.frTranslation.id)
    assertThat(issuesBefore).isNotEmpty

    // Update translation to be clean and re-check
    testData.frTranslation.text = "Bonjour monde."
    entityManager.flush()

    qaCheckBatchService.runChecksAndPersist(
      testData.project.id,
      testData.testKey.id,
      testData.frTranslation.language.id,
    )
    entityManager.flush()

    val issuesAfter = qaIssueRepository.findAllByTranslationId(testData.frTranslation.id)
    assertThat(issuesAfter).isEmpty()
  }

  @Test
  @Transactional
  fun `reports QA_CHECK_RUN event`() {
    refetchEntities()

    qaCheckBatchService.runChecksAndPersist(
      testData.project.id,
      testData.testKey.id,
      testData.frTranslation.language.id,
    )
    entityManager.flush()

    assertPostHogEventReported(postHog, "QA_CHECK_RUN")
  }
}
