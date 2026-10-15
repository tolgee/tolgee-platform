package io.tolgee.ee.aiReview

import io.tolgee.development.testDataBuilder.builders.AiReviewRuleBuilder
import io.tolgee.dtos.request.key.ComplexEditKeyDto
import io.tolgee.dtos.request.key.TagKeyDto
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.aiReview.AiReviewOutcome
import io.tolgee.model.enums.aiReview.AiReviewRuleMode
import io.tolgee.testing.assert
import io.tolgee.util.aiReviewTextHash
import org.junit.jupiter.api.Test

class AiReviewTagChangeTest : AbstractAiReviewTest() {
  private fun seedReviewedUiGerman(
    rule: AiReviewRuleBuilder,
    outcome: AiReviewOutcome = AiReviewOutcome.AUTO_REVIEWED,
    score: Int = 90,
    threshold: Int = 85,
  ) {
    if (outcome == AiReviewOutcome.AUTO_REVIEWED) {
      testData.uiGerman.self.state = TranslationState.REVIEWED
    }
    testData.uiGerman.addAiReview {
      this.outcome = outcome
      textHash = aiReviewTextHash(testData.uiGerman.self.text)
      this.rule = rule.self
      this.score = score
      this.threshold = threshold
    }
  }

  private fun tagUiKey(tag: String) {
    performAuthPut("/v2/projects/$projectId/keys/${testData.uiKey.self.id}/tags", TagKeyDto(name = tag)).andIsOk
  }

  @Test
  fun `stricter rule after tagging demotes the translation`() {
    val base = testData.addRule { tagIn = mutableListOf("ui") }
    testData.addRule {
      tagIn = mutableListOf("strict")
      threshold = 95
    }
    seedReviewedUiGerman(base)
    saveTestData()
    val translationId = testData.uiGerman.self.id

    tagUiKey("strict")

    waitForReview(translationId) {
      it!!.outcome.assert.isEqualTo(AiReviewOutcome.BELOW_THRESHOLD)
    }
    val review = findReview(translationId)!!
    review.threshold.assert.isEqualTo(95)
    review.score.assert.isEqualTo(90)
    getTranslation(translationId).state.assert.isEqualTo(TranslationState.TRANSLATED)
  }

  @Test
  fun `never rule after tagging demotes the translation`() {
    val base = testData.addRule { tagIn = mutableListOf("ui") }
    testData.addRule { tagIn = mutableListOf("strict") }.neverForLanguage(testData.german.self)
    seedReviewedUiGerman(base)
    saveTestData()
    val translationId = testData.uiGerman.self.id

    tagUiKey("strict")

    waitForReview(translationId) {
      it!!.outcome.assert.isEqualTo(AiReviewOutcome.BELOW_THRESHOLD)
    }
    findReview(translationId)!!.threshold.assert.isNull()
    getTranslation(translationId).state.assert.isEqualTo(TranslationState.TRANSLATED)
  }

  @Test
  fun `less strict rule after tagging keeps the review`() {
    val base = testData.addRule { tagIn = mutableListOf("ui") }
    testData.addRule {
      tagIn = mutableListOf("lenient")
      threshold = 60
    }
    seedReviewedUiGerman(base)
    saveTestData()
    val translationId = testData.uiGerman.self.id

    tagUiKey("lenient")

    waitForProjectJobsCompleted()
    findReview(translationId)!!.outcome.assert.isEqualTo(AiReviewOutcome.AUTO_REVIEWED)
    getTranslation(translationId).state.assert.isEqualTo(TranslationState.REVIEWED)
  }

  @Test
  fun `removing a tag never promotes`() {
    val base = testData.addRule { tagIn = mutableListOf("ui") }
    testData.addRule {
      tagIn = mutableListOf("strict")
      threshold = 95
    }
    val strictTag = testData.uiKey.addTag("strict")
    seedReviewedUiGerman(base, outcome = AiReviewOutcome.BELOW_THRESHOLD, threshold = 95)
    saveTestData()
    val translationId = testData.uiGerman.self.id

    performAuthDelete("/v2/projects/$projectId/keys/${testData.uiKey.self.id}/tags/${strictTag.id}").andIsOk

    waitForProjectJobsCompleted()
    findReview(translationId)!!.outcome.assert.isEqualTo(AiReviewOutcome.BELOW_THRESHOLD)
    getTranslation(translationId).state.assert.isEqualTo(TranslationState.TRANSLATED)
    aiReviewJobs().assert.isEmpty()
  }

  @Test
  fun `moving the key to a namespace of a stricter rule demotes the translation`() {
    testData.project.useNamespaces = true
    val base = testData.addRule { tagIn = mutableListOf("ui") }
    testData.addRule {
      namespaceIn = mutableListOf("strict-ns")
      threshold = 95
    }
    seedReviewedUiGerman(base)
    saveTestData()
    val translationId = testData.uiGerman.self.id

    performAuthPut(
      "/v2/projects/$projectId/keys/${testData.uiKey.self.id}/complex-update",
      ComplexEditKeyDto(name = "ui-key", namespace = "strict-ns"),
    ).andIsOk

    waitForReview(translationId) {
      it!!.outcome.assert.isEqualTo(AiReviewOutcome.BELOW_THRESHOLD)
    }
    findReview(translationId)!!.threshold.assert.isEqualTo(95)
    getTranslation(translationId).state.assert.isEqualTo(TranslationState.TRANSLATED)
  }

  @Test
  fun `stricter rule demotes a dry run result without touching the state`() {
    val base = testData.addRule { tagIn = mutableListOf("ui") }
    testData.addRule {
      tagIn = mutableListOf("strict")
      threshold = 95
    }
    seedReviewedUiGerman(base, outcome = AiReviewOutcome.WOULD_AUTO_REVIEW)
    saveTestData()
    val translationId = testData.uiGerman.self.id

    tagUiKey("strict")

    waitForReview(translationId) {
      it!!.outcome.assert.isEqualTo(AiReviewOutcome.BELOW_THRESHOLD)
    }
    findReview(translationId)!!.threshold.assert.isEqualTo(95)
    getTranslation(translationId).state.assert.isEqualTo(TranslationState.TRANSLATED)
  }

  @Test
  fun `stricter dry run rule after tagging keeps the active review`() {
    val base = testData.addRule { tagIn = mutableListOf("ui") }
    testData.addRule {
      tagIn = mutableListOf("strict")
      threshold = 95
      mode = AiReviewRuleMode.DRY_RUN
    }
    seedReviewedUiGerman(base)
    saveTestData()
    val translationId = testData.uiGerman.self.id

    tagUiKey("strict")

    waitForProjectJobsCompleted()
    findReview(translationId)!!.outcome.assert.isEqualTo(AiReviewOutcome.AUTO_REVIEWED)
    getTranslation(translationId).state.assert.isEqualTo(TranslationState.REVIEWED)
  }

  @Test
  fun `dry run never rule after tagging keeps the active review`() {
    val base = testData.addRule { tagIn = mutableListOf("ui") }
    testData
      .addRule {
        tagIn = mutableListOf("strict")
        mode = AiReviewRuleMode.DRY_RUN
      }.neverForLanguage(testData.german.self)
    seedReviewedUiGerman(base)
    saveTestData()
    val translationId = testData.uiGerman.self.id

    tagUiKey("strict")

    waitForProjectJobsCompleted()
    findReview(translationId)!!.outcome.assert.isEqualTo(AiReviewOutcome.AUTO_REVIEWED)
    getTranslation(translationId).state.assert.isEqualTo(TranslationState.REVIEWED)
  }
}
