package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.AiReviewRuleBuilder
import io.tolgee.development.testDataBuilder.builders.KeyBuilder
import io.tolgee.development.testDataBuilder.builders.TranslationBuilder
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.aiReview.AiReviewOutcome
import io.tolgee.model.enums.aiReview.AiReviewRuleMode
import io.tolgee.util.aiReviewTextHash

class AiReviewFilterTestData : AiReviewTestData() {
  val uiRule: AiReviewRuleBuilder
  val legalRule: AiReviewRuleBuilder

  init {
    uiRule =
      addRule {
        name = "UI"
        tagIn = mutableListOf("ui")
      }.neverForLanguage(french.self)
    legalRule =
      addRule {
        name = "Legal"
        mode = AiReviewRuleMode.DRY_RUN
        tagIn = mutableListOf("legal")
      }

    uiGerman.self.state = TranslationState.REVIEWED
    uiGerman.withResult(AiReviewOutcome.AUTO_REVIEWED, uiRule, score = 92)
    uiCzech.withResult(AiReviewOutcome.BELOW_THRESHOLD, uiRule, score = 70)
    uiFrench.self.state = TranslationState.REVIEWED
    uiFrench.withResult(AiReviewOutcome.BELOW_THRESHOLD, uiRule, score = 60)
    legalGerman.withResult(AiReviewOutcome.WOULD_AUTO_REVIEW, legalRule, score = 95, dryRun = true)
    checkoutGerman.withResult(AiReviewOutcome.BELOW_THRESHOLD, legalRule, score = 50, dryRun = true)
    plainGerman.withResult(AiReviewOutcome.FAILED, uiRule, score = null)

    addUiKey("ui-stale", "de", "Neuer Text") {
      withResult(AiReviewOutcome.BELOW_THRESHOLD, uiRule, score = 40, textHash = aiReviewTextHash("Alter Text"))
    }
    addUiKey("ui-new", "de", "Neu") {}
    addUiKey("ui-pending", "de", "Wartet") { withResult(AiReviewOutcome.PENDING, uiRule, score = null) }
    addUiKey("ui-never", "fr", "Jamais") {}
  }

  private fun addUiKey(
    name: String,
    languageTag: String,
    text: String,
    ft: TranslationBuilder.() -> Unit,
  ): KeyBuilder =
    projectBuilder.addKey { this.name = name }.build {
      addTag("ui")
      addTranslation("en", name)
      addTranslation(languageTag, text).apply(ft)
    }

  private fun TranslationBuilder.withResult(
    outcome: AiReviewOutcome,
    rule: AiReviewRuleBuilder,
    score: Int?,
    dryRun: Boolean = false,
    textHash: String = aiReviewTextHash(self.text),
  ) {
    addAiReview {
      this.outcome = outcome
      this.rule = rule.self
      this.textHash = textHash
      this.score = score
      this.dryRun = dryRun
      threshold = 85
    }
  }
}
