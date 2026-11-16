package io.tolgee.ee.development

import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.aiReview.AiReviewFailureReason
import io.tolgee.model.enums.aiReview.AiReviewStatus

class AiReviewTranslationsViewTestData : AiReviewTestData() {
  init {
    keyWithRow("k.approved", AiReviewStatus.APPROVED, 90)
    keyWithRow("k.below", AiReviewStatus.BELOW_THRESHOLD, 40)
    keyWithRow("k.drypass", AiReviewStatus.SCORE_ONLY_PASS, 90)
    keyWithRow("k.dryfail", AiReviewStatus.SCORE_ONLY_FAIL, 30)
    keyWithRow("k.failed", AiReviewStatus.FAILED, null)
    keyWithRow("k.pending", AiReviewStatus.PENDING, null)
    keyWithRow("k.skipped", AiReviewStatus.SKIPPED_QA_ISSUES, null)
    keyWithRow("k.overridden", AiReviewStatus.OVERRIDDEN, 92)
    addKeyWithTranslations("k.none")
    addKeyWithTranslations("k.untagged", tags = emptyList())
    addRule {
      name = "Never legal"
      never = true
      tagIn = mutableListOf("legal")
    }
    addKeyWithTranslations("k.never", tags = listOf("ui", "legal"))
    addKeyWithTranslations("k.outdated").de.self.outdated = true
    addKeyWithTranslations("k.reviewed").de.self.state = TranslationState.REVIEWED
  }

  private fun keyWithRow(
    keyName: String,
    status: AiReviewStatus,
    score: Int?,
  ) {
    val key = addKeyWithTranslations(keyName)
    if (status == AiReviewStatus.APPROVED) {
      key.de.self.state = TranslationState.REVIEWED
      key.de.self.auto = true
    }
    addReview(key.de, status, score) {
      rule = this@AiReviewTranslationsViewTestData.rule
      if (status == AiReviewStatus.FAILED) failureReason = AiReviewFailureReason.PROVIDER_ERROR
    }
  }
}
