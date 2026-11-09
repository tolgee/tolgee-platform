package io.tolgee.ee.aiReview

import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.events.OnBatchJobFinalized
import io.tolgee.batch.request.AiReviewRequest
import io.tolgee.batch.request.SetTranslationsStateStateRequest
import io.tolgee.configuration.tolgee.machineTranslation.LlmProperties
import io.tolgee.constants.Feature
import io.tolgee.dtos.PromptResult
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.component.aiReview.AiReviewActivityListener
import io.tolgee.ee.component.llm.OpenaiApiService
import io.tolgee.ee.development.AiReviewTestData
import io.tolgee.ee.service.aiReview.AiReviewActionService
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.Project
import io.tolgee.model.Prompt
import io.tolgee.model.UserAccount
import io.tolgee.model.aiReview.AiReviewRule
import io.tolgee.model.aiReview.TranslationAiReview
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.model.enums.LlmProviderType
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.aiReview.AiReviewFailureReason
import io.tolgee.model.enums.aiReview.AiReviewRuleMode
import io.tolgee.model.enums.aiReview.AiReviewStatus
import io.tolgee.model.qa.TranslationQaIssue
import io.tolgee.model.translation.Translation
import io.tolgee.repository.aiReview.TranslationAiReviewRepository
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import java.util.Date
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
  private lateinit var aiReviewActionService: AiReviewActionService

  @Autowired
  @MockitoBean
  private lateinit var openaiApiService: OpenaiApiService

  @Autowired
  @MockitoSpyBean
  private lateinit var aiReviewActivityListener: AiReviewActivityListener

  lateinit var testData: AiReviewTestData
  lateinit var strictRule: AiReviewRule
  lateinit var pendingRow: TranslationAiReview

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
    pendingRow = testData.addReview(testData.deTranslationBuilder, AiReviewStatus.PENDING).self
  }

  @AfterEach
  fun cleanup() {
    reset(aiReviewActivityListener)
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
  fun `an open QA issue appearing during the call drops the result`() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.AI_REVIEW, Feature.QA_CHECKS)
    testData.project.useQaChecks = true
    testData.rule.skipOpenQaIssues = true
    testData.deTranslation.qaChecksStale = false
    save()
    stubWithMidFlightChange {
      val translation = entityManager.find(Translation::class.java, testData.deTranslation.id)
      entityManager.persist(TranslationQaIssue(translation = translation))
    }

    runJob(testData.deTranslation.id)

    waitForStatus(AiReviewStatus.SKIPPED_QA_ISSUES)
    val row = review(testData.deTranslation.id)!!
    row.batchJobId.assert.isNull()
    translation(testData.deTranslation.id).state.assert.isEqualTo(TranslationState.TRANSLATED)
  }

  @Test
  fun `losing the feature during the call drops the result`() {
    save()
    stubWithMidFlightChange {
      enabledFeaturesProvider.forceEnabled = emptySet()
    }

    runJob(testData.deTranslation.id)

    val row = review(testData.deTranslation.id)!!
    row.status.assert.isEqualTo(AiReviewStatus.PENDING)
    row.batchJobId.assert.isNull()
    translation(testData.deTranslation.id).state.assert.isEqualTo(TranslationState.TRANSLATED)
  }

  @Test
  fun `a human review during the call drops the result and keeps the human review`() {
    testData.rule.threshold = 95
    testData.deTranslation.auto = true
    save()
    stubWithMidFlightChange {
      val translation = entityManager.find(Translation::class.java, testData.deTranslation.id)
      translation.state = TranslationState.REVIEWED
    }

    runJob(testData.deTranslation.id)

    translation(testData.deTranslation.id).state.assert.isEqualTo(TranslationState.REVIEWED)
    review(testData.deTranslation.id).assert.isNull()
    verify(openaiApiService, times(1)).translate(any(), any(), any())
  }

  @Test
  fun `a human reject during a re-review of an approval keeps the rejection`() {
    approvalQueuedForReReview()
    save()
    stubWithMidFlightChange {
      val translation = entityManager.find(Translation::class.java, testData.deTranslation.id)
      aiReviewActionService.reject(translation, testData.user.id)
    }

    runJob(testData.deTranslation.id)

    translation(testData.deTranslation.id).state.assert.isEqualTo(TranslationState.TRANSLATED)
    val row = review(testData.deTranslation.id)!!
    row.status.assert.isEqualTo(AiReviewStatus.REJECTED)
    row.batchJobId.assert.isNull()
    rule().autoReviewedCount.assert.isEqualTo(0)
    rule().rejectedCount.assert.isEqualTo(1)
    verify(openaiApiService, times(1)).translate(any(), any(), any())
  }

  @Test
  fun `an un-review during a re-review of an approval drops the result`() {
    approvalQueuedForReReview()
    save()
    stubWithMidFlightChange {
      val translation = entityManager.find(Translation::class.java, testData.deTranslation.id)
      translation.state = TranslationState.TRANSLATED
    }

    runJob(testData.deTranslation.id)

    translation(testData.deTranslation.id).state.assert.isEqualTo(TranslationState.TRANSLATED)
    val row = review(testData.deTranslation.id)!!
    row.batchJobId.assert.isNull()
    row.reReviewRequested.assert.isFalse()
    row.reviewedAt.assert.isNull()
    rule().autoReviewedCount.assert.isEqualTo(0)
    verify(openaiApiService, times(1)).translate(any(), any(), any())
  }

  @Test
  fun `an un-review during a re-review of a FAILED marker deletes the marker`() {
    testData.deTranslation.state = TranslationState.REVIEWED
    testData.deTranslation.auto = true
    pendingRow.status = AiReviewStatus.FAILED
    pendingRow.reReviewRequested = true
    save()
    stubWithMidFlightChange {
      val translation = entityManager.find(Translation::class.java, testData.deTranslation.id)
      translation.state = TranslationState.TRANSLATED
    }

    runJob(testData.deTranslation.id)

    review(testData.deTranslation.id).assert.isNull()
    verify(openaiApiService, times(1)).translate(any(), any(), any())
  }

  @Test
  fun `an AI claim after a bulk review chunk commits keeps the human review`() {
    testData.rule.threshold = 95
    testData.deTranslation.auto = true
    save()
    doNothing().whenever(aiReviewActivityListener).onBatchJobFinalized(any<OnBatchJobFinalized>())
    stubWithMidFlightChange {}

    bulkSetGermanState(TranslationState.REVIEWED)
    runJob(testData.deTranslation.id)

    translation(testData.deTranslation.id).state.assert.isEqualTo(TranslationState.REVIEWED)
    review(testData.deTranslation.id).assert.isNull()
    verify(openaiApiService, never()).translate(any(), any(), any())
  }

  @Test
  fun `an AI claim after a bulk un-review chunk commits keeps the rejection`() {
    approvalQueuedForReReview()
    save()
    doNothing().whenever(aiReviewActivityListener).onBatchJobFinalized(any<OnBatchJobFinalized>())
    stubWithMidFlightChange {}

    bulkSetGermanState(TranslationState.TRANSLATED)
    runJob(testData.deTranslation.id)

    translation(testData.deTranslation.id).state.assert.isEqualTo(TranslationState.TRANSLATED)
    review(testData.deTranslation.id)!!.status.assert.isEqualTo(AiReviewStatus.REJECTED)
    rule().rejectedCount.assert.isEqualTo(1)
    verify(openaiApiService, never()).translate(any(), any(), any())
  }

  @Test
  fun `a stale-hash claim clears the previous failure fields`() {
    pendingRow.textHash = "stale-hash"
    pendingRow.lastFailureReason = AiReviewFailureReason.PARSE_ERROR
    pendingRow.lastFailedAt = Date()
    save()
    llmProperties.providers = mutableListOf()

    runJob(testData.deTranslation.id)

    waitForStatus(AiReviewStatus.FAILED)
    val row = review(testData.deTranslation.id)!!
    row.failureReason.assert.isEqualTo(AiReviewFailureReason.NO_PROVIDER)
    row.lastFailureReason.assert.isNull()
    row.lastFailedAt.assert.isNull()
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

  private fun approvalQueuedForReReview() {
    testData.deTranslation.state = TranslationState.REVIEWED
    testData.deTranslation.auto = true
    pendingRow.status = AiReviewStatus.APPROVED
    pendingRow.score = 90
    pendingRow.rule = testData.rule
    pendingRow.threshold = 85
    pendingRow.reReviewRequested = true
  }

  private fun rule(): AiReviewRule =
    executeInNewTransaction(platformTransactionManager) {
      entityManager.find(AiReviewRule::class.java, testData.rule.id)
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

  private fun bulkSetGermanState(state: TranslationState) {
    val job =
      executeInNewTransaction(platformTransactionManager) {
        batchJobService.startJob(
          request =
            SetTranslationsStateStateRequest().apply {
              keyIds = listOf(testData.key.id)
              languageIds = listOf(testData.german.id)
              this.state = state
            },
          project = entityManager.getReference(Project::class.java, testData.project.id),
          author = entityManager.getReference(UserAccount::class.java, testData.user.id),
          type = BatchJobType.SET_TRANSLATIONS_STATE,
          isHidden = false,
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
