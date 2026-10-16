package io.tolgee.ee.aiReview

import io.tolgee.batch.data.BatchTranslationTargetItem
import io.tolgee.constants.Message
import io.tolgee.dtos.request.translation.SetTranslationsWithKeyDto
import io.tolgee.exceptions.LlmRateLimitedException
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.aiReview.AiReviewOutcome
import io.tolgee.service.aiReview.AiReviewBatchService
import io.tolgee.testing.assert
import io.tolgee.util.aiReviewTextHash
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.client.ResourceAccessException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

class AiReviewFailureTest : AbstractAiReviewTest() {
  @Autowired
  lateinit var aiReviewBatchService: AiReviewBatchService

  private fun inOtherThread(action: () -> Unit) {
    var error: Throwable? = null
    thread { runCatching(action).onFailure { error = it } }.join()
    error?.let { throw it }
  }

  private fun onFirstLlmCall(action: () -> Unit) {
    val calls = AtomicInteger()
    doAnswer {
      if (calls.getAndIncrement() == 0) {
        action()
      }
      it.callRealMethod()
    }.`when`(llmProviderService).callProvider(any(), any(), any(), anyOrNull())
  }

  @Test
  fun `out of credits is stored as a failure and leaves the state`() {
    machineTranslationProperties.freeCreditsAmount = 0
    testData.addRule()
    saveTestData()
    val translationId = testData.uiGerman.self.id

    setText("ui-key", "de", "Jetzt bezahlen")

    waitForReview(translationId) {
      it!!.outcome.assert.isEqualTo(AiReviewOutcome.FAILED)
    }
    findReview(translationId)!!.errorCode.assert.isEqualTo(Message.OUT_OF_CREDITS.code)
    getTranslation(translationId).state.assert.isEqualTo(TranslationState.TRANSLATED)
    waitForProjectJobsCompleted()
    aiReviewJobs().map { it.status.completed }.assert.containsOnly(true)
  }

  @Test
  fun `failed review can be retried`() {
    Mockito
      .doThrow(ResourceAccessException("Connection refused"))
      .`when`(llmProviderService)
      .callProvider(any(), any(), any(), anyOrNull())
    testData.addRule()
    saveTestData()
    val translationId = testData.uiGerman.self.id
    setText("ui-key", "de", "Jetzt bezahlen")
    waitForReview(translationId) {
      it!!.outcome.assert.isEqualTo(AiReviewOutcome.FAILED)
    }
    waitForProjectJobsCompleted()
    Mockito.reset(llmProviderService)

    performAuthPost("/v2/projects/$projectId/ai-review/translations/$translationId/retry", null).andIsOk

    waitForReview(translationId) {
      it!!.outcome.assert.isEqualTo(AiReviewOutcome.AUTO_REVIEWED)
    }
    getTranslation(translationId).state.assert.isEqualTo(TranslationState.REVIEWED)
  }

  @Test
  fun `only failed reviews can be retried`() {
    val rule = testData.addRule()
    testData.uiGerman.addAiReview {
      outcome = AiReviewOutcome.BELOW_THRESHOLD
      textHash = aiReviewTextHash(testData.uiGerman.self.text)
      this.rule = rule.self
      score = 50
      threshold = 85
    }
    saveTestData()

    performAuthPost("/v2/projects/$projectId/ai-review/translations/${testData.uiGerman.self.id}/retry", null)
      .andIsBadRequest
      .andHasErrorMessage(Message.AI_REVIEW_CANNOT_BE_RETRIED)
  }

  @Test
  fun `a failure after the LLM call releases the claim so the retried chunk reviews the row`() {
    testData.addRule()
    saveTestData()
    val translationId = testData.uiGerman.self.id
    val failures = AtomicInteger()
    doAnswer {
      val result = it.callRealMethod()
      if (failures.getAndIncrement() == 0) {
        throw IllegalStateException("Simulated failure after the LLM call")
      }
      result
    }.`when`(llmProviderService).callProvider(any(), any(), any(), anyOrNull())

    setText("ui-key", "de", "Jetzt bezahlen")

    waitForReview(translationId) {
      it!!.outcome.assert.isEqualTo(AiReviewOutcome.AUTO_REVIEWED)
    }
    getTranslation(translationId).state.assert.isEqualTo(TranslationState.REVIEWED)
  }

  @Test
  fun `provider error is stored as a failure`() {
    Mockito
      .doThrow(ResourceAccessException("Connection refused"))
      .`when`(llmProviderService)
      .callProvider(any(), any(), any(), anyOrNull())
    testData.addRule()
    saveTestData()
    val translationId = testData.uiGerman.self.id

    setText("ui-key", "de", "Jetzt bezahlen")

    waitForReview(translationId) {
      it!!.outcome.assert.isEqualTo(AiReviewOutcome.FAILED)
    }
    findReview(translationId)!!.errorCode.assert.isEqualTo(Message.LLM_PROVIDER_ERROR.code)
    getTranslation(translationId).state.assert.isEqualTo(TranslationState.TRANSLATED)
  }

  @Test
  fun `rate limited review is retried instead of failing`() {
    val calls = AtomicInteger()
    doAnswer {
      if (calls.getAndIncrement() == 0) {
        throw LlmRateLimitedException(retryAt = System.currentTimeMillis() + 100)
      }
      it.callRealMethod()
    }.`when`(llmProviderService).callProvider(any(), any(), any(), anyOrNull())
    testData.addRule()
    saveTestData()
    val translationId = testData.uiGerman.self.id

    setText("ui-key", "de", "Jetzt bezahlen")

    waitForReview(translationId) {
      it!!.outcome.assert.isEqualTo(AiReviewOutcome.AUTO_REVIEWED)
    }
    calls.get().assert.isEqualTo(2)
    getTranslation(translationId).state.assert.isEqualTo(TranslationState.REVIEWED)
  }

  @Test
  fun `drops the result when the text changes during the LLM call`() {
    testData.addRule()
    saveTestData()
    val translationId = testData.uiGerman.self.id
    onFirstLlmCall {
      inOtherThread {
        performAuthPut(
          "/v2/projects/$projectId/translations",
          SetTranslationsWithKeyDto(key = "ui-key", translations = mapOf("de" to "Jetzt FAKE_AI_REVIEW_CRITICAL")),
        ).andIsOk
      }
    }

    setText("ui-key", "de", "Jetzt bezahlen")

    waitForReview(translationId) {
      it!!.textHash.assert.isEqualTo(aiReviewTextHash("Jetzt FAKE_AI_REVIEW_CRITICAL"))
      it.outcome.assert.isEqualTo(AiReviewOutcome.BELOW_THRESHOLD)
    }
    waitForProjectJobsCompleted()
    findReview(translationId)!!.score.assert.isEqualTo(25)
    getTranslation(translationId).state.assert.isEqualTo(TranslationState.TRANSLATED)
  }

  @Test
  fun `drops the result when a human changes the state during the LLM call`() {
    testData.addRule()
    saveTestData()
    val translationId = testData.uiGerman.self.id
    onFirstLlmCall {
      inOtherThread {
        performAuthPut("/v2/projects/$projectId/translations/$translationId/set-state/REVIEWED", null).andIsOk
      }
    }

    setText("ui-key", "de", "Jetzt bezahlen")

    waitForProjectJobsCompleted()
    findReview(translationId).assert.isNull()
    getTranslation(translationId).state.assert.isEqualTo(TranslationState.REVIEWED)
  }

  @Test
  fun `a second job does not call the LLM while the first one is running`() {
    testData.addRule()
    saveTestData()
    val translationId = testData.uiGerman.self.id
    val item = BatchTranslationTargetItem(testData.uiKey.self.id, testData.german.self.id)
    onFirstLlmCall {
      executeInNewTransaction(platformTransactionManager) {
        aiReviewBatchService.reviewItem(projectId, item)
      }
    }

    setText("ui-key", "de", "Jetzt bezahlen")

    waitForReview(translationId) {
      it!!.outcome.assert.isEqualTo(AiReviewOutcome.AUTO_REVIEWED)
    }
    waitForProjectJobsCompleted()
    verify(llmProviderService, times(1)).callProvider(any(), any(), any(), anyOrNull())
  }
}
