package io.tolgee.ee.aiReview

import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.request.AiReviewRequest
import io.tolgee.configuration.tolgee.machineTranslation.LlmProperties
import io.tolgee.constants.Feature
import io.tolgee.dtos.PromptResult
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.component.llm.OpenaiApiService
import io.tolgee.ee.development.AiReviewTestData
import io.tolgee.ee.service.qa.QaIssueService
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.Project
import io.tolgee.model.aiReview.TranslationAiReview
import io.tolgee.model.enums.LlmProviderType
import io.tolgee.model.enums.aiReview.AiReviewStatus
import io.tolgee.model.enums.qa.QaIssueState
import io.tolgee.model.qa.TranslationQaIssue
import io.tolgee.repository.aiReview.TranslationAiReviewRepository
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest
@AutoConfigureMockMvc
class AiReviewQaGateTest : AuthorizedControllerTest() {
  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  private lateinit var batchJobService: BatchJobService

  @Autowired
  private lateinit var translationAiReviewRepository: TranslationAiReviewRepository

  @Autowired
  private lateinit var qaIssueService: QaIssueService

  @Autowired
  @MockitoBean
  private lateinit var openaiApiService: OpenaiApiService

  lateinit var testData: AiReviewTestData
  lateinit var openIssue: TranslationQaIssue

  @BeforeEach
  fun setup() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.AI_REVIEW, Feature.QA_CHECKS)
    llmProperties.enabled = true
    llmProperties.providers =
      mutableListOf(
        LlmProperties.LlmProvider(
          name = "server-provider",
          type = LlmProviderType.OPENAI,
          apiUrl = "http://test.com",
          model = "gpt-test",
        ),
      )
    clearInvocations(openaiApiService)
    whenever(openaiApiService.defaultAttempts()).thenReturn(listOf(30))
    whenever(openaiApiService.translate(any(), any(), any()))
      .thenReturn(PromptResult(response = """{"issues": []}""", usage = null))
    testData = AiReviewTestData()
    testData.project.useQaChecks = true
    testData.rule.skipOpenQaIssues = true
    testData.deTranslation.qaChecksStale = false
    openIssue = testData.deTranslationBuilder.addQaIssue { state = QaIssueState.OPEN }.self
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
    userAccount = null
    enabledFeaturesProvider.forceEnabled = null
    llmProperties.enabled = false
    llmProperties.providers = mutableListOf()
  }

  private fun save() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  @Test
  fun `an open QA issue with skipOpenQaIssues gives SKIPPED_QA_ISSUES and no LLM call`() {
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.PENDING)
    save()

    runJob(testData.deTranslation.id)

    review(testData.deTranslation.id)!!.status.assert.isEqualTo(AiReviewStatus.SKIPPED_QA_ISSUES)
    verify(openaiApiService, never()).translate(any(), any(), any())
  }

  @Test
  fun `with skipOpenQaIssues off the review runs despite open issues`() {
    testData.rule.skipOpenQaIssues = false
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.PENDING)
    save()

    runJob(testData.deTranslation.id)

    review(testData.deTranslation.id)!!.status.assert.isEqualTo(AiReviewStatus.APPROVED)
  }

  @Test
  fun `the option is ignored when QA is off for the project`() {
    testData.project.useQaChecks = false
    testData.deTranslation.qaChecksStale = true
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.PENDING)
    save()

    runJob(testData.deTranslation.id)

    review(testData.deTranslation.id)!!.status.assert.isEqualTo(AiReviewStatus.APPROVED)
  }

  @Test
  fun `ignoring the last open issue reviews the translation`() {
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.SKIPPED_QA_ISSUES)
    save()

    ignoreIssue(openIssue.id)

    waitForNotThrowing(pollTime = 100, timeout = 30_000) {
      review(testData.deTranslation.id)!!.status.assert.isEqualTo(AiReviewStatus.APPROVED)
    }
  }

  @Test
  fun `ignoring one of two open issues does not review`() {
    val secondIssue = testData.deTranslationBuilder.addQaIssue { state = QaIssueState.OPEN }.self
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.SKIPPED_QA_ISSUES)
    save()

    ignoreIssue(secondIssue.id)

    review(testData.deTranslation.id)!!.status.assert.isEqualTo(AiReviewStatus.SKIPPED_QA_ISSUES)
    verify(openaiApiService, never()).translate(any(), any(), any())
  }

  private fun ignoreIssue(issueId: Long) {
    executeInNewTransaction(platformTransactionManager) {
      qaIssueService.ignoreIssue(testData.project.id, testData.deTranslation.id, issueId)
    }
  }

  private fun runJob(vararg translationIds: Long) {
    val job =
      executeInNewTransaction(platformTransactionManager) {
        val projectRef = entityManager.getReference(Project::class.java, testData.project.id)
        batchJobService.startJob(
          request = AiReviewRequest(translationIds = translationIds.toList()),
          project = projectRef,
          author = null,
          type = BatchJobType.AI_REVIEW,
          isHidden = true,
        )
      }
    waitForNotThrowing(pollTime = 100, timeout = 30_000) {
      batchJobService
        .getJobDtoNoCache(job.id)
        .status.completed.assert
        .isTrue()
    }
  }

  private fun review(translationId: Long): TranslationAiReview? =
    executeInNewTransaction(platformTransactionManager) {
      translationAiReviewRepository.findByTranslationId(translationId)
    }
}
