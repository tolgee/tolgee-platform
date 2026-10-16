package io.tolgee.ee.aiReview

import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.data.BatchTranslationTargetItem
import io.tolgee.batch.request.AiReviewRequest
import io.tolgee.development.testDataBuilder.builders.AiReviewRuleBuilder
import io.tolgee.development.testDataBuilder.builders.TranslationBuilder
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.aiReview.AiReviewIssue
import io.tolgee.model.enums.TranslationProtection
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.aiReview.AiReviewIssueSeverity
import io.tolgee.model.enums.aiReview.AiReviewOutcome
import io.tolgee.testing.assert
import io.tolgee.util.aiReviewTextHash
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.Test
import java.util.Date

class AiReviewInvalidationTest : AbstractAiReviewTest() {
  private fun seedAutoReviewed(
    translation: TranslationBuilder,
    rule: AiReviewRuleBuilder,
  ) {
    translation.self.state = TranslationState.REVIEWED
    translation.addAiReview {
      outcome = AiReviewOutcome.AUTO_REVIEWED
      textHash = aiReviewTextHash(translation.self.text)
      this.rule = rule.self
      score = 95
      threshold = 85
      issues = mutableListOf(AiReviewIssue(AiReviewIssueSeverity.MINOR, "style", "Nit", 5))
    }
  }

  @Test
  fun `text change replaces the stored result`() {
    val rule = testData.addRule()
    seedAutoReviewed(testData.uiGerman, rule)
    saveTestData()
    val translationId = testData.uiGerman.self.id

    setText("ui-key", "de", "Jetzt bezahlen")

    waitForReview(translationId) {
      it!!.textHash.assert.isEqualTo(aiReviewTextHash("Jetzt bezahlen"))
      it.outcome.assert.isEqualTo(AiReviewOutcome.AUTO_REVIEWED)
    }
    val review = findReview(translationId)!!
    review.score.assert.isEqualTo(100)
    review.issues.assert.isEmpty()
  }

  @Test
  fun `text change clears the result even when AI review is off`() {
    val rule = testData.addRule()
    seedAutoReviewed(testData.uiGerman, rule)
    saveTestData()
    tolgeeProperties.featureFlags.enabledForAll = emptyList()

    setText("ui-key", "de", "Jetzt bezahlen")

    findReview(testData.uiGerman.self.id).assert.isNull()
  }

  @Test
  fun `drops a pending row whose text changed before the job ran`() {
    testData.addRule()
    testData.uiGerman.addAiReview {
      outcome = AiReviewOutcome.PENDING
      textHash = aiReviewTextHash("Old text")
    }
    saveTestData()
    val translationId = testData.uiGerman.self.id

    executeInNewTransaction(platformTransactionManager) {
      batchJobService.startJob(
        request =
          AiReviewRequest(
            target = listOf(BatchTranslationTargetItem(testData.uiKey.self.id, testData.german.self.id)),
          ),
        project = projectService.get(projectId),
        author = null,
        type = BatchJobType.AI_REVIEW,
        isHidden = true,
      )
    }

    waitForProjectJobsCompleted()
    findReview(translationId).assert.isNull()
    getTranslation(translationId).state.assert.isEqualTo(TranslationState.TRANSLATED)
  }

  @Test
  fun `manual state change of an auto-reviewed translation records the rejection`() {
    val rule = testData.addRule()
    seedAutoReviewed(testData.uiGerman, rule)
    saveTestData()
    val translationId = testData.uiGerman.self.id

    performAuthPut("/v2/projects/$projectId/translations/$translationId/set-state/TRANSLATED", null).andIsOk

    val review = findReview(translationId)!!
    review.outcome.assert.isEqualTo(AiReviewOutcome.REJECTED)
    review.resolvedBy!!
      .id.assert
      .isEqualTo(testData.user.id)
    review.resolvedAt.assert.isNotNull()
    getTranslation(translationId).state.assert.isEqualTo(TranslationState.TRANSLATED)
  }

  @Test
  fun `below threshold result is hidden once a human reviews the translation`() {
    val rule = testData.addRule()
    testData.uiCzech.addAiReview {
      outcome = AiReviewOutcome.BELOW_THRESHOLD
      textHash = aiReviewTextHash(testData.uiCzech.self.text)
      this.rule = rule.self
      score = 50
      threshold = 85
    }
    saveTestData()
    val translationId = testData.uiCzech.self.id

    performAuthPut("/v2/projects/$projectId/translations/$translationId/set-state/REVIEWED", null).andIsOk

    performAuthGet("/v2/projects/$projectId/translations?languages=cs").andIsOk.andAssertThatJson {
      node("_embedded.keys[0].keyName").isEqualTo("ui-key")
      node("_embedded.keys[0].translations.cs.aiReview").isNull()
    }
    performAuthGet("/v2/projects/$projectId/ai-review/translations/$translationId").andIsNotFound
    findReview(translationId)!!.outcome.assert.isEqualTo(AiReviewOutcome.BELOW_THRESHOLD)
  }

  @Test
  fun `source text change re-reviews the siblings it set back to translated`() {
    val rule = testData.addRule()
    seedAutoReviewed(testData.uiGerman, rule)
    testData.uiCzech.self.text = "Zaplatit FAKE_AI_REVIEW_MAJOR"
    testData.uiCzech.addAiReview {
      outcome = AiReviewOutcome.BELOW_THRESHOLD
      textHash = aiReviewTextHash("Zaplatit FAKE_AI_REVIEW_MAJOR")
      this.rule = rule.self
      score = 50
      threshold = 85
    }
    saveTestData()
    val czechId = testData.uiCzech.self.id
    val germanId = testData.uiGerman.self.id

    setText("ui-key", "en", "Pay right now")

    waitForReview(czechId) {
      it!!.outcome.assert.isEqualTo(AiReviewOutcome.BELOW_THRESHOLD)
      it.score.assert.isEqualTo(75)
    }
    waitForReview(germanId) {
      it!!.outcome.assert.isEqualTo(AiReviewOutcome.AUTO_REVIEWED)
      it.score.assert.isEqualTo(100)
    }
    getTranslation(germanId).state.assert.isEqualTo(TranslationState.REVIEWED)
    findReview(testData.uiFrench.self.id).assert.isNull()
  }

  @Test
  fun `source text change keeps auto-reviewed results when reviewed translations are protected`() {
    testData.project.translationProtection = TranslationProtection.PROTECT_REVIEWED
    val rule = testData.addRule()
    seedAutoReviewed(testData.uiGerman, rule)
    saveTestData()
    val germanId = testData.uiGerman.self.id

    setText("ui-key", "en", "Pay right now")

    waitForProjectJobsCompleted()
    val review = findReview(germanId)!!
    review.outcome.assert.isEqualTo(AiReviewOutcome.AUTO_REVIEWED)
    review.score.assert.isEqualTo(95)
    getTranslation(germanId).state.assert.isEqualTo(TranslationState.REVIEWED)
  }

  @Test
  fun `a pending row claimed by another worker does not start an endless chain of stuck-item jobs`() {
    testData.addRule()
    testData.uiCzech.addAiReview {
      outcome = AiReviewOutcome.PENDING
      textHash = aiReviewTextHash(testData.uiCzech.self.text)
      claimedAt = Date()
    }
    saveTestData()

    setText("ui-key", "de", "Jetzt bezahlen")

    waitForReview(testData.uiGerman.self.id) {
      it!!.outcome.assert.isEqualTo(AiReviewOutcome.AUTO_REVIEWED)
    }
    waitForProjectJobsCompleted()
    Thread.sleep(1_000)
    waitForProjectJobsCompleted()
    aiReviewJobs().assert.hasSizeLessThanOrEqualTo(2)
    findReview(testData.uiCzech.self.id)!!.outcome.assert.isEqualTo(AiReviewOutcome.PENDING)
  }

  @Test
  fun `pending rows left behind are reviewed after the next AI review job`() {
    testData.addRule()
    testData.uiCzech.addAiReview {
      outcome = AiReviewOutcome.PENDING
      textHash = aiReviewTextHash(testData.uiCzech.self.text)
    }
    saveTestData()

    setText("ui-key", "de", "Jetzt bezahlen")

    waitForReview(testData.uiCzech.self.id) {
      it!!.outcome.assert.isEqualTo(AiReviewOutcome.AUTO_REVIEWED)
    }
    getTranslation(testData.uiCzech.self.id).state.assert.isEqualTo(TranslationState.REVIEWED)
  }
}
