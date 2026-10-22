package io.tolgee.ee.development

import io.tolgee.development.testDataBuilder.FT
import io.tolgee.development.testDataBuilder.builders.AiReviewRuleBuilder
import io.tolgee.development.testDataBuilder.builders.TranslationAiReviewBuilder
import io.tolgee.development.testDataBuilder.builders.TranslationBuilder
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.model.Language
import io.tolgee.model.Prompt
import io.tolgee.model.aiReview.AiReviewRule
import io.tolgee.model.aiReview.TranslationAiReview
import io.tolgee.model.enums.aiReview.AiReviewRuleMode
import io.tolgee.model.enums.aiReview.AiReviewStatus
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation

class AiReviewTestData : BaseTestData() {
  lateinit var german: Language
  lateinit var french: Language
  lateinit var key: Key
  lateinit var deTranslationBuilder: TranslationBuilder
  lateinit var frTranslationBuilder: TranslationBuilder
  lateinit var ruleBuilder: AiReviewRuleBuilder
  lateinit var translatePrompt: Prompt

  val deTranslation: Translation get() = deTranslationBuilder.self
  val frTranslation: Translation get() = frTranslationBuilder.self
  val rule: AiReviewRule get() = ruleBuilder.self

  init {
    project.useQaChecks = false
    projectBuilder.build {
      german = addGerman().self
      french = addFrench().self
      key =
        addKey { name = "checkout.pay" }
          .build {
            addTag("ui")
            addTranslation("en", "Pay now")
            deTranslationBuilder = addTranslation("de", "Jetzt bezahlen")
            frTranslationBuilder = addTranslation("fr", "Payer maintenant")
          }.self
      translatePrompt =
        addPrompt {
          name = "Translate"
          providerName = "default"
        }.self
      ruleBuilder =
        addAiReviewRule {
          name = "UI"
          mode = AiReviewRuleMode.ACTIVE
          threshold = 85
          tagIn = mutableListOf("ui")
        }
    }
  }

  fun addRule(ft: FT<AiReviewRule>): AiReviewRuleBuilder = projectBuilder.addAiReviewRule(ft)

  fun addReview(
    translationBuilder: TranslationBuilder,
    status: AiReviewStatus,
    score: Int?,
    ft: FT<TranslationAiReview> = {},
  ): TranslationAiReviewBuilder =
    translationBuilder.addAiReview {
      this.status = status
      this.score = score
      ft(this)
    }
}
