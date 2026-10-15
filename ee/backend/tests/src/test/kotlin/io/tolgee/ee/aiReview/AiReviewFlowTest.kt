package io.tolgee.ee.aiReview

import io.tolgee.activity.data.ActivityType
import io.tolgee.constants.Feature
import io.tolgee.dtos.LlmParams
import io.tolgee.dtos.LlmResponseSchema
import io.tolgee.dtos.request.key.ComplexEditKeyDto
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.enums.AssignableTranslationState
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.aiReview.AiReviewOutcome
import io.tolgee.model.enums.aiReview.AiReviewRuleMode
import io.tolgee.repository.activity.ActivityRevisionRepository
import io.tolgee.testing.assert
import io.tolgee.util.aiReviewTextHash
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired

class AiReviewFlowTest : AbstractAiReviewTest() {
  @Autowired
  lateinit var activityRevisionRepository: ActivityRevisionRepository

  @Test
  fun `auto-reviews a passing translation as system`() {
    testData.addRule { tagIn = mutableListOf("ui") }
    saveTestData()
    val translationId = testData.uiGerman.self.id

    setText("ui-key", "de", "Jetzt bezahlen")

    waitForReview(translationId) {
      it!!.outcome.assert.isEqualTo(AiReviewOutcome.AUTO_REVIEWED)
    }
    val review = findReview(translationId)!!
    review.score.assert.isEqualTo(100)
    review.threshold.assert.isEqualTo(85)
    review.dryRun.assert.isFalse()
    review.textHash.assert.isEqualTo(aiReviewTextHash("Jetzt bezahlen"))
    review.providerName.assert.isEqualTo("default")
    review.reviewedAt.assert.isNotNull()
    getTranslation(translationId).state.assert.isEqualTo(TranslationState.REVIEWED)

    waitForProjectJobsCompleted()
    executeInNewTransaction(platformTransactionManager) {
      val aiRevisions =
        activityRevisionRepository.findAll().filter {
          it.type == ActivityType.AI_REVIEW &&
            it.projectId == projectId
        }
      aiRevisions.assert.hasSize(1)
      aiRevisions
        .single()
        .authorId.assert
        .isNull()
    }

    performAuthGet("/v2/projects/$projectId/translations/$translationId/history")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.revisions[0].activityType").isEqualTo("AI_REVIEW")
        node("_embedded.revisions[0].author").isNull()
        node("_embedded.revisions[0].modifications.state.new").isEqualTo("REVIEWED")
        node("_embedded.revisions[1].activityType").isEqualTo("SET_TRANSLATIONS")
        node("_embedded.revisions[1].author.id").isEqualTo(testData.user.id)
      }
  }

  @Test
  fun `dry run stores the result without changing the state`() {
    testData.addRule { mode = AiReviewRuleMode.DRY_RUN }
    saveTestData()
    val translationId = testData.uiGerman.self.id

    setText("ui-key", "de", "Jetzt bezahlen")

    waitForReview(translationId) {
      it!!.outcome.assert.isEqualTo(AiReviewOutcome.WOULD_AUTO_REVIEW)
    }
    findReview(translationId)!!.dryRun.assert.isTrue()
    getTranslation(translationId).state.assert.isEqualTo(TranslationState.TRANSLATED)
  }

  @Test
  fun `keeps translation below threshold as translated`() {
    testData.addRule { threshold = 80 }
    saveTestData()
    val translationId = testData.uiGerman.self.id

    setText("ui-key", "de", "Jetzt FAKE_AI_REVIEW_MAJOR bezahlen FAKE_AI_REVIEW_MINOR")

    waitForReview(translationId) {
      it!!.outcome.assert.isEqualTo(AiReviewOutcome.BELOW_THRESHOLD)
    }
    val review = findReview(translationId)!!
    review.score.assert.isEqualTo(70)
    review.issues!!
      .map { it.points }
      .assert
      .containsExactlyInAnyOrder(25, 5)
    getTranslation(translationId).state.assert.isEqualTo(TranslationState.TRANSLATED)
  }

  @Test
  fun `stores failure for unparseable output and leaves the state`() {
    testData.addRule()
    saveTestData()
    val translationId = testData.uiGerman.self.id

    setText("ui-key", "de", "Jetzt FAKE_AI_REVIEW_INVALID")

    waitForReview(translationId) {
      it!!.outcome.assert.isEqualTo(AiReviewOutcome.FAILED)
    }
    val review = findReview(translationId)!!
    review.errorCode.assert.isEqualTo("ai_review_invalid_output")
    review.errorDetail.assert.isEqualTo("not a json")
    review.score.assert.isNull()
    getTranslation(translationId).state.assert.isEqualTo(TranslationState.TRANSLATED)
  }

  @Test
  fun `never touches a translation a human reviewed in the same edit`() {
    testData.addRule()
    saveTestData()
    val translationId = testData.uiGerman.self.id

    performAuthPut(
      "/v2/projects/$projectId/keys/${testData.uiKey.self.id}/complex-update",
      ComplexEditKeyDto(
        name = "ui-key",
        translations = mapOf("de" to "Jetzt bezahlen"),
        states = mapOf("de" to AssignableTranslationState.REVIEWED),
      ),
    ).andIsOk

    waitForProjectJobsCompleted()
    findReview(translationId).assert.isNull()
    aiReviewJobs().assert.isEmpty()
    getTranslation(translationId).state.assert.isEqualTo(TranslationState.REVIEWED)
  }

  @Test
  fun `ignores base language edits`() {
    testData.addRule()
    saveTestData()

    setText("ui-key", "en", "Pay right now")

    waitForProjectJobsCompleted()
    aiReviewJobs().assert.isEmpty()
    executeInNewTransaction(platformTransactionManager) {
      translationAiReviewRepository.findAll().assert.isEmpty()
    }
  }

  @Test
  fun `does nothing for keys no rule matches`() {
    testData.addRule { tagIn = mutableListOf("legal") }
    saveTestData()

    setText("ui-key", "de", "Jetzt bezahlen")

    waitForProjectJobsCompleted()
    aiReviewJobs().assert.isEmpty()
    findReview(testData.uiGerman.self.id).assert.isNull()
  }

  @Test
  fun `never rule blocks the review`() {
    testData.addRule { threshold = 50 }
    testData.addRule { tagIn = mutableListOf("ui") }.neverForLanguage(testData.german.self)
    saveTestData()

    setText("ui-key", "de", "Jetzt bezahlen")

    waitForProjectJobsCompleted()
    aiReviewJobs().assert.isEmpty()
    findReview(testData.uiGerman.self.id).assert.isNull()
  }

  @Test
  fun `consumes MT credits`() {
    machineTranslationProperties.freeCreditsAmount = 1_000_000
    testData.addRule()
    saveTestData()
    val organizationId = testData.project.organizationOwner.id
    val initialBalance = mtCreditBucketService.getCreditBalances(organizationId).creditBalance

    setText("ui-key", "de", "Jetzt bezahlen")

    waitForReview(testData.uiGerman.self.id) {
      it!!.outcome.assert.isEqualTo(AiReviewOutcome.AUTO_REVIEWED)
    }
    waitForNotThrowing(pollTime = 100, timeout = 10_000) {
      mtCreditBucketService
        .getCreditBalances(organizationId)
        .creditBalance.assert
        .isLessThan(initialBalance)
    }
  }

  @Test
  fun `does nothing when the feature flag is off`() {
    testData.addRule()
    saveTestData()
    tolgeeProperties.featureFlags.enabledForAll = emptyList()

    setText("ui-key", "de", "Jetzt bezahlen")

    waitForProjectJobsCompleted()
    aiReviewJobs().assert.isEmpty()
    findReview(testData.uiGerman.self.id).assert.isNull()
  }

  @Test
  fun `does nothing when the plan lacks the feature`() {
    testData.addRule()
    saveTestData()
    enabledFeaturesProvider.forceEnabled = setOf(Feature.AI_PROMPT_CUSTOMIZATION)

    setText("ui-key", "de", "Jetzt bezahlen")

    waitForProjectJobsCompleted()
    aiReviewJobs().assert.isEmpty()
    findReview(testData.uiGerman.self.id).assert.isNull()
  }

  @Test
  fun `reviews with the custom review prompt of the rule`() {
    testData.addRule { prompt = testData.reviewPrompt.self }
    saveTestData()
    val translationId = testData.uiGerman.self.id

    setText("ui-key", "de", "Jetzt bezahlen")

    waitForReview(translationId) {
      it!!.outcome.assert.isEqualTo(AiReviewOutcome.AUTO_REVIEWED)
    }
    val params = argumentCaptor<LlmParams>()
    verify(llmProviderService).callProvider(any(), any(), params.capture(), anyOrNull())
    params.firstValue.responseSchema.assert
      .isEqualTo(LlmResponseSchema.AI_REVIEW)
    params.firstValue.messages
      .mapNotNull { it.text }
      .joinToString("\n")
      .assert
      .contains("Be strict about tone.")
    executeInNewTransaction(platformTransactionManager) {
      translationAiReviewRepository
        .findByTranslationId(translationId)!!
        .prompt!!
        .id.assert
        .isEqualTo(testData.reviewPrompt.self.id)
    }
  }
}
