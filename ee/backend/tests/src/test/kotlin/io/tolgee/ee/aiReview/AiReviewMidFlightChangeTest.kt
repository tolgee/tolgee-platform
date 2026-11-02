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
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.Project
import io.tolgee.model.Prompt
import io.tolgee.model.aiReview.AiReviewRule
import io.tolgee.model.aiReview.TranslationAiReview
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.model.enums.LlmProviderType
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.aiReview.AiReviewRuleMode
import io.tolgee.model.enums.aiReview.AiReviewStatus
import io.tolgee.repository.aiReview.TranslationAiReviewRepository
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.util.concurrent.atomic.AtomicBoolean

@SpringBootTest
@AutoConfigureMockMvc
class AiReviewMidFlightChangeTest : AuthorizedControllerTest() {
  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  private lateinit var batchJobService: BatchJobService

  @Autowired
  private lateinit var translationAiReviewRepository: TranslationAiReviewRepository

  @Autowired
  @MockitoBean
  private lateinit var openaiApiService: OpenaiApiService

  lateinit var testData: AiReviewTestData
  lateinit var strictRule: AiReviewRule

  @BeforeEach
  fun setup() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.AI_REVIEW)
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
    testData = AiReviewTestData()
    testData.rule.prompt = testData.reviewPrompt
    strictRule =
      testData
        .addRule {
          name = "Strict"
          mode = AiReviewRuleMode.ACTIVE
          threshold = 99
          enabled = false
          tagIn = mutableListOf("ui")
        }.self
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.PENDING, null)
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
  fun `a prompt revision change during the call drops the result and the row is reviewed again`() {
    save()
    stubWithMidFlightChange {
      val prompt = entityManager.find(Prompt::class.java, testData.reviewPrompt.id)
      prompt.revision += 1
    }

    runJob(testData.deTranslation.id)

    waitForStatus(AiReviewStatus.APPROVED)
    review(testData.deTranslation.id)!!.promptRevision.assert.isEqualTo(1)
    verify(openaiApiService, times(2)).translate(any(), any(), any())
  }

  @Test
  fun `a switch to Dry run during the call drops the result and the re-review stays dry`() {
    save()
    stubWithMidFlightChange {
      val rule = entityManager.find(AiReviewRule::class.java, testData.rule.id)
      rule.mode = AiReviewRuleMode.DRY_RUN
    }

    runJob(testData.deTranslation.id)

    waitForStatus(AiReviewStatus.DRY_RUN_PASS)
    translation(testData.deTranslation.id).state.assert.isEqualTo(TranslationState.TRANSLATED)
    verify(openaiApiService, times(2)).translate(any(), any(), any())
  }

  @Test
  fun `a stricter rule winning during the call drops the result and the re-review uses it`() {
    save()
    stubWithMidFlightChange {
      val rule = entityManager.find(AiReviewRule::class.java, strictRule.id)
      rule.enabled = true
    }

    runJob(testData.deTranslation.id)

    waitForStatus(AiReviewStatus.BELOW_THRESHOLD)
    val row = review(testData.deTranslation.id)!!
    row.rule!!
      .id.assert
      .isEqualTo(strictRule.id)
    row.threshold.assert.isEqualTo(99)
    translation(testData.deTranslation.id).state.assert.isEqualTo(TranslationState.TRANSLATED)
    verify(openaiApiService, times(2)).translate(any(), any(), any())
  }

  @Test
  fun `a threshold raised during the call keeps the result and judges against the new value`() {
    save()
    stubWithMidFlightChange {
      val rule = entityManager.find(AiReviewRule::class.java, testData.rule.id)
      rule.threshold = 95
    }

    runJob(testData.deTranslation.id)

    val row = review(testData.deTranslation.id)!!
    row.status.assert.isEqualTo(AiReviewStatus.BELOW_THRESHOLD)
    row.score.assert.isEqualTo(90)
    row.threshold.assert.isEqualTo(95)
    translation(testData.deTranslation.id).state.assert.isEqualTo(TranslationState.TRANSLATED)
    verify(openaiApiService, times(1)).translate(any(), any(), any())
  }

  private fun stubWithMidFlightChange(change: () -> Unit) {
    val changed = AtomicBoolean(false)
    whenever(openaiApiService.translate(any(), any(), any())).thenAnswer {
      if (changed.compareAndSet(false, true)) {
        executeInNewTransaction(platformTransactionManager) {
          change()
        }
      }
      PromptResult(response = SCORE_90_RESPONSE, usage = null)
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
        .status.assert
        .isEqualTo(BatchJobStatus.SUCCESS)
    }
  }

  private fun waitForStatus(status: AiReviewStatus) {
    waitForNotThrowing(pollTime = 100, timeout = 30_000) {
      review(testData.deTranslation.id)?.status.assert.isEqualTo(status)
    }
  }

  private fun review(translationId: Long): TranslationAiReview? =
    executeInNewTransaction(platformTransactionManager) {
      translationAiReviewRepository.findByTranslationId(translationId)?.also { it.rule?.id }
    }

  private fun translation(id: Long) =
    executeInNewTransaction(platformTransactionManager) {
      translationService.get(id)
    }

  companion object {
    private const val SCORE_90_RESPONSE =
      """{"issues": [{"severity": "minor", "category": "style", "message": "Small issue.", "points": 10}]}"""
  }
}
